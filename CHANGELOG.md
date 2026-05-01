# Changelog

## Unreleased

### Changed

* Improved spacing and internal padding for control buttons, preset timer buttons, and the custom timer action.
* Tightened the preset round format grid with dedicated button sizing and gutters.
* Added repository code ownership so pull requests require approval from vi-code.

### Added

* Added a Gradle Android project with separate phone and Wear OS modules.
* Added a Wear OS companion app that can start, pause, resume, and end workouts from the watch.
* Added Wear Data Layer syncing so phone and watch workout state stay aligned.
* Added watch-side Health Services recording for boxing workouts with heart-rate and calorie data.
* Added phone-side SQLite storage for workout sessions, raw heart-rate samples, calories, and synced summaries.
* Added a compact heart-rate and calorie summary panel to the phone app.
* Added Gradle build helpers for cached, streamed Android/Wear builds and live build-log tailing.
* Added an About tab with author information, a Venmo coffee link, and vi-code.github.io.

### Fixed

* Added SDK discovery support for local builds and changed automation builds to avoid long-lived Gradle daemon hangs.

## 0.1

Initial native Android release.

### Changed

* Replaced the rough native layout with a more polished Android interface.
* Added a status chip, phase progress bar, cleaner timer card, stronger controls, and better custom timer inputs.
* Added a generated boxing launcher icon.
* Updated the APK build so Android resources are packaged directly into the app.

### Kept

* Random boxing combo calls.
* Custom round, rest, pace, and combo length settings.
* Native text to speech.
* Native round bell.
* Local settings storage on device.
