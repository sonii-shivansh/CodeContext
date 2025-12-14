# API Documentation

## Overview

CodeContext provides both a **CLI interface** and a **REST API** for programmatic access.

---

## CLI API

### Installation

```bash
./gradlew installDist
```

### Basic Usage

```bash
# Analyze current directory
./build/install/codecontext/bin/codecontext analyze .

# Analyze specific directory
./build/install/codecontext/bin/codecontext analyze /path/to/project

# With options
./build/install/codecontext/bin/codecontext analyze . --no-cache --clear-cache
```

### Commands

#### `analyze`

Analyzes a codebase and generates an interactive report.

**Syntax:**
```bash
codecontext analyze <path> [options]
```

**Arguments:**
- `<path>` - Path to analyze (default: current directory)

**Options:**
- `--no-cache` - Disable caching for this run
- `--clear-cache` - Clear cache before analyzing

**Output:**
- HTML report: `output/index.html`
- AI insights (if enabled): `output/ai-insights.md`

**Example:**
```bash
codecontext analyze ~/projects/my-app --clear-cache
```

**Output:**
```
🚀 Starting CodeContext analysis for: ~/projects/my-app
📂 Scanning repository...
   Found 247 files
🧠 Parsing code...
   Parsed 247 files
📜 Analyzing Git history...
🕸️ Building dependency graph...
🗺️ Your Codebase Map
├─ 🔥 Hot Zones (Top 5):
│   ├─ UserService.kt (0.0847)
│   ├─ DatabaseConfig.kt (0.0623)
│   └─ ...
📊 Generating report...
✅ Report: ~/projects/my-app/output/index.html
✨ Complete in 3421ms
```

---

#### `server`

Starts a REST API server.

**Syntax:**
```bash
codecontext server [options]
```

**Options:**
- `--port <number>` - Port to listen on (default: 8080)
- `--host <address>` - Host address (default: 0.0.0.0)

**Example:**
```bash
codecontext server --port 3000
```

---

#### `ai-assistant`

Generates AI-powered code insights.

**Syntax:**
```bash
codecontext ai-assistant <path> [options]
```

**Requirements:**
- OpenAI API key in config or environment variable

**Example:**
```bash
export OPENAI_API_KEY=sk-...
codecontext ai-assistant .
```

---

#### `evolution`

Tracks codebase evolution over time.

**Syntax:**
```bash
codecontext evolution <path>
```

**Output:**
- Timeline of changes
- Hotspot evolution
- Contributor activity

---

## REST API

### Starting the Server

```bash
codecontext server --port 8080
```

### Endpoints

#### `POST /analyze`

Triggers analysis of a codebase.

**Request:**
```json
{
  "path": "/path/to/project",
  "options": {
    "enableCache": true,
    "clearCache": false
  }
}
```

**Response:**
```json
{
  "status": "success",
  "analysisId": "abc123",
  "stats": {
    "filesScanned": 247,
    "filesParsed": 247,
    "graphNodes": 247,
    "graphEdges": 892
  },
  "reportUrl": "/report/abc123"
}
```

**Status Codes:**
- `200` - Analysis completed successfully
- `400` - Invalid request
- `500` - Analysis failed

---

#### `GET /report/:id`

Retrieves analysis report.

**Request:**
```
GET /report/abc123
```

**Response:**
```json
{
  "id": "abc123",
  "timestamp": "2025-12-14T20:00:00Z",
  "path": "/path/to/project",
  "hotspots": [
    {
      "file": "UserService.kt",
      "score": 0.0847,
      "description": "Main user service"
    }
  ],
  "learningPath": [
    {
      "file": "Utils.kt",
      "reason": "Foundation utilities"
    }
  ],
  "graph": {
    "nodes": [...],
    "links": [...]
  }
}
```

---

#### `GET /health`

Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "version": "0.1.0",
  "uptime": 3600
}
```

---

## Programmatic API (Kotlin)

### Using as a Library

Add to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.codecontext:codecontext-core:0.1.0")
}
```

### Example Usage

```kotlin
import com.codecontext.core.scanner.RepositoryScanner
import com.codecontext.core.parser.ParserFactory
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.output.ReportGenerator

fun analyzeProject(path: String) {
    // 1. Scan files
    val scanner = RepositoryScanner()
    val files = scanner.scan(path)
    
    // 2. Parse files
    val parsedFiles = files.map { file ->
        val parser = ParserFactory.getParser(file)
        parser.parse(file)
    }
    
    // 3. Build graph
    val graph = RobustDependencyGraph()
    graph.build(parsedFiles)
    graph.analyze()
    
    // 4. Get hotspots
    val hotspots = graph.getTopHotspots(10)
    hotspots.forEach { (file, score) ->
        println("$file: $score")
    }
    
    // 5. Generate report
    val generator = ReportGenerator()
    generator.generate(graph, "output/report.html", parsedFiles, emptyList())
}
```

---

## Configuration API

### Config File

Location: `.codecontext.json` in project root

**Example:**
```json
{
  "maxFilesAnalyze": 10000,
  "hotspotCount": 15,
  "enableCache": true,
  "ai": {
    "enabled": false,
    "apiKey": "",
    "model": "gpt-4"
  }
}
```

### Loading Config

```kotlin
import com.codecontext.core.config.ConfigLoader

val config = ConfigLoader.load()
println("Max files: ${config.maxFilesAnalyze}")
```

---

## Data Models

### ParsedFile

```kotlin
data class ParsedFile(
    val file: File,
    val packageName: String,
    val imports: List<String>,
    val gitMetadata: GitMetadata = GitMetadata(),
    val description: String = ""
)
```

### GitMetadata

```kotlin
data class GitMetadata(
    val lastModified: Long = 0,
    val changeFrequency: Int = 0,
    val topAuthors: List<String> = emptyList(),
    val recentMessages: List<String> = emptyList()
)
```

### LearningStep

```kotlin
data class LearningStep(
    val file: String,
    val description: String,
    val reason: String
)
```

---

## Error Handling

### CLI Errors

```bash
❌ No source files found
❌ Too many files (12000). Limit: 10000
❌ Failed to build graph: ...
❌ Analysis failed: ...
```

### API Errors

```json
{
  "error": {
    "code": "INVALID_PATH",
    "message": "Path does not exist: /invalid/path",
    "details": {}
  }
}
```

**Error Codes:**
- `INVALID_PATH` - Path doesn't exist
- `TOO_MANY_FILES` - Exceeds max file limit
- `PARSE_ERROR` - Failed to parse file
- `GRAPH_BUILD_ERROR` - Failed to build dependency graph
- `INTERNAL_ERROR` - Unexpected error

---

## Rate Limiting (REST API)

- **Default:** 100 requests per minute per IP
- **Burst:** Up to 10 concurrent analyses

---

## Examples

### Analyze and Get Hotspots

```kotlin
val scanner = RepositoryScanner()
val files = scanner.scan(".")

val parser = CodeParallelParser()
val parsed = runBlocking { parser.parseFiles(files) }

val graph = RobustDependencyGraph()
graph.build(parsed)
graph.analyze()

val hotspots = graph.getTopHotspots(5)
```

### Custom Parser

```kotlin
class PythonParser : LanguageParser {
    override fun parse(file: File): ParsedFile {
        val content = file.readText()
        val packageName = extractPackage(content)
        val imports = extractImports(content)
        return ParsedFile(file, packageName, imports)
    }
}

// Register
ParserFactory.register("py", PythonParser())
```

---

## Webhooks (Future)

Coming soon: Webhook support for real-time notifications.

```json
{
  "event": "analysis.completed",
  "data": {
    "analysisId": "abc123",
    "timestamp": "2025-12-14T20:00:00Z"
  }
}
```

---

## SDK Support (Planned)

- **JavaScript/TypeScript** - npm package
- **Python** - pip package
- **Go** - Go module

---

## Support

For API questions or issues:
- 📧 Email: shivanshsoni568@gmail.com
- 🐛 Issues: https://github.com/sonii-shivansh/CodeContext/issues
- 💬 Discussions: https://github.com/sonii-shivansh/CodeContext/discussions
