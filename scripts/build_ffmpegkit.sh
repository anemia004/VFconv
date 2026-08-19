#!/bin/bash
set -e

# Install Nix (single-user, no sudo needed on GitHub runner)
curl -L https://nixos.org/nix/install | sh
. /home/runner/.nix-profile/etc/profile.d/nix.sh

# Clone FFmpegKitNext at version 8.1.1
git clone --depth 1 --branch v8.1.1 https://github.com/arthenica/ffmpeg-kit-next.git
cd ffmpeg-kit-next

# List available Nix profiles and select one containing "android"
./nix-android.sh --list-profiles > profiles.txt
PROFILE=$(grep -i "android" profiles.txt | head -1 | awk '{print $1}')
if [ -z "$PROFILE" ]; then
  echo "No Android profile found!"
  exit 1
fi
echo "Using profile: $PROFILE"

# Build with GPL and essential codecs, for arm-v7a, arm-v7a-neon, arm64-v8a
./nix-android.sh -p "$PROFILE" \
  --enable-gpl \
  --enable-x264 \
  --enable-x265 \
  --enable-libvpx \
  --enable-opus \
  --disable-x86 \
  --disable-x86-64

# Find the generated AAR (any .aar file; there will be exactly one)
AAR=$(find . -name "*.aar" | head -1)
if [ -z "$AAR" ]; then
  echo "No AAR file found!"
  exit 1
fi

# Copy the AAR to app/libs
mkdir -p ../app/libs
cp "$AAR" ../app/libs/ffmpeg-kit-next-android-gpl-8.1.1.aar

echo "AAR copied to ../app/libs/ffmpeg-kit-next-android-gpl-8.1.1.aar"
