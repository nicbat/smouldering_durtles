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
- Emulator testing is available. The user connected a Pixel 9 Pro and the dashboard build was installed successfully; they confirmed launcher visibility. The Material settings update (4472ee8) was subsequently installed with adb install -r; installation succeeded, preserving app data. Settings behavior on the phone has not yet been confirmed by the user. Never run instrumentation tests on their account-bearing phone.
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
the current 48-test run is `test-results/panels-full-instrumentation.txt`.

With the user's phone also attached, use explicit emulator targeting for tests.
Disable test-device animations in the isolated, unsaved emulator session (never on
the user's phone); Espresso otherwise races newly opened windows:

```sh
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 shell settings put global window_animation_scale 0
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 shell settings put global transition_animation_scale 0
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 shell settings put global animator_duration_scale 0
```

Build/install/run:

```sh
ANDROID_HOME=/home/nicbat/Android/Sdk ./gradlew assembleDebug assembleRelease assembleDebugAndroidTest
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
/home/nicbat/Android/Sdk/platform-tools/adb -s emulator-5580 shell am instrument -w com.smoky_durtles.wk.debug.test/androidx.test.runner.AndroidJUnitRunner
```
Debug ID: `com.smoky_durtles.wk.debug`; release ID: `com.smoky_durtles.wk`.
Java namespace remains `com.smouldering_durtles.wk`. Release currently uses debug signing; it is a local development artifact.

## Latest evidence

Foundation `0b6b531`, reliability `4780982`, Compose overview `713d144`, and
Material settings `4472ee8` are pushed. The overflow/dashboard panels milestone
passes **48 instrumentation tests, 0 failures on Android 16**. Debug and release
APKs build successfully; aapt confirms minimum API 21 and target 36. Current logs:
`/tmp/smoky-panels-final-build.log`, `/tmp/smoky-panels-full-tests.log`, and the copy
`test-results/panels-full-instrumentation.txt` (ignored). The old Gradle HTML report
is not the current test result.

New tests cover populated chart counts and colors, SRS/level search callbacks,
hidden panels taking no space, large/dark layouts, original Classic layouts, real
MainActivity modern-to-Classic recreation, and overflow navigation into Settings.
Real recreation exposed a MaterialButton SavedState mismatch on the retry button;
the modern dashboard theme now explicitly uses AppCompatViewInflater and the
legacy button style. This keeps view classes stable across appearance changes.
An initial full run had two UI-window timing failures; settings editor tests now
select dialog roots explicitly, the overflow test selects its popup root, and the
passing full run disabled animations in the isolated emulator.

Synthetic screenshots in `captures/smoky-dashboard-panels/` cover light/dark,
forecast, SRS, and level rows at 1x/2x text. Level-panel captures draw the entire
measured view so all rows/legend can be inspected. The overflowing large-text
level legend was replaced with ConstraintLayout Flow wrapping. These fixtures
inflate the real dashboard layout in an AboutActivity host; they are not live
account screenshots. Earlier captures remain under `captures/smoky-settings/`
and `captures/smoky-dashboard/`. Captures are ignored and regeneratable.

Phone: the dashboard launcher and settings-build installation were confirmed in
prior turns. The newest overflow/chart build has **not** been installed there.
No account credentials or submissions were used for this milestone. Actual
review/lesson navigation, pre-Android-12 devices, and wallpaper-change integration
still need coverage. The emulator used for this work is stopped at handoff.

## Architecture boundaries

The initial Compose migration adds `dashboard/DashboardController.kt` (LiveData
adapter) and `DashboardOverview.kt` (pure UI/state/theme). MainActivity passes its
existing callbacks and interaction guard. The Compose overview replaces the legacy header; existing chart/statistics Views
remain below it with a new presentation layer. Display settings expose
Modern dashboard and Wallpaper colors. The latest milestone also styles the
existing charts, but does not replace their models or rewrite study workflows.

Kotlin/compiler plugin: 2.2.21. Compose BOM: 2025.04.01, deliberately pinned to an
API 21-compatible generation. AndroidX Test 1.7 / Espresso 3.7 fixes reflection on
the Android 16 emulator. Do not upgrade the BOM blindly without reviewing minimum
SDK compatibility. `.kotlin/` is generated and ignored.

MainActivity owns dashboard bindings and existing launch handlers. SessionActivity
and Session still own study workflows. ActiveTheme owns semantic/custom palettes;
ThemeUtil still supports application-level colors and now has a context overload
for screen-local colors. Dashboard chart chrome uses the scoped view context;
subject/SRS colors still come from ActiveTheme arrays. Compose owns overview UI
state and its color scheme; legacy study activities keep their own themes.

Settings use Material Components Views **1.12.0** to preserve API 21 compatibility
and the existing AndroidX Preferences tree. `AbstractActivity.customizeActivityTheme`
is overridden by PreferencesActivity and modern MainActivity only. That activity applies a scoped Material 3
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
through the original `switchWidget` ID. Sliders and contextual menus remain future work; toolbar overflow is now shared
Material presentation across activities. `MaterialSettingsTest` covers changes, veto/cancel, drafts on recreation,
Classic fallback, dark contrast, and nested navigation. It does not prove encrypted
credential entry on a physical device or a real wallpaper-change refresh.

The overflow/dashboard follow-up adds `ui/MaterialToolbar.java`,
`dashboard/DashboardStyle.java`, `modern_dashboard_styles.xml`, and alternate SRS/
level layouts. `AbstractActivity` configures popup appearance before menu inflation
and recreates after relevant appearance preferences change. Main uses dashboard
flags for its popup; other activities use settings flags. The popup theme is a
complete Material3 DayNight or DynamicColors theme, so it also works over legacy
AppCompat activities without changing their activity theme. AppCompat keeps menu
selection/submenu behavior. No reflection or custom replacement menu model.

Modern MainActivity uses a scoped Material theme. DashboardStyle adds surfaces and
titles while preserving original chart instances/IDs, observers and callbacks.
Titled panels measure zero height when their data view is GONE. Timeline labels
scale with font size and wrap their legend, and time labels reduce density to
avoid overlap. Level rows show counts from the existing buckets and retain the
90% marker logic and browse filters. SRS rows use stage swatches rather than filled
tiles. Other statistics receive surface styling only; subject tables still need
further design work. DashboardOverview includes days at the current level again.

`DashboardAppearanceTest` posts synthetic objects through the real LiveData and
restores previous preferences/values. It intercepts browse callbacks via an Actment
proxy; it never stores subjects or account fixtures. `ToolbarAppearanceTest` uses
a monitor to block the no-token redirect while exercising real MainActivity;
notification prompts are suppressed through a restored test preference.

Historical migration schemas 48–67 are available in commit `b36c696` under
`app/schemas/com.the_tinkering.wk.db.AppDatabase/`; use original bytes under the
current database class directory. Never fabricate historical schema contents.

## Before ending a session

- State what is implemented versus still proposed, and record exact test results.
- Note unresolved build/test failures and any running processes.
- Update TASKS and this file, commit coherent changes, push and verify remote HEAD.
- Link the branch/commit and current APK in the user-facing update.
