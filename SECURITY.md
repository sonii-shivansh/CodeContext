# Security Policy

## Supported versions

| Version | Supported |
| --- | --- |
| `0.2.x` | Yes |
| `0.1.x` | Limited; upgrade recommended |

## Reporting a vulnerability

Do not disclose security vulnerabilities in a public issue. Report them privately to **shivanshsoni568@gmail.com** with:

- a concise description;
- affected version, commit, or deployment mode;
- reproduction steps or a proof of concept;
- security impact and possible mitigations;
- any relevant logs with secrets removed.

Please do not include API keys, credentials, private source code, or personal data unless absolutely necessary.

The project aims to acknowledge reports within 48 hours and provide an initial assessment within seven days. Fix and disclosure timelines depend on severity, reproducibility, and release risk.

## Security model

CodeContext is designed primarily for local analysis:

- analyzed source files are parsed and never executed;
- Git operations are read-only from the application's perspective;
- server paths are canonicalized and restricted to allowed roots;
- configurable file limits and rate limiting reduce resource abuse;
- generated reports use random identifiers rather than source directory names;
- provider credentials are sent in headers and are not intentionally logged;
- AI features are disabled by default and may send repository-derived context to an external provider when enabled.

The REST server does **not** provide authentication, tenant isolation, report authorization, TLS termination, report expiration, or a complete public-internet deployment boundary. Add those controls before exposing it outside a trusted local or internal network.

## Deployment guidance

- Bind the server to `127.0.0.1` for local use.
- Configure `CODECONTEXT_ALLOWED_PATHS` to the smallest required set of directories.
- Keep `.codecontext.json` and API keys out of source control.
- Place the server behind authentication, TLS, request quotas, and a trusted-origin policy when deployed remotely.
- Review reports before sharing them because they may contain file names, paths, Git authors, commit messages, and source-derived descriptions.
- Treat generated HTML and external visualization assets as part of the deployment supply chain.
- Keep dependencies and the JDK patched.

## Contact

Security reports: **shivanshsoni568@gmail.com**
