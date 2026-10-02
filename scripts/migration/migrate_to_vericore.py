#!/usr/bin/env python3
"""Controlled CodeContext -> Vericore identity migration.

This is intentionally not a blind repository-wide replacement. It applies
explicit identity mappings, protects the external CodeContext website
identifier, preserves historical files, renames product-specific paths, and
leaves third-party identifiers untouched.
"""

from __future__ import annotations

import argparse
from pathlib import Path

SKIP_DIRS = {".git", ".gradle", "build", "node_modules", "output", ".idea", "target"}
HISTORICAL_FILES = {
    Path("CHANGELOG.md"),
    Path("docs/VERICORE_MIGRATION.md"),
    Path("docs/VERICORE_MIGRATION_SPEC.md"),
    Path("docs/VERICORE_MIGRATION_PLAN.md"),
}
BINARY_SUFFIXES = {
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".pdf", ".jar", ".zip", ".gz",
    ".class", ".woff", ".woff2", ".ttf", ".otf", ".so", ".dll", ".exe",
}

PATH_RENAMES = {
    "src/main/kotlin/com/codecontext": "src/main/kotlin/com/vericore",
    "src/test/kotlin/com/codecontext": "src/test/kotlin/com/vericore",
    ".codecontext.json.template": ".vericore.json.template",
    ".codecontext-architecture-contract.json.template": ".vericore-architecture-contract.json.template",
}
FILE_RENAMES = {
    "CodeContextConfig.kt": "VericoreConfig.kt",
    "CodeContextException.kt": "VericoreException.kt",
    "CodeContextServer.kt": "VericoreServer.kt",
}


def is_skipped(path: Path, root: Path) -> bool:
    rel = path.relative_to(root)
    if len(rel.parts) >= 2 and rel.parts[0] == "scripts" and rel.parts[1] == "migration":
        return True
    return any(part in SKIP_DIRS for part in rel.parts)


def is_text_file(path: Path) -> bool:
    if path.suffix.lower() in BINARY_SUFFIXES:
        return False
    try:
        data = path.read_bytes()
    except OSError:
        return False
    return b"\x00" not in data


def protect_external(text: str) -> str:
    return text.replace("CodeContext-Website", "__VERICORE_EXTERNAL_WEBSITE__")


def restore_external(text: str) -> str:
    return text.replace("__VERICORE_EXTERNAL_WEBSITE__", "CodeContext-Website")


def transform(text: str) -> str:
    text = protect_external(text)
    replacements = (
        ("sonii-shivansh/CodeContext", "sonii-shivansh/Vericore"),
        ("com.codecontext", "com.vericore"),
        ("CodeContextConfig", "VericoreConfig"),
        ("CodeContextException", "VericoreException"),
        ("CodeContextServer", "VericoreServer"),
        ("CODECONTEXT_", "VERICORE_"),
        ("codecontext.config.home", "vericore.config.home"),
        (".codecontext-architecture-contract.json", ".vericore-architecture-contract.json"),
        (".codecontext.json", ".vericore.json"),
        (".codecontext/", ".vericore/"),
        ("build/install/codecontext", "build/install/vericore"),
        ("CodeContext", "Vericore"),
        ("codecontext", "vericore"),
    )
    for old, new in replacements:
        text = text.replace(old, new)
    return restore_external(text)


def rename_paths(root: Path) -> None:
    for old, new in sorted(PATH_RENAMES.items(), key=lambda item: len(item[0]), reverse=True):
        source = root / old
        target = root / new
        if source.exists() and not target.exists():
            target.parent.mkdir(parents=True, exist_ok=True)
            source.rename(target)

    for source in sorted(root.rglob("*"), key=lambda p: len(p.parts), reverse=True):
        if not source.is_file() or is_skipped(source, root):
            continue
        if source.name not in FILE_RENAMES:
            continue
        target = source.with_name(FILE_RENAMES[source.name])
        if not target.exists():
            source.rename(target)


def migrate(root: Path) -> None:
    rename_paths(root)

    for path in sorted(root.rglob("*")):
        if not path.is_file() or is_skipped(path, root) or not is_text_file(path):
            continue
        rel = path.relative_to(root)
        if rel in HISTORICAL_FILES:
            continue
        original = path.read_text(encoding="utf-8")
        updated = transform(original)
        if updated != original:
            path.write_text(updated, encoding="utf-8")

    executor = root / ".github/workflows/vericore-migration-executor.yml"
    if executor.exists():
        executor.unlink()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", nargs="?", default=".")
    args = parser.parse_args()
    migrate(Path(args.root).resolve())


if __name__ == "__main__":
    main()
