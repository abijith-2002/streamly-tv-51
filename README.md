# Project Repository

This is the initial README file for the project.

## Build Stability Notes (CI/Docker)

- The project is configured to run Gradle without the daemon and with conservative worker settings to improve reliability in containerized CI.
- If you invoke Gradle manually, prefer:
  ./gradlew clean assembleDebug --no-daemon --stacktrace

- Key configuration already applied:
  - org.gradle.daemon=false
  - org.gradle.workers.max=2
  - org.gradle.parallel=false
  - org.gradle.caching=false
  - org.gradle.jvmargs includes -Dsun.zip.disableMemoryMapping=true to avoid packaging errors in constrained environments.