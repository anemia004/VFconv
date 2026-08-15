# VFconv

VFconv is an Android app for converting video formats using FFmpeg, featuring a modern liquid glass UI with an animated gradient background.

## Features
- Select video from device (SAF, no storage permission)
- Choose output format: MP4, MKV, WebM
- Select video codec: H.264, H.265, VP9, AV1
- Adjust resolution and bitrate
- Progress bar with cancellation
- Glassmorphism UI with custom dropdowns

## Build
1. Clone the repository.
2. Open in Android Studio.
3. For release builds, set up a keystore and configure GitHub Actions.
4. Sync Gradle and build.

## Dependencies
- FFmpegKit (full package)
- Kotlin Coroutines
- AndroidX
- RealtimeBlurView

## License
This project uses FFmpeg under LGPL/GPL. Ensure compliance when distributing.
