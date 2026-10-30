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

Foundation commit `0b6b531` is pushed. Three focused tests passed on Android 16;
debug/release built and installed alongside upstream. Historical schemas have now been restored byte-for-byte from Git history.
The full suite passes: 25 tests on Android 16, including all 20 database migrations
and a regression test for Guru palette slots in every theme.

## Architecture boundaries

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
