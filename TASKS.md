# Smoky Durtles working tasks

Start here for current work. Read [HANDOFF.md](HANDOFF.md) for setup and session handoff;
[FORK_PLAN.md](FORK_PLAN.md) holds architecture findings and the longer backlog.

## Current milestone

November 3 feedback: quiet connection status, stationary SRS toasts, compact critical items,
persisted collapsible dashboard sections and immediate modern study page changes.

Dashboard iteration: hourly arrivals separate from backlog, compact recent items and rounded
multicolor progress, one-row/split SRS keys, mute consistency, lighter item paging, and
shared level-up-path counts/priority/notifications. Continue using [UI_GUIDE.md](UI_GUIDE.md).
Widgets remain last; preserve grading, scheduling, eligible queues and stored palettes.

The last agent-confirmed phone installation was **5749f22**. The user now has
`./install-phone.sh` and may have updated independently; installed revision has not been
rechecked. Do not run instrumentation on the phone. Push tested changes to
`origin/smoky-durtles/foundation`; installation remains a separate user action/request.

## Current small-changes pass

- [x] Connection errors use an overflow red dot and Retry action; recovery clears both.
- [x] SRS notices use a separate toast window, preserving editor geometry, focus and draft.
- [x] Critical-condition subjects share the compact grid and retain accuracy/navigation context.
- [x] All modern dashboard sections have independent persisted collapse controls, including setup/help.
- [x] Embedded overview explicitly sets its theme foreground; dark mode no longer falls back to black for plain labels/messages.
- [x] Modern study transitions replace immediately; Classic keeps its existing animation setting.
- [x] Debug/release/test builds and all **136 tests pass on Android 16**; populated/light/dark/large-text/IME screenshots reviewed. Evidence in HANDOFF.
- [ ] Physical-device feedback after the user installs with `./install-phone.sh`.

## Current dashboard iteration

- [x] Tapped-day hourly bars with exact counts and day navigation; remove nested statistical details.
- [x] Exclude available-now backlog from upcoming-hour colors and counts; show it separately.
- [x] Compact recent unlock/burn tiles retain the latest-ten/30-day query and subject navigation.
- [x] Rounded 6dp multicolor level bars share Jōyō/JLPT geometry and keep exact counts/90% markers.
- [x] Compact SRS key plus persisted Apprentice I–IV/Guru I–II split, exact counts and search actions.
- [x] Disable/re-enable Listen controls on mute changes and activity return.
- [x] Reuse adjacent detail fragment/back-stack entry; prefetch before short fades, cancel stale loads.
- [x] Shared level-up path from unpassed current-level kanji/prerequisites and permanent passedAt.
- [x] Available path counts and independent accessible lesson/review priority switches on dashboard/settings.
- [x] Notifications include positive path counts; omit all zero-count categories and update silently.
- [x] Debug/release/test builds; **133 tests pass on Android 16 and Android 11**;
  populated visual review complete and exact evidence recorded in HANDOFF.
- [ ] Physical-device feedback on motion and forecast/legend density after `./install-phone.sh`.

## Current phone-feedback pass

- [x] Related subject details share the compact headline, audio, divider and metadata treatment.
- [x] Disable inherited elevation/translationZ animations on styled modern buttons.
- [x] Tap translations to reveal or re-blur; hold either language to copy the original sentence.
- [x] Suppress singleton search headings at either level; retain ordering, counts and collapse tags.
- [x] Daily forecast strips: first week without internal scrolling, tappable days/hours, full horizon.
- [x] Local calendar/DST aggregation, exact daily/category/cumulative/level-up counts; Classic retained.
- [x] Flat SRS transition notice with old/new stages outside the question glyph.
- [x] Debug/release/test builds pass; **122 tests pass on Android 16 and Android 11**.
  Populated/light/dark/large-text captures inspected; evidence recorded in HANDOFF.
- [ ] User iteration on forecast density and interactions after installing with `./install-phone.sh`.

## Lesson and curriculum panel cleanup

- [x] Compact adaptive lesson headline; stable Previous/Next footer and lesson position.
- [x] Preserve accepted/primary reading information, hidden-reading reveal, audio/pitch,
  image radicals, smaller/zero glyph settings, navigation guards and original callbacks.
- [x] Metadata behind Details, expansion restored; quiet Add synonym and divider treatment.
- [x] Jōyō/JLPT: compact progress rows with honest tracked-kanji denominator and tappable
  exact stage counts; maintain Classic, existing ordering and panel visibility settings.
- [x] Record lesson-specific layout and semantic-density lessons in UI_GUIDE.
- [x] Debug/release/test builds pass; **114 tests pass on both Android 16 and Android 11**.
  Refreshed light/dark, 2x-text, landscape, image-radical and panel screenshots inspected.
- [ ] User validation with real lessons; install using `./install-phone.sh` when ready.

## Phone install command

- [x] `./install-phone.sh`: check/reuse the latest local debug build, install over USB,
  preserve data, and open the app. Gradle rebuilds only changed inputs.
- [x] Verify missing/ambiguous/unauthorized/disconnected phone and build/install failures;
  unchanged actual build reports all 39 tasks up-to-date. See README/HANDOFF.

## Current density/color pass

- [x] Single item-accuracy summary, compact subject breakdown, real empty state, retained actions.
- [x] Learning colors preference: Subtle modern default / Neutral / Original; stored palettes intact.
- [x] Apply paired fills/ink to review, subject cards, logs and dashboard labels; label search types.
- [x] Compact level rows with 8dp bars and original exact counts/90% target/search actions.
- [x] SRS total + 12dp distribution + adaptive tappable stage legend and exact counts.
- [x] Forecast overview, measured tick spacing, hourly detail dialogs, accessible list and Neutral mode.
- [x] Update UI guide with reusable color and data-density principles.
- [x] Populated/large-text screenshots reviewed; debug/release/test builds pass including release lint.
- [x] **101-test full suites pass on Android 16 and Android 11**; final palette/detail additions
  pass all **9 focused dashboard tests on both**. Current suite has 104 tests; see HANDOFF.
- [x] Post-60 and available-study colors follow the preference; named Post-60 details preserve meaning.
- [ ] Install on request; user validation with actual review/lesson data and preferred color mode.

## Current refinement

- [x] Record UI principles, official references, source boundaries and review checklist in UI_GUIDE.
- [x] Separate review prompt/input/actions, quiet the question-type label, support constrained IME space.
- [x] Responsive search grid and compact metadata; keyword-first browse, readable filter units.
- [x] Shared button hierarchy, consistent spacing, dashboard/font/download/utility refinements.
- [x] Inspect refreshed light/dark, narrow/2x, wide-grid, IME and scrolled review captures.
- [x] Debug/release builds; **80 tests pass on Android 16 and Android 11**. Focused attached-view
  capture/contrast test also passes after a test-only fixture correction. See HANDOFF for logs.
- [x] Install and launch refinement build 5749f22 on Pixel 9 Pro, preserving app data.
- [x] Feedback on refined build received; addressed in current density/color pass.

## Completed

- [x] Separate Smoky Durtles app IDs, authorities, links, labels, and upstream credits.
- [x] Reproduce and fix toolbar search requiring two taps.
- [x] Three foundation instrumentation tests; debug/release builds; side-by-side install.
- [x] Push foundation to `origin/smoky-durtles/foundation` (`0b6b531`).

## Dashboard foundation (historical)

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

The first presentation pass is implemented across app-owned screens. Existing Java Views,
models and callbacks remain; Compose is confined to the dashboard overview. Custom charts,
subject renderers, the color wheel, Android document pickers and keyboard retain their
specialized behavior. This is coverage completion, not a claim that every device/flow has
been exhaustively tested.

- [x] Browse overview, level navigation, saved searches, preset dialogs, advanced fields,
  switches/selectors, sorting, refine/back navigation and result action dialogs.
- [x] Subject headline, metadata, meanings/readings, mnemonics, examples, related cards,
  pitch/stroke chrome, synonyms and contextual actions. Metadata wraps at 2x text.
- [x] Detail previous/next replaces one detail back-stack entry; one Back returns to results.
- [x] Lesson, unanswered/answered review, Anki, self-study setup, session controls,
  confirmation dialogs, log rows/groups and summary presentation.
- [x] Offline in-memory presentation fixtures, question draft preservation across recreation
  and Modern/Classic switching; log collapse/click behavior. No grading/submission writes.
- [x] Recent/critical/burned subject tables, remaining Jōyō/JLPT/lesson-review statistics,
  setup/help/error panels and existing dashboard navigation. Statistics reflow at large text.
- [x] Sliders, outlined range/URL editors, theme/color customization, font selection/import,
  contextual menus and consistent appearance labels; existing preference keys preserved.
- [x] About/Support/setup help, keyboard/digraph help, audio downloads, import/export,
  burn/resurrect and confirmation/progress presentation. System file pickers remain native.
- [x] Shared Material surfaces, stable-class buttons, neutral text, toolbar/dialog/dropdown
  appearance and fallback system-bar contrast. Subject/SRS colors stay independent.
- [x] Modern interface / Wallpaper colors in interface labels; independent dashboard switches.
- [x] Debug/release builds and full suite: **75 tests, 0 failures on Android 16 and Android 11**.
  API 21 utility tests pass (3); the complete suite hits the existing Jackson blocker below.
- [ ] Physical-phone long review/lesson sessions, keyboard/insets, landscape and accessibility review.
- [ ] Actual wallpaper-change refresh on Android 12+, TalkBack and broader custom-palette checks.
- [ ] Process death/offline recovery and pending-submission parity fixtures (not covered by UI tests).
- [ ] Resolve pre-existing API 21 Jackson ObjectMapper initialization failure or decide supported minimum.

Automated checks cover static/dynamic theme paths, light/dark, 2x font fixtures, synthetic
semantic colors, callbacks, navigation and recreation. They do not prove network submissions,
real account sync, audio playback or process-death recovery. See HANDOFF for exact platform results.

## Phase 2 — refine, fix and optimize

- [x] Apply first phone-feedback refinement: review geometry, responsive search density, shared action hierarchy.
- [ ] Gather feedback on the refined build and adjust remaining navigation/density issues.
- [ ] Refine dashboard section ordering and other usability details after the full flow is available.
- [ ] Profile startup, scrolling, memory, chart rendering and audio; make measured optimizations.
- [ ] Investigate offline audio delays, cancellation/focus and synchronization/recovery edge cases.
- [ ] Broader physical-device validation and longer study sessions with user feedback.
- [ ] Durable release signing and update delivery; do not commit signing keys.

## Phase 3 — widgets last

- [ ] Inspect upstream PR #153, then design and test widget support after the in-app work above.

Installed milestones: dashboard, settings 4472ee8, overflow/charts b074d72 and search ab784d0,
followed by full UI build **6bbe197**, all preserving app data. The latest build launched
successfully; user validation remains pending. Build/test evidence and compatibility
limitations are recorded in HANDOFF.md.

## Open decisions

- Phone is Pixel 9 Pro; Android version/launcher behavior and minimum supported SDK (currently 21) remain to confirm.
- Permanent Classic support versus transition-only fallback.
- Public distribution and release signing. The fork already exists at `nicbat/smouldering_durtles`.

Update checkboxes and evidence after each milestone. Record partial work honestly;
do not mark a passing focused test as a passing full suite.
