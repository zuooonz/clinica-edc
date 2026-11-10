#!/usr/bin/env python3
"""Verify pinned upstream source trees and the only allowed PDFBox 2 adaptations.

Offline integrity check. Authenticity review of the pinned GitHub commit remains
part of code review; changing this file and its anchors requires a new review.
"""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "vendor/pdfbox2-layout"
COMMIT = "6902dd0568061afff4da1d05a4cbd5a7f2b9dc8f"
TREES = {
    "src/main/java": "bfe42cb5b84c0c15df3188f99a0406e123a463c1",
    "pdfbox2-layout/compatibility": "113e828b6eb83cc18460747336268b0fd6cad141",
}
LICENSE_BLOB = "f1b5dec407b65b871fa82352c41ed28790f5370c"
REPLACEMENTS = (
    (b"org.apache.pdfbox.pdmodel.edit.PDPageContentStream", b"org.apache.pdfbox.pdmodel.PDPageContentStream"),
    (b"org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject", b"org.apache.pdfbox.pdmodel.graphics.PDXObject"),
)


def git_hash(kind, data):
    return hashlib.sha1(kind.encode() + b" " + str(len(data)).encode() + b"\0" + data).hexdigest()


def tree_hash(directory):
    entries = []
    for path in sorted(directory.iterdir(), key=lambda p: p.name + ("/" if p.is_dir() else "")):
        if path.is_symlink():
            raise ValueError("Upstream source symlink is not allowed")
        mode, digest = ("40000", tree_hash(path)) if path.is_dir() else ("100644", git_hash("blob", path.read_bytes()))
        entries.append(mode.encode() + b" " + path.name.encode() + b"\0" + bytes.fromhex(digest))
    return git_hash("tree", b"".join(entries))


def verify(generated=None):
    upstream = ROOT / "upstream"
    manifest = json.loads((ROOT / "provenance.json").read_text())
    if manifest["commit"] != COMMIT or manifest["repository"] != "https://github.com/ralfstuckert/pdfbox-layout":
        raise ValueError("Unexpected upstream identity")
    actual = {p.relative_to(upstream).as_posix() for p in upstream.rglob("*") if p.is_file()}
    if actual != set(manifest["files"]):
        raise ValueError("Upstream file set differs from provenance manifest")
    for relative, identity in manifest["files"].items():
        path = upstream / relative
        if path.is_symlink():
            raise ValueError("Upstream source symlink is not allowed")
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != identity["sha256"] or git_hash("blob", data) != identity["git_blob"]:
            raise ValueError("Upstream source modified: " + relative)
    for relative, expected in TREES.items():
        if tree_hash(upstream / relative) != expected:
            raise ValueError("Pinned upstream Git tree mismatch: " + relative)
    if git_hash("blob", (upstream / "LICENSE").read_bytes()) != LICENSE_BLOB:
        raise ValueError("Pinned MIT license mismatch")
    if generated:
        expected_sources = {}
        for path in (upstream / "src/main/java").rglob("*.java"):
            data = path.read_bytes()
            for before, after in REPLACEMENTS:
                data = data.replace(before, after)
            expected_sources[path.relative_to(upstream / "src/main/java").as_posix()] = data
        for path in (upstream / "pdfbox2-layout/compatibility").rglob("*.java"):
            expected_sources[path.relative_to(upstream / "pdfbox2-layout/compatibility").as_posix()] = path.read_bytes()
        actual_sources = {p.relative_to(generated).as_posix() for p in generated.rglob("*") if p.is_file()}
        if actual_sources != set(expected_sources):
            raise ValueError("Generated source file set differs from upstream adaptation")
        for relative, expected in expected_sources.items():
            path = generated / relative
            if path.is_symlink() or path.read_bytes() != expected:
                raise ValueError("Unexpected generated source change: " + relative)
    print("PASS: pinned PDFBox Layout 1.0.1 source trees, file hashes and MIT license" +
          ("; generated PDFBox 2 adaptation is exact" if generated else ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--generated", type=Path, help="Also verify an existing generated source directory")
    args = parser.parse_args()
    verify(args.generated)
