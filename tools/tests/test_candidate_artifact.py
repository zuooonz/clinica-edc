"""Validate fail-closed candidate artifact inspection with tiny synthetic ZIPs."""
from io import BytesIO
import json
from pathlib import Path
import sys
import tempfile
import unittest
from zipfile import ZipFile

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
from prepare_candidate_baseline import artifact_info, LAYOUT  # noqa: E402


class CandidateArtifactTest(unittest.TestCase):
    def artifact(self, *, old_layout=False, license_missing=False, license_changed=False,
                 wrong_commit=False, wrong_coordinate=False):
        core = BytesIO()
        with ZipFile(core, "w") as jar:
            prefix = "org/akaza/openclinica/i18n/"
            jar.writestr(prefix + "words.properties", "version_number=1.4.0rc1\n")
            jar.writestr(prefix + "licensing.properties",
                         "Version_release=Version: 1.4.0rc1 - Changeset: 123456789abc 2026-11-10\n")
        layout = BytesIO()
        with ZipFile(layout, "w") as jar:
            if not license_missing:
                license_text = (TOOLS.parent / "vendor/pdfbox2-layout/upstream/LICENSE").read_bytes()
                jar.writestr("META-INF/pdfbox-layout/LICENSE", license_text + (b"changed" if license_changed else b""))
            jar.writestr("META-INF/pdfbox-layout/provenance.json", json.dumps({
                "commit": "0" * 40 if wrong_commit else "6902dd0568061afff4da1d05a4cbd5a7f2b9dc8f"}))
            jar.writestr("META-INF/maven/org.clinica.vendor/pdfbox2-layout/pom.properties",
                         "groupId=" + ("unexpected" if wrong_coordinate else "org.clinica.vendor")
                         + "\nartifactId=pdfbox2-layout\nversion=1.0.1-clinica.1\n")
        archive = BytesIO()
        with ZipFile(archive, "w") as war:
            war.writestr("WEB-INF/lib/LibreClinica-core-1.4.0rc1.jar", core.getvalue())
            war.writestr(LAYOUT, layout.getvalue())
            if old_layout:
                war.writestr("WEB-INF/lib/pdfbox2-layout-1.0.1.jar", b"synthetic obsolete dependency")
        return archive

    def inspect(self, **options):
        with tempfile.TemporaryDirectory(prefix="clinica-artifact-") as temp:
            path = Path(temp) / "synthetic.war"
            path.write_bytes(self.artifact(**options).getvalue())
            return artifact_info(path)

    def test_valid_metadata_is_parsed_without_claiming_build_authenticity(self):
        result = self.inspect()
        self.assertEqual(result["artifact_changeset"], "123456789abc")
        self.assertEqual(result["version"], "1.4.0rc1")

    def test_obsolete_dependency_is_rejected_even_with_new_dependency(self):
        with self.assertRaises(ValueError):
            self.inspect(old_layout=True)

    def test_license_must_be_present_and_exact(self):
        for options in [{"license_missing": True}, {"license_changed": True}]:
            with self.subTest(options=options), self.assertRaises((ValueError, KeyError)):
                self.inspect(**options)

    def test_unexpected_source_commit_rejected(self):
        with self.assertRaises(ValueError):
            self.inspect(wrong_commit=True)

    def test_unexpected_maven_coordinate_rejected(self):
        with self.assertRaises(ValueError):
            self.inspect(wrong_coordinate=True)


if __name__ == "__main__":
    unittest.main()
