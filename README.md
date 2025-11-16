# Project Repository

This is the initial README file for the project.

## Build Stability Notes (CI/Docker)

- The project is configured to run Gradle without the daemon and with conservative worker settings to improve reliability in containerized CI.
- If you invoke Gradle manually, prefer:
  ./gradlew clean :app:assembleDebug --no-daemon --stacktrace --info

- Key configuration already applied:
  - org.gradle.daemon=false
  - org.gradle.workers.max=2
  - org.gradle.parallel=false
  - org.gradle.caching=false
  - org.gradle.jvmargs includes -Dsun.zip.disableMemoryMapping=true to avoid packaging errors in constrained environments.

- Resource merge troubleshooting:
  - Ensure all resources are under app/src/main/res with valid folders only (drawable/, layout/, mipmap-*, values/, etc.).
  - Do not add empty productFlavor or buildType res directories; they can cause merge to expect missing values_*.arsc.flat.
  - Avoid invalid qualifiers like values-b+sr+Latn or malformed folder names.
  - If merge fails, re-run with: ./gradlew :app:mergeDebugResources --no-daemon --stacktrace --info
  - Clean intermediates: ./gradlew clean before rebuilding to force aapt2 to recompile resources.