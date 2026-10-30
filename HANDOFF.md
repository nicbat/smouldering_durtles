# Smoky Durtles agent handoff

## Start a new session

1. Read this file, [TASKS.md](TASKS.md), and the current milestone in [FORK_PLAN.md](FORK_PLAN.md).
2. Run `git status --short`, `git branch -vv`, and `git remote -v`. Preserve unfinished edits.
3. Continue the first incomplete current-milestone task. Inspect the actual code before relying on historical notes.
4. Run appropriate tests, update TASKS and this handoff, commit, and push to the user's fork.

## Authorization and repository

- The user approved developing a personal fork named **Smoky Durtles**, including a substantial UI overhaul, bug fixes, performance work, and widgets.
- The user explicitly wants completed work pushed to their fork. Current origin: `git@github.com:nicbat/smouldering_durtles.git`.
- Working branch: `smoky-durtles/foundation`. Do not push to upstream or force-push. No request to merge into main yet.
- No phone is needed to continue: emulator testing is available. Physical-device validation remains later work.
- Keep review grading, scheduling, local custom colors, and pending submissions intact while changing presentation.
- No secrets, API tokens, signing keys, or user-data fixtures in commits. No real review submissions during automated tests.

## Build and test

Local SDK: `/home/nicbat/Android/Sdk`; JDK 17. Supply SDK via environment rather than overwriting local configuration.

```sh
ANDROID_HOME=/home/nicbat/Android/Sdk ./gradlew assembleDebug assembleRelease
ANDROID_HOME=/home/nicbat/Android/Sdk ANDROID_SERIAL=emulator-5580 ./gradlew connectedDebugAndroidTest
```

Existing AVD: `tg` (Android 16). Run an isolated writable session without saving its state:

```sh
/home/nicbat/Android/Sdk/emulator/emulator -avd tg -read-only -no-snapshot-save -no-window -no-audio -no-boot-anim -port 5580
```

Wait for `adb -s emulator-5580 shell getprop sys.boot_completed` to return `1`.
Stop only the emulator started for the task using `adb -s emulator-5580 emu kill`.
Do not wipe or overwrite the user's AVD.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Report: `test-reports/connected/debug/index.html`.
Debug ID: `com.smoky_durtles.wk.debug`; release ID: `com.smoky_durtles.wk`.
Java namespace remains `com.smouldering_durtles.wk`. Release currently uses debug signing; it is a local development artifact.

## Latest evidence

Foundation `0b6b531` and reliability `4780982` are pushed. The Compose overview
milestone passes **32 instrumentation tests, 0 failures on Android 16**, including
all database migrations, search/identity/palette regressions, six Compose UI state
tests, and an actual controller bridge hosted in an existing AppCompat activity.
Both debug and release APKs build successfully. Static light/dark and 2x-font
screenshots were visually inspected using synthetic data. Local captures are in
`captures/smoky-dashboard/` (ignored by Git; tests can regenerate them).

The user has not tested on their phone yet. Full account-backed study flows,
Classic switching, pre-Android-12 devices, and wallpaper changes still need
integration coverage. No real account credentials were used. The emulator started
for this work is stopped at handoff. See TASKS for the next work item.

## Architecture boundaries

The initial Compose migration adds `dashboard/DashboardController.kt` (LiveData
adapter) and `DashboardOverview.kt` (pure UI/state/theme). MainActivity passes its
existing callbacks and interaction guard. The Compose overview replaces only the
legacy header; custom charts/statistics remain below it. Display settings expose
Modern dashboard and Wallpaper colors. Do not describe this as a full dashboard
or review-screen rewrite.

Kotlin/compiler plugin: 2.2.21. Compose BOM: 2025.04.01, deliberately pinned to an
API 21-compatible generation. AndroidX Test 1.7 / Espresso 3.7 fixes reflection on
the Android 16 emulator. Do not upgrade the BOM blindly without reviewing minimum
SDK compatibility. `.kotlin/` is generated and ignored.

MainActivity owns dashboard bindings and existing launch handlers. SessionActivity
and Session still own study workflows. ActiveTheme owns semantic/custom palettes;
ThemeUtil resolves legacy application-level colors. Compose should use its own
UI color scheme, leaving subject colors and legacy activity themes independent.

Historical migration schemas 48–67 are available in commit `b36c696` under
`app/schemas/com.the_tinkering.wk.db.AppDatabase/`; use original bytes under the
current database class directory. Never fabricate historical schema contents.

## Before ending a session

- State what is implemented versus still proposed, and record exact test results.
- Note unresolved build/test failures and any running processes.
- Update TASKS and this file, commit coherent changes, push and verify remote HEAD.
- Link the branch/commit and current APK in the user-facing update.
