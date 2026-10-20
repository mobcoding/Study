# AAB Tool GUI 5.1

New Swing-based AAB installer GUI that wraps official `bundletool` and local `adb`.

## Features

- Uses official `bundletool-all-1.18.3.jar`
- Supports `connected-device` and `universal` build modes
- Supports both GUI and CLI workflows
- Uses FlatLaf light/dark themes selected once at startup from the Windows default app mode or macOS appearance (`/usr/bin/defaults read -g AppleInterfaceStyle`). Restart the tool after changing the system theme; there is no background theme polling. On macOS, a missing appearance preference means light mode. Unsupported platforms and detection failures fall back to light mode.
- Advanced options include System / Light / Dark theme selection. Manual changes apply immediately and are saved for the next launch. System mode uses the theme detected at startup.
- Supports dragging a `.aab` file directly into the GUI `AAB file` input
- Supports dropdown selection of online Android devices in the GUI `Device ID` field
- Streams full `bundletool`, `adb`, and `keytool` logs into the GUI
- Auto-generates a debug keystore when signing fields are blank
- Auto-detects the newest local Android SDK `aapt2` when available
- Auto-launches the app after a successful install by default
- Provides a bottom "一键卸载" action that removes the current AAB's installed package and data without rebuilding an APK set
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

- App jar: `dist\aabtool-gui-5.1.jar`
- bundletool and FlatLaf runtimes: included in the standalone app JAR
- Windows launcher: `dist\launch-aabtool-gui.bat`
- CLI launcher: `dist\launch-aabtool-cli.bat`

Build dependencies are bundled in `vendor/`: bundletool 1.18.3 and FlatLaf 3.6.1 (Apache-2.0).
The build preserves existing files in `dist/` and stops if compilation or packaging fails.

Theme checks (after building):

```powershell
javac --release 17 -encoding UTF-8 -cp dist\aabtool-gui-5.1.jar -d build\test-classes src\test\java\com\codex\aabtoolgui\AppThemeTest.java
java -Djava.awt.headless=true -cp "dist\aabtool-gui-5.1.jar;build\test-classes" com.codex.aabtoolgui.AppThemeTest
```

## CLI

```powershell
java -jar dist\aabtool-gui-5.1.jar `
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
