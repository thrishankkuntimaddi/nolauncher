# NoLauncher

A calm Android home screen. **No icons. No badges. No notification dots. Just names.**

- **Home** — up to 7 essential apps as plain text, a search bar, and a Focus button. Black, no wallpaper.
- **All apps** — swipe up for an A–Z list that curves away like a drum.
- **Search** — prefix, word and initials matching (`gm` → Google Maps). Enter opens the top hit; no match offers a web search.
- **Focus** — one tap toggles Do Not Disturb (priority only).
- **Long-press an app** — keep on home, rename, hide, app info, uninstall.

## Install

1. Download `NoLauncher-v*.apk` from [Releases](../../releases/latest) on your phone.
2. Open it and allow "Install unknown apps" for your browser/files app when asked.
3. Open **NoLauncher → Get started**, choose **NoLauncher** as your default home app, then pick your essentials.

If you skipped the prompt: *Settings → Apps → Default apps → Home app → NoLauncher*.

**For the full no-distraction setup:** tap the moon button once and allow Do Not Disturb access, so Focus can silence notifications.

## Gestures

| Gesture | Action |
|---|---|
| Swipe up | All apps |
| Swipe down | Notification shade |
| Long-press app | Options |
| Long-press empty home | Settings |
| Pull down at top of list / Back | Close list |

## Build

Requires JDK 17 and the Android SDK (compileSdk 36).

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # signed if a keystore.properties exists (see app/build.gradle.kts)
```

Min Android 8.0 (API 26).
