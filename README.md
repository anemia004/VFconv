# VFconv

**VFconv** is an Android video converter app built with **FFmpegKitNext** and simple Android UI.

**IMPORTANT:** This project is intended **solely for personal, testing, and hobby purposes**.  


## Features

- **Select video** from device storage (no storage permission required – uses Storage Access Framework)
- **Choose output format** (MP4, MKV, WebM) with a liquid‑glass tab bar
- **Select codec**:
  - Copy (Fastest) – remux without re‑encoding
  - H.264 (CPU / hardware)
  - H.265 (CPU / hardware)
  - VP9 (CPU)
- **Resolution** scaling (Original, 1080p, 720p, 480p, 360p)
- **Bitrate** selection
- **Hardware acceleration toggle** (MediaCodec on/off)
- **Progress bar** with live conversion progress
- **Cancel** conversion
- **Custom output folder** via settings
- **Glassy UI** with moving background and liquid‑glass components

## Build

### Requirements
- Android Studio or Gradle 8.7+
- JDK 17
- Android SDK (API 35)

### Dependencies
- FFmpegKitNext AAR (MediaCodec enabled) – see [FFmpegKitNext release](https://github.com/anemia004/ffmpeg-kit-next/releases)
- `smart-exception-java` for FFmpegKitNext

### Build steps
1. Clone this repository.
2. Place the FFmpegKitNext AAR in `app/libs/` (or let Gradle auto‑download it).
3. Run `gradle assembleRelease` (or use the provided GitHub Actions workflow).

## Usage

1. Launch the app.
2. Tap **Select Video**.
3. Choose output format (MP4, MKV, WebM) from the pill tab bar.
4. Choose codec, resolution, and bitrate.
5. Optionally enable hardware acceleration in **Settings**.
6. Tap **Convert**.
7. If no output folder is set, choose where to save; otherwise the file is saved to the preset folder.

## License

This project is licensed under the **GNU General Public License v3.0 (GPL v3.0)**.  
You may obtain a copy of the license at [https://www.gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html).

### Third‑Party Licenses
- **FFmpegKitNext** – GPL v3.0 (with x264, x265)
- **smart‑exception‑java** – Apache 2.0
- **AndroidX libraries** – Apache 2.0

## Important Notice

Some codecs (`x264`, `x265`) may be subject to software patents in certain countries.  
- **OpenH264 is not included**, so no MPEG LA licensing fees apply.

**You are solely responsible for determining the appropriateness of using or redistributing this software and assume any risks associated with your exercise of permissions under this license.** Consult a legal professional if you distribute globally.

## Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
