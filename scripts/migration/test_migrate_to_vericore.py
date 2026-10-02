import importlib.util
import tempfile
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("migrate_to_vericore", ROOT / "scripts" / "migration" / "migrate_to_vericore.py")


class VericoreMigrationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / "src/main/kotlin/com/codecontext").mkdir(parents=True)
        (self.root / "docs").mkdir()
        (self.root / "README.md").write_text(
            "# CodeContext\nhttps://github.com/sonii-shivansh/CodeContext\nhttps://sonii-shivansh.github.io/CodeContext-Website/\n",
            encoding="utf-8",
        )
        (self.root / "CHANGELOG.md").write_text(
            "[0.6.0] CodeContext was released.\n",
            encoding="utf-8",
        )
        (self.root / "src/main/kotlin/com/codecontext/CodeContextConfig.kt").write_text(
            "package com.codecontext\nclass CodeContextConfig\n",
            encoding="utf-8",
        )
        (self.root / ".codecontext.json.template").write_text(
            "CODECONTEXT_AI_PROVIDER\n.codecontext/\n",
            encoding="utf-8",
        )

    def tearDown(self):
        self.temp.cleanup()

    def load_module(self):
        module = importlib.util.module_from_spec(SPEC)
        SPEC.loader.exec_module(module)
        return module

    def test_transformation_is_scoped_and_preserves_history_and_external_website(self):
        module = self.load_module()
        module.migrate(self.root)

        self.assertTrue((self.root / "src/main/kotlin/com/vericore/VericoreConfig.kt").is_file())
        self.assertFalse((self.root / "src/main/kotlin/com/codecontext/CodeContextConfig.kt").exists())

        readme = (self.root / "README.md").read_text(encoding="utf-8")
        self.assertIn("Vericore", readme)
        self.assertIn("sonii-shivansh/Vericore", readme)
        self.assertIn("CodeContext-Website", readme)

        changelog = (self.root / "CHANGELOG.md").read_text(encoding="utf-8")
        self.assertIn("CodeContext was released", changelog)

        config = (self.root / ".vericore.json.template").read_text(encoding="utf-8")
        self.assertIn("VERICORE_AI_PROVIDER", config)
        self.assertIn(".vericore/", config)
        self.assertFalse((self.root / ".codecontext.json.template").exists())


if __name__ == "__main__":
    unittest.main()
