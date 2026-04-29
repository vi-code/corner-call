# Corner Call

Corner Call is a simple boxing timer I built for bag work. Pick a round format, hit Start, and the app calls out fresh combinations so you can keep moving without looking at a screen.

The first version is intentionally small. Everything runs on the device, preferences stay in the browser, and the Android build wraps the same app in a lightweight WebView.

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

## Run The Web App

```bash
npm start
```

Then open:

```text
http://127.0.0.1:5173/
```

## Build The APK

```bash
npm run build:apk
```

The debug APK lands here:

```text
dist/cornercall0.1.apk
```

## Notes

This is an MVP, but the basics are there: random combo calls, round and rest timing, voice output, a round bell, local settings, and a packaged Android APK.
