#!/usr/bin/env bash
set -euo pipefail

DIST_DIR="${1:?usage: prepare-runtime.sh <distribution-directory>}"
APP_HOME="$(cd "$DIST_DIR" && pwd)"
JDK_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")") }"
JDEPS="$JDK_HOME/bin/jdeps"
JLINK="$JDK_HOME/bin/jlink"

if [[ ! -x "$JDEPS" || ! -x "$JLINK" ]]; then
  echo "ERROR: a JDK with jdeps and jlink is required." >&2
  exit 1
fi

rm -rf "$APP_HOME/jre"
mapfile -t JARS < <(find "$APP_HOME/lib" -type f -name '*.jar' | sort)
if [[ ${#JARS[@]} -eq 0 ]]; then
  echo "ERROR: no application jars found under $APP_HOME/lib" >&2
  exit 1
fi

MODULES="$($JDEPS --multi-release 21 --ignore-missing-deps --print-module-deps --recursive "${JARS[@]}" | tail -n 1 | tr -d '[:space:]')"
if [[ -z "$MODULES" ]]; then
  echo "ERROR: jdeps returned no runtime modules." >&2
  exit 1
fi

# HTTPS and modern TLS providers are used by CodeContext's Gemini client.
MODULES="$MODULES,jdk.crypto.ec,java.net.http"

"$JLINK" \
  --add-modules "$MODULES" \
  --bind-services \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --output "$APP_HOME/jre"

SCRIPT="$APP_HOME/bin/codecontext"
if [[ ! -f "$SCRIPT" ]]; then
  echo "ERROR: launcher not found: $SCRIPT" >&2
  exit 1
fi

if ! grep -q 'CODECONTEXT_BUNDLED_JAVA' "$SCRIPT"; then
  sed -i '/^# Add default JVM options here/i\\# Prefer the runtime bundled with this distribution.\nif [ -x "$APP_HOME/jre/bin/java" ]; then\n    JAVA_HOME="$APP_HOME/jre"\n    export JAVA_HOME\nfi\n# CODECONTEXT_BUNDLED_JAVA' "$SCRIPT"
fi

chmod +x "$SCRIPT"
"$APP_HOME/jre/bin/java" -version
"$SCRIPT" --version
