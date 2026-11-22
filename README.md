# NoLauncher

A calm Android home screen. **No icons. No badges. No notification dots. Just names.**
It doesn't give you more ways to open things — it gives you fewer, and holds you to your limits.

## What it does

- **Home** — up to 7 essential apps as plain text. Black, no wallpaper.
- **Groups, in a circle** — swipe left or right to move through your groups (Finance, Social, Messaging, Phone, Tools…) and back round to home. Same look: just a small title and names. Created automatically from your installed apps on first run; fully editable.
- **Time limits** — daily minutes per app (e.g. X 10 min, Reddit 5 min, YouTube 10 min). A quiet pill warns you at 5 min and counts down the last minute; at zero the app is closed and you're sent home.
  - Stricter limits apply now; looser limits (or removing one) only start **tomorrow**.
  - Need it for study or work? **One** 15-minute extension per app per day, after a 10-second pause.
- **Who's waiting** — swipe down to see which apps have notifications: name, count, time. Never the content. Tap to open, hold to clear.
- **All apps** — swipe up for an A–Z list that curves away like a drum, with search.
- **Focus** — the moon button toggles Do Not Disturb.

## Gestures

| Gesture | Action |
|---|---|
| Swipe left / right | Next / previous group (loops back to home) |
| Swipe up | All apps |
| Swipe down | Who's waiting (notification names) |
| Long-press an app | Keep on home, time limit, rename, hide, info, uninstall |
| Long-press empty home | Settings |
| Long-press empty group page | Edit that group |

## Install

1. Download `NoLauncher-v*.apk` from [Releases](../../releases/latest) on your phone and install it.
2. Open **NoLauncher → Get started**, choose it as your default **Home app**, pick your essentials.
3. Turn on the permissions (each is one-time):
   - **Time limits:** Settings → Time limits → *Turn on enforcement* → Accessibility → *NoLauncher time limits* → On.
   - **Who's waiting:** swipe down → *Allow access*.
   - **Focus:** tap the moon → allow Do Not Disturb access.

> **Android 13+:** for apps installed from an APK, the Accessibility and Notification access switches may be greyed out ("Restricted setting"). Open *Settings → Apps → NoLauncher → ⋮ → Allow restricted settings*, then try again.

**Privacy:** the time-limit service only looks at *which app* is in front (its package name). It never reads screen content. Nothing leaves your phone.

## Build

Requires JDK 17 and the Android SDK (compileSdk 36). Min Android 8.0.

```sh
./gradlew assembleDebug
./gradlew assembleRelease   # signed if a keystore.properties exists (see app/build.gradle.kts)
```
