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

# Build for Android with GPL enabled (includes libx264, libx265, etc.)
./nix-android.sh -p "$PROFILE" --enable-gpl

# Find the generated AAR (any .aar containing "gpl" and "android")
AAR=$(find . -name "*.aar" | grep -i "gpl" | grep -i "android" | head -1)
if [ -z "$AAR" ]; then
  echo "No GPL Android AAR found!"
  exit 1
fi

# Create app/libs if not exists and copy the AAR
mkdir -p ../app/libs
cp "$AAR" ../app/libs/ffmpeg-kit-next-android-gpl-8.1.1.aar

echo "AAR copied to ../app/libs/ffmpeg-kit-next-android-gpl-8.1.1.aar"
