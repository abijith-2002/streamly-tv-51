#!/usr/bin/env bash
set -euo pipefail

# Purpose:
# - Perform a thorough clean to remove any corrupted incremental caches that can cause
#   :app:mergeDebugJavaResource NoSuchFileException (e.g., missing zip-cache entries).
# - Enforce non-daemon, no configuration cache, no build cache build in Docker/CI.
# - Build the debug APK and leave artifacts in standard outputs.

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "[ci] Removing module build dirs..."
rm -rf app/build || true

echo "[ci] Removing project .gradle caches (project-level)..."
rm -rf .gradle || true

# Optionally clear Gradle user home caches if running in CI agent/container with writable GRADLE_USER_HOME
if [[ -n "${GRADLE_USER_HOME:-}" && -d "${GRADLE_USER_HOME}" ]]; then
  echo "[ci] Cleaning GRADLE_USER_HOME caches selectively..."
  # Remove transforms and incremental directories that can cause stale state
  rm -rf "${GRADLE_USER_HOME}/caches/transforms-"* || true
  rm -rf "${GRADLE_USER_HOME}/caches/jars-"* || true
  rm -rf "${GRADLE_USER_HOME}/caches/build-cache-"* || true
fi

echo "[ci] Running Gradle clean (no daemon, no config cache)..."
./gradlew clean --no-daemon --no-build-cache --stacktrace --info

echo "[ci] Building :app:assembleDebug (no daemon, no config cache, no build cache)..."
./gradlew :app:assembleDebug --no-daemon --no-build-cache --stacktrace --info

echo "[ci] Build completed. APK should be under app/build/outputs/apk/debug/"
