# Insta Chat Only

A phone app that opens straight to your Instagram DMs. The feed, reels, explore, stories and profiles are blocked.

## Android app (`android/`)
This is a small WebView app locked to `instagram.com/direct/…`. Login and two-factor pages are allowed too. Any other Instagram page sends you back to the inbox, including pages Instagram opens without reloading. Links to other websites open in your normal browser.

### Build
1. Install [Android Studio](https://developer.android.com/studio). It includes the JDK 17 and Android SDK this needs.
2. **File → Open** the `android` folder and wait for the Gradle sync to finish.
3. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
   The output is `android/app/build/outputs/apk/debug/app-debug.apk`.

### Install on your phone
- **USB:** turn on USB debugging on the phone, then run `adb install -r app-debug.apk`.
- **Or** copy the APK to the phone, open it, and allow "Install unknown apps" when asked.

### Notes
- You log in once, and the app keeps you logged in.
- You can attach photos and videos in chats.
- There are no push notifications, because Instagram only sends those to its own app and to desktop browsers.
- If someone sends you a post or reel in a chat, opening it takes you back to the inbox. That's intentional.
- If Instagram changes its URLs, edit `ALLOWED_PATH` in `MainActivity.java`.

## Desktop (`extension/`)
This is the same lock as a Chrome/Edge extension. Load it unpacked from `chrome://extensions`, then install instagram.com as an app.
