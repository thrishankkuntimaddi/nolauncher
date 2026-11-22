# Publishing NoLauncher on Google Play

Website (GitHub Pages, from `docs/`): https://thrishankkuntimaddi.github.io/nolauncher/

## Signing (do this once)

- Builds are signed with the key in `~/.android/nolauncher-signing/` (kept outside the repo; back it up somewhere safe — losing it means you can't update the app).
- On Play Console choose **Play App Signing** (default). Google keeps the app-signing key; the key above becomes your **upload key**. Upload the `.aab` signed with it.

## Before every upload

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`. Play rejects a versionCode it has seen.
2. Build: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
3. Upload the `.aab` in Play Console → Testing (internal) first, then Production.

## Store listing — what Play asks for

- App icon 512 × 512 PNG, feature graphic 1024 × 500, at least 2 screenshots (phone/tablet as applicable)
- Short description (80 chars) and full description (4000) — the website text works as a start
- Category: **Personalization**; contact email; **Privacy policy URL**: `https://thrishankkuntimaddi.github.io/nolauncher/privacy.html`
- Content rating questionnaire (no violence etc. → Everyone), Target audience: 18+ / not for children
- **Data safety**: no data collected, no data shared; data stays on device.

## Sensitive permissions — declarations Play will ask for

- **AccessibilityService** (time limits): Play only allows this with a declaration form and a **prominent in-app disclosure** shown before the user turns it on (what it does, that it doesn't read content). Explain the core use: enforcing user-chosen app time limits. Be ready for review to ask for a short video of the feature. Not an "accessibility tool" (`isAccessibilityTool` stays false).
- **Notification listener**: "who's waiting" — names and counts only.
- **ACCESS_NOTIFICATION_POLICY**: Focus (Do Not Disturb) button.
- Home/launcher apps are allowed; declare the app as a launcher in the listing.
