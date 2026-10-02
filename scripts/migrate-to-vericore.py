#!/usr/bin/env python3
"""One-time, allowlisted CodeContext -> Vericore repository migration."""
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TEXT_ROOTS = [ROOT / "src", ROOT / "docs", ROOT / ".github", ROOT / "scripts", ROOT / "README.md", ROOT / "CONTRIBUTING.md", ROOT / "SECURITY.md", ROOT / "TRADEMARKS.md", ROOT / "CODE_OF_CONDUCT.md", ROOT / "DCO.md", ROOT / "build.gradle.kts", ROOT / "settings.gradle.kts", ROOT / "gradle.properties", ROOT / ".gitignore", ROOT / ".devcontainer", ROOT / ".codecontext.json.template", ROOT / ".codecontext-architecture-contract.json.template"]
SKIP_FILES = {ROOT / "CHANGELOG.md", ROOT / "docs/VERICORE_MIGRATION_SPEC.md", ROOT / "docs/VERICORE_MIGRATION_PLAN.md", Path(__file__)}
OLD_GITHUB = "sonii-shivansh/CodeContext"
NEW_GITHUB = "sonii-shivansh/Vericore"

def files_to_process():
    seen = set()
    for item in TEXT_ROOTS:
        candidates = [item] if item.is_file() else ([p for p in item.rglob("*") if p.is_file()] if item.is_dir() else [])
        for p in candidates:
            if p in SKIP_FILES or p in seen or any(part in {"build", ".gradle", ".git", "node_modules", "target"} for part in p.parts):
                continue
            try: p.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError): continue
            seen.add(p); yield p

def replace_identity(text: str) -> str:
    text = text.replace("com.codecontext", "com.vericore")
    text = text.replace(OLD_GITHUB, NEW_GITHUB)
    text = re.sub(r"\bCodeContext\b", "Vericore", text)
    text = re.sub(r"\bcodecontext\b", "vericore", text)
    text = text.replace("CODECONTEXT_", "VERICORE_")
    text = text.replace("codecontext.config.home", "vericore.config.home")
    text = text.replace(".codecontext-architecture-contract.json", ".vericore-architecture-contract.json")
    text = text.replace(".codecontext.json", ".vericore.json")
    text = text.replace(".codecontext/", ".vericore/")
    text = text.replace("build/install/codecontext", "build/install/vericore")
    return text

def git_mv(old: Path, new: Path):
    if old.exists() and not new.exists(): subprocess.run(["git", "mv", str(old), str(new)], cwd=ROOT, check=True)

def main():
    for p in files_to_process():
        original = p.read_text(encoding="utf-8"); updated = replace_identity(original)
        if updated != original: p.write_text(updated, encoding="utf-8")
    git_mv(ROOT / "src/main/kotlin/com/codecontext/core/config/CodeContextConfig.kt", ROOT / "src/main/kotlin/com/codecontext/core/config/VericoreConfig.kt")
    git_mv(ROOT / "src/main/kotlin/com/codecontext/core/exceptions/CodeContextException.kt", ROOT / "src/main/kotlin/com/codecontext/core/exceptions/VericoreException.kt")
    git_mv(ROOT / "src/main/kotlin/com/codecontext/server/CodeContextServer.kt", ROOT / "src/main/kotlin/com/codecontext/server/VericoreServer.kt")
    git_mv(ROOT / "src/main/kotlin/com/codecontext", ROOT / "src/main/kotlin/com/vericore")
    git_mv(ROOT / "src/test/kotlin/com/codecontext", ROOT / "src/test/kotlin/com/vericore")
    git_mv(ROOT / ".codecontext.json.template", ROOT / ".vericore.json.template")
    git_mv(ROOT / ".codecontext-architecture-contract.json.template", ROOT / ".vericore-architecture-contract.json.template")
    for root in (ROOT / "src/main/kotlin/com/vericore", ROOT / "src/test/kotlin/com/vericore"):
        if root.exists():
            for p in root.rglob("*.kt"):
                original = p.read_text(encoding="utf-8"); updated = replace_identity(original)
                if updated != original: p.write_text(updated, encoding="utf-8")
    print("Vericore migration transformation completed")

if __name__ == "__main__": main()
