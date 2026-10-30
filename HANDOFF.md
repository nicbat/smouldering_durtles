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
- Emulator testing is available. The user connected a Pixel 9 Pro and the dashboard build was installed successfully; they confirmed launcher visibility. The settings update is not installed on the phone yet. Never run instrumentation tests on their account-bearing phone.
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

APK: `app/build/outputs/apk/debug/app-debug.apk`. The old Gradle HTML report at
`test-reports/connected/debug/index.html` covers the 32-test dashboard milestone;
the current 42-test run is `test-results/settings-full-instrumentation.txt`.

With the user's phone also attached, use explicit emulator targeting for tests:

```sh
ANDROID_HOME=/home/nicbat/Android/Sdk ./gradlew assembleDebug assembleRelease assembleDebugAndroidTest
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 shell am instrument -w com.smoky_durtles.wk.debug.test/androidx.test.runner.AndroidJUnitRunner
```
Debug ID: `com.smoky_durtles.wk.debug`; release ID: `com.smoky_durtles.wk`.
Java namespace remains `com.smouldering_durtles.wk`. Release currently uses debug signing; it is a local development artifact.

## Latest evidence

Foundation `0b6b531`, reliability `4780982`, and Compose overview `713d144` are
pushed. The Material settings follow-up passes **42 instrumentation tests, 0
failures on Android 16**, including the prior 32 tests and ten settings tests.
Debug and release APKs build successfully; aapt confirms minimum API 21 and target
36. The full run used explicit emulator-targeted adb instrumentation. Its output
is `test-results/settings-full-instrumentation.txt` (ignored), also available at
`/tmp/smoky-settings-full-tests.log`. A subsequent focused dark-mode test adds an
assertion for light-on-dark system-bar icons and passes (1/1). Screenshots wait for
window transitions before capture; these transitions initially made the status
icons appear dark in an early screenshot despite the correct window flags.

Light/dark settings, choice/edit dialogs, and overflow menus were visually
inspected. Regeneratable captures are in `captures/smoky-settings/`; earlier
synthetic dashboard captures remain in `captures/smoky-dashboard/` (both ignored).
A first focused run had a dialog-window focus race; the bounded-number test now
executes the pending fragment transaction before interacting, and the full run
passes.

Phone evidence is installation/launcher visibility only, for the dashboard build.
Settings validation used the isolated emulator, no credentials or real account
submissions. Full account-backed study flows, dashboard Classic switching,
pre-Android-12 devices, larger-font settings, and actual wallpaper changes remain
to test. The emulator started for this work is stopped at handoff.

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

Settings use Material Components Views **1.12.0** to preserve API 21 compatibility
and the existing AndroidX Preferences tree. `AbstractActivity.customizeActivityTheme`
is a no-op outside PreferencesActivity. That activity applies a scoped Material 3
DayNight theme and optional DynamicColors before view inflation, and tints legacy
menu icons for the new surface. The application-level ActiveTheme remains intact.
`modern_settings` and `settings_dynamic_color` are independent of dashboard flags.
Preference appearance changes post recreation after persistence; nested back-stack
restoration is covered. Modern settings follow system light/dark mode; Classic
selectors continue to affect study/legacy screens.

`settings/MaterialPreferenceDialogs.java` supplies Material builders while retaining
AndroidX List/Edit dialog lifecycle, listeners and persistence. The custom range/URL
dialogs use its `CustomDialog` shell but keep their original inner layouts. Standard
editors use an outlined TextInputEditText; numeric/password binding stays with the
existing Preference classes. Material switches reuse SwitchPreferenceCompat binding
through the original `switchWidget` ID. Sliders and menus outside settings remain
future work. `MaterialSettingsTest` covers changes, veto/cancel, drafts on recreation,
Classic fallback, dark contrast, and nested navigation. It does not prove encrypted
credential entry on a physical device or a real wallpaper-change refresh.

Historical migration schemas 48–67 are available in commit `b36c696` under
`app/schemas/com.the_tinkering.wk.db.AppDatabase/`; use original bytes under the
current database class directory. Never fabricate historical schema contents.

## Before ending a session

- State what is implemented versus still proposed, and record exact test results.
- Note unresolved build/test failures and any running processes.
- Update TASKS and this file, commit coherent changes, push and verify remote HEAD.
- Link the branch/commit and current APK in the user-facing update.
