#!/usr/bin/env python3
"""One-time, allowlisted CodeContext -> Vericore migration.

CI runs this once on the approved migration branch. Workflow files are deliberately
excluded from this first commit so the GitHub token only needs contents write access;
CI/release workflow identifiers are migrated in a follow-up verified commit.
"""
from pathlib import Path
import re, subprocess
ROOT=Path(__file__).resolve().parents[1]
ROOTS=[ROOT/'src',ROOT/'docs',ROOT/'scripts',ROOT/'README.md',ROOT/'CONTRIBUTING.md',ROOT/'SECURITY.md',ROOT/'TRADEMARKS.md',ROOT/'CODE_OF_CONDUCT.md',ROOT/'DCO.md',ROOT/'build.gradle.kts',ROOT/'settings.gradle.kts',ROOT/'gradle.properties',ROOT/'.gitignore',ROOT/'.devcontainer',ROOT/'.codecontext.json.template',ROOT/'.codecontext-architecture-contract.json.template']
SKIP={ROOT/'CHANGELOG.md',ROOT/'docs/VERICORE_MIGRATION_SPEC.md',ROOT/'docs/VERICORE_MIGRATION_PLAN.md',Path(__file__)}
def files():
    seen=set()
    for x in ROOTS:
        ps=[x] if x.is_file() else ([p for p in x.rglob('*') if p.is_file()] if x.is_dir() else [])
        for p in ps:
            if p in SKIP or p in seen or any(a in {'build','.gradle','.git','node_modules','target'} for a in p.parts): continue
            try:p.read_text(encoding='utf-8')
            except (UnicodeDecodeError,OSError):continue
            seen.add(p);yield p
def rep(s):
    s=s.replace('com.codecontext','com.vericore').replace('sonii-shivansh/CodeContext','sonii-shivansh/Vericore')
    s=re.sub(r'\bCodeContext\b','Vericore',s);s=re.sub(r'\bcodecontext\b','vericore',s)
    s=s.replace('CODECONTEXT_','VERICORE_').replace('codecontext.config.home','vericore.config.home')
    s=s.replace('.codecontext-architecture-contract.json','.vericore-architecture-contract.json').replace('.codecontext.json','.vericore.json').replace('.codecontext/','.vericore/')
    return s.replace('build/install/codecontext','build/install/vericore')
def mv(a,b):
    if a.exists() and not b.exists():subprocess.run(['git','mv',str(a),str(b)],cwd=ROOT,check=True)
for p in files():
    a=p.read_text(encoding='utf-8');b=rep(a)
    if a!=b:p.write_text(b,encoding='utf-8')
mv(ROOT/'src/main/kotlin/com/codecontext/core/config/CodeContextConfig.kt',ROOT/'src/main/kotlin/com/codecontext/core/config/VericoreConfig.kt')
mv(ROOT/'src/main/kotlin/com/codecontext/core/exceptions/CodeContextException.kt',ROOT/'src/main/kotlin/com/codecontext/core/exceptions/VericoreException.kt')
mv(ROOT/'src/main/kotlin/com/codecontext/server/CodeContextServer.kt',ROOT/'src/main/kotlin/com/codecontext/server/VericoreServer.kt')
mv(ROOT/'src/main/kotlin/com/codecontext',ROOT/'src/main/kotlin/com/vericore')
mv(ROOT/'src/test/kotlin/com/codecontext',ROOT/'src/test/kotlin/com/vericore')
mv(ROOT/'.codecontext.json.template',ROOT/'.vericore.json.template')
mv(ROOT/'.codecontext-architecture-contract.json.template',ROOT/'.vericore-architecture-contract.json.template')
for r in [ROOT/'src/main/kotlin/com/vericore',ROOT/'src/test/kotlin/com/vericore']:
    if r.exists():
        for p in r.rglob('*.kt'):
            a=p.read_text(encoding='utf-8');b=rep(a)
            if a!=b:p.write_text(b,encoding='utf-8')
print('Vericore migration transformation completed')
