# DISPATCHER Android

Native Kotlin shell for the live DISPATCHER site:

https://nursego-athens.costaskaounas.chatgpt.site

## Included

- Android WebView shell with JavaScript and DOM storage.
- Android system Back navigation through WebView history.
- HTML file input support through the Android document picker.
- External HTTP(S), telephone, SMS, email, maps, Viber and intent links routed to installed Android apps.
- Internal links on the exact DISPATCHER host remain inside the app.
- Main-frame loading progress, offline/error state and retry.
- Cookies and WebView state retained through configuration changes.
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
