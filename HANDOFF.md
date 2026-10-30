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
the previous 52-test run is `test-results/search-full-instrumentation.txt`.
Current evidence is listed below; older reports are historical.

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

## Latest evidence — full in-app modern pass

Debug, release and instrumentation APKs build successfully (including release vital lint).
All 75 instrumentation tests pass on both Android 16 and Android 11. There is no separate
`app/src/test` unit-test source set. This is 23 additional tests beyond the previous 52.

- Android 16/API 36.1: **75 tests, 0 failures**, 59.932 seconds.
- Android 11/API 30: **75 tests, 0 failures**, 57.546 seconds.
- Android 5.0.2/API 21: three utility appearance tests pass, but the full run aborts
  on Jackson ObjectMapper class initialization (existing jackson-databind 2.15.0).
  Minimum SDK remains 21; full runtime compatibility there is **not verified**.
- Build log: `/tmp/smoky-full-ui-build.log`.
- Full-run logs: `/tmp/smoky-full-ui-api36-final-tests.log`,
  `/tmp/smoky-full-ui-api30-final-tests.log`. Ignored local copies are
  `test-results/full-ui-api36-instrumentation.txt` and `test-results/full-ui-api30-instrumentation.txt`.
- API 21 logs: `/tmp/smoky-full-ui-api21-tests.log`, `/tmp/smoky-api21-logcat.txt`.
- Captures: ignored/regeneratable `captures/smoky-full-ui/`,
  `captures/smoky-full-ui-api30/`, `captures/smoky-settings-completion/`.

Tests use in-memory synthetic subjects/session items, restored preferences and LiveData,
plus actual activity recreation/navigation. They do not write account fixtures or submit
answers. Existing migration/database, palette, search, dashboard and settings tests remain.
New coverage includes browse filter round trips, preset/level actions, utility layouts,
font/color state, sliders, question draft retention, study mode binding, semantic feedback
contrast, detail back navigation and populated/collapsible session logs. Study tests restore
Session fields after temporarily injecting synthetic state; they do not exercise grading or
server synchronization. An initial dashboard fixture failure on both modern platforms was
fixed by explicitly restoring/posting the missing LiveFirstTimeSetup fixture value. The new
session-log fixture also needed the existing enable_advanced gate alongside full_session_log;
its first run failed until both preferences were set/restored. Review caught an overly broad
detail-stack replacement, now restricted to explicit paging with a drilldown regression test.

Visual QA caught and fixed unreadable large-text statistics/metadata columns, summary
feedback contrast and the cramped audio relocation row. Inspected light/dark and 2x-font
captures cover browse, study, settings, details, dashboard statistics and utilities.
Real physical keyboard/insets, landscape, TalkBack, wallpaper-change refresh, prolonged
study sessions and process-death/offline submission recovery remain validation work.

Phone: full UI implementation **6bbe197** was installed on Pixel 9 Pro, serial
`4C191FDAP00202`, on 2026-10-30 using `adb install -r` (Success), preserving app data.
MainActivity launched with `am start -W` (Status: ok). User behavior validation remains
pending; no instrumentation or account actions were performed. Never run tests
on that account-bearing phone. Install updates only with `adb -s 4C191FDAP00202 install -r`
when requested; preserve its data and debug signing key.

Task emulator state: all three task-owned emulators stopped. Root used read-only unsaved `tg`
on emulator-5580. Additional task-only API30 (5582) and API21 (5584) AVDs use
`/tmp/smoky-api30-sdk` and `/home/nicbat/.cache/smoky-ui-api21`; no existing SDK/config
or AVD was overwritten. Current host emulator binary 36.3.10 was used because the
separately downloaded 37 binary crashed. Do not delete or reset the user's AVD/phone.

## Current architecture boundaries

- `ui/ModernUi.java` and `modern_ui_styles.xml` provide scoped Material 3 context,
  dynamic colors, neutral roles, rounded stable-class controls and toolbar/system-bar
  appearance. `ModernActivity` applies this to utility pages. Regular AppCompat Button
  inflation is intentional: switching Classic/Modern must not restore MaterialButton
  state into a different class. Explicit MaterialButton layouts disable saved state or
  use distinct IDs where needed. The pre-23 status bar/pre-26 navigation bar stay dark
  because those versions cannot show dark icons.
- **Modern interface** (`modern_settings`) and **Wallpaper colors in interface**
  (`settings_dynamic_color`) now cover browse/details/study/settings/tools. Existing keys
  and values are preserved. Dashboard flags remain independent. Modern uses system
  light/dark; Classic uses existing selectors. `ActiveTheme` still owns subject/SRS
  palettes. `wkTextColorPrimary` separates legacy neutral text from Material accent color.
- `AbstractActivity` keeps lifecycle/interaction handling, menu configuration and the
  theme hook. `refreshToolbarAppearance` applies scoped chrome after fragment titles;
  `styleActivityContent` is opt-in, not a blanket study-content recoloring pass.
- BrowseActivity itself retains ActiveTheme, while `SearchStyle`, `BrowseToolbar`, browse
  overview/advanced form/detail layouts use wrapped contexts. `SubjectCardBinder` keeps
  semantic renderers and IDs; related cards gain shapes only in a modern context. Explicit previous/next
  paging replaces the current back-stack entry so one Back returns to results. Related-item
  links still push their own entry; tests cover nested details and deep-linked roots.
- `study/StudyStyle.java` styles lesson/review/Anki/summary pages **before semantic binding**,
  then rounds subject/feedback surfaces with their actual colors. Correct/incorrect
  feedback ink is selected for contrast. Subject metadata is stacked at runtime with
  IDs/direct-parent visibility preserved. Study fragments, SessionActivity and Session
  retain grading, scheduling, answer acceptance and submission behavior.
- Session-log holders apply neutral headers and semantic rounded rows while retaining
  SessionLogAdapter grouping, restricted-mode hiding and original navigation callbacks.
  Font sizes, custom rich-text spans, audio handlers, stroke/pitch renderers and color
  wheel behavior remain. Modern design does not mean rewriting these in Compose.
- Dashboard overview remains Compose (`DashboardController.kt`, `DashboardOverview.kt`).
  MainActivity passes original actions/interaction guards. `DashboardStyle` and modern
  SRS/level/JLPT/Jōyō/breakdown layouts modernize existing models/observers. Recent/critical/
  burned tables keep semantic labels and callbacks. Statistics use wrapping labeled
  groups so 2x text is readable. No chart calculation/90% target logic changed.
- Settings retain AndroidX Preferences persistence/validation. MaterialPreferenceDialogs
  preserves positive/cancel/change-veto behavior. MaterialSliderPreference extends the
  existing SeekBarPreference contract, including commit-on-release. Range/URL interiors
  are outlined. Color/font tools retain original storage/import handlers; color selection
  and repeated child-ID saved-state restoration bugs were fixed.
- Utility pages reuse their original actions and stable view IDs. Modern audio-download
  rows and activity layout reflow status/relocation/actions. Import/export/backup system
  document pickers and external browser handoffs remain Android-owned UI.

Dependency versions remain Kotlin/compiler plugin 2.2.21, Compose BOM 2025.04.01 and
Material Components Views 1.12.0. AndroidX Test 1.7 / Espresso 3.7 supports the Android 16
emulator. Review minimum-SDK compatibility before upgrades; the Jackson issue above exists
independently of this UI pass. Historical Room schemas 48–67 were restored from original
commit `b36c696`; never fabricate migration fixtures.

## Next work

1. This build is installed and launched on the phone; get feedback across browse,
   details, lesson/review input, summary/log and settings. Keep Classic available.
2. Address any reproducible bugs and usability issues; complete broader accessibility,
   landscape, wallpaper and recovery validation without submitting real test answers.
3. Profile startup, scroll, memory, chart and audio behavior before optimization. Do not
   claim emulator/debug timing as a performance gain.
4. Resolve minimum-SDK/Jackson compatibility and durable release signing/update delivery.
5. Widgets last; inspect upstream PR #153 before porting overlapping work.

## Before ending a session

- Record implemented vs proposed work, exact test results and remaining limitations.
- Stop only task-owned emulators and note any remaining processes.
- Update TASKS and this file, commit, push to the fork branch and verify remote HEAD.
- Link the branch/commit and current APK; distinguish built from phone-installed versions.
