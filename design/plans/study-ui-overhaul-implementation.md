# Study UI overhaul implementation

Implemented 2026-11-09 against [the behavior plan](study-ui-overhaul.md) and [v8 composition reference](../explorations/study-overhaul-v8.html). Native Android views and existing study/session machinery remain the foundation.

## Delivered

- Shared adaptive choice grids, subject identity colors, meaning/reading mnemonic cues, and SRS roles. Original/Neutral palette preferences remain available. Four-stage choices use a row, 2×2, or one column at extreme text sizes.
- Single scrolling lessons/details: visible parts of speech, expanded mnemonics, left-aligned example translations, compact related items at the bottom, reading-specific speaker controls, pitch paths above kana with Japanese · English pattern labels.
- Existing stroke data and renderer support static entry, Play, Step, Reset, All strokes and a position label. System-disabled animations remain static. Missing data hides the writing section; no new stroke dataset or licensing assumptions. Writing stays behind the full-details reveal in reviews.
- Review textbox retained; first deliberate reveal shows only the attempted side. Second shows all details, including both mnemonics. Icon undo remains beside the input after an eligible previous incorrect answer, restoring its text through the existing undo path.
- Library root unifies browsing and live local text search, with filters for types, stages, level range, exact API part of speech, optional pitch, JLPT and Jōyō. Search results recycle views and reject stale asynchronous delivery. Saved searches and existing bulk result actions remain reachable. Toolbar quick search focuses the input and offers five results plus full results.
- Lesson selection: editable maximum with 3/5/10 shortcuts, types, All/Current scope, coverage and English aligned right. Final batch preview permits revisiting items before starting its quiz.
- Due-only custom reviews: type/stage selection, All/Current, presets, real eligible coverage and stage breakdown. Eligibility is queried again before starting; a changed set requires reviewing the refreshed preview. Selection survives recreation.
- Custom quiz remains practice, with condensed filters and a matching-item preview. Practice missed items follows normal session finishing and pending-upload handling before starting a separate practice session.
- Summary counts completed radicals/kanji/vocabulary, question/error counts, elapsed wall time and missed question labels. Incomplete/abandoned items do not inflate completion totals.
- Compact dashboard count buttons retain level-up path counts, active-session/vacation/loading behavior, existing lower level bars and their passed/Guru rule. Toolbar search/menu; no bottom navigation.
- One forecast combines thin arrival bars and cumulative waiting line, with labelled left/right scales, hourly 24-hour and calendar-day 7-day/extended horizons. Due-now items are included once. Caption layouts and formatting are cached; drawing skips offscreen slots. Accessible date selection is available without visible step/expand buttons.

## Verification

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
- Work is uncommitted in a checkout that already contained extensive edits. No bulk revert, phone installation or push was performed.
