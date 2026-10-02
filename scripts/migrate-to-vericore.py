#!/usr/bin/env python3
"""One-time, allowlisted CodeContext -> Vericore repository migration.

This script intentionally does not perform an unrestricted repository-wide replacement.
Historical changelog content and external CodeContext-Website references are preserved.
"""
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]

TEXT_ROOTS = [
    ROOT / "src",
    ROOT / "docs",
    ROOT / ".github",
    ROOT / "scripts",
    ROOT / "README.md",
    ROOT / "CONTRIBUTING.md",
    ROOT / "SECURITY.md",
    ROOT / "TRADEMARKS.md",
    ROOT / "CODE_OF_CONDUCT.md",
    ROOT / "DCO.md",
    ROOT / "build.gradle.kts",
    ROOT / "settings.gradle.kts",
    ROOT / "gradle.properties",
    ROOT / ".gitignore",
    ROOT / ".devcontainer",
    ROOT / ".codecontext.json.template",
    ROOT / ".codecontext-architecture-contract.json.template",
]
SKIP_FILES = {
    ROOT / "CHANGELOG.md",  # historical release identity is preserved
    ROOT / "docs/VERICORE_MIGRATION_SPEC.md",
    ROOT / "docs/VERICORE_MIGRATION_PLAN.md",
    Path(__file__),
}

OLD_GITHUB = "sonii-shivansh/CodeContext"
NEW_GITHUB = "sonii-shivansh/Vericore"


def files_to_process():
    seen = set()
    for item in TEXT_ROOTS:
        if item.is_file():
            candidates = [item]
        elif item.is_dir():
            candidates = [p for p in item.rglob("*") if p.is_file()]
        else:
            candidates = []
        for p in candidates:
            if p in SKIP_FILES or p in seen:
                continue
            # Avoid binaries and generated/build trees.
            if any(part in {"build", ".gradle", ".git", "node_modules", "target"} for part in p.parts):
                continue
            try:
                p.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError):
                continue
            seen.add(p)
            yield p


def replace_identity(text: str) -> str:
    # Package/group identifiers first.
    text = text.replace("com.codecontext", "com.vericore")
    # Repository URLs and project-derived URLs.
    text = text.replace(OLD_GITHUB, NEW_GITHUB)
    # Canonical product/CLI identity.
    text = re.sub(r"\bCodeContext\b", "Vericore", text)
    text = re.sub(r"\bcodecontext\b", "vericore", text)
    # Environment/property identifiers are intentionally migrated to canonical names.
    text = text.replace("CODECONTEXT_", "VERICORE_")
    text = text.replace("codecontext.config.home", "vericore.config.home")
    # Generated/configuration filenames in current docs/templates/config code.
    text = text.replace(".codecontext-architecture-contract.json", ".vericore-architecture-contract.json")
    text = text.replace(".codecontext.json", ".vericore.json")
    text = text.replace(".codecontext/", ".vericore/")
    # Distribution/install paths.
    text = text.replace("build/install/codecontext", "build/install/vericore")
    return text


def git_mv(old: Path, new: Path):
    if old.exists() and not new.exists():
        subprocess.run(["git", "mv", str(old), str(new)], cwd=ROOT, check=True)


def main():
    # Source/test namespace directories are moved as a directory tree after content edits.
    for p in files_to_process():
        original = p.read_text(encoding="utf-8")
        updated = replace_identity(original)
        if updated != original:
            p.write_text(updated, encoding="utf-8")

    # Identity-bearing Kotlin class/file names.
    git_mv(ROOT / "src/main/kotlin/com/codecontext/core/config/CodeContextConfig.kt",
           ROOT / "src/main/kotlin/com/codecontext/core/config/VericoreConfig.kt")
    git_mv(ROOT / "src/main/kotlin/com/codecontext/core/exceptions/CodeContextException.kt",
           ROOT / "src/main/kotlin/com/codecontext/core/exceptions/VericoreException.kt")
    git_mv(ROOT / "src/main/kotlin/com/codecontext/server/CodeContextServer.kt",
           ROOT / "src/main/kotlin/com/codecontext/server/VericoreServer.kt")

    # Move package directories to match package declarations.
    git_mv(ROOT / "src/main/kotlin/com/codecontext", ROOT / "src/main/kotlin/com/vericore")
    git_mv(ROOT / "src/test/kotlin/com/codecontext", ROOT / "src/test/kotlin/com/vericore")

    # Rename canonical templates; legacy templates are retained as compatibility documentation only.
    git_mv(ROOT / ".codecontext.json.template", ROOT / ".vericore.json.template")
    git_mv(ROOT / ".codecontext-architecture-contract.json.template", ROOT / ".vericore-architecture-contract.json.template")

    # Re-run identity replacement on moved files, because their paths changed after the first pass.
    for root in (ROOT / "src/main/kotlin/com/vericore", ROOT / "src/test/kotlin/com/vericore"):
        if root.exists():
            for p in root.rglob("*.kt"):
                original = p.read_text(encoding="utf-8")
                updated = replace_identity(original)
                if updated != original:
                    p.write_text(updated, encoding="utf-8")

    # Remove old identity from this one-time executor's tracked source after it has done its work.
    # The workflow removes the executor itself in the same commit.
    print("Vericore migration transformation completed")


if __name__ == "__main__":
    main()
