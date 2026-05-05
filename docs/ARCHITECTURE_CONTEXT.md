# Architecture Context For AI Coding Assistants

## High-Level Shape

Corner Call is a Gradle Android project with two application modules and a shared Java source tree:

- `phone`: Android phone application and primary workout UI.
- `wear`: Wear OS companion application and Health Services recorder.
- `shared`: protocol and metric data classes compiled into both apps.

Both apps use the same application id, `com.cornercall.app`, so signed phone and watch builds can communicate over the Wear OS Data Layer.

## Phone Architecture

The phone activity is intentionally split into small superclass layers:

- `MainActivity`: Android lifecycle and startup/shutdown wiring.
- `PhoneWearActivity`: Wear command handling, Data Layer sending, and settings persistence.
- `PhoneWorkoutActivity`: timer, phases, combo selection, speech, bell, and render logic.
- `PhoneLowerPanelsActivity`: lower training panels for combo controls, notes, and punch reference.
- `PhoneTopLayoutActivity`: header, tabs, timer card, controls, heart-rate panel, format panel, and about panel.
- `PhoneViewHelpersActivity`: native view factory helpers, layout params, formatting, and utility methods.
- `PhoneStateActivity`: shared fields, constants, presets, combo data, and the Wear broadcast receiver.

This inheritance chain keeps the original native Android implementation style while avoiding a single very large activity file. Prefer adding new UI sections to the narrowest layer that already owns similar UI. Prefer adding workout behavior to `PhoneWorkoutActivity` and Wear sync behavior to `PhoneWearActivity`.

## Wear Architecture

The Wear app is split by responsibility:

- `MainActivity`: compact watch UI and local button actions.
- `HeartRateService`: foreground Health Services workout recording and Wear Data Layer sync.
- `HeartRateNotification`: foreground notification and notification channel creation.
- `HeartRatePayloadMapper`: writes metric payload arrays into Wear `DataMap` instances.
- `WatchWearListenerService`: receives phone commands and starts the foreground recording service.

Keep Health Services work in `HeartRateService`. Keep notification-only changes in `HeartRateNotification`. Keep payload shape changes in shared classes first, then update `HeartRatePayloadMapper` and phone storage/listeners.

## Data Layer Protocol

Immediate control commands use `MessageClient` paths:

```text
/cornercall/control/start
/cornercall/control/pause
/cornercall/control/resume
/cornercall/control/end
```

Each command carries a `SessionState` JSON payload with:

- `sessionId`
- `origin`
- `action`
- `status`
- `startedAt`
- `workPhase`
- `round`
- `remainingSeconds`

Both sides ignore their own origin to avoid command loops. Both sides mirror the latest state to:

```text
/cornercall/session-state
```

Metric payloads use `DataClient` paths:

```text
/cornercall/hr/{sessionId}/{chunkId}
```

The watch syncs pending samples on pause and completion. The phone deduplicates samples by `session_id + timestamp_ms`, stores raw rows in SQLite, and recalculates session summaries.

## Metric Storage

Phone SQLite tables:

- `workout_sessions`: one row per workout session with summary values.
- `heart_rate_samples`: raw timestamped bpm and calorie samples.

Current summary fields:

- min bpm
- average bpm
- max bpm
- calories
- sample count
- last synced time

## Build Behavior

Use `scripts/build-gradle.ps1` for local and Codex builds. It:

- creates `dist/`
- creates `local.properties` from `%LOCALAPPDATA%\Android\Sdk` when missing
- stops stale Gradle daemons first
- defaults to `--no-daemon`
- uses cache and parallel execution
- streams output to `dist/gradle-build.log`
- copies phone and wear debug APKs into `dist/`

`local.properties`, generated APKs, Gradle caches, and module build folders are intentionally ignored.

## Change Guidelines

- Keep Java files near 250-350 lines when possible.
- Keep protocol changes backward tolerant. Missing arrays or fields should default safely.
- Keep Wear recording active only during active workout time.
- Sync samples on pause and completion.
- Do not store generated APKs or logs in Git.
- Prefer native Android views and Java to match the current codebase.
- Run the cached Gradle build and phone unit tests after protocol, storage, or Wear sync changes.
