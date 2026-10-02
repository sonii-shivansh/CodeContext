# Vericore Release Preparation Script (Windows)
# Version: 0.7.0

$ErrorActionPreference = "Stop"

Write-Host "🚀 Vericore Release Preparation" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

$VERSION = "v0.7.0"

# Step 1: Clean previous builds
Write-Host "📦 Step 1: Cleaning previous builds..." -ForegroundColor Blue
& .\gradlew.bat clean
Write-Host "✅ Clean complete" -ForegroundColor Green
Write-Host ""

# Step 2: Run tests
Write-Host "🧪 Step 2: Running tests..." -ForegroundColor Blue
& .\gradlew.bat test
Write-Host "✅ All tests passed" -ForegroundColor Green
Write-Host ""

# Step 3: Build project
Write-Host "🔨 Step 3: Building project..." -ForegroundColor Blue
& .\gradlew.bat build
Write-Host "✅ Build complete" -ForegroundColor Green
Write-Host ""

# Step 4: Create distribution
Write-Host "📦 Step 4: Creating distribution..." -ForegroundColor Blue
& .\gradlew.bat installDist
Write-Host "✅ Distribution created" -ForegroundColor Green
Write-Host ""

# Step 5: Verify canonical distribution
$APP = "build\install\vericore\bin\vericore.bat"
Write-Host "🔎 Step 5: Verifying Vericore distribution..." -ForegroundColor Blue
if (-not (Test-Path $APP)) {
    Write-Host "❌ Expected executable not found: $APP" -ForegroundColor Red
    exit 1
}
& .\$APP --version
Write-Host "✅ Vericore distribution verified" -ForegroundColor Green
Write-Host ""

# Step 6: Package release
Write-Host "📦 Step 6: Packaging release..." -ForegroundColor Blue
$RELEASE_NAME = "vericore-$VERSION"
$RELEASE_DIR = "build\release"

if (Test-Path $RELEASE_DIR) {
    Remove-Item -Recurse -Force $RELEASE_DIR
}
New-Item -ItemType Directory -Force -Path $RELEASE_DIR | Out-Null

Push-Location build\install
Compress-Archive -Path vericore -DestinationPath "..\..\$RELEASE_DIR\$RELEASE_NAME.zip" -Force
Write-Host "✅ Created $RELEASE_NAME.zip" -ForegroundColor Green
Pop-Location
Write-Host ""

# Step 7: Generate checksums
Write-Host "🔐 Step 7: Generating checksums..." -ForegroundColor Blue
Push-Location $RELEASE_DIR
Get-FileHash -Algorithm SHA256 "$RELEASE_NAME.zip" |
    Select-Object @{Name='Hash';Expression={$_.Hash.ToLower()}}, @{Name='File';Expression={Split-Path $_.Path -Leaf}} |
    ForEach-Object { "$($_.Hash)  $($_.File)" } |
    Out-File -FilePath checksums.txt -Encoding utf8
Write-Host "✅ Checksums generated" -ForegroundColor Green
Pop-Location
Write-Host ""

# Step 8: Create release notes
Write-Host "📝 Step 8: Creating release notes..." -ForegroundColor Blue
$releaseNotes = @"
# Vericore $VERSION Release Notes

## 🎉 Features

### Core Analysis
- **Smart Git Analysis**: Single-pass commit analysis for optimal performance
- **Parallel Parsing**: Chunked file processing with intelligent caching
- **Cycle-Aware Dependency Graph**: Robust handling of circular dependencies
- **AI-Powered Insights**: Optional Gemini integration for code analysis

### Commands
- ``analyze``: Comprehensive codebase analysis with hotspot detection
- ``evolution``: Track codebase changes over time
- ``server``: REST API server mode for programmatic access
- ``mcp``: MCP server mode for AI-agent integration

### Output
- Interactive HTML reports with D3.js visualizations
- Learning path generation for new developers
- AI insights in markdown format
- Hotspot detection and ranking

## 📦 Installation

### From Release Archive

``````bash
# Extract archive
Expand-Archive vericore-$VERSION.zip

# Add to PATH (PowerShell)
`$env:Path += ";`$(Get-Location)\vericore\bin"

# Verify installation
vericore --version
vericore --help
``````

### From Source

``````bash
git clone https://github.com/sonii-shivansh/CodeContext.git
cd CodeContext
.\gradlew.bat installDist
.\build\install\vericore\bin\vericore.bat --help
``````

## 🚀 Quick Start

``````bash
# Analyze current directory
vericore analyze .

# View report
start output\index.html
``````

## 🔧 Configuration

Vericore uses its canonical configuration namespace by default. Legacy CodeContext configuration files remain supported only through the documented migration compatibility path and emit a non-fatal migration warning.

## 📊 System Requirements

- **JVM**: Java 11 or higher
- **Memory**: 2GB RAM minimum, 4GB recommended
- **Disk**: 100MB for installation, additional space for cache

## 🐛 Known Issues

- Large repositories (>10,000 files) may require increased heap size
- Git analysis requires repository to be initialized
- AI features require valid Gemini API key

## 📚 Documentation

- [README](https://github.com/sonii-shivansh/CodeContext/blob/main/README.md)
- [API Documentation](https://github.com/sonii-shivansh/CodeContext/blob/main/docs/API.md)
- [Contributing Guide](https://github.com/sonii-shivansh/CodeContext/blob/main/CONTRIBUTING.md)

## 🙏 Acknowledgments

Built with:
- Kotlin
- JGit for Git analysis
- JGraphT for graph algorithms
- Ktor for REST API
- D3.js for visualizations

## 📧 Support

- Issues: https://github.com/sonii-shivansh/CodeContext/issues
- Email: shivanshsoni568@gmail.com
- Discussions: https://github.com/sonii-shivansh/CodeContext/discussions

## 📄 License

MIT License - See LICENSE file for details
"@

$releaseNotes | Out-File -FilePath "$RELEASE_DIR\RELEASE_NOTES.md" -Encoding utf8
Write-Host "✅ Release notes created" -ForegroundColor Green
Write-Host ""

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "✨ Vericore release preparation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📦 Release artifacts:" -ForegroundColor Cyan
Write-Host "   - Location: $RELEASE_DIR\" -ForegroundColor White
Get-ChildItem $RELEASE_DIR | Format-Table Name, Length, LastWriteTime
Write-Host ""
Write-Host "📋 Next steps:" -ForegroundColor Cyan
Write-Host "   1. Review release notes: $RELEASE_DIR\RELEASE_NOTES.md" -ForegroundColor White
Write-Host "   2. Test the distribution: .\build\install\vericore\bin\vericore.bat --help" -ForegroundColor White
Write-Host "   3. Create GitHub release with artifacts from $RELEASE_DIR\" -ForegroundColor White
Write-Host "   4. Update CHANGELOG.md" -ForegroundColor White
Write-Host "   5. Tag release: git tag $VERSION && git push origin $VERSION" -ForegroundColor White
Write-Host ""
Write-Host "🎉 Ready to release $VERSION!" -ForegroundColor Blue
