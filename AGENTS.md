# Repository Guidelines

## Project Structure & Module Organization

`app/` is the Android application module. Kotlin and Compose sources live under `app/src/main/java/com/eltavine/duckdetector/`: shared infrastructure is in `core/`, detectors in `features/`, and the application shell in `ui/`. Feature packages normally separate `data`, `domain`, `presentation`, and `ui` responsibilities. Native C++, JNI, and assembly live under `app/src/main/cpp/` and build into `libduckdetector.so`.

Resources and assets are in `app/src/main/res/` and `app/src/main/assets/`. JVM tests mirror production packages in `app/src/test/`; device tests belong in `app/src/androidTest/`. `build-logic/` contains convention plugins, `gradle/` contains the wrapper and version catalog, and `docs/adb-cli.md` documents the CLI.

## Build, Test, and Development Commands

- `./gradlew :app:assembleDebug` (`gradlew.bat` on Windows): build the debug APK and all supported native ABIs.
- `./gradlew :app:testDebugUnitTest`: run the JVM unit suite.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug`: standard pre-PR validation.
- `./gradlew :app:externalNativeBuildDebug`: validate JNI/C++ changes directly.
- `./gradlew :app:connectedDebugAndroidTest`: run instrumentation tests on a connected device.

Use SDK 37, Build Tools 37.0.0, JDK 17, NDK 30.0.16138531, and CMake 4.1.2.

## Coding Style & Naming Conventions

Follow Kotlin official style with four-space indentation. Use `PascalCase` for types, `camelCase` for functions and properties, and `snake_case` for Android resources. Keep probes focused: collection belongs in `data`, verdict models in `domain`, and display mapping in `presentation`. JNI payload changes must update the Kotlin parser and remain structured and auditable. Explain non-obvious compatibility or security reasoning, not self-evident code.

Preserve the fork's ADB CLI and runtime localization. New user-visible English detector text requires a matching `values-zh-rCN` translation and catalog entry.

## Testing Guidelines

Tests use JUnit 4; device tests use AndroidX Test and Espresso. Name test classes `*Test` and describe behavior in test methods. New probes need result/parser coverage; reducer or mapper changes need regression tests. Native, KeyStore, Binder, SELinux, `/proc`, mount, and cgroup behavior also requires device verification, or an explicit gap in the PR.

## Commit & Pull Request Guidelines

Use focused Conventional Commit subjects such as `feat(nativeroot): add probe` or `fix(tee): preserve certificate state`. Keep subjects imperative and avoid `update`, `misc`, or unrelated formatting. PRs should explain intent, affected evidence semantics, compatibility risk, and exact validation commands. Link relevant issues; include screenshots for UI changes and device/API/ABI details for platform-sensitive changes.

## Security & Configuration

Never commit keystores, passwords, private keys, tokens, or `local.properties`. Release signing uses the `ANDROID_KEYSTORE_*` environment variables; without the complete set, release builds use the debug key and are not official artifacts.
