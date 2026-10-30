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
the current 52-test run is `test-results/search-full-instrumentation.txt`.

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

The search/consistency milestone passes **52 instrumentation tests, 0 failures on
Android 16** (33.965 seconds). Debug/release/test APK builds succeed. Logs:
`/tmp/smoky-search-build.log`, `/tmp/smoky-search-focused-tests.log` (4 tests),
`/tmp/smoky-search-full-tests.log`, and the ignored copy
`test-results/search-full-instrumentation.txt`. Earlier Gradle HTML reports are stale.

`search/SearchAppearanceTest.kt` uses synthetic subjects held only in memory. It
checks result grouping/counts, collapse/expand and IDs, both card/title click
callbacks, unchanged shared card rendering outside search, dark/2x-font wrapping,
and real BrowseActivity search prefill, back stack, and Modern/Classic recreation.
The real activity test blocks the no-token redirect, runs a read-only local search,
and restores changed preferences. No account credentials, DB fixture writes, or
study submissions are used. Settings tests now locate the renamed appearance option
by its string resource.

Recreation tests exposed and fixed two bugs: a Material tutorial-button SavedState
being restored to a Classic Button (distinct IDs now), and an old Classic toolbar
receiving Material colors after the preference changed but before recreation
(style using the inflated context, not the new preference). The initial fixture
assumed four result groups; existing sorting combines vocabulary/kana vocabulary,
so the test correctly expects three. Sorting behavior is unchanged.

Visual QA: `captures/smoky-search/` contains light populated results, an empty result
screen, and a dark card at 2x text. Full-screen fixtures use an AboutActivity host;
the empty search uses the real BrowseActivity. Captures are ignored/regeneratable.
The completed development milestone still needs phone validation and pre-Android-12
coverage. The task emulator is stopped at handoff.

Phone: search/consistency implementation **ab784d0** was installed successfully on
the Pixel 9 Pro with adb install -r at the user’s request, preserving app data.
Physical-device behavior validation remains pending. Do not run instrumentation
on that account-bearing device.

## Search presentation boundary

`ui/SearchStyle.java` wraps only the result layout and browsing toolbar in a
Material context; BrowseActivity itself keeps ActiveTheme. `ui/BrowseToolbar.java`
uses that context for input/icons and keeps browsing chrome neutral when a legacy
subject headline requests a subject-colored toolbar. The subject headline inside
the page retains its semantic color. `AbstractActivity.refreshToolbarAppearance`
is a no-op hook, overridden only by BrowseActivity and called after fragment title
updates. Result cards use a scoped context marker; shared SubjectCardBinder remains
unchanged for Classic screens and other callers. The default result layout is a
full-width card with the existing subject renderer (including radical images),
meaning, reading, level and SRS/wait information. Long titles fit to available width.

Group headers and the adapter retain the existing result tree, sort order, filters,
collapsed tags, and subject IDs. Header actions have accessible expand/collapse
labels. Modern tutorial-dismiss has a distinct ID from Classic to avoid cross-class
saved-state restoration. Advanced form rows explicitly inflate using the legacy
activity context; migrate that editor as a separate next step, not by changing all
of BrowseActivity's theme. Item-detail bodies and review/lesson activities remain
legacy, and their presentation consistency is still unfinished.

The existing `modern_settings` / `settings_dynamic_color` preferences cover search
and browsing chrome too, now labeled **Modern settings and search** / **Wallpaper
colors in settings and search**. No preference keys were renamed. Dashboard controls
remain independent. `SmokyMaterialTheme` plus `material_colors.xml` share static
accent resources across Material Views and DashboardTheme; dynamic overlays still
supply wallpaper colors when enabled. Semantic ActiveTheme colors remain independent.

User priority: complete a modern-design first pass across **all in-app screens**, then
run a dedicated refinement/optimization phase; **widgets last**. TASKS.md now contains
the ordered seven-batch coverage checklist (browse, details, study, dashboard, settings,
utilities, global completion). Keep regression testing and correctness fixes within each
batch rather than deferring them. Next implementation: browse overview and advanced filters.
The latest roadmap update changes documentation only; the 52-test implementation baseline
and phone build ab784d0 are unchanged.

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
