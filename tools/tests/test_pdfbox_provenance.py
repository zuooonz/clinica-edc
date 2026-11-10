"""Exercise rejection paths of the vendored-source provenance gate."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import shutil
import tempfile
import unittest

TOOLS = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("provenance", TOOLS / "verify_pdfbox_layout.py")
provenance = importlib.util.module_from_spec(spec)
spec.loader.exec_module(provenance)
ORIGINAL = provenance.ROOT


class ProvenanceTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="clinica-provenance-")
        self.root = Path(self.temp.name) / "layout"
        shutil.copytree(ORIGINAL / "upstream", self.root / "upstream")
        shutil.copyfile(ORIGINAL / "provenance.json", self.root / "provenance.json")
        provenance.ROOT = self.root
        self.source = self.root / "upstream/src/main/java/rst/pdfbox/layout/elements/Document.java"

    def tearDown(self):
        provenance.ROOT = ORIGINAL
        self.temp.cleanup()

    def verify(self, generated=None):
        with contextlib.redirect_stdout(io.StringIO()):
            provenance.verify(generated)

    def test_original_pinned_source_passes(self):
        self.verify()

    def test_modified_source_rejected(self):
        self.source.write_bytes(self.source.read_bytes() + b"\n// changed\n")
        with self.assertRaisesRegex(ValueError, "source modified"):
            self.verify()

    def test_removed_source_rejected(self):
        self.source.unlink()
        with self.assertRaisesRegex(ValueError, "file set"):
            self.verify()

    def test_extra_source_rejected(self):
        (self.source.parent / "Extra.java").write_text("class Extra {}")
        with self.assertRaisesRegex(ValueError, "file set"):
            self.verify()

    def test_manifest_hash_rewrite_cannot_replace_pinned_tree(self):
        self.source.write_bytes(self.source.read_bytes() + b"\n// changed\n")
        manifest_path = self.root / "provenance.json"
        manifest = json.loads(manifest_path.read_text())
        identity = manifest["files"][self.source.relative_to(self.root / "upstream").as_posix()]
        identity["sha256"] = provenance.hashlib.sha256(self.source.read_bytes()).hexdigest()
        identity["git_blob"] = provenance.git_hash("blob", self.source.read_bytes())
        manifest_path.write_text(json.dumps(manifest))
        with self.assertRaisesRegex(ValueError, "Git tree mismatch"):
            self.verify()

    def test_upstream_identity_rewrite_rejected(self):
        path = self.root / "provenance.json"
        manifest = json.loads(path.read_text())
        manifest["commit"] = "0" * 40
        path.write_text(json.dumps(manifest))
        with self.assertRaisesRegex(ValueError, "identity"):
            self.verify()

    def test_unexpected_generated_change_rejected(self):
        generated = self.root / "generated"
        shutil.copytree(ORIGINAL / "target/generated-sources/pdfbox2", generated)
        self.verify(generated)
        target = generated / "rst/pdfbox/layout/util/CompatibilityHelper.java"
        target.write_bytes(target.read_bytes() + b"\n// changed\n")
        with self.assertRaisesRegex(ValueError, "generated source change"):
            self.verify(generated)


if __name__ == "__main__":
    unittest.main()
