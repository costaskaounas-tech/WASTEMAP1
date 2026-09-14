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
- Cookies persist in the app; WebView URL/history restore after recreation (unsaved page form/DOM state is not guaranteed).
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

- This is a native Kotlin shell around the live website, not a rewrite of its screens in Kotlin. The website is unchanged.
- Back follows website history. Menus/dialogs or tabs that do not add browser history need site-side Back integration; the shell does not guess private app state.
- File selection supports existing documents/images, including multiple selections. Direct camera capture is not included.
- External links open installed apps; calls and SMS still require the user's action in those apps. Arbitrary intent actions and local file/content navigation are blocked.
- Internet is needed. There is no added offline database, background push service, notification sound, or FCM integration.
- Browser and WebView cookies/storage are separate. Existing browser-only data is not migrated. External OAuth/login completion inside WebView has not been verified.
- No patient records, credentials or signing keys are embedded in the project or APK. Real authenticated use still communicates with the existing site and its backend.
- Automated builds and URL-policy unit tests do not replace testing on an Android device. Login, file uploads, maps/Viber and UI Back require device acceptance checks.
- Debug signing is temporary. Different CI runs can generate different debug certificates, so reinstalling a later build may require uninstalling the earlier one (which removes app-local data). Use a stable private release key before distribution.
