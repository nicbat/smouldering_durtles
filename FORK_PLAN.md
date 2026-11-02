# Smoky Durtles investigation and roadmap

Recorded 2026-10-30. Baseline: upstream `main` at `c11758c`.

Status: implementation started with user authorization. The fork is named **Smoky Durtles**. This note preserves the investigation and the personal-fork roadmap. Work is on `smoky-durtles/foundation`; completed milestones are pushed to `nicbat/smouldering_durtles`.

## Current direction

Finish modern UI coverage across the entire in-app experience, then optimize and refine
screen by screen. Widgets remain last. TASKS.md contains the current seven-batch coverage
inventory and completion criteria. Correctness checks and regression fixes accompany each
implementation batch; grading/scheduling/submission logic stays intact. No full Compose
rewrite is required to reach this design goal.

## Current milestone

November 2 follow-up: compact summary/level/SRS displays, actionable review forecast and
optional Subtle/Neutral/Original learning colors. Stored WaniKani/custom palettes remain
intact; softer presentation is now the modern default. See TASKS/HANDOFF for current proof.

The first pass has now received user feedback. A cross-app refinement follows the official
Material role/component guidance and [UI_GUIDE.md](UI_GUIDE.md): coherent study controls,
responsive search grid, compact browse/dashboard layouts, font/download/utility hierarchy.
The guide records observations and conventions so future agents can apply them consistently.
See HANDOFF/TASKS for final verification and installed-build status.

The first complete in-app modern design pass is implemented: browse/filters, details,
lesson/review/Anki/self-study/summary/log UI, remaining dashboard tables, settings tools,
font/color customization and utility/help pages. Shared Material Views styling preserves
existing classes/state and callbacks; the dashboard overview remains Compose.

The app-wide switch is **Modern interface**, with independent wallpaper-color and dashboard
controls. Classic remains selectable. Subject/SRS colors now have a presentation preference;
stored custom palettes, grading and submission engines are retained. TASKS/HANDOFF record current build/test evidence, the API 21 Jackson blocker,
and remaining phone/recovery validation. Next is refinement/optimization; **widgets last**.

### Previous milestones

Overflow/dashboard follow-up: shared toolbar popup styling, scoped Material theme
for modern MainActivity, and refreshed forecast/SRS/level panels. Existing chart
models, search callbacks, custom semantic palettes, and Classic layouts are retained.
Level progress adds passed/total labels and the overview again shows time on level.
`DashboardStyle` provides presentation only; `ThemeUtil` can resolve scoped view
colors without replacing the application theme. Synthetic fixtures exercise the
real chart observers without account data. Real MainActivity recreation exposed a
MaterialButton saved-state mismatch; retaining AppCompat inflation in the modern
dashboard theme fixes switching back to Classic. See TASKS/HANDOFF for validation.


Settings follow-up: add Material Components for Android 1.12.0 to reuse the existing
AndroidX Preferences model. Only PreferencesActivity receives the Material 3 theme;
AbstractActivity exposes a no-op theme hook for this purpose. Modern settings and
wallpaper colors have independent preferences, while review/lesson screens keep
ActiveTheme. Choice and edit dialog subclasses retain AndroidX listeners, persistence,
and restoration. Custom range/URL editors receive Material dialog shells.
See TASKS and HANDOFF for current validation and remaining work.

This uses the official [Material Views integration](https://github.com/material-components/material-components-android/blob/1.12.0/docs/getting-started.md)
and [MaterialSwitch](https://github.com/material-components/material-components-android/blob/1.12.0/docs/components/Switch.md)
without converting the preference tree to Compose.


Progress update: reliability fixtures and Guru palette fixes are complete. The
initial Compose Material 3 overview now uses real LiveData counts and the original
session callbacks, with Classic and wallpaper-color preferences. Existing charts
and study screens remain in Views. The full suite now passes 32 instrumentation tests on Android 16; debug and release builds succeed. See TASKS and HANDOFF for current validation
and remaining work; earlier estimates below are historical planning context.

- Independent application IDs: `com.smoky_durtles.wk` and `com.smoky_durtles.wk.debug`, distinct provider authorities, automatic variant-specific test ID, and `smoky-durtles://` subject links.
- Updated launcher/About/Support identity with upstream credits; Java namespace and database schema identity retained.
- User-reported search bug: the top search control needs two taps. Reproduced by an instrumentation test: one expansion left the editor hidden. Fixed by using AppCompat SearchView in both toolbar menus. The same test now passes for initial expansion, focus, collapse, and reopening.
- Next milestones: regression fixtures and selected bug fixes; one complete Compose Material 3 dashboard; in-app flow and screen consistency; measured optimization; widgets last.
- Classic study screens remain available while new UI is developed. No credentials, user data, public publishing, or permanent release keys are configured by this milestone.

### Foundation validation

- Debug and release APKs build successfully; release remains debug-signed for local development.
- Three focused Android 16 instrumentation tests pass: search expansion/reopening, provider and subject-link isolation, and default search metadata resolution with a distinct application ID.
- The search test failed before the control change and passed afterward.
- Original upstream debug and Smoky debug APKs installed side by side successfully. No account credentials or existing user data were migrated.
- Fully qualified default-search metadata preserves routing while the Java namespace remains unchanged.
- Full database migration fixture failures remain outstanding; this milestone does not claim full-suite success or physical-device keyboard validation.

## Direction

The original objective was an incremental Material 3 migration suitable for upstream. Under that constraint, a locally themed Views-based dashboard action panel was the recommended first prototype.

The newer preference is a personal daily-use fork, with substantial redesign acceptable and ample AI assistance available. For that direction, evaluate **Kotlin and Compose Material 3 for a new dashboard**, retaining the Java data/session engine and existing review/lesson screens initially. The user has authorized starting this roadmap; the first implementation focuses on identity and search, with the dashboard following.

Extra AI capacity can help with implementation, test fixtures, comparisons, and reviews. The limiting factors remain reproducible behavior, device testing, data integrity, and whether the result feels good in daily use. Build a complete replacement one workflow at a time rather than coupling a UI rewrite with changes to scheduling or answer grading.

Keep the recognizable radical blue, kanji pink, and vocabulary purple independent of wallpaper accents. Preserve existing custom learning palettes. Keep Classic available during transition; permanent support for two complete interfaces is a separate maintenance decision.

## Verified architecture (upstream baseline)

- [app/build.gradle](app/build.gradle): minimum SDK 21, compile/target SDK 36, Java 8 source compatibility; AppCompat 1.6.1, Preference 1.2.1, Room 2.6.1, WorkManager, ConstraintLayout, RecyclerView, Glide, Lottie, and Pikolo. No declared Material Components or Compose dependency. Main sources contain 363 Java files and no Kotlin files.
- [build.gradle](build.gradle) and [Gradle wrapper](gradle/wrapper/gradle-wrapper.properties): AGP 9.2.1 and Gradle 9.4.1. [gradle.properties](gradle.properties) disables built-in Kotlin; Compose adoption needs deliberate Kotlin/compiler setup.
- [AbstractActivity](app/src/main/java/com/smouldering_durtles/wk/activities/AbstractActivity.java): `onCreateBase()` around line 185 inflates the supplied layout, installs the support toolbar and insets, and binds shared observers. `onCreate()` and `getTheme()` are final. Insets currently depend partly on child position.
- [AbstractFragment](app/src/main/java/com/smouldering_durtles/wk/fragments/AbstractFragment.java): constructor supplies the XML resource to AndroidX Fragment; `onViewCreatedLocal()` performs screen binding.
- [MainActivity](app/src/main/java/com/smouldering_durtles/wk/activities/MainActivity.java) and [activity_main.xml](app/src/main/res/layout/activity_main.xml): toolbar, banners, nested scrolling, session actions, timeline, SRS breakdown, level/JLPT/Joyo progress, and subject tables. The activity binds existing IDs and LiveData; launch handlers call the existing Session model.
- Composite views include `AvailableSessionsView`, `SessionButtonsView`, `SrsBreakDownView`, `LevelProgressView`, and `SubjectInfoView`. These wrap conventional layouts and controls. `LevelDurationView`, `UpcomingReviewsView`, and `SyncProgressView` are specialized text views.
- Custom drawing includes `TimeLineBarChart`, `LevelProgressBarView`, `Post60ProgressBarView`, `PitchInfoDiagramView`, `StrokeDiagramView`, and `SubjectInfoButtonView`. `RigidGridLayout` implements custom layout. Replacing these is more involved than restyling their containers.
- [SessionActivity.updateFragment()](app/src/main/java/com/smouldering_durtles/wk/activities/SessionActivity.java), around line 158, selects lesson, unanswered, answered, Anki, and summary fragments through [Session.getNewFragment()](app/src/main/java/com/smouldering_durtles/wk/model/Session.java), around line 1089. The model therefore has presentation coupling to untangle before a complete UI replacement.
- [UnansweredSessionFragment](app/src/main/java/com/smouldering_durtles/wk/fragments/UnansweredSessionFragment.java) owns input transformations, editor actions, focus, question sizing, and IME insets. Treat review input as behavior, not just decoration.
- [BrowseActivity](app/src/main/java/com/smouldering_durtles/wk/activities/BrowseActivity.java) navigates overview, search, subject details, and logs. [PreferencesFragment](app/src/main/java/com/smouldering_durtles/wk/fragments/PreferencesFragment.java) loads AndroidX preferences with custom dialogs from [preferences.xml](app/src/main/res/xml/preferences.xml).

## Theme findings and migration choices

[styles.xml](app/src/main/res/values/styles.xml) defines six AppCompat-based theme styles. [ActiveTheme](app/src/main/java/com/smouldering_durtles/wk/enums/ActiveTheme.java) associates styles with subject/SRS/progression/Anki palettes and lazy customization loading. [GlobalSettings.Display](app/src/main/java/com/smouldering_durtles/wk/GlobalSettings.java), around line 789, chooses `theme` or `nightTheme` based on system night mode. Custom colors are stored as `theme_customizations_<ENUM_NAME>`.

[WkApplication.getTheme()](app/src/main/java/com/smouldering_durtles/wk/WkApplication.java), around line 219, and `AbstractActivity.getTheme()`, around line 661, cache and apply legacy themes. [ThemeUtil.getColor()](app/src/main/java/com/smouldering_durtles/wk/util/ThemeUtil.java) resolves colors from the application context, so it cannot see an activity-specific or view-specific dynamic overlay. It returns red if resolution does not produce a color integer.

Important compatibility constraints:

- Legacy `colorPrimary` often means ordinary text color (black in the light theme), while Material 3 uses it as an accent. Migrated ordinary text should use `onSurface` roles.
- Some custom views cache colors during construction; a palette change needs recreation or explicit refresh.
- Some code checks specifically for `ActiveTheme.LIGHT`. New theme identities do not automatically follow the intended branch.
- The app declares `colorPrimaryInverse`, also defined by Material. Verify resource merging and semantics when introducing MDC; a collision-related build failure has not been demonstrated.
- A global parent-theme replacement can change control inflation, dimensions, background tinting, typography, and review contrast.
- Separate interface style, static/dynamic Material palette, and learning palette. Do not harmonize subject colors automatically.
- Provide explicit static light/dark fallbacks; use dynamic colors on supported Android 12+ devices. Do not assume SDK version alone proves dynamic-color availability.

**Views-based Material 3:** least disruptive, good for small upstream contributions, compatible with existing Java/XML patterns. MDC is now in maintenance mode. MDC 1.13 supports API 21; 1.14 requires API 23. Transitive dependency changes still require regression testing.

**Hybrid Compose:** preferred candidate for the personal fork. Introduce a coherent dashboard screen with explicit UI state and actions. Reuse existing repositories/models through adapters, preserve SessionActivity, and handle lifecycle, saved state, and insets deliberately. Kotlin and dependency setup add initial cost. Current AndroidX releases generally default to API 23; choose minimum SDK based on actual devices rather than automatically preserving or dropping API 21.

**Full Compose:** possible later, but not required for a modern app. It entails input, navigation, session presentation, preferences, and custom rendering changes. Do not rewrite data/networking simply to match the new UI toolkit.

Sources: [MDC guidance](https://github.com/material-components/material-components-android/blob/master/docs/getting-started.md), [MDC releases](https://github.com/material-components/material-components-android/releases), [dynamic colors](https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md), [Compose in Views](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/compose-in-views), [AndroidX minimum SDK policy](https://developer.android.com/jetpack/androidx/versions).

## Build and test baseline

Update: original schemas 48–67 have since been restored from commit `b36c696`.
The full suite now passes (25 tests on Android 16), including the new all-theme
Guru palette test. Both DARK and NORD shared the indexing defect and are fixed.
The results below describe the original investigation baseline.

The debug app and instrumentation APK built successfully using the existing tracked identification files. These were not replaced. See the [README](README.md) for the identification templates required when preparing a separately branded build.

Commands used:

```sh
ANDROID_HOME=/home/nicbat/Android/Sdk ./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest
ANDROID_HOME=/home/nicbat/Android/Sdk ANDROID_SERIAL=emulator-5580 ./gradlew connectedDebugAndroidTest
```

The first attempt without the environment override failed to locate the SDK. Gradle installed Build Tools 36 using an already accepted SDK license. No project configuration changes were needed.

- Local unit tests: `NO-SOURCE`.
- Instrumentation on an Android 16 emulator: 21 executed, 1 passed, 20 failed.
- All 20 database migration tests fail because historical Room schema assets 48–67 are missing. This does not establish that the migrations themselves are wrong. Recover authentic historical schemas; do not fabricate them from the current database or disable the tests to obtain a green result.
- The notification test passed, but it is not meaningful review/lesson UI coverage.
- The existing emulator was run read-only without snapshot saving, then stopped.
- Generated local report: `test-reports/connected/debug/index.html` (not a portable committed artifact).

## Bug and reliability backlog

### Concrete source finding

**Dark-theme Guru color customization indexing:** `ActiveTheme.DARK.loadCustomizations()` reads the two passed-stage overrides with `custom.get(i+14)` around line 288. The customization UI assigns Guru I/II to slots 15 and 16; other theme implementations read `i+15`. Thus the DARK implementation overlaps the preceding customization slot and misses slot 16 for this pair. Add a focused test with distinct sentinel colors, reproduce in the UI, then correct the offset. Sources: [ActiveTheme](app/src/main/java/com/smouldering_durtles/wk/enums/ActiveTheme.java), [ThemeCustomizationActivity](app/src/main/java/com/smouldering_durtles/wk/activities/ThemeCustomizationActivity.java), lines 82–83 and 150–151. Source inconsistency confirmed; runtime reproduction pending.

### Reported bugs to reproduce

- **Offline audio timeout:** upstream [#146](https://github.com/jerryhcooke/smouldering_durtles/issues/146) reports slow preparation without network/local audio. [AudioUtil](app/src/main/java/com/smouldering_durtles/wk/util/AudioUtil.java) still contains synchronous `MediaPlayer.prepare()` calls, including lines 628 and 682. Trace callers and cancellation before deciding on asynchronous preparation or a player abstraction. Test airplane mode, slow networks, rapid subject changes, and audio focus. A current main-thread freeze has not been established here.
- **Search keyboard focus:** upstream [#168](https://github.com/jerryhcooke/smouldering_durtles/issues/168). Reproduce cold launch, returning from another screen, rotation, and hardware keyboards before changing focus logic.
- **New-phone restore problems:** upstream [#148](https://github.com/jerryhcooke/smouldering_durtles/issues/148). Audit restore, encrypted preferences, authentication recovery, and cached data. The issue report does not establish a root cause in this checkout.

### Reliability audits rather than confirmed defects

- [AppDatabase.getInstance()](app/src/main/java/com/smouldering_durtles/wk/db/AppDatabase.java), around line 368, enables destructive fallback when a migration path is unavailable. Identify locally irreplaceable data and pending reviews before changing recovery policy. This is a risk to investigate, not evidence that routine upgrades currently erase data.
- [BackgroundSyncWorker.doWork()](app/src/main/java/com/smouldering_durtles/wk/services/BackgroundSyncWorker.java) marks the hour before work, can sleep five seconds, wraps work in `safe()`, and returns success. Audit interaction with the existing API task retry mechanism before changing WorkManager outcomes; do not add competing retries blindly.
- Add process-death/offline recovery tests for partially completed sessions and queued submissions. Protect against duplicate submissions during retries and concurrent use of the upstream app and fork.
- Add a visible sync state with last successful sync, pending operations, and actionable errors, without logging tokens or answer data unnecessarily.

## Performance opportunities requiring measurements

No performance improvement percentages are claimed. Establish startup, dashboard scroll frame time, answer-to-next-question latency, memory, and background activity measurements first.

- [MainActivity.onResumeLocal()](app/src/main/java/com/smouldering_durtles/wk/activities/MainActivity.java), around line 210, force-refreshes many live models. Profile redundant queries and updates; coalesce or invalidate selectively if traces justify it.
- [LiveSubjectTableView.update()](app/src/main/java/com/smouldering_durtles/wk/views/LiveSubjectTableView.java) builds/binds table rows rather than virtualizing a list. It already reuses existing rows. Measure actual list sizes before replacing it with RecyclerView or a Compose lazy list.
- Profile chart drawing allocations and invalidations. Retain custom charts if they are fast and usable; adding a generic chart library is not automatically an optimization.
- Measure audio startup and cache misses, then improve preload/cancellation/cache policy without increasing background downloads unexpectedly.
- Release minification is disabled in `app/build.gradle`. Evaluate R8/resource shrinking in an isolated release build, with reflection/serialization and backup round-trip tests. Add Baseline Profiles after real user flows stabilize.

Use [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) for startup and interaction measurements. Benchmark representative release builds on a physical device; emulator/debug timing is not a performance claim.

## Widget roadmap

Widgets already exist. [SessionWidgetProvider](app/src/main/java/com/smouldering_durtles/wk/services/SessionWidgetProvider.java) uses RemoteViews, selects three layouts by size, shows lessons/reviews and upcoming availability, and opens MainActivity. [Provider metadata](app/src/main/res/xml/session_appwidget_info.xml) supports resizing and requests hourly updates. Background alert processing also updates widgets when counts change.

Potential upgrades:

- Small review-count widget, separate lesson widget, and a wider combined overview.
- Material You colors, explicit light/dark fallbacks, rounded surfaces, and good resize behavior.
- Separate Review, Lessons, and Resume actions routed through session checks rather than bypassing existing session logic.
- Last-updated/stale state and an explicit refresh action with throttling.
- Upcoming-review timeline or next-review time for larger sizes.
- Per-widget configuration and a preview/pin flow.
- Accessibility labels and coherent behavior across launchers, font sizes, reboot, offline operation, and process death.

Evaluate open [PR #153](https://github.com/jerryhcooke/smouldering_durtles/pull/153) before rebuilding the same work. Review its diff, attribution, compatibility, and update behavior before selectively porting it; do not assume it has been merged or tested locally.

RemoteViews is the shortest path to improving the existing implementation. [Jetpack Glance](https://developer.android.com/develop/ui/compose/glance) is an alternative for a Kotlin-based fork, but its composables are not interchangeable with ordinary Compose UI components. An in-app Compose dashboard cannot simply be reused as a widget. Updates remain subject to Android background scheduling; do not promise an exact live countdown.

## UI and daily-use ideas

- A calmer dashboard with one prominent next action, review/lesson counts, upcoming work, and optional statistics. Offer comfortable and compact density and configurable section visibility/order.
- Material 3 typography, dynamic colors, deliberate spacing, edge-to-edge layout, consistent menus/dialogs, and accessible touch targets.
- Searchable settings organized around Study, Appearance, Audio, Sync, and Data, retaining advanced options without overwhelming the default view.
- Better search with immediate focus, recent searches, filters, and clear empty states.
- Pull-to-refresh with an explicit completion/error state and last-sync timestamp.
- Better review presentation after parity tests: uncluttered subject header, readable answer feedback, consistent audio and synonym actions, and stable keyboard positioning.
- Optional daily goals, completed-today counts, and session summaries that distinguish local activity from server-authoritative data.
- Improved offline audio management and a clear indication of what is downloaded.
- Later: tablet layouts, high-contrast/e-ink appearance, and richer subject browsing. Keep learning/answer acceptance changes separate from visual redesign.

## Original proposed personal-fork milestones (historical)

1. **Independent development build and regression baseline.** Choose name/application ID; audit provider authorities, URI schemes, test IDs, and signing for side-by-side installation. Keep secrets and signing keys outside Git. Confirm backup/export/import behavior and use fixture data or a controlled test account for automated session tests. Recover migration fixtures and add review/session regression tests. Planning estimate: 3–7 focused engineering days, excluding unavailable historical fixtures.
2. **One modern dashboard screen.** Add Kotlin/Compose and a small explicit dashboard state/action adapter. Preserve SessionActivity and existing launch logic. Implement static/dynamic colors, semantic learning colors, error/offline/setup states, and an easy return to Classic. Estimate: 1–2 weeks including device validation.
3. **Widgets and selected bug fixes.** Review #153, modernize one widget, reproduce audio/search issues, and fix the theme index error with regression coverage. Split independent work into separate changes. Estimate: 1–2 weeks, depending on reproduction and widget scope.
4. **Measured optimization and settings/search redesign.** Work from traces, not code aesthetics. Introduce changes in small batches with before/after evidence. Estimate: 1–3 weeks for selected targets.
5. **Review/lesson UI overhaul.** Introduce presentation adapters and migrate one session state at a time. Validate grading, kana handling, undo behavior where supported, audio, offline queues, interruption, process death, and summaries. Estimate: 2–4+ weeks; highest risk and greatest uncertainty.

Estimates assume an experienced developer, available fixtures/device testing, and reuse of core logic. AI assistance changes coding throughput but does not eliminate validation. Upstream-compatible bug fixes can still be contributed independently from the personal UI direction.

## Original smaller Views prototype retained as an alternative

An optional Material dashboard action panel could change `app/build.gradle`, `GlobalSettings.java`, `res/xml/preferences.xml`, `MainActivity.java`, and `views/SessionButtonsView.java`; add `util/DashboardTheme.java`, `layout/session_buttons_material.xml`, static colors/themes/strings resources, and an instrumentation test. Preserve control IDs and callbacks and apply a complete Material base theme plus optional dynamic overlay only to that panel. Leave `ActiveTheme`, `WkApplication`, `AbstractActivity`, and session fragments unchanged initially. Estimate: 3–5 days.

This remains the smaller upstream-friendly option; it is not a required detour before a Compose dashboard.

## Decisions still open

- Actual phone/tablet models, Android versions, and launcher; whether API 21–22 support matters for personal use.
- Public distribution and signing/update process. Name and application IDs are now chosen above.
- Future selective Compose use; the current implementation uses Compose only for the dashboard overview.
- Whether Classic remains permanently or only during migration.
- Which custom themes/palettes must carry forward exactly.
- Specific optimization targets after phone feedback; widgets are explicitly last.
- A safe fixture/test-account strategy and how to migrate local data into the separately installed fork.

Agreed implementation direction: **a separately installable personal build with a complete modern in-app presentation pass, followed by refinement and measured optimization, with widgets last.** Preserve existing study logic and independently test changes. Earlier estimates above are historical, not remaining-work estimates.
