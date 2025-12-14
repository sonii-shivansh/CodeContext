# Contributing to CodeContext

Thank you for your interest in contributing to CodeContext! We welcome contributions from everyone.

## 🛠️ How to Contribute

### 1. Report Bugs
- Check the [Issue Tracker](#) to see if the bug has already been reported.
- If not, create a new issue with a clear description, reproduction steps, and environment details.

### 2. Suggest Features
- We love new ideas! Open an issue with the label `enhancement` to discuss your proposal.

### 3. Submit Pull Requests
1.  **Fork** the repository and clone it locally.
2.  Create a **new branch** for your feature or fix: `git checkout -b feature/amazing-feature`.
3.  **Commit** your changes with descriptive messages.
4.  **Add Tests**: Ensure your changes are covered by tests (`PropertyTest` or `EdgeCaseTest` where applicable).
5.  **Verify**: Run `./gradlew test` to ensure all tests pass.
6.  Push to your fork and submit a **Pull Request**.

## 🧪 Development Setup

Prerequisites:
- JDK 21+
- Git

Build command:
```bash
./gradlew build
```

Run locally:
```bash
./gradlew run --args="analyze ."
```

## 📏 Coding Standards
- We use **Kotlin** (strictly typed).
- Follow standard Kotlin coding conventions.
- Ensure all new files have KDoc headers.

Thank you for helping us make codebases easier to understand! 🚀
