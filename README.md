# Corner Call

Corner Call is a native Android boxing timer for bag work. Pick a round format, hit Start, and the app calls out fresh combinations so you can keep moving without looking at a screen.

Everything runs on the phone. The app uses Android views, Android text to speech, Android audio, and local device storage for settings.

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

## Build The APK

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-apk.ps1
```

The APK lands here:

```text
dist/cornercall0.1.apk
```

## Notes

This is version 0.1. It has native controls, random combo calls, round and rest timing, voice output, a round bell, and local settings.
