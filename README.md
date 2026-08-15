# VFconv

VFconv is a modern Android video converter app with a **liquid glass UI** and an animated gradient background. It uses **Media3 Transformer** (hardware-accelerated) to convert videos between different formats and codecs.

## Features

- 📁 Select video from device using Storage Access Framework (no storage permission required)
- 🎞 Convert to MP4, MKV, or WebM
- 🎥 Choose video codec: H.264, H.265, VP9, AV1
- 📏 Adjust resolution (Original, 1080p, 720p, 480p, 360p)
- ⚡ Set bitrate (Default, 1 Mbps, 2 Mbps, 4 Mbps, 8 Mbps)
- 📊 Real-time progress bar with cancel option
- 🌌 Liquid glass UI with smooth animations
- 🔧 Settings dialog: update, clear cache, choose download folder

## Screenshots

> (Add screenshots here if available)

## How It Works

The app uses **AndroidX Media3 Transformer** to transcode videos. Media3 leverages device hardware codecs, making conversion fast and efficient. No external FFmpeg binaries are required.

## Build

### Requirements

- Android Studio Hedgehog or newer
- Android SDK 35
- JDK 17

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/VFconv.git
