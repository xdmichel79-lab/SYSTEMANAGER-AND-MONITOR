# SYSTEM MANAGER & MONITOR — GitHub Actions preparation

## What this package contains
- Android project baseline from Phase 7 (Boot Manager / permissions).
- GitHub Actions workflow at `.github/workflows/android-build.yml`.
- Phase 8 ADB Control Center source preserved under `integration-reference/phase8-adb-control-center/` for later integration.

## Important scope
This is a CI-prepared project baseline, not a completed integration of every planned module and not a prebuilt APK. Phase 8 is intentionally kept outside `app/src/main` because its Shizuku dependency/provider and UserService/AIDL integration still need to be wired and tested. Do not move it into the app source tree yet.

## Build pipeline
The workflow runs on pushes to `main`, `master`, or `develop`, pull requests, and manual dispatch. It uses JDK 17, Gradle 9.6.0, Android SDK platform 37 and Build Tools 36.0.0; runs `testDebugUnitTest` and `assembleDebug`; then uploads the debug APK as a workflow artifact.

The root Android Gradle Plugin was aligned to 9.3.1 because Kotlin Gradle Plugin 2.4.21 documents compatibility through AGP 9.3.1. The project targets compileSdk 37. Review dependency/plugin versions again if you change the Kotlin or AGP versions.

## Upload to GitHub (manual; do not connect AI Studio)
1. Extract this ZIP. Open the `SYSTEM-MANAGER-AND-MONITOR` folder inside it.
2. In your GitHub repository `SYSTEM-MANAGER-AND-MONITOR`, choose **Add file → Upload files**.
3. Upload the *contents* of that folder, including the hidden `.github` folder. Do not upload the outer ZIP as a single file.
4. Commit to `main`.
5. Open **Actions** in the repository and select **Android CI**.
6. After it succeeds, open the run and download `system-manager-monitor-debug-apk` from **Artifacts**.

## Wrapper status
The supplied project has `gradle-wrapper.properties` but does not contain `gradlew`, `gradlew.bat`, or `gradle-wrapper.jar`. The GitHub workflow therefore installs Gradle 9.6.0 and invokes `gradle` directly. This is sufficient for this CI workflow; generate and commit a complete Gradle Wrapper later if you want consistent local command-line builds.

## Local limitations
No successful build is claimed by this package. GitHub Actions is the next real build validation. Fix any compile/test errors shown in the Actions log before treating the APK as verified.
