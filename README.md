# VFconv

VFconv is an Android video converter app built with FFmpegKitNext.

## Features

- Select video from device storage (no storage permission required – uses Storage Access Framework)
- Choose output format (MP4, MKV, WebM) with a liquid‑glass tab bar
- Select codec:
  - Copy (Fastest) – remux without re‑encoding
  - H.264 (CPU / hardware)
  - H.265 (CPU / hardware)
  - VP9 (CPU)
- Resolution scaling (Original, 1080p, 720p, 480p, 360p)
- Bitrate selection
- Hardware acceleration toggle (MediaCodec on/off)
- Progress bar with live conversion progress
- Cancel conversion
- Custom output folder via settings
- Glassy UI with moving background and liquid‑glass components

## Build

### Requirements
- Android Studio or Gradle 8.7+
- JDK 17
- Android SDK (API 35)

### Dependencies
- FFmpegKitNext (MediaCodec enabled)
- smart-exception-java for FFmpegKitNext

### Build steps
1. Clone this repository.
2. Place the FFmpegKitNext AAR in `app/libs/` or let Gradle auto‑download it.
3. Run `gradle assembleRelease` (or use the provided GitHub Actions workflow).

## Third-Party Components

This app uses **FFmpegKitNext**, whose source is available at:  
[https://github.com/arthenica/ffmpeg-kit-next](https://github.com/arthenica/ffmpeg-kit-next)

FFmpegKitNext is licensed under GPL v3.0.

## License

This project is licensed under the **GNU General Public License v3.0 (GPL v3.0)**.  
You may obtain a copy of the license at [https://www.gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html).

## Patent Notice

**This is a test project intended for personal and hobby use only.**  
If you are planning to clone or use this project, please be aware that:
This project includes video codecs (x264, x265) that may be covered by patents in certain countries.  
Commercial use of these codecs may require separate patent licenses from the respective patent holders.  
OpenH264 is not included.

**You are solely responsible for determining whether your use requires patent licenses or royalties, and for obtaining any necessary licenses.** Consult a qualified legal professional if you distribute globally.

## Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
