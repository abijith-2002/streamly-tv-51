#!/usr/bin/env bash
set -euo pipefail

# Purpose:
# Recursively copy the APK output directory to a specified destination (e.g., /app/build/outputs/apk/debug)
# Fixes failures where a plain `cp` without -r was used for directories.

# Usage:
#   scripts/copy_apk_outputs.sh [DEST_DIR]
# Defaults:
#   DEST_DIR=/app/build/outputs/apk/debug

SRC_DIR="app/build/outputs/apk/debug"
DEST_DIR="${1:-/app/build/outputs/apk/debug}"

echo "[copy_apk_outputs] Source: ${SRC_DIR}"
echo "[copy_apk_outputs] Destination: ${DEST_DIR}"

# Ensure source exists
if [ ! -d "${SRC_DIR}" ]; then
  echo "[copy_apk_outputs] ERROR: Source directory '${SRC_DIR}' does not exist. Build the APK first (e.g., ./gradlew :app:assembleDebug)."
  exit 1
fi

# Ensure destination exists
mkdir -p "${DEST_DIR}"

# Prefer rsync if available for robust directory sync; fallback to cp -r
if command -v rsync >/dev/null 2>&1; then
  # -a archive (preserve times, perms), -r recursive, --delete optional if we want clean sync (omitted here)
  rsync -a "${SRC_DIR}/" "${DEST_DIR}/"
else
  cp -r "${SRC_DIR}/" "${DEST_DIR}/"
fi

echo "[copy_apk_outputs] Copy completed successfully."
