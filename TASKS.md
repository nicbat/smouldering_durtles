# Smoky Durtles working tasks

Start here for current work. Read [HANDOFF.md](HANDOFF.md) for setup and session handoff;
[FORK_PLAN.md](FORK_PLAN.md) holds architecture findings and the longer backlog.

## Current milestone

Complete modern design across the whole in-app experience before the dedicated
optimization/refinement phase. Next implementation batch: browse overview and advanced filters,
then item details and study flows. Widgets remain last. See the ordered coverage checklist below.
Development and emulator testing are authorized. Search build ab784d0 is installed on the user's
Pixel 9 Pro with adb install -r; physical-device behavior validation remains pending.
Do not run instrumentation tests on that phone or use its account data.
Push tested milestones to the user's fork on the current feature branch.

## Completed

- [x] Separate Smoky Durtles app IDs, authorities, links, labels, and upstream credits.
- [x] Reproduce and fix toolbar search requiring two taps.
- [x] Three foundation instrumentation tests; debug/release builds; side-by-side install.
- [x] Push foundation to `origin/smoky-durtles/foundation` (`0b6b531`).

## Latest completed milestone

- [x] Restore original Room schemas 48–67 from `b36c696`; full suite passes (25 tests, Android 16).
- [x] Fix DARK and NORD Guru customization slots; all-theme palette regression test passes.
- [x] Add a Compose dashboard overview with real counts and existing session actions.
- [x] Keep Classic selectable and semantic subject colors unchanged.
- [x] Exercise loading, no-work, vacation, active-session, disabled controls and large text in Compose tests; inspect light/dark/large-text screenshots.
- [x] Build debug/release APKs and pass the full suite: **32 tests, 0 failures on Android 16**. Commit/push this milestone with the updated handoff.

## Settings milestone

- [x] Material Components Views 1.12.0, scoped to PreferencesActivity; API 21 minimum retained.
- [x] Material switches, choice dialogs, outlined text editors, confirmation dialogs, and themed toolbar/overflow menus.
- [x] Material dialog shells for the existing URL and number-range editors, preserving their field logic.
- [x] Independent Modern settings and wallpaper-color controls, with Classic fallback and system light/dark mode.
- [x] Fix legacy white toolbar icon contrast and informational text contrast in the modern theme.
- [x] Debug/release builds and full Android 16 suite: **42 tests, 0 failures** (ten settings regressions).
- [x] Inspect light/dark settings, choice/edit dialogs, and overflow menu screenshots; focused system-bar contrast test passes.
- [x] Update handoff and push the settings milestone to the fork.

## Overflow and dashboard panels milestone

- [x] Rounded, padded Material overflow popups, including legacy activities; light/dark and optional wallpaper colors.
- [x] Scoped dashboard Material theme, readable toolbar icons, and matching system bars.
- [x] Titled review forecast with larger scalable labels and wrapping chart legends.
- [x] SRS distribution rows retain stage colors and existing search filters.
- [x] Level rows show passed/total counts; time on level returns to the overview; keep 90% target logic.
- [x] Updated surfaces for level/Post-60/Jōyō/JLPT/breakdown panels; Classic remains selectable.
- [x] Offline synthetic fixtures cover counts/colors/search callbacks, hidden panels, dark/large text, and original layouts.
- [x] Real MainActivity recreation and overflow navigation tests; fix MaterialButton saved-state mismatch on return to Classic.
- [x] Debug/release builds succeed; **48 instrumentation tests pass, 0 failures on Android 16**.
- [x] Inspect light/dark, populated charts, 2x text, and rounded overflow screenshots; update docs and push.

## Search and consistency milestone

- [x] Material search result cards, scalable group headings, and loading/empty/error messages.
- [x] Keep grouping, collapse/expand, subject ordering/IDs, click navigation, and WaniKani colors.
- [x] Scoped Material browsing toolbar and search input; study and detail-body themes stay isolated.
- [x] Existing Modern settings / wallpaper toggles now cover search too; labels explain their scope.
- [x] Share static accent resources between Compose dashboard, settings, search, and overflow menus.
- [x] Fix Material/Classic tutorial-button state mismatch and a theme-change/menu callback race.
- [x] Debug/release builds and full Android 16 suite: **52 tests, 0 failures**.
- [x] Inspect light and dark/2x-font search captures with synthetic subjects; no account/DB writes.
- [x] Install search update ab784d0 on the Pixel 9 Pro with adb install -r, preserving app data. Physical-device behavior validation remains pending.

## Phase 1 — complete modern UI coverage

User direction: finish a coherent first pass across **all in-app screens**, then optimize and
adjust them as a whole. Deliver and push testable batches; do not postpone correctness fixes,
behavior regression tests, or basic accessibility until Phase 2. No implementation blocker;
the task emulator is stopped at handoff. Existing Classic fallback remains during migration.

1. **Browse and filters**
   - [ ] Browse overview, level navigation, saved searches, preset management.
   - [ ] Advanced search fields, selectors, sorting, filter actions and refine/back navigation.
   - [ ] Result action dialogs: saving presets, star ratings and confirmations.
   - Source: `BrowseOverviewFragment`, `AdvancedSearchFormView`, `SearchResultFragment`,
     `fragment_browse_overview.xml`, `advanced_search.xml`, `search_result_form.xml`.
2. **Item details and study materials**
   - [ ] Subject headline, meanings/readings, mnemonics, examples, related subjects, pitch information.
   - [ ] Audio, copy/link actions, synonyms/notes editors and contextual dialogs.
   - [ ] Previous/next item navigation and returning to the same results/scroll position.
   - Source: `SubjectInfoFragment`, `SubjectInfoView`, `SubjectInfoHeadlineView`,
     `StudyMaterialsActivity`, `subject_info*.xml`, `activity_study_materials.xml`, `synonym.xml`.
   - Shared subject Views also appear in lessons/reviews: scope styling and verify both consumers.
3. **Entire study flow**
   - [ ] Establish offline fixtures for question/answer navigation, interruption/resume and pending submissions.
   - [ ] Lesson presentation and lesson-to-quiz transition.
   - [ ] Review answer entry, keyboard/focus, correct/incorrect feedback and answer actions.
   - [ ] Anki mode, self-study setup, session controls, wrap-up/abandon confirmations.
   - [ ] Session log/history and completion summary, including result filtering/actions.
   - Source: `SessionActivity`, `SelfStudyStartActivity`, `AbstractSessionFragment` and its
     Lesson/Unanswered/Answered/Anki/Summary subclasses, `SessionLogFragment`, `session_buttons.xml`.
   - Preserve grading, scheduling, answer overrides, audio and submission behavior.
4. **Finish the dashboard**
   - [ ] Recent, critical and burned subject tables and their navigation.
   - [ ] Remaining Post-60/Jōyō/JLPT/breakdown interiors; these received surfaces, not a full redesign.
   - [ ] Setup, loading, sync/error/retry and empty states outside the new overview.
   - Source: `MainActivity`, `LiveSubjectTableView`, dashboard custom Views and `activity_main.xml`.
5. **Finish settings and customization**
   - [ ] Sliders, number-range/URL editor interiors, remaining contextual menus.
   - [ ] Theme/custom-color editor, font selection/import and custom preference rows.
   - [ ] Explain appearance controls consistently across all migrated screens; preserve existing preference values.
   - Source: `PreferencesFragment`, `ThemeCustomizationActivity`, `FontSelectionActivity`,
     `FontImportActivity`, `pref_number_range.xml`, `pref_tagged_url.xml` and `preferences.xml`.
6. **Utility and help screens**
   - [ ] API-key/setup help, About/Support, keyboard and digraph help.
   - [ ] Audio downloads, data import/export and their progress/error/confirmation UI.
   - [ ] Burn/resurrect screens and confirmations; preserve their safeguards and behavior.
   - [ ] Check backup/restore handoffs; Android-owned file pickers retain system UI.
   - Source: corresponding `activities/*Activity.java` and `activity_*.xml`; `BackupActivity`
     launches system document pickers rather than providing a custom page.
7. **App-wide completion check**
   - [ ] One consistent set of surfaces, typography, spacing, corners and action hierarchy.
   - [ ] Toolbar/navigation, dropdowns, dialogs, inputs, progress/error/empty states and transitions.
   - [ ] Touch targets, accessibility labels, large fonts, landscape, keyboard and system-bar insets.
   - [ ] Light/dark, static/dynamic color, custom subject/SRS colors, API 21/30 fallback and API 31+ wallpaper changes.
   - [ ] Classic/modern switching, back stack and recreation across the complete study/browse flow.

Phase 1 is complete when every user-facing app-owned screen/action has been checked and uses
the shared modern design when enabled, with any intentional exceptions recorded. A rounded
menu alone does not mean the underlying screen is migrated. Preserve custom charts and Views
where appropriate; modern design does not require rewriting every screen in Compose.

## Phase 2 — refine, fix and optimize

- [ ] Use phone feedback to adjust density, search card sizes, information hierarchy and navigation friction.
- [ ] Refine dashboard section ordering and other usability details after the full flow is available.
- [ ] Profile startup, scrolling, memory, chart rendering and audio; make measured optimizations.
- [ ] Investigate offline audio delays, cancellation/focus and synchronization/recovery edge cases.
- [ ] Broader physical-device validation and longer study sessions with user feedback.
- [ ] Durable release signing and update delivery; do not commit signing keys.

## Phase 3 — widgets last

- [ ] Inspect upstream PR #153, then design and test widget support after the in-app work above.

Installed milestones: dashboard, settings 4472ee8, overflow/charts b074d72 and search ab784d0,
all preserving app data. Current baseline: **52 passing instrumentation tests**, debug/release
builds successful. This roadmap update changes documentation only.

## Open decisions

- Phone is Pixel 9 Pro; Android version/launcher behavior and minimum supported SDK (currently 21) remain to confirm.
- Permanent Classic support versus transition-only fallback.
- Public distribution and release signing. The fork already exists at `nicbat/smouldering_durtles`.

Update checkboxes and evidence after each milestone. Record partial work honestly;
do not mark a passing focused test as a passing full suite.
