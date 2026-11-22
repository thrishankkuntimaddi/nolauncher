# NoLauncher

A calm Android home screen. **No icons. No badges. No notification dots. Just names.**
It doesn't give you more ways to open things — it gives you fewer, and holds you to your limits.

## What it does

- **Home** — up to 7 essential apps as plain text. Black, no wallpaper.
- **Side panels** — swipe right or left on home: a panel slides over ~80% of the screen (home stays visible in the rest) with your important apps — names plus their logos in monochrome. Width adapts to the screen.
- **Dock** — a search circle plus a pill of small monochrome icons: Focus, extra quick apps (scroll sideways), and Settings.
- **All apps** — swipe up for an A–Z list that curves away like a drum. Swipe sideways to walk through groups (Messaging, Phone, AI, Social, Watch & Listen, Finance, Shopping & Food, Travel, Work, Tools, Utilities, Games, Other) — it loops. Scrolling shrinks the search bar back into the dock.
- **Time limits** — daily minutes per app (e.g. X 10 min, Reddit 5 min, YouTube 10 min). A quiet pill warns at 5 min and counts down the last minute; at zero the app closes and you're sent home.
  - Stricter limits apply now; looser ones (or removing one) only start **tomorrow**.
  - Studying? **One** 15-minute extension per app per day, after a 10-second pause.
- **Who's waiting** — swipe down for which apps have notifications: name, count, time. Never the content.
- **Setup & Settings** — a step-by-step setup on first launch; everything is editable later from ⚙ in the dock.

## Gestures

| Gesture | Action |
|---|---|
| Swipe right / left on home | Left / right panel of important apps |
| Swipe up | All apps (swipe sideways there for groups) |
| Swipe down | Who's waiting (notification names) |
| Long-press an app | Keep on home, time limit, rename, hide, info, uninstall |
| Long-press the dock | Edit dock |
| Long-press a panel / group title | Edit it |
| Long-press empty home | Settings |

## Install

1. Download `NoLauncher-v*.apk` from [Releases](../../releases/latest) and install it.
   - **Play Protect "App blocked"?** Some regions block APKs that request Accessibility/notification access when installed from a browser. Install from a computer instead: enable USB debugging, then `adb install -r NoLauncher-v*.apk`.
2. Open **NoLauncher → Get started**, choose it as your default **Home app**, pick your essentials.
3. The setup walks you through permissions (later: ⚙ → Permissions).

> **Android 13+:** for apps installed from an APK, the Accessibility and Notification access switches may be greyed out ("Restricted setting"). Open *Settings → Apps → NoLauncher → ⋮ → Allow restricted settings*, then try again.

**Privacy:** the time-limit service only looks at *which app* is in front (its package name). It never reads screen content. Nothing leaves your phone.

## Build

Requires JDK 17 and the Android SDK (compileSdk 36). Min Android 8.0.

```sh
./gradlew assembleDebug
./gradlew assembleRelease   # signed if a keystore.properties exists (see app/build.gradle.kts)
```
