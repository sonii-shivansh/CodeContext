# Security Policy

## Supported Versions

We release patches for security vulnerabilities. Currently supported versions:

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security seriously. If you discover a security vulnerability, please follow these steps:

### 1. **Do Not** Open a Public Issue

Please do not create a public GitHub issue for security vulnerabilities.

### 2. Report Privately

Send an email to: **[shivanshsoni568@gmail.com]** with:

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### 3. Response Timeline

- **Initial Response:** Within 48 hours
- **Status Update:** Within 7 days
- **Fix Timeline:** Depends on severity
  - Critical: 1-7 days
  - High: 7-30 days
  - Medium/Low: 30-90 days

### 4. Disclosure Policy

- We will acknowledge your report within 48 hours
- We will provide a more detailed response within 7 days
- We will work with you to understand and resolve the issue
- Once fixed, we will publicly disclose the vulnerability (with credit to you, if desired)

## Security Best Practices

When using CodeContext:

1. **Keep Dependencies Updated:** Regularly update to the latest version
2. **Validate Input:** Be cautious when analyzing untrusted codebases
3. **Review Output:** Check generated reports before sharing publicly
4. **Limit Permissions:** Run with minimal necessary permissions

## Security Features

- No external network calls (except Git operations)
- No data collection or telemetry
- All analysis is local
- No code execution from analyzed files

## Contact

For security concerns: **[shivanshsoni568@gmail.com]**

Thank you for helping keep CodeContext and our users safe!
