# Corner Call Project Outline

## Purpose

Corner Call is a native Android boxing timer with a companion Wear OS app. The phone app runs the round timer, voice coach, combo engine, and workout summary UI. The watch app controls the same workout and records heart-rate plus calorie data during active workout time.

## Repository Layout

```text
.
|-- phone/                  Android phone app module
|-- wear/                   Wear OS companion app module
|-- shared/                 Java classes shared by phone and wear modules
|-- scripts/                Build and log-tail helpers
|-- dist/                   Generated APK outputs, ignored except legacy tracked artifacts
|-- docs/                   Project outline and architecture context
|-- build.gradle            Root Gradle build
|-- settings.gradle         Gradle module list
|-- gradle.properties       Gradle cache, daemon, and AndroidX settings
```

## Main Modules

### phone

The phone module owns the primary workout experience:

- Native Android views for the timer, controls, settings, and summary panels.
- Text-to-speech combo calling and round bell audio.
- Workout timer state and combo generation.
- Wear Data Layer command sending and receiving.
- SQLite storage for workout sessions and raw metric samples.

### wear

The Wear OS module owns the companion watch experience:

- Compact watch UI for workout state, heart rate, calories, and sync status.
- Start, pause, resume, and end controls.
- Foreground service for Health Services exercise recording.
- Heart-rate and calorie sample batching and sync.

### shared

The shared source set contains plain Java protocol/data classes used by both modules:

- `WearPaths` for all Data Layer paths, statuses, and origins.
- `SessionState` for control messages and mirrored session state.
- `HeartRatePayload`, `HeartRateSample`, `HeartRateSummary`, and `HeartRateStats` for metric sync and summary calculation.

## Build Commands

Normal cached build:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-gradle.ps1
```

One-time clean build:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-gradle.ps1 -Clean
```

Live log tail:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/tail-gradle-log.ps1
```

Phone unit tests:

```powershell
.\gradlew.bat :phone:testDebugUnitTest --no-daemon --build-cache --info --console=plain
```

## Generated Outputs

The build helper writes:

```text
dist/cornercall-phone-debug.apk
dist/cornercall-wear-debug.apk
dist/gradle-build.log
```

These are local build artifacts and should not be committed.
