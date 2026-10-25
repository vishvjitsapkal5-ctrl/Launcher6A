# UltraLite Launcher

A minimal native Java launcher project intended for Android 9 (API 28) devices such as the Redmi 6A.

## Features
- Android Home launcher intent filter
- Installed app discovery using `PackageManager`
- Alphabetically sorted app drawer
- Local app-name search
- Tap an app icon to launch it
- Five app shortcuts on the home screen
- Clock and date
- Dark, low-complexity interface
- No network permission, analytics, ads, or background service

## Build with GitHub Actions
1. Upload the entire project directory to a GitHub repository, preserving folders.
2. Open the repository's **Actions** tab.
3. Select **Build UltraLite Launcher APK** and click **Run workflow**, or push a commit to `main`/`master`.
4. Open the completed workflow run.
5. Download the `UltraLiteLauncher-debug-apk` artifact.
6. Extract the artifact ZIP to obtain `app-debug.apk`.

## Install
Transfer `app-debug.apk` to the phone and install it. Android may ask you to allow installation from the app you used to open the APK. Only install APKs you trust.

Then open MIUI's Home app/default apps settings and select **UltraLite Launcher**. Menu wording varies by MIUI version.

## Notes
- `minSdk` is 28 (Android 9).
- The project compiles against SDK 35 and targets API 28 to keep Android 9 compatibility while avoiding Android 11 package-visibility restrictions for this version.
- The first five alphabetically sorted apps are shown in the dock; favorites are not yet configurable.
- This project has not been tested on a physical Redmi 6A. Test it before relying on it as your default launcher.
