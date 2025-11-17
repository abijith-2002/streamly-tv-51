#!/usr/bin/env bash
set -euo pipefail

# Safe local build approximating CI behavior (no daemon, no build cache)
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

./gradlew clean :app:assembleDebug --no-daemon --no-build-cache --stacktrace --info
