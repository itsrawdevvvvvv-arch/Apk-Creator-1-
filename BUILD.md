# Building `raw_edit` v2 (Debug APK)

## 1. Prerequisites
- **JDK**: Java 17 (`JDK 17`)
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 28` (Android 9 Pie)
- **Gradle**: Gradle `8.13` (configured in `gradle/wrapper/gradle-wrapper.properties`)

## 2. Pinned Toolchain (`gradle/libs.versions.toml`)
- `agp = "8.13.2"`
- `kotlin = "2.2.21"`
- `ksp = "2.2.21-2.0.5"`
- `composeBom = "2026.06.01"`
- `navigationCompose = "2.9.8"`
- `lifecycle = "2.10.0"`
- `room = "2.8.5"`
- `datastore = "1.3.0"`
- `media3 = "1.11.1"`

## 3. Build Command
From the `raw_edit/` project root directory:

```bash
./gradlew assembleDebug
```

*(Note: Because this archive strictly excludes binary `.jar` assets per the zero-binary deliverable protocol, if `gradle/wrapper/gradle-wrapper.jar` is not present locally, either open the project in Android Studio or run `gradle wrapper --gradle-version 8.13` once with a local Gradle installation, and `./gradlew assembleDebug` will automatically use it.)*

## 4. Output Location
The compiled, installable debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on any connected Android 9+ (API 28+) device via:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
