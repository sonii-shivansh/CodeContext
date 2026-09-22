# API Reference

CodeContext exposes a CLI and a local REST API. The REST API is implemented by `com.codecontext.server.CodeContextServer` and is intended for trusted local or internal use.

## Build and start

```bash
./gradlew installDist
./build/install/codecontext/bin/codecontext server --host 127.0.0.1 --port 8080
```

The default server bind address should remain loopback for local use. Deployments that bind beyond loopback must provide authentication, trusted-origin controls, TLS, request quotas, and report authorization at the deployment boundary.

## CLI

```bash
codecontext analyze <path>
codecontext server [--host <address>] [--port <number>]
codecontext ai-assistant <path>
codecontext evolution <path>
```

Run `codecontext <command> --help` for the options available in the installed version.

## REST endpoints

### `GET /`

Returns a plain-text service banner.

```text
CodeContext API is running. 🚀
```

### `GET /health`

Returns service health and version information.

```json
{
  "status": "healthy",
  "version": "0.2.0"
}
```

`GET /health/live` and `GET /health/ready` return lightweight liveness and readiness responses.

### `POST /analyze`

Analyzes an existing local repository and generates an HTML report.

Remote URLs are rejected by the current endpoint. The request path must resolve to a readable directory under one of the configured allowed roots.

Request:

```json
{
  "repoPath": "/workspace/example"
}
```

Successful response:

```json
{
  "fileCount": 247,
  "hotspots": [
    {
      "file": "UserService.kt",
      "score": 0.0847
    }
  ],
  "reportUrl": "/reports/9d8f1f2a-7d8f-4b2e-9b0f-4c2d8c6c7a10.html"
}
```

The report identifier is randomly generated. The response never exposes the server's absolute filesystem path.

Possible status codes:

- `200 OK`: analysis completed.
- `400 Bad Request`: invalid path, unsupported remote URL, or file-count limit exceeded.
- `429 Too Many Requests`: rate limit exceeded.
- `500 Internal Server Error`: analysis or report generation failed.

### `GET /reports/{id}.html`

Serves a generated report from the `output/` directory. Report retention and authorization are deployment responsibilities; the application currently does not expire reports or associate them with users.

### `POST /ask`

Answers a question using the configured AI provider and repository context.

Request:

```json
{
  "repoPath": "/workspace/example",
  "question": "Where is authentication configured?"
}
```

AI must be enabled in `.codecontext.json`. The configured provider may receive prompt context derived from the repository. Do not enable this feature for confidential source code unless the provider and data handling are approved.

Possible status codes:

- `200 OK`: AI response returned.
- `400 Bad Request`: invalid path, invalid question, or AI disabled.
- `429 Too Many Requests`: rate limit exceeded.
- `502 Bad Gateway`: provider request failed.

### `POST /analyze-org`

Analyzes multiple local repositories with bounded concurrency.

Request:

```json
[
  "/workspace/service-a",
  "/workspace/service-b"
]
```

At most 20 repositories may be submitted per request. Each repository is subject to the configured `maxFilesAnalyze` limit. Results are returned in request order.

## Path security

The server resolves paths with `Path.toRealPath()` and accepts only readable directories that are equal to or descendants of an allowed root.

By default, allowed roots are:

- the current working directory;
- the system temporary directory.

Override them with `CODECONTEXT_ALLOWED_PATHS`, separated by the platform path separator. Configure the smallest possible set of roots.

## Rate limiting

Rate limiting is enabled by default and is configured through:

```json
{
  "rateLimit": {
    "enabled": true,
    "requestsPerMinute": 60,
    "requestsPerHour": 1000
  }
}
```

The server returns `Retry-After`, `X-RateLimit-Limit`, and `X-RateLimit-Remaining` headers where applicable.

## Configuration

The server reads `.codecontext.json` from the current working directory. Important fields include:

```json
{
  "excludePaths": [".git", "build", "node_modules"],
  "maxFilesAnalyze": 5000,
  "enableCache": true,
  "ai": {
    "enabled": false,
    "provider": "gemini",
    "apiKey": "",
    "model": "gemini-2.5-flash"
  }
}
```

Never commit a populated `apiKey` value.

## Kotlin API

The core pipeline can also be used directly from Kotlin:

```kotlin
suspend fun analyzeProject(path: String) {
    val config = ConfigLoader.load()
    val files = RepositoryScanner(config).scan(path)
    val parsed = CodeParallelParser(CacheManager()).parseFiles(files)
    val graph = RobustDependencyGraph()
    graph.build(parsed).getOrThrow()
    graph.analyze().getOrThrow()

    graph.getTopHotspots(10).forEach { (file, score) ->
        println("$file: $score")
    }
}
```

The project is currently distributed as an application rather than a published Maven library.

## Error shape

Errors use a deliberately small public shape:

```json
{
  "error": "Invalid or unsafe repository path"
}
```

Provider and internal failures are sanitized before being returned to clients.
