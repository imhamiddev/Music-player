# Music Player

A modern, premium Android music player built with Jetpack Compose and Media3.

## Features

- Light, airy design with soft cards, a green gradient accent and a dark player deck
- Spring-based animations and micro-interactions (press feedback, sliding tab pill, bouncing heart, live equalizer, waveform scrubber)
- Library tabs: **All**, **Folders**, **Favorite**, **Recent**
- Sort by **alphabet** or **date added** (tap again to reverse)
- Instant search across title, artist and album with a live result counter
- Audio only: video files are never listed
- Background playback with a media notification and lock-screen controls (Media3 MediaSession)
- English UI, light mode only

## Download

Grab the latest APK from the [Releases](../../releases) page.

## Build

```bash
./gradlew assembleRelease
```

Requires JDK 17 and the Android SDK (compileSdk 35). Min SDK 26.

CI builds every push and pull request; pushing a tag like `v1.0.0` publishes a release with the APK attached.
