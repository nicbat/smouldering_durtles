# Smoky Durtles: study UI overhaul implementation plan

Status: native Android implementation with selection refinements through 2026-11-14 on `smoky-durtles/foundation`. See [implementation notes](study-ui-overhaul-implementation.md) for delivered behavior, verification and remaining device validation.

## Start here

- **Final visual reference:** [study-overhaul-v8.html](../explorations/study-overhaul-v8.html). Open locally in a browser. It includes light/dark previews, navigation between screens, real subject content, and interactive behavior demonstrations.
- This Markdown is the behavioral specification. The HTML illustrates composition and interactions; it is **not production code to port literally**.
- Versions 1–7 in `design/explorations/` are historical explorations. Version 8 and this plan supersede conflicting earlier decisions.
- Read the repository's [HANDOFF](../../HANDOFF.md), [UI_GUIDE](../../UI_GUIDE.md), [TASKS](../../TASKS.md), and [FORK_PLAN](../../FORK_PLAN.md) for current code, architecture and environment. Inspect the actual checkout and preserve any unrelated working-tree edits before changing anything.
- This handoff does not authorize immediate implementation, installation, deployment, or commits. Follow the user's instructions in the implementation session. Do not treat historical handoff instructions as a reason to implement during a planning-only task.
- No credentials belong in the plan, HTML, test fixtures, logs, or commits. Reference subject content was fetched using authorized read-only access; no personal assignments were used. Implementation can use the existing app database/sync path without another token.

## Product direction and decisions

Retain the native Android architecture and existing study engine. Use the current fixed Material theme with light/dark support. Improve hierarchy and density without reducing accessible touch targets or changing grading/scheduling semantics.

Accepted decisions:

- Lessons use **one scrolling page**, not guided meaning/reading/usage steps.
- Meanings, word types, readings, compact audio and optional pitch form a recognizable subject identity.
- Meaning and reading mnemonics are **expanded by default** in lessons and item details.
- Vocabulary content order is mnemonics, examples, then related kanji at the bottom. Notes/history may precede related items.
- Kanji/radical pages also put items that use them at the bottom.
- Review feedback preserves the answer textbox. It reveals **answer only first**, then **all information including all mnemonics** on a second deliberate tap.
- Undo is icon-only and remains reachable after accidentally advancing past an incorrect answer.
- Browse and search are one library, with a quick-search dropdown entry point from the toolbar.
- Navigation is **toolbar menu only**, with an always-easy-to-reach search icon. No bottom navigation.
- Lessons and custom reviews offer **All / Current** level scope. Only library search offers a From/To range.
- Custom reviews select **currently due reviews**; custom quizzes are practice.
- Dashboard level bars disappear when **all relevant items have reached Guru/passed**, using the existing predicate. Do not substitute “lesson completed,” a 90% level-up threshold, or “burned.”
- SRS controls must form a single row or balanced grid. No clipped/overflowing rows anywhere in the interface.

## Shared visual system

### Semantic colors

Use reusable theme roles/tokens, not per-screen hard-coded values. Version 8 demonstrates a candidate palette, not a replacement for the app's theme infrastructure.

- Subject identity/reference: radical blue, kanji pink, vocabulary purple; kana-only vocabulary follows vocabulary.
- Mnemonic target meaning: green for kanji/vocabulary. Radical meanings retain radical blue.
- Reading/pronunciation cues: amber across both kanji and vocabulary.
- SRS: Apprentice rose, Guru purple, Master blue, Enlightened teal, Burned gray. Keep these identical across SRS legends, filters, badges, selected-review previews and dashboard distributions.
- General controls, charts and selected states use interface colors where subject/SRS semantics do not apply. Incorrect/correct feedback keeps its status semantics.
- Labels, shape/checkmarks, and accessibility state must communicate meaning independently of color. Preserve the app's learning-color preferences, including Neutral and user palettes; map semantic roles through them rather than breaking stored customization.

Mnemonic markup requires context. In 大学, “big” and “study” reference component kanji and remain pink; “university/college” is the target meaning and is green; reading memory cues are amber. Preserve `<radical>`, `<kanji>`, `<vocabulary>`, `<meaning>`, `<reading>` and Japanese spans. Reuse the rich-text pipeline and safe parsing. The HTML's target-meaning string comparison is a demonstration, **not a general solution**. Account for inflections, alternate meanings, nested markup and links without recoloring all references indiscriminately. Do not flatten or rewrite WaniKani prose.

### Adaptive layout contract

- At least 48dp action targets, scalable text, shared 16dp phone gutters and deliberate spacing.
- Four SRS filters: one row only when all fit; otherwise **2×2**. At extreme font scales, a single column is preferable to clipping.
- Five-stage legend: one row if it fits; otherwise two columns with the last item spanning both, or an equally deliberate accessible arrangement.
- Apply equivalent measured-width behavior to subject selectors, action bars, chips, counters, form pairs and toolbars. No accidental third item overflowing a two-item row. Do not shrink text to force a fit.
- Long translations, Japanese compounds, labels and numbers must wrap or reflow. Do not ellipsize essential actions or readings.
- Preserve fixed study actions above the keyboard and allow vertical scrolling. No accidental horizontal scrolling of the page; an intentionally scrollable chart is different.
- Use native focus/accessibility semantics, selected states, descriptive icon labels and keyboard support. Keep Japanese font settings, smaller glyph caps, image radicals and zero/hide preferences.

### Pitch accent

Align the pitch path **directly above reading morae**, not above kanji glyphs or in a disconnected side chart. Put the speaker icon next to the annotated reading; label the pattern clearly.

Shared labels in headers, filters and help:

- **平板 · Flat** — no accent drop; accent number 0. “Flat” does not mean every mora starts at the same height.
- **頭高 · Falling** — drop after the first mora.
- **中高 · Rise–fall** — drop inside the word, after an internal mora.
- **尾高 · Final drop** — drop after the final mora, detectable on a following particle.

Use the reading's actual accent number and source metadata. Account for mora segmentation (small kana combinations, っ, ん and long vowels), alternative readings, multiple patterns, and one-mora words. When needed, an optional trailing particle/guide can distinguish 平板 from 尾高; never invent a particle as part of the subject reading. Do not infer pitch from kanji spelling or assume one pattern per item. Hide unavailable pitch rather than showing empty space. The database can distinguish missing/undetermined and deliberately empty pitch information: preserve that distinction.

These labels describe Tokyo-style accent. See the [Japanese pronunciation reference](https://www.nihongo-ews.mext.go.jp/contents_files/download/?cfid=56&content_id=172). Pattern number remains authoritative; English descriptors are explanatory UI text.

## Screen specifications and acceptance criteria

### 1. Dashboard and navigation

- Compact level/day/sync status; review and lesson counts inside their respective action buttons.
- Preserve resume, vacation, offline, sync error and zero-work states. An active session takes precedence over creating another one.
- Toolbar search icon opens focused quick search. Overflow menu includes full library access, custom reviews, custom quiz and settings.
- Remove the redundant headline “Level progress” meter. Retain the existing lower level bars and their real counts/search actions.
- Omit fully passed levels. A partially passed level stays until all relevant items satisfy the existing passed/Guru rule; check permanent passed state versus current SRS regression using existing logic.
- Keep SRS distribution, JLPT/Jōyō coverage and other existing useful sections. The HTML is not exhaustive permission to remove panels.

### 2. Forecast

- One chart: arrival bars occupying 70% of each slot on the left Y scale and cumulative waiting line on the right Y scale. Series labels explicitly identify axis ownership; both count reviews.
- Tap/accessible-select a bar for exact date/time, arriving count, and cumulative waiting count. No previous/next-bar buttons and no separate Expand button.
- **24 hours** uses hourly buckets; **7 days** uses local calendar-day buckets. Use one compact two-option selector; no Extended button.
- Today is a partial day; cross-midnight arrivals belong to the correct next day. Test DST and timezone changes. Do not aggregate calendar days by arbitrary fixed 24-hour slices.
- Cumulative counts include currently due reviews and assume none are completed. Forecast shows known assignments, not speculative reviews generated by future successful answers. Avoid double-counting due items in the first bar.
- Axes need legible units, zero/bounds and scale changes. Line/bar intersections have no equality meaning. Provide accessible exact values so dual-axis interpretation is optional.
- Preserve selection/scroll through refresh where possible.
- Profile `TimeLineBarChart`: current code builds text layouts during drawing and measures captions across time slots. Cache formatting/layout where safe, avoid per-frame allocations and unnecessary offscreen work, and measure before/after. Thin bars alone are not a performance fix.

### 3. Lessons and item details

- Compact type/level label, Japanese subject, primary meaning, alternate accepted meanings, part-of-speech tags, reading(s), audio icon and optional pitch.
- Audio remains tied to the correct reading/voice, mute state and offline availability; preserve existing settings and cached-audio behavior.
- Expanded meaning and reading mnemonics; optional hints can remain disclosures.
- Japanese examples with individually revealable, **left-aligned** English translations. Preserve existing reveal/re-blur behavior and long-press copy of the full original sentence.
- Keep editable personal notes and meaning synonyms distinct: synonyms affect accepted meanings; notes do not. Preserve existing sync handlers.
- Vocabulary: compact component-kanji tiles at bottom, using **destination subject colors**.
- Kanji: preserve on’yomi, kun’yomi, nanori, primary/accepted flags. Do not present every reference reading as an accepted answer. Include compact writing section; related vocabulary at bottom.
- Radicals: meaning content and related kanji at bottom; support image-only radicals; no empty reading/audio/pitch sections.
- Preserve browse paging, related-item Back behavior, state restoration and font/accessibility settings.

Stroke order: reuse existing reference paths and renderer. Loop according to the animation preference, with upcoming strokes at 25% opacity. Hide playback controls by default; the settings option exposes Play, step, reset and a static sequence. Respect system-disabled animations. Current-stroke/total labels suffice. Avoid explanatory paragraphs inside the ordinary UI. Existing user motion preferences still apply.

### 4. Active review and post-answer details

Use one stable subject/input/action geometry across active and answered states. The real Android keyboard owns text input; the HTML keypad is only a space illustration.

State contract:

1. **Unanswered:** subject and question-type cue; no answer, audio, pitch, examples or explanatory content that leaks the answer.
2. **Incorrect, unrevealed:** preserve exactly what the learner typed in the textbox and retain normal incorrect feedback. Undo icon beside the input. Fixed primary Next action and a secondary Show meaning / Show reading action.
3. **First reveal:** show only the correct answer for the attempted question. Meaning shows accepted meaning information; reading shows accepted reading(s), audio/pitch if allowed. **No mnemonics, hints, examples, other answer, notes or component meanings.**
4. **Full reveal:** the same button in the same fixed position changes to Show all details. This explicit second action shows the other half, mnemonics, hints, examples and related information.
5. **Next after incorrect:** the next unanswered question has an icon to undo the previous eligible incorrect answer. Restore the previous question and its actual entered text through existing session history; do not overwrite the next question or duplicate reporting.
6. **Undo:** clear reveal state and restore the correct input/edit/focus state. Honor existing undo eligibility and queued/reported answer constraints.

Reset reveal state per question; preserve appropriate state across rotation/recreation without leaking from the previous item. Correct-answer behavior must also remain coherent. Do not implement mnemonic word masking: the entire mnemonic belongs behind full reveal. Do not replace the answer textbox with a correct-answer label. Accessible labels must not contain the unrevealed answer.

Audit session grading, double-check, retry, ignore/undo and Anki configurations before changing presentation. This design does not authorize changing those semantics.

### 5. Browse/search and quick search

- Empty query browses; typed query searches Japanese, readings and meanings. One library and one filtering engine serve both entry points.
- Toolbar search immediately focuses the input and invokes the native keyboard. Dropdown/overlay shows a small compact result list, explicit subject types/colors, reading and meaning.
- Clear query, Close/Escape/Back, keyboard navigation and outside dismissal behave predictably. Restore focus and previous screen state on dismissal.
- View all results & filters opens the library with the same query. Do not leave hidden filters causing surprising quick-search results; quick search is a global lookup unless active constraints are visibly disclosed.
- Results retain search query/filter/scroll when returning from an item.
- Library controls: subject types, **All / Current / Range** WaniKani levels, part of speech, pitch pattern, JLPT reference and Jōyō grade.
- Range is From/To, inclusive, validated within 1–60. Current uses actual account level; All respects data/subscription availability, not a hard-coded fixture.
- AND across filter dimensions; alternatives within a dimension are OR. If a control is single-select, do not silently implement multiple selection.
- POS uses real API values/categories. Do not use naive substring tests where “noun” accidentally matches unrelated categories; document normalization.
- Pitch uses reference data by reading; match any supported pattern unless an explicit reading-specific filter is added. No-data is distinct from 平板/0.
- Jōyō applies to kanji, with school grades and secondary-school grouping. JLPT coverage depends on the available dataset: do not fabricate vocab classifications from kanji membership or imply an official modern syllabus.
- Missing/unsupported metadata must be distinguishable from a negative classification. Show clear active filter summaries and an easy reset; an empty result is honest.
- Debounce/cancel database searches and stale result loads. Do not download all subjects or make network requests per keystroke. Extend existing query/preset machinery where appropriate.

### 6. Lesson batch

- Open a full-screen selection page with toolbar, scrolling choices/preview and pinned Start action. Save choices across recreation; successful Start opens the session and finishes selection.

- Choose size (quick values 3/5/10 plus direct numeric entry), subject-type combination, and **All available levels / Current level**.
- Count and preview update immediately. Batch size is a maximum and cannot exceed the eligible available pool; zero eligible items disables Start with a clear reason.
- Use real unlocked/unlearned eligibility first, then filters/order/limit. Preserve existing lesson priorities and configuration defaults.
- Japanese character and type/level on the left; English meaning **right-aligned** in each row, wrapping for long meanings.
- Before-lessons and ready-for-quiz states share the selected batch. Revisit lessons or Start quiz. Actual progress/counts must follow the real batch, not a fixed five-item walkthrough.

### 7. Custom reviews versus custom quiz

**Custom reviews:** use the same full-page structure as lesson selection, entered from overflow. Close a stale selection page when returning after another session starts. Select only due eligible assignments. Presets All due, All kanji, Apprentice + Guru; independent subject types and SRS groups; **All / Current** level scope.

- Show selected/total due, percentage, excluded count, and aggregate subject-type counts. Show available due counts within the colored SRS filter buttons, respecting type/level scope even when that stage is off. Avoid a duplicate button-shaped SRS legend. Do not preview individual review subjects, readings or meanings: these spoil upcoming questions.
- Filter toggles update the loaded due snapshot immediately, without a loading flash. Counts and selection use the same predicate and time snapshot; revalidate before starting as assignments change.
- Respect existing active sessions, pending local results, vacation and synchronization state. Do not create competing sessions or bring future reviews forward.
- Group all Apprentice sub-stages and both Guru sub-stages for this UI, while preserving underlying stage IDs. Burned is not a due-review stage; omit it here but retain it in distributions/practice where relevant.
- Empty intersection disables Start. Never present fake counts while loading.

**Custom quiz:** unrestricted practice with existing self-study semantics; no WaniKani SRS changes. Retain compact presets, paired fields and preview. Subject types use large independent colored tiles with checkmarks. Make clear which selection a preset changes. The HTML's synthetic counts and limited preview are not selection logic.

### 8. Session summary

- Completed item count, duration and an accurate, explicitly defined accuracy metric.
- Number of radicals, kanji, vocabulary (and kana vocabulary as supported) completed. Do not lose a type or double-count an item with both question types.
- Separate meaning/reading totals; list missed items and which question types were missed, with consistent subject and cue colors.
- Practice missed items launches practice, not duplicate SRS reviews. Home remains easy to reach.
- Inspect existing summary accounting before adopting the mockup's first-attempt metric. If its data cannot support that metric, implement the aggregation deliberately or retain an accurate supported label; never relabel another statistic.
- Handle zero questions/items, perfect sessions, retries, undone answers, lesson quizzes and mixed subject types.

## Data/source and prototype limitations

The reference HTML contains a small deduplicated set of real WaniKani subjects, including 大学 (level 5), 大, 学 and related items. Preserve all accepted meanings, reading flags and marked-up text. API reference: [WaniKani subjects](https://docs.api.wanikani.com/20170710/#subjects).

The following are **illustrative only**:

- Assignment/SRS statuses, due counts, forecast values, summary counts, lesson eligibility and account level.
- Supplementary pitch/JLPT/Jōyō filter fixtures. Production must use verified local reference records.
- Simplified stroke paths and the on-screen keyboard.
- HTML search only covers a small fixture; its duplicate function overrides and string substitutions reflect iterative mockup development. Do not copy this structure into the app.
- Sample submit/Next/Start actions often jump to another design state instead of running a complete session. Implement actual session callbacks, eligibility and lifecycle behavior.
- No actual audio is played by the mockup. Preserve native audio controls and state.
- Existing screen content not rendered in the prototype has not automatically been rejected; consult this plan and current features before removal.

## Code map to investigate

Paths below are under `app/src/main/java/com/smouldering_durtles/wk/` unless noted. Reconfirm them in the current checkout.

- Theme/layout: `ui/ModernUi.java`, `ui/LearningColors.java`, `ui/SearchStyle.java`; shared styles/resources.
- Subject content: `views/SubjectInfoView.java`, `views/SubjectInfoHeadlineView.java`, `fragments/SubjectInfoFragment.java`, `study/LessonStyle.java`, `fragments/LessonSessionFragment.java`.
- Reference content: `model/SubjectReferenceData.java`, `model/PitchInfo.java`, `views/PitchInfoView.java`, `views/PitchInfoDiagramView.java`, `views/StrokeDiagramView.java`, `tasks/LoadReferenceDataTask.java`, `tasks/DownloadPitchInfoTask.java`.
- API/database: `api/model/ApiSubject.java`, `db/model/Subject.java`, `db/dao/SubjectSyncDao.java`, existing subject queries.
- Review state: `model/Session.java` (including `canUndo`/`undoAndRetry` and history), `db/model/SessionItem.java`, `activities/SessionActivity.java`, `fragments/UnansweredSessionFragment.java`, `fragments/AnsweredSessionFragment.java`, `fragments/AnkiSessionFragment.java`.
- Search: `activities/BrowseActivity.java`, `fragments/SearchResultFragment.java`, `views/AdvancedSearchFormView.java`, `model/AdvancedSearchParameters.java`, search adapters and saved presets.
- Session selection: `activities/SelfStudyStartActivity.java`, `model/SubjectSelectionRules.java`, `model/Session.java`, selection/order settings. Trace normal lesson/review entry points before adding custom ones.
- Dashboard: `dashboard/DashboardOverview.kt`, `dashboard/DashboardController.kt`, `views/TimeLineBarChart.java`, `livedata/LiveTimeLine.java`, `model/TimeLine.java`, `views/SrsBreakDownView.java`, `views/LevelProgressView.java`, `model/LevelProgress.java`.
- Summary: `study/SummaryPresentation.java`, `fragments/SummarySessionFragment.java`, session-summary/log models.

## Suggested implementation sequence

1. **Baseline and shared roles:** inspect working tree, current behavior and relevant tests; capture populated light/dark baselines. Add semantic roles and adaptive reusable selectors/badges without altering stored colors.
2. **Subject detail foundation:** header/audio/pitch, rich mnemonics, example alignment, related-item order and writing controls. Cover all four subject types and long content.
3. **Review state integration:** stable textbox/actions, two-stage reveal, current/previous-answer undo and keyboard/lifecycle handling. Treat this as the highest-risk state change.
4. **Library and quick search:** unify query state; extend filters; make overlay focus/back/selection reliable; optimize local querying.
5. **Lesson and review selection:** batch scope/size, due-only custom review predicates/counts, practice distinction, resumed-session integration.
6. **Dashboard/forecast/summary:** compact header/nav, real stage colors, passed-level removal, hourly/daily chart and measured performance, accurate summary breakdowns.
7. **Polish and regression:** inspect all changed screens at narrow/large-text configurations, confirm preservation of existing settings and workflows, then update project docs and handoff with evidence.

Use coherent, reviewable increments. The implementation agent can choose file boundaries after tracing dependencies; a wholesale UI framework migration is not required.

## Verification and completion checklist

### Visual/accessibility

- [ ] Light/dark; Subtle/Neutral/Original learning colors and custom palettes; no unreadable foreground/background pair.
- [ ] Narrow phones (320/360dp), ordinary phones, landscape, tablets, 1×/1.5×/2× font scale; image radicals, long multi-kanji vocabulary and long English translations.
- [ ] SRS controls: deliberate row or 2×2 grid, no overflow, clipping or hidden selected states. At extreme text sizes, reflow further.
- [ ] All other control groups, labels, textboxes, toolbars and pinned footers fit or vertically reflow. No global horizontal overflow.
- [ ] Keyboard open/closed, TalkBack, keyboard focus, 48dp targets and explicit icon names. No unrevealed answers in accessibility labels.
- [ ] Search overlay focus, dismissal, back stack and return-to-result position; retain query across full-library handoff.

### Functional/data

- [ ] First reveal contains no mnemonics/hints/examples/other answer; full reveal exposes them deliberately; reset/rotation/next-item behavior is correct.
- [ ] Undo current and previous eligible incorrect answer restores exact question/text and reporting invariants; repeated Next/Undo does not duplicate jobs or grades.
- [ ] Accepted versus reference readings, alternate meanings, notes/synonyms, multiple pitch patterns and missing data are handled honestly.
- [ ] Filter AND/OR rules, ranges, unknown references, part-of-speech categories, current level and zero results.
- [ ] Batch and custom-review count/preview/session equality under changing data; due-only selection, grouped SRS stages, priorities and active sessions.
- [ ] Forecast bucket boundaries, due inclusion, calendar days, DST and selection; profile real scrolling before claiming improvement.
- [ ] Summary accuracy denominator/type totals match actual session events, including retries/undo and mixed question types.
- [ ] Fully passed level bars disappear according to existing passed/Guru behavior; partially passed levels remain.

### Delivery evidence

- Run focused existing/unit/instrumentation checks appropriate to each change, then the required build/regression checks from the current repository guidance.
- Use isolated test/emulator fixtures. **Never run instrumentation on the user's account-bearing phone or submit real reviews during tests.**
- Capture populated screenshots of the key states and an active → incorrect → Next → undo walkthrough. HTML browser checks do not establish native Android correctness or performance.
- Record changed behavior, test/build results, screenshots, known limitations and any unresolved data-source constraints. Do not claim unimplemented fixture behavior is complete.

## Current handoff state

Only `design/` artifacts were created for this exploration. No Android source was modified by this design task. The existing working tree contains other changes; this statement does not imply it is clean.

The user approved the overall direction and requested this plan for a subsequent Sol implementation agent. Remaining runtime details should be resolved by inspecting the existing app, with the accepted decisions above as the specification. No further broad visual redesign is needed before beginning that separately requested implementation.
