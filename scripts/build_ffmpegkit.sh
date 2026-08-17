#!/bin/bash
set -e

# Install Nix (single-user, no sudo needed on GitHub runner)
curl -L https://nixos.org/nix/install | sh
. /home/runner/.nix-profile/etc/profile.d/nix.sh

# Clone FFmpegKitNext at version 8.1.1
git clone --depth 1 --branch v8.1.1 https://github.com/arthenica/ffmpeg-kit-next.git
cd ffmpeg-kit-next

# Build for Android with GPL enabled (includes libx264, libx265, etc.)
./nix-android.sh --enable-gpl

# Copy the generated AAR to the app's libs folder
cp android/build/outputs/aar/ffmpeg-kit-next-android-gpl-*.aar ../app/libs/ffmpeg-kit-next-android-gpl-8.1.1.aar
