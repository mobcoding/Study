# AAB Tool GUI 3.0

New Swing-based AAB installer GUI that wraps official `bundletool` and local `adb`.

## Features

- Uses official `bundletool-all-1.18.3.jar`
- Supports `connected-device` and `universal` build modes
- Supports both GUI and CLI workflows
- Supports dragging a `.aab` file directly into the GUI `AAB file` input
- Supports dropdown selection of online Android devices in the GUI `Device ID` field
- Streams full `bundletool`, `adb`, and `keytool` logs into the GUI
- Auto-generates a debug keystore when signing fields are blank
- Auto-detects the newest local Android SDK `aapt2` when available
- Auto-launches the app after a successful install by default
- Provides a bottom "卸载重装" action that removes the current AAB's installed package and data before reinstalling it
- Runs legacy-style checks after `build-apks` by default
- Reuses a validated local APK-set cache for the same AAB, device configuration, signing key and toolchain; this is enabled by default in Advanced options
- Reports Chinese strings, AdMob IDs, `Log` usage, StringFog markers, and rough obfuscation heuristics
- Surfaces common failure hints for malformed AABs, signature mismatches, and multi-device cases

## Build

```powershell
cd E:\GitHub\Study\tools\aabtool-gui
.\build.ps1
```

## Output

- App jar: `dist\aabtool-gui-3.0.jar`
- bundletool runtime: `dist\lib\bundletool-all-1.18.3.jar`
- Windows launcher: `dist\launch-aabtool-gui.bat`
- CLI launcher: `dist\launch-aabtool-cli.bat`

## CLI

```powershell
java -jar dist\aabtool-gui-3.0.jar `
  --aab E:\GitHub\Study\app\build\outputs\aabresguard\release\app_build.aab `
  --output E:\GitHub\Study\app\build\outputs\aabresguard\release\app_build-device.apks
```

Common options:

- `--mode connected-device|universal`
- `--no-install`
- `--no-analysis`
- `--no-reuse-apks`
- `--no-launch`
- `--replace-incompatible`
- `--device-id <serial>`
- `--ks <keystore> --ks-pass <password> --key-alias <alias> --key-pass <password>`
- `--aapt2 <path>`

## Notes

- If logs show `ProtoDeserialize.cpp` or `unknown compound value`, the AAB itself is malformed and should be compared against the plain bundle output before post-processing.
- Auto launch resolves the package and `launchable-activity` from the generated APK set with `aapt2 dump badging`, then starts it with `adb shell am start`, and falls back to `monkey` when needed.
- `--replace-incompatible` or the GUI checkbox will uninstall the existing app and retry once when `INSTALL_FAILED_UPDATE_INCOMPATIBLE` is detected.
- Legacy checks are best-effort heuristics. StringFog may be reported either from APK markers or, when running inside a source checkout, from nearby Gradle wiring.
- Leave keystore fields blank if you only need a temporary debug-signed install.
- Cached APK sets are stored under `~/.aabtool-gui/cache/apks`. A cache hit skips both `build-apks` and the repeated static inspection; a device-configuration mismatch automatically invalidates and rebuilds the entry once.
- On macOS, `adb` is commonly located at `~/Library/Android/sdk/platform-tools/adb`.
- The GUI now auto-detects `adb` from `ANDROID_SDK_ROOT`, `ANDROID_HOME`, `local.properties`, common SDK locations, shell PATH, or PATH inherited by the process, and passes that `adb` path into bundletool on macOS too.
