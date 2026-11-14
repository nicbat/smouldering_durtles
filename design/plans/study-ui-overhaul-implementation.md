# Study UI overhaul implementation

Implemented 2026-11-09 against [the behavior plan](study-ui-overhaul.md) and [v8 composition reference](../explorations/study-overhaul-v8.html). Native Android views and existing study/session machinery remain the foundation.

## Follow-up from phone feedback

- Embedded dashboard overview uses a rectangular transparent surface so its top-left level label is not clipped by a second rounded corner.
- Forecast has a single compact 24 hours / 7 days selector, no Extended button, a short axis legend, and bars occupying 70% of each slot rather than 28%.
- Stroke diagrams again loop according to the existing animation preference, with upcoming strokes rendered at 25% opacity. Playback controls are hidden by default and available through “Show stroke playback controls” in settings. System-disabled animations remain respected.
- Mnemonic highlights resolve light/dark contrast from the displayed text, with semantic colored text, a tinted background and a 4.5:1 contrast floor.
- Follow-up validation: debug compilation and `git diff --check` passed; no instrumentation tests were run for this feedback pass. Phone visual confirmation remains pending. Settings retirement is discussion-only at this point.

## Full-page session selection — 2026-11-14

Lesson batch and Custom reviews now use `SessionSelectionActivity` instead of a dialog,
with a toolbar, scrolling controls/preview and pinned primary Start button. Dashboard
lessons and overflow Custom reviews enter the appropriate mode. Choices survive recreation;
Start opens the session and removes selection from the back stack. A selection page resumed
after another session starts closes itself.

The page preserves semantic styling and falls back to themed text below 4.5:1 contrast.
The account-free recreation test blocks the token-setup redirect. Debug/test builds pass;
all four StudyOverhaul tests pass in each of light and dark mode. Both entry points,
titles, Start styling, preview contrast and session/back-stack transitions were manually
checked with offline synthetic data. Evidence: `captures/selection-page/`. The separate
background-session edge case and physical-device acceptance remain untested.

## Shared button spacing and colors — 2026-11-14

Choice grids now own 8dp internal and 12dp trailing group gaps, including stacked
lesson/review selectors, library/custom-quiz filters and stroke controls. Column fit
includes gaps. Semantic tinted fills, outlines and readable hue-preserving ink replace
the universal lavender fill; selected/unselected states differ visually and retain
checkmarks/accessibility state. Repeated utility styling preserves them. Soft subject selectors
use stable blue/pink/purple hues instead of legacy dark panel/button grays; Original
retains its stored palette.
Neutral mode still uses a single interface accent. Removed the page-specific monochrome
button fallback from selection.

Added missing 8dp action gaps in keyboard help, saved presets, previous/next subject
navigation and debug utility stacks. Regression coverage checks shared recoloring,
selected-state contrast in both themes, group spacing and recreation. Debug/test builds
pass. Final focused run: **16/16 pass** (13.964s), including the rendered fill assertion.
The broader run passed 149/150; the only failure is the stale `SessionPrioritySettingsTest`
expecting the pre-flattening settings destinations. Inspected real selection pages in
light/dark, toggled states and 2× text, plus dashboard/review/footer and keyboard help.
Evidence: `captures/button-spacing/`; `/tmp/smoky-buttons-final-focused-tests.log` and
`/tmp/smoky-buttons-final-tests.log`. No account-bearing phone or real submissions.

## Custom-review toggle stability and inline counts — 2026-11-14

Review toggles filter the loaded due snapshot immediately. Only initial/resume loads and
changed eligibility require a database refresh; Start continues to revalidate eligibility.
Unchanged choice buttons retain their drawable. Stage controls carry “N due” counts,
scoped to subject type/level independently of stage selection, replacing the duplicate
button-shaped breakdown. Regression coverage checks repeated Apprentice toggles, no
Loading transition, stable unrelated backgrounds, type/scope counts, and large-text labels.
Final focused validation: **18/18 pass** (17.689s); debug/test builds pass. Light/dark and
2× text checks plus repeated-toggle recording are in local `captures/review-counts/`.
The isolated emulator is stopped.

## Selection polish and spoiler prevention — 2026-11-14

Increased shared choice-group separation from 12dp to 20dp while retaining 8dp within
groups. Pinned Start lessons/reviews actions now have a 56dp minimum height, 16sp label
and 14dp vertical padding. Summary text is split into a clear selected-count heading
and smaller supporting totals/type counts. Lesson preview rows separate glyph, level and
meaning, and the maximum-lessons input has a persistent label.

Custom reviews no longer renders any individual subject/answer preview. Only aggregate
counts and SRS breakdown remain. Lessons keep their item preview. A regression test
injects a known subject and answer and verifies they are absent in reviews and present
in lessons, alongside the taller action's minimum height. Debug/test builds and all
17 focused checks pass (15.661s). Evidence: `captures/selection-polish/` and
`/tmp/smoky-selection-polish-tests.log`.

## Delivered

- Shared adaptive choice grids, subject identity colors, meaning/reading mnemonic cues, and SRS roles. Original/Neutral palette preferences remain available. Four-stage choices use a row, 2×2, or one column at extreme text sizes.
- Single scrolling lessons/details: visible parts of speech, expanded mnemonics, left-aligned example translations, compact related items at the bottom, reading-specific speaker controls, pitch paths above kana with Japanese · English pattern labels.
- Existing stroke data and renderer support looping animation and optional Play, Step, Reset, All strokes controls and a position label. System-disabled animations remain static. Missing data hides the writing section; no new stroke dataset or licensing assumptions. Writing stays behind the full-details reveal in reviews.
- Review textbox retained; first deliberate reveal shows only the attempted side. Second shows all details, including both mnemonics. Icon undo remains beside the input after an eligible previous incorrect answer, clearing the textbox through the existing undo path.
- Library root unifies browsing and live local text search, with filters for types, stages, level range, exact API part of speech, optional pitch, JLPT and Jōyō. Search results recycle views and reject stale asynchronous delivery. Saved searches and existing bulk result actions remain reachable. Toolbar quick search focuses the input and offers five results plus full results.
- Lesson selection: editable maximum with 3/5/10 shortcuts, types, All/Current scope, coverage and English aligned right. Final batch preview permits revisiting items before starting its quiz.
- Due-only custom reviews: type/stage selection, All/Current, presets, real eligible coverage and available due counts inside the stage filters, with no individual review previews. Eligibility is queried again before starting; a changed set requires reviewing the refreshed preview. Selection survives recreation.
- Custom quiz remains practice, with condensed filters and a matching-item preview. Practice missed items follows normal session finishing and pending-upload handling before starting a separate practice session.
- Summary counts completed radicals/kanji/vocabulary, question/error counts, elapsed wall time and missed question labels. Incomplete/abandoned items do not inflate completion totals.
- Compact dashboard count buttons retain level-up path counts, active-session/vacation/loading behavior, existing lower level bars and their passed/Guru rule. Toolbar search/menu; no bottom navigation.
- One forecast combines arrival bars occupying 70% of each slot and cumulative waiting line, with labelled left/right scales, hourly 24-hour and calendar-day 7-day horizons. Due-now items are included once. Caption layouts and formatting are cached; drawing skips offscreen slots. Accessible date selection is available without visible step/expand buttons.

## Original overhaul verification (before the follow-ups above)

- `assembleDebug assembleDebugAndroidTest assembleRelease lintRelease`: passed. Release lint has warnings but no errors.
- Android emulator regression suite covering browse, search, dashboard, study and forecast: **89 tests passed**. Earlier failures from retired screen structure/labels were updated to the new contracts; the full run then passed.
- Final grid-density/forecast-label follow-up: **7 focused layout, metadata, stroke, rotation and forecast checks passed**.
- `git diff --check`: passed.
- Screenshots reviewed for multi-kanji lessons, full review reveal, pitch at 2× text, stroke rendering, large-text stage choices, dashboard, library and custom-review selection. Local evidence: `captures/study-overhaul/`.
- Warmed software-Canvas forecast draw diagnostic (100 draws): approximately 1.12 ms at 360dp/1× and 0.87 ms at 280dp/2× on this emulator. Caption identity is asserted across repeated draws. These are current-only measurements, not a before/after speedup claim.

Screenshots use synthetic fixtures and an isolated Android emulator; no account-bearing phone was used and no real reviews were submitted. Two release-lint issues found during validation were addressed: notification permission rejection and the status-bar API annotation.

## Practical limits

- Pitch/JLPT/Jōyō/stroke information uses the existing optional reference data. No pitch is inferred from spelling. The current pitch model has no per-record source attribution to display. Known empty pitch data remains distinct from unavailable data in filters.
- Explicit meaning/reading tags have semantic colors. Contextual subject tags use the subject type and accepted meaning aliases to recognize the target; ambiguous prose/inflected references retain their source tag color. This is deliberately conservative, not a general natural-language classifier. Rich markup and links are preserved.
- Forecast software-Canvas timing is an emulator diagnostic, not an end-to-end phone frame-rate claim. No comparable pre-change timing was recorded. Physical-device scrolling, keyboard, TalkBack and offline/audio acceptance remain to be tried.
- Existing legacy advanced-result actions remain a separate reachable results view; the normal browse/search entry is the new library.
- Delivery includes the accumulated phone-feedback and selection refinements on `smoky-durtles/foundation`. Captures and build/test logs remain local ignored artifacts; no phone installation was performed.
