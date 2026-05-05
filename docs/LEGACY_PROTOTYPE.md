# Legacy Prototype

Corner Call started as a small hand-built prototype before it became a standard Gradle Android project.

The legacy prototype included:

- a root `index.html` browser entry point
- root `src/` JavaScript and CSS files for the first interactive timer
- a hand-built `android/` folder and `scripts/build-apk.ps1` APK packaging script
- checked-in APK files under `dist/`

Those files are no longer active source. The production code now lives in the Gradle modules:

- `phone/` for the Android phone app
- `wear/` for the Wear OS companion app
- `shared/` for Java protocol and metric classes used by both apps

Generated APKs are local artifacts. Build them with `scripts/build-gradle.ps1`, distribute debug builds from `dist/` only when needed, and use CI artifacts, GitHub Releases, or Play/App distribution for release builds.
