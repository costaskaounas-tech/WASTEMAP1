# DISPATCHER Android

Native Android launcher for the live DISPATCHER site:

https://nursego-athens.costaskaounas.chatgpt.site

## Included

- Trusted Web Activity (TWA) launcher for the exact DISPATCHER host.
- The browser's existing Google and ChatGPT session is used for the site's built-in sign-in flow.
- Android Back, HTML file selection and external links are handled by the browser, where the site expects them.
- App-link association for the exact DISPATCHER host.
- HTTPS-only network security; no storage permission is requested.

## Open and run

1. Open the `DISPATCHER-Android` folder in Android Studio.
2. Use JDK 17.
3. Let Gradle sync.
4. Select a device and press Run.

Command-line debug build:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Output:

`app/build/outputs/apk/debug/app-debug.apk`

The provided debug APK is installable directly after enabling installation from the browser or file manager used to open it. It is not a production Play Store release. A production release requires a private signing key and preferably an Android App Bundle.

## Scope and limitations

- This is a native Kotlin launcher around the live website, not a rewrite of its screens in Kotlin.
- The site uses the browser's secure ChatGPT sign-in. A Chrome-compatible browser is required.
- Until the browser validates the site's association file, it shows the site in a browser tab rather than fullscreen TWA mode; sign-in still works.
- Internet is needed. There is no added offline database, background push service, notification sound, or FCM integration.
- No patient records, credentials or signing keys are embedded in the project or APK. Real authenticated use still communicates with the existing site and its backend.
- Automated builds do not replace testing on an Android device.
- Debug signing is temporary. A later debug build can require uninstalling an earlier build before installation. Use a stable private release key before distribution.
