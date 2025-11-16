#!/usr/bin/env bash
set -euo pipefail

# Move the debug APK to the project root for convenience.
# Adds checks and clearer logging for CI environments.

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
DEST_DIR="."

echo "[move_apk] Looking for ${APK_PATH}"
if [ ! -f "${APK_PATH}" ]; then
  echo "[move_apk] ERROR: APK not found at ${APK_PATH}. Did you run './gradlew :app:assembleDebug'?"
  exit 1
fi

mv "${APK_PATH}" "${DEST_DIR}"
echo "[move_apk] Moved app-debug.apk to ${DEST_DIR}"
