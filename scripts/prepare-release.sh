#!/bin/bash

# Vericore Release Preparation Script
# Version: 0.7.0

set -euo pipefail

echo "🚀 Vericore Release Preparation"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

VERSION="v0.7.0"

# Step 1: Clean previous builds
echo -e "${BLUE}📦 Step 1: Cleaning previous builds...${NC}"
./gradlew clean
echo -e "${GREEN}✅ Clean complete${NC}"
echo ""

# Step 2: Run tests
echo -e "${BLUE}🧪 Step 2: Running tests...${NC}"
./gradlew test
echo -e "${GREEN}✅ All tests passed${NC}"
echo ""

# Step 3: Build project
echo -e "${BLUE}🔨 Step 3: Building project...${NC}"
./gradlew build
echo -e "${GREEN}✅ Build complete${NC}"
echo ""

# Step 4: Create distribution
echo -e "${BLUE}📦 Step 4: Creating distribution...${NC}"
./gradlew installDist
echo -e "${GREEN}✅ Distribution created${NC}"
echo ""

# Step 5: Verify canonical distribution
APP="build/install/vericore/bin/vericore"
echo -e "${BLUE}🔎 Step 5: Verifying Vericore distribution...${NC}"
if [[ ! -x "${APP}" ]]; then
    echo -e "${YELLOW}❌ Expected executable not found: ${APP}${NC}"
    exit 1
fi
"./${APP}" --version
echo -e "${GREEN}✅ Vericore distribution verified${NC}"
echo ""

# Step 6: Package release
RELEASE_NAME="vericore-${VERSION}"
RELEASE_DIR="build/release"
echo -e "${BLUE}📦 Step 6: Packaging release...${NC}"

rm -rf "${RELEASE_DIR}"
mkdir -p "${RELEASE_DIR}"

cd build/install
if command -v zip &> /dev/null; then
    zip -r "../../${RELEASE_DIR}/${RELEASE_NAME}.zip" vericore/
    echo -e "${GREEN}✅ Created ${RELEASE_NAME}.zip${NC}"
else
    tar -czf "../../${RELEASE_DIR}/${RELEASE_NAME}.tar.gz" vericore/
    echo -e "${GREEN}✅ Created ${RELEASE_NAME}.tar.gz${NC}"
fi
cd ../..
echo ""

# Step 7: Generate checksums
echo -e "${BLUE}🔐 Step 7: Generating checksums...${NC}"
cd "${RELEASE_DIR}"
if command -v sha256sum &> /dev/null; then
    sha256sum ${RELEASE_NAME}.* > checksums.txt
elif command -v shasum &> /dev/null; then
    shasum -a 256 ${RELEASE_NAME}.* > checksums.txt
else
    echo -e "${YELLOW}⚠️ SHA-256 utility not available; skipping checksums${NC}"
fi
echo -e "${GREEN}✅ Checksums generated${NC}"
cd ../..
echo ""

# Step 8: Create release notes
cat > "${RELEASE_DIR}/RELEASE_NOTES.md" << EOF
# Vericore ${VERSION} Release Notes

## 🎉 Features

### Core Analysis
- **Smart Git Analysis**: Single-pass commit analysis for optimal performance
- **Parallel Parsing**: Chunked file processing with intelligent caching
- **Cycle-Aware Dependency Graph**: Robust handling of circular dependencies
- **AI-Powered Insights**: Optional Gemini integration for code analysis

### Commands
- \`analyze\`: Comprehensive codebase analysis with hotspot detection
- \`evolution\`: Track codebase changes over time
- \`server\`: REST API server mode for programmatic access
- \`mcp\`: MCP server mode for AI-agent integration

### Output
- Interactive HTML reports with D3.js visualizations
- Learning path generation for new developers
- AI insights in markdown format
- Hotspot detection and ranking

## 📦 Installation

### From Release Archive

\`\`\`bash
# Extract archive
unzip vericore-${VERSION}.zip
# or
tar -xzf vericore-${VERSION}.tar.gz

# Add to PATH
export PATH=\$PATH:\$(pwd)/vericore/bin

# Verify installation
vericore --version
vericore --help
\`\`\`

### From Source

\`\`\`bash
git clone https://github.com/sonii-shivansh/Vericore.git
cd Vericore
./gradlew installDist
./build/install/vericore/bin/vericore --help
\`\`\`

## 🚀 Quick Start

\`\`\`bash
# Analyze current directory
vericore analyze .

# View report
open output/index.html
\`\`\`

## 🔧 Configuration

Vericore uses its canonical configuration namespace by default. Legacy configuration files and environment variables remain supported only through the documented migration compatibility path and emit a non-fatal migration warning.

## 📊 System Requirements

- **JVM**: Java 21 or higher
- **Memory**: 2GB RAM minimum, 4GB recommended
- **Disk**: 100MB for installation, additional space for cache

## 🐛 Known Issues

- Large repositories (>10,000 files) may require increased heap size
- Git analysis requires repository to be initialized
- AI features require valid Gemini API key

## 📚 Documentation

- [README](https://github.com/sonii-shivansh/Vericore/blob/main/README.md)
- [API Documentation](https://github.com/sonii-shivansh/Vericore/blob/main/docs/API.md)
- [Contributing Guide](https://github.com/sonii-shivansh/Vericore/blob/main/CONTRIBUTING.md)

## 🙏 Acknowledgments

Built with:
- Kotlin
- JGit for Git analysis
- JGraphT for graph algorithms
- Ktor for REST API
- D3.js for visualizations

## 📧 Support

- Issues: https://github.com/sonii-shivansh/Vericore/issues
- Email: shivanshsoni568@gmail.com
- Discussions: https://github.com/sonii-shivansh/Vericore/discussions

## 📄 License

MIT License - See LICENSE file for details
EOF

echo -e "${GREEN}✅ Release notes created${NC}"
echo ""

echo "=================================="
echo -e "${GREEN}✨ Vericore release preparation complete!${NC}"
echo ""
echo "📦 Release artifacts:"
echo "   - Location: ${RELEASE_DIR}/"
ls -lh "${RELEASE_DIR}/"
echo ""
echo "📋 Next steps:"
echo "   1. Review release notes: ${RELEASE_DIR}/RELEASE_NOTES.md"
echo "   2. Test the distribution: ${APP} --help"
echo "   3. Create GitHub release with artifacts from ${RELEASE_DIR}/"
echo "   4. Update CHANGELOG.md"
echo "   5. Tag release: git tag ${VERSION} && git push origin ${VERSION}"
echo ""
echo -e "${BLUE}🎉 Ready to release ${VERSION}!${NC}"
