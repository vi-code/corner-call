# Corner Call

Corner Call is a native Android boxing timer for bag work. Pick a round format, hit Start, and the app calls out fresh combinations so you can keep moving without looking at a screen.

The phone app is paired with a companion Wear OS app. The phone runs the boxing timer and voice coach, while the watch can control the same session and record heart rate plus calorie totals during active workout time.

The active source of truth is the Gradle project: `phone/`, `wear/`, and `shared/`. Older prototype files were archived out of the active tree; see `docs/LEGACY_PROTOTYPE.md` for context.

## What It Does

The app can run common boxing sessions like 5 rounds of 3 minutes, 12 rounds of 2 minutes, or 3 rounds of 5 minutes. You can also set your own round count, work time, rest time, call pace, and combo length.

Combos use the standard orthodox punch numbers:

```text
1 jab
2 cross
3 lead hook
4 rear hook
5 lead uppercut
6 rear uppercut
```

It can also mix in simple defensive calls like duck, slip, and roll.

## Wear OS Companion

The Wear OS app uses Android Health Services with `ExerciseType.BOXING`. When the workout is active, the watch foreground service subscribes to `HEART_RATE_BPM` and `CALORIES_TOTAL`. The watch pauses Health Services recording when the workout is paused, resumes it when training resumes, and ends it when the workout completes.

The watch UI shows the current session state, latest heart rate, calorie total, and sync status. It can start, pause, resume, and end the workout. The phone can do the same, and both apps mirror the latest state so reconnects can recover.

## How The Connection Works

Phone and watch communicate through the Wear OS Data Layer using the shared package `com.cornercall.app`.

Immediate workout controls use `MessageClient`:

```text
/cornercall/control/start
/cornercall/control/pause
/cornercall/control/resume
/cornercall/control/end
```

Each control message carries a shared `SessionState` payload with the session id, origin, status, start time, current phase, round, and remaining seconds. The receiver ignores messages from its own origin, applies the timer state, and mirrors the latest state to a Data Layer item at `/cornercall/session-state`.

Heart-rate and calorie sync uses `DataClient` items:

```text
/cornercall/hr/{sessionId}/{chunkId}
```

The watch batches pending samples whenever the workout is paused or completed. Each sample includes a timestamp, bpm value, and cumulative calories. Payloads are chunked before they get large, marked as pause or complete events, and sent urgently. The phone stores raw samples in SQLite and deduplicates them by `sessionId + timestamp_ms`, then updates a summary with min bpm, average bpm, max bpm, sample count, calories, and last synced time.

## Build The APKs

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-gradle.ps1
```

The APKs land here:

```text
dist/cornercall-phone-debug.apk
dist/cornercall-wear-debug.apk
```

The Wear OS APK is a separate artifact; it is not bundled inside the phone debug APK in this v1 workflow. Generated APKs should not be committed to GitHub. Keep debug builds local under `dist/`, or distribute builds through CI artifacts, GitHub Releases, or Play/App distribution for release workflows.

For live build progress in another terminal:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/tail-gradle-log.ps1
```

## Notes

This is a native Android and Wear OS project. It has native controls, random combo calls, round and rest timing, voice output, a round bell, local settings, watch controls, and local workout metric storage.
