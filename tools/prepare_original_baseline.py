#!/usr/bin/env python3
"""Prepare a private, loopback-only upstream 1.4.0 comparison instance.

Does not start services, alter an existing instance, or deploy candidate code.
Run from any directory; all generated material stays under ignored work/.
"""
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess

ROOT = Path(__file__).resolve().parents[1]
WAR_URL = "https://libreclinica.org/downloads/LibreClinica-web-1.4.0.war"
WAR_SHA256 = "25378635ab396195d2bc8d58ee2988383fccf0699d2c5222800c8a37524179c7"
TOMCAT = "tomcat:9-jdk11@sha256:cf13b145adf437e0c30ff3238016b09f9e904d2d75a714894ced0c8b7ea61554"
POSTGRES = "postgres:16-alpine@sha256:aa6cb33cf4c19c765cefcf48d96176258800870a9a1e930ad126200f9d825139"


def main():
    os.umask(0o077)
    target = ROOT / "work" / "original"
    # Refuse to regenerate credentials or silently replace an existing setup.
    target.mkdir(parents=True, exist_ok=False)
    archive = target / "LibreClinica.war"
    subprocess.run(["curl", "--fail", "--location", "--silent", "--show-error",
                    "--max-time", "180", WAR_URL, "-o", str(archive)], check=True)
    digest = hashlib.sha256(archive.read_bytes()).hexdigest()
    if digest != WAR_SHA256:
        archive.unlink()
        raise SystemExit("Official WAR checksum mismatch; preparation stopped")
    password = secrets.token_urlsafe(32)
    config_dir = target / "config"
    config_dir.mkdir()
    config = (ROOT / "docker/config/datainfo.properties").read_text()
    replacements = {
        "dbPass": password,
        "mailHost": "127.0.0.1",
        "mailPort": "9",
        "sysURL": "http://localhost:18080/LibreClinica/MainMenu",
    }
    config = "\n".join(
        key + "=" + replacements[key] if key in replacements else line
        for line in config.splitlines() for key in [line.split("=", 1)[0]]
    ) + "\n"
    (config_dir / "datainfo.properties").write_text(config)
    compose = {
        "name": "clinica-original-" + secrets.token_hex(4),
        "services": {
            "db": {
                "image": POSTGRES,
                "environment": {"POSTGRES_USER": "clinica", "POSTGRES_DB": "libreclinica",
                                "POSTGRES_PASSWORD": password},
                "volumes": ["db:/var/lib/postgresql/data"],
                "healthcheck": {"test": ["CMD", "pg_isready", "-h", "127.0.0.1",
                                          "-U", "clinica", "-d", "libreclinica"],
                                "interval": "2s", "timeout": "5s", "retries": 30},
            },
            "app": {
                "image": TOMCAT,
                "ports": ["127.0.0.1:18080:8080"],
                "depends_on": {"db": {"condition": "service_healthy"}},
                "volumes": [
                    f"{archive}:/usr/local/tomcat/webapps/LibreClinica.war:ro",
                    f"{config_dir}:/usr/local/tomcat/libreclinica.config:ro",
                    "data:/usr/local/tomcat/libreclinica.data", "logs:/usr/local/tomcat/logs"],
            },
        },
        "volumes": {"db": {}, "data": {}, "logs": {}},
    }
    (target / "compose.json").write_text(json.dumps(compose, indent=2) + "\n")
    print("Prepared verified upstream 1.4.0 WAR and private configuration.")
    print("Start: docker compose -p " + compose["name"] + " -f work/original/compose.json up -d")
    print("URL: http://localhost:18080/LibreClinica/")
    print("This is an upstream comparison instance, not a Clinica EDC source build.")


if __name__ == "__main__":
    main()
