# SeqRename

Android app that renames selected gallery photos with a sequential
naming pattern (`Pole_001.jpg`, `Pole_002.jpg`, ...). Built as a
Capacitor app with one custom native plugin — no PC required, built
entirely via a VPS + GitHub Actions, same pattern as Jagruti.

## How it works

- **Pick**: `Select Photos` opens Android's built-in document/photo
  picker (`ACTION_OPEN_DOCUMENT`, multi-select). No runtime storage
  permissions needed — the system picker grants access itself.
- **Preview**: prefix + start number + digit padding + step generate
  the new names client-side so you can check them before renaming.
- **Rename**: on Android 11+ (API 30+), the app requests one-time
  write consent for the exact selected files via
  `MediaStore.createWriteRequest` (a single system dialog covering
  all selected photos), then updates `DISPLAY_NAME` for each via
  `ContentResolver`. On Android 10 and below it renames directly.

All native logic lives in one file:
`android/app/src/main/java/com/prasonnis/seqrename/SeqRenamePlugin.java`

## Setup on your VPS (mobile-only workflow)

1. Push this project to a GitHub repo (via your browser-based editor
   or `git` over SSH from Termius).
2. The included `.github/workflows/build.yml` builds a debug APK on
   every push to `main` — Node + JDK + Gradle all run on GitHub's
   runners, so your VPS/phone never needs Android SDK installed.
3. After a push, go to the repo's **Actions** tab in your phone
   browser → open the latest run → download the `seqrename-debug-apk`
   artifact → install it on your phone.

## Editing on mobile

- UI/logic: `www/index.html` (plain HTML/CSS/JS, no build step —
  edit and push, no bundler needed)
- Native rename logic: `android/app/src/main/java/com/prasonnis/seqrename/SeqRenamePlugin.java`
- After editing native Java files, no local rebuild is needed —
  just push; GitHub Actions runs `npx cap sync android` and
  `./gradlew assembleDebug` for you.
- If you add/change `www/` files only, `npx cap sync android` isn't
  strictly required locally since the workflow re-syncs on every
  build — but if you ever do edit locally, run it before committing.

## Known limitation

On Android 10 (API 29), renaming a photo your app didn't create can
throw a `RecoverableSecurityException` for some OEM gallery apps.
If you hit failures only on an Android 10 device, that's why — the
fix is either targeting Android 11+ only, or adding a
`RecoverableSecurityException` catch that launches
`e.getUserAction().getActionIntent()` for per-file consent. Not
included here to keep the plugin simple; ask if you need it added.

## App ID

`com.prasonnis.seqrename` — change in `capacitor.config.ts` and
`android/app/build.gradle` (`applicationId`) before a real release.
<!-- test Thu Sep 24 09:24:50 UTC 2026 -->
test edit Thu Sep 24 09:24:59 UTC 2026
