#!/usr/bin/env python3
"""Stop-write backup/restore rehearsal of the generated synthetic original instance.

Requires an already-running tools/prepare_original_baseline.py instance and its
browser smoke credentials. Uses a new project/volumes, compares all public table
contents and sequence states plus file hashes, then runs the browser smoke.
Only this invocation's restore project is removed; backups stay private in work/.
"""
import json
import os
import re
from pathlib import Path
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

from check_database_baseline import run

ROOT = Path(__file__).resolve().parents[1]


def main():
    os.umask(0o077)
    base = ROOT / "work/original"
    source_file = base / "compose.json"
    config = json.loads(source_file.read_text())
    source_project = config.get("name", "")
    if not re.fullmatch(r"clinica-original(?:-[a-f0-9]{8})?", source_project):
        raise SystemExit("Expected a generated original synthetic project name")
    source = ["docker", "compose", "-p", source_project, "-f", str(source_file)]
    # The target must get new project-scoped volumes. Do not inherit manually
    # configured external/named volumes which could alias an existing instance.
    if (config.get("volumes") != {"db": {}, "data": {}, "logs": {}}
            or set(config.get("services", {})) != {"app", "db"}
            or config["services"]["db"].get("volumes") != ["db:/var/lib/postgresql/data"]
            or config["services"]["app"].get("volumes", []) != [
                f"{base / 'LibreClinica.war'}:/usr/local/tomcat/webapps/LibreClinica.war:ro",
                f"{base / 'config'}:/usr/local/tomcat/libreclinica.config:ro",
                "data:/usr/local/tomcat/libreclinica.data", "logs:/usr/local/tomcat/logs"]):
        raise SystemExit("Only the generated project-scoped synthetic volume configuration is supported")
    if not (base / "admin.json").exists():
        raise SystemExit("Run the original browser smoke and initialize its password first")
    services = run(*source, "ps", "--status", "running", "--services").decode().splitlines()
    if set(services) != {"app", "db"}:
        raise SystemExit("Original app and db must already be running; refusing to change stopped services")
    project = "clinica-restore-" + uuid.uuid4().hex[:12]
    backup = base / project
    backup.mkdir()
    config["name"] = project
    config["services"]["app"]["ports"] = ["127.0.0.1:18081:8080"]
    restore_file = backup / "compose.json"
    restore_file.write_text(json.dumps(config, indent=2) + "\n")
    target = ["docker", "compose", "-p", project, "-f", str(restore_file)]

    def sql(compose, statement):
        return run(*compose, "exec", "-T", "db", "psql", "-X", "-At", "-v",
                   "ON_ERROR_STOP=1", "-U", "clinica", "-d", "libreclinica",
                   data=statement.encode())

    def fingerprint(compose):
        tables = sql(compose, "SELECT quote_ident(schemaname)||'.'||quote_ident(tablename) "
                     "FROM pg_tables WHERE schemaname='public' ORDER BY 1").decode().splitlines()
        sequences = sql(compose, "SELECT quote_ident(schemaname)||'.'||quote_ident(sequencename) "
                        "FROM pg_sequences WHERE schemaname='public' ORDER BY 1").decode().splitlines()
        commands = ["SELECT '" + table.replace("'", "''") + "', count(*), "
                    "md5(coalesce(string_agg(row_to_json(t)::text,E'\\n' "
                    "ORDER BY row_to_json(t)::text),'')) FROM " + table + " t;"
                    for table in tables]
        commands += ["SELECT '" + sequence.replace("'", "''") + "',last_value,is_called FROM "
                     + sequence + ";" for sequence in sequences]
        return sql(compose, "\n".join(commands)), len(tables), len(sequences)

    file_hashes = ["run", "--rm", "-T", "--no-deps", "--entrypoint", "sh", "app", "-c",
                   "set -e; cd /usr/local/tomcat/libreclinica.data; "
                   "task_hashes=$(mktemp); trap 'rm -f \"$task_hashes\"' EXIT; "
                   "find . -type f -exec sha256sum {} + >\"$task_hashes\"; "
                   "LC_ALL=C sort \"$task_hashes\""]
    stopped = False
    target_attempted = False
    try:
        run(*source, "stop", "app")
        stopped = True
        before, tables, sequences = fingerprint(source)
        archive = run(*source, "exec", "-T", "db", "pg_dump", "-U", "clinica",
                      "-d", "libreclinica", "-Fc", "--no-owner", "--no-acl")
        (backup / "database.dump").write_bytes(archive)
        files = run(*source, "run", "--rm", "-T", "--no-deps", "--entrypoint", "tar",
                    "app", "-C", "/usr/local/tomcat/libreclinica.data", "-czf", "-", ".")
        (backup / "files.tar.gz").write_bytes(files)
        original_files = run(*source, *file_hashes)
        target_attempted = True
        run(*target, "up", "-d", "--wait", "db")
        run(*target, "exec", "-T", "db", "pg_restore", "-U", "clinica", "-d",
            "libreclinica", "--exit-on-error", "--no-owner", "--no-acl", data=archive)
        run(*target, "run", "--rm", "-T", "--no-deps", "--entrypoint", "tar", "app",
            "-C", "/usr/local/tomcat/libreclinica.data", "-xzf", "-", data=files)
        after, _, _ = fingerprint(target)
        if before != after or original_files != run(*target, *file_hashes):
            raise RuntimeError("Restored table, sequence or application file mismatch")
        (backup / "fingerprints.txt").write_bytes(before)
        run(*target, "up", "-d", "app")
        url = "http://127.0.0.1:18081/LibreClinica/"
        deadline = time.monotonic() + 120
        while True:
            try:
                with urllib.request.urlopen(url, timeout=5) as response:
                    if response.status == 200:
                        break
            except (urllib.error.URLError, TimeoutError, ConnectionError):
                pass
            if time.monotonic() >= deadline:
                raise RuntimeError("Restored app did not become ready in 120 seconds")
            time.sleep(1)
        print(run("node", str(ROOT / "tools/browser-baseline/smoke.cjs"),
                  env=dict(os.environ, CLINICA_BASE_URL=url)).decode().strip())
        print(f"PASS: restored {tables} table fingerprints, {sequences} sequences and application file hashes")
        print("Scope: default synthetic study only; no real or complete clinical workflow data.")
    finally:
        primary_failure = sys.exc_info()[0] is not None
        errors = []
        for enabled, command in [(target_attempted, target + ["down", "-v"]),
                                 (stopped, source + ["start", "app"])]:
            if enabled:
                try:
                    run(*command)
                except (RuntimeError, subprocess.TimeoutExpired) as error:
                    errors.append(str(error))
        if errors:
            message = "Restore cleanup/restart failed: " + "; ".join(errors)
            if primary_failure:
                print(message, file=sys.stderr)
            else:
                raise RuntimeError(message)


if __name__ == "__main__":
    main()
