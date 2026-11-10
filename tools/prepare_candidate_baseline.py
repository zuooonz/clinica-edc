#!/usr/bin/env python3
"""Prepare an isolated source-built candidate; never start or stop Docker services.

Copies the built WAR into private ignored work/candidate, records artifact and
checkout identities separately, and refuses to replace any existing setup.
A package artifact alone does not prove that its build used strict checksums.
"""
import hashlib
from io import BytesIO
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
from zipfile import ZipFile

from check_database_baseline import IMAGE as POSTGRES
from prepare_original_baseline import TOMCAT
from verify_pdfbox_layout import LICENSE_BLOB, git_hash

ROOT = Path(__file__).resolve().parents[1]
LAYOUT = "WEB-INF/lib/pdfbox2-layout-1.0.1-clinica.1.jar"


def property_value(data, key):
    matches = re.findall(r"^" + re.escape(key) + r"\s*[=:]\s*(.*?)\s*$",
                         data.decode("utf-8"), re.MULTILINE)
    if len(matches) != 1 or not matches[0] or "${" in matches[0]:
        raise ValueError("Missing, duplicate or unfiltered artifact property: " + key)
    return matches[0]


def artifact_info(archive):
    with ZipFile(archive) as war:
        core = [name for name in war.namelist()
                if re.fullmatch(r"WEB-INF/lib/LibreClinica-core-[^/]+\.jar", name)]
        layouts = [name for name in war.namelist()
                   if name.startswith("WEB-INF/lib/") and "layout" in name.lower()
                   and name.endswith(".jar")]
        if len(core) != 1 or layouts != [LAYOUT]:
            raise ValueError("Expected one core jar and only the pinned vendored PDFBox Layout jar")
        with ZipFile(BytesIO(war.read(core[0]))) as jar:
            prefix = "org/akaza/openclinica/i18n/"
            version = property_value(jar.read(prefix + "words.properties"), "version_number")
            release = property_value(jar.read(prefix + "licensing.properties"), "Version_release")
        match = re.fullmatch(r"Version: (\S+) - Changeset: ([0-9a-f]{12,40}) (.+)", release)
        if not match or match[1] != version:
            raise ValueError("Artifact release identity is missing or inconsistent")
        with ZipFile(BytesIO(war.read(LAYOUT))) as jar:
            license_text = jar.read("META-INF/pdfbox-layout/LICENSE")
            provenance = json.loads(jar.read("META-INF/pdfbox-layout/provenance.json"))
            properties = jar.read("META-INF/maven/org.clinica.vendor/pdfbox2-layout/pom.properties")
            if (git_hash("blob", license_text) != LICENSE_BLOB
                    or provenance.get("commit") != "6902dd0568061afff4da1d05a4cbd5a7f2b9dc8f"
                    or property_value(properties, "groupId") != "org.clinica.vendor"
                    or property_value(properties, "artifactId") != "pdfbox2-layout"
                    or property_value(properties, "version") != "1.0.1-clinica.1"):
                raise ValueError("Vendored PDFBox Layout license, provenance or coordinates mismatch")
        return {"version": version, "artifact_changeset": match[2],
                "artifact_release": release, "layout": LAYOUT,
                "layout_upstream_commit": provenance["commit"]}


def main():
    os.umask(0o077)
    source = ROOT / "web/target/LibreClinica-web.war"
    info = artifact_info(source)
    git_sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT))
    target = ROOT / "work/candidate"
    target.mkdir(parents=True, exist_ok=False)
    archive = target / "LibreClinica.war"
    # Write using umask instead of copy2, which could copy permissive source modes.
    with source.open("rb") as incoming, archive.open("xb") as outgoing:
        shutil.copyfileobj(incoming, outgoing)
    # Validate the exact copy, so a concurrently replaced build cannot be mislabeled.
    if artifact_info(archive) != info:
        raise ValueError("WAR identity changed during preparation; inspect private candidate directory")
    info.update({"war_sha256": hashlib.sha256(archive.read_bytes()).hexdigest(),
                 "checkout_git_sha": git_sha, "checkout_dirty": dirty,
                 "identity_note": "Artifact release comes from WAR; checkout identity is observed at preparation, not proof of artifact source.",
                 "tomcat_image": TOMCAT, "postgres_image": POSTGRES})
    (target / "build-info.json").write_text(json.dumps(info, indent=2) + "\n")
    password = secrets.token_urlsafe(32)
    config_dir = target / "config"
    config_dir.mkdir()
    replacements = {"dbPass": password, "mailHost": "127.0.0.1", "mailPort": "9",
                    "sysURL": "http://localhost:18082/LibreClinica/MainMenu"}
    config = (ROOT / "docker/config/datainfo.properties").read_text()
    config = "\n".join(key + "=" + replacements[key] if key in replacements else line
                       for line in config.splitlines() for key in [line.split("=", 1)[0]]) + "\n"
    (config_dir / "datainfo.properties").write_text(config)
    project = "clinica-candidate-" + secrets.token_hex(6)
    compose = {
        "name": project,
        "services": {
            "db": {"image": POSTGRES,
                   "environment": {"POSTGRES_USER": "clinica", "POSTGRES_DB": "libreclinica",
                                   "POSTGRES_PASSWORD": password},
                   "volumes": ["db:/var/lib/postgresql"],
                   "healthcheck": {"test": ["CMD", "pg_isready", "-h", "127.0.0.1",
                                             "-U", "clinica", "-d", "libreclinica"],
                                   "interval": "2s", "timeout": "5s", "retries": 30}},
            "app": {"image": TOMCAT, "ports": ["127.0.0.1:18082:8080"],
                    "depends_on": {"db": {"condition": "service_healthy"}},
                    "volumes": [f"{archive}:/usr/local/tomcat/webapps/LibreClinica.war:ro",
                                f"{config_dir}:/usr/local/tomcat/libreclinica.config:ro",
                                "data:/usr/local/tomcat/libreclinica.data",
                                "logs:/usr/local/tomcat/logs"]}},
        "volumes": {"db": {}, "data": {}, "logs": {}},
    }
    (target / "compose.json").write_text(json.dumps(compose, indent=2) + "\n")
    print("Prepared private candidate WAR, build identity and isolated configuration.")
    print("Start: docker compose -p " + project + " -f work/candidate/compose.json up -d")
    print("Browser: CLINICA_INSTANCE=candidate node tools/browser-baseline/smoke.cjs")
    print("URL: http://localhost:18082/LibreClinica/")
    print("Scope: preparation only; no Docker startup, installation or clinical workflow acceptance.")


if __name__ == "__main__":
    main()
