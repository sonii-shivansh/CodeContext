#!/bin/bash

# CodeContext Release Preparation Script
# Version: 0.1.0

set -e  # Exit on error

echo "🚀 CodeContext Release Preparation"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

VERSION="v0.1.0"

# Step 1: Clean previous builds
echo -e "${BLUE}📦 Step 1: Cleaning previous builds...${NC}"
./gradlew clean
echo -e "${GREEN}✅ Clean complete${NC}"
echo ""

# Step 2: Run tests
echo -e "${BLUE}🧪 Step 2: Running tests...${NC}"
./gradlew test
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed${NC}"
else
    echo -e "${YELLOW}⚠️  Some tests failed. Continue anyway? (y/n)${NC}"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Release cancelled."
        exit 1
    fi
fi
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

# Step 5: Package release
echo -e "${BLUE}📦 Step 5: Packaging release...${NC}"
RELEASE_NAME="codecontext-${VERSION}"
RELEASE_DIR="build/release"

# Create release directory
mkdir -p "${RELEASE_DIR}"

# Create zip archive
cd build/install
if command -v zip &> /dev/null; then
    zip -r "../../${RELEASE_DIR}/${RELEASE_NAME}.zip" codecontext/
    echo -e "${GREEN}✅ Created ${RELEASE_NAME}.zip${NC}"
else
    tar -czf "../../${RELEASE_DIR}/${RELEASE_NAME}.tar.gz" codecontext/
    echo -e "${GREEN}✅ Created ${RELEASE_NAME}.tar.gz${NC}"
fi
cd ../..
echo ""

# Step 6: Generate checksums
echo -e "${BLUE}🔐 Step 6: Generating checksums...${NC}"
cd "${RELEASE_DIR}"
if command -v sha256sum &> /dev/null; then
    sha256sum ${RELEASE_NAME}.* > checksums.txt
    echo -e "${GREEN}✅ Checksums generated${NC}"
elif command -v shasum &> /dev/null; then
    shasum -a 256 ${RELEASE_NAME}.* > checksums.txt
    echo -e "${GREEN}✅ Checksums generated${NC}"
fi
cd ../..
echo ""

# Step 7: Create release notes
echo -e "${BLUE}📝 Step 7: Creating release notes...${NC}"
cat > "${RELEASE_DIR}/RELEASE_NOTES.md" << EOF
# CodeContext ${VERSION} Release Notes

## 🎉 Features

### Core Analysis
- **Smart Git Analysis**: Single-pass commit analysis for optimal performance
- **Parallel Parsing**: Chunked file processing with intelligent caching
- **Cycle-Aware Dependency Graph**: Robust handling of circular dependencies
- **AI-Powered Insights**: Optional Gemini integration for code analysis

### Commands
- \`analyze\`: Comprehensive codebase analysis with hotspot detection
- \`ai-assistant\`: AI-powered code insights and recommendations
- \`evolution\`: Track codebase changes over time
- \`server\`: REST API server mode for programmatic access

### Output
- Interactive HTML reports with D3.js visualizations
- Learning path generation for new developers
- AI insights in markdown format
- Hotspot detection and ranking

## 📦 Installation

### From Release Archive

\`\`\`bash
# Extract archive
unzip codecontext-${VERSION}.zip
# or
tar -xzf codecontext-${VERSION}.tar.gz

# Add to PATH
export PATH=\$PATH:\$(pwd)/codecontext/bin

# Verify installation
codecontext --help
\`\`\`

### From Source

\`\`\`bash
git clone https://github.com/sonii-shivansh/Vericore.git
cd CodeContext
./gradlew installDist
./build/install/vericore/bin/codecontext --help
\`\`\`

## 🚀 Quick Start

\`\`\`bash
# Analyze current directory
codecontext analyze .

# View report
open output/index.html

# Enable AI insights (requires Gemini API key)
# Add to .vericore.json:
{
  "ai": {
    "enabled": true,
    "apiKey": "your-api-key",
    "model": "gemini-1.5-flash"
  }
}
\`\`\`

## 🔧 Configuration

Create \`.vericore.json\` in your project root:

\`\`\`json
{
  "maxFilesAnalyze": 10000,
  "hotspotCount": 10,
  "enableCache": true,
  "ai": {
    "enabled": false,
    "apiKey": "",
    "model": "gemini-1.5-flash"
  }
}
\`\`\`

## 📊 System Requirements

- **JVM**: Java 11 or higher
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

# Summary
echo "=================================="
echo -e "${GREEN}✨ Release preparation complete!${NC}"
echo ""
echo "📦 Release artifacts:"
echo "   - Location: ${RELEASE_DIR}/"
ls -lh "${RELEASE_DIR}/"
echo ""
echo "📋 Next steps:"
echo "   1. Review release notes: ${RELEASE_DIR}/RELEASE_NOTES.md"
echo "   2. Test the distribution: ./build/install/vericore/bin/codecontext --help"
echo "   3. Create GitHub release with artifacts from ${RELEASE_DIR}/"
echo "   4. Update CHANGELOG.md"
echo "   5. Tag release: git tag ${VERSION} && git push origin ${VERSION}"
echo ""
echo -e "${BLUE}🎉 Ready to release ${VERSION}!${NC}"
