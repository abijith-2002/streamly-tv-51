# Scripts

- copy_apk_outputs.sh
  - Recursively copies the APK output directory to the desired destination.
  - Use this in Docker/CI stages that need to collect build artifacts.
  - Usage: scripts/copy_apk_outputs.sh /app/build/outputs/apk/debug

- move.sh
  - Moves app-debug.apk to the project root with sanity checks.
