#!/usr/bin/env python3
"""Disposable PostgreSQL transport/persistence rehearsal; NOT an EDC workflow test.

Only synthetic data is used. The unique container and volume created by this
invocation are removed on exit. No host port is published.
"""
import os
import hashlib
import secrets
import subprocess
import sys
import tempfile
import time
import uuid

IMAGE = "postgres:18.1-alpine@sha256:aa6eb304ddb6dd26df23d05db4e5cb05af8951cda3e0dc57731b771e0ef4ab29"


def local_docker_env(env=None):
    selected = dict(os.environ if env is None else env)
    for key in ("DOCKER_CONTEXT", "DOCKER_TLS", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH"):
        selected.pop(key, None)
    selected["DOCKER_HOST"] = "unix:///var/run/docker.sock"
    return selected


def run(*args, data=None, env=None):
    if args[0] == "docker":
        env = local_docker_env(env)
    result = subprocess.run(args, input=data, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, env=env, timeout=120)
    if result.returncode:
        raise RuntimeError(f"{args[0]} {args[1]} failed: "
                           + result.stderr.decode("utf-8", errors="replace"))
    return result.stdout


def main():
    name = "clinica-probe-" + uuid.uuid4().hex[:12]
    volume = name + "-data"
    created_container = False
    created_volume = False
    env = dict(os.environ, POSTGRES_PASSWORD=secrets.token_urlsafe(32))

    def sql(statement, database="synthetic"):
        return run("docker", "exec", "-i", name, "psql", "-X", "-v",
                   "ON_ERROR_STOP=1", "-U", "probe", "-d", database,
                   "-At", data=statement.encode("utf-8"))

    def ready():
        deadline = time.monotonic() + 60
        while time.monotonic() < deadline:
            result = subprocess.run(
                ["docker", "exec", name, "pg_isready", "-h", "127.0.0.1", "-U", "probe",
                 "-d", "synthetic"], stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL, timeout=10, env=local_docker_env())
            if result.returncode == 0:
                return
            time.sleep(1)
        raise RuntimeError("PostgreSQL did not become ready in 60 seconds")

    try:
        run("docker", "volume", "create", volume)
        created_volume = True
        run("docker", "create", "--name", name,
            "--mount", f"type=volume,src={volume},dst=/var/lib/postgresql",
            "-e", "POSTGRES_PASSWORD", "-e", "POSTGRES_USER=probe",
            "-e", "POSTGRES_DB=synthetic", IMAGE, env=env)
        created_container = True
        run("docker", "start", name)
        ready()
        sql("""
CREATE TABLE transport_probe (
    id integer PRIMARY KEY, label text NOT NULL, narrative text NOT NULL,
    amount numeric(12,4) NOT NULL, visit_date date NOT NULL,
    recorded_at timestamptz NOT NULL
);
INSERT INTO transport_probe VALUES (
    1, '合成受试者：中文往返', repeat('合成中文长文本；', 1000),
    1234.5678, DATE '2026-11-10', TIMESTAMPTZ '2026-11-10 16:00:00+08'
);
""")
        query = """
SET TIME ZONE 'UTC';
SELECT label, length(narrative), md5(narrative), amount, visit_date,
       recorded_at FROM transport_probe ORDER BY id;
"""
        narrative = "合成中文长文本；" * 1000
        expected = ("SET\n合成受试者：中文往返|" + str(len(narrative)) + "|"
                    + hashlib.md5(narrative.encode("utf-8")).hexdigest()
                    + "|1234.5678|2026-11-10|2026-11-10 08:00:00+00\n").encode("utf-8")
        if sql(query) != expected:
            raise RuntimeError("Chinese, decimal, date or timezone round-trip mismatch")
        run("docker", "restart", name)
        ready()
        if sql(query) != expected:
            raise RuntimeError("Persistence mismatch after restart")
        # A real pg_dump archive is restored into a separate empty database.
        # TemporaryDirectory is private and never placed in public artifacts.
        archive = run("docker", "exec", name, "pg_dump", "-U", "probe",
                      "-d", "synthetic", "-Fc", "--no-owner", "--no-acl")
        with tempfile.TemporaryDirectory(prefix="clinica-synthetic-") as temp:
            path = os.path.join(temp, "synthetic.dump")
            with open(path, "xb") as backup:
                backup.write(archive)
            sql("CREATE DATABASE restored", database="postgres")
            with open(path, "rb") as backup:
                run("docker", "exec", "-i", name, "pg_restore", "-U", "probe",
                    "-d", "restored", "--exit-on-error", "--no-owner",
                    "--no-acl", data=backup.read())
        if sql(query, database="restored") != expected:
            raise RuntimeError("Restore mismatch")
        print("PASS: UTF-8 Chinese, long text, decimal, date, timezone, restart persistence and pg_dump/pg_restore")
        print("Scope: synthetic PostgreSQL probe only; EDC schemas, files and workflows are not covered.")
    finally:
        # Never remove pre-existing resources or other instances.
        primary_failure = sys.exc_info()[0] is not None
        cleanup_errors = []
        for created, command in [
            (created_container, ("docker", "rm", "-f", name)),
            (created_volume, ("docker", "volume", "rm", volume)),
        ]:
            if created:
                try:
                    run(*command)
                except (RuntimeError, subprocess.TimeoutExpired) as error:
                    cleanup_errors.append(str(error))
        if cleanup_errors:
            message = "Cleanup failed: " + "; ".join(cleanup_errors)
            if primary_failure:
                print(message, file=sys.stderr)
            else:
                raise RuntimeError(message)


if __name__ == "__main__":
    main()
