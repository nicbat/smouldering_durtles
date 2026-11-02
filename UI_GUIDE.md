# Smoky Durtles UI guide

This is the design and review reference for changes across the app. Read it with
[TASKS.md](TASKS.md) and [HANDOFF.md](HANDOFF.md). Updated 2026-11-02 after phone feedback
that the first Material pass looked jarring. Widgets remain a later project.

## Direction and evidence

Build a focused Japanese-study tool using Material 3, with WaniKani subjects as the
visual focal point. Reuse the existing Views and study behavior. A theme change is
not sufficient: inspect the arrangement, emphasis, alignment and density of each page.

The first screenshots exposed specific problems:

- Review actions overlapped the pink question panel, while a white meaning capsule
  interrupted dark mode. The answer field and feedback had different margins.
- Search gave a single kanji an almost empty full-width card. Large headings and cards
  meant only a few results fit on screen.
- Sixty outlined level buttons preceded keyword search in Browse.
- Summary actions touched, and a percentage column clipped even at ordinary text size.
- Font previews and dashboard panels used oversized corners and inconsistent spacing.
- Utility forms inherited desktop-like side-by-side fields that squeezed at 2x text.

These are layout problems, not reasons to remove accessible touch targets or meaningful
content. Fix the arrangement before shrinking text or hiding controls.

## Official guidance and our choices

Use current Material 3 guidance for role and component semantics. Match implementation
APIs to the installed Material Components **1.12.0** and Compose BOM **2025.04.01**;
do not copy newer Expressive APIs without a separate compatibility decision.

- [Material color roles](https://m3.material.io/styles/color/the-color-system): pair
  foregrounds with their container roles; dynamic colors must preserve those pairings.
- [Android Material buttons, pinned to 1.12.0](https://github.com/material-components/material-components-android/blob/1.12.0/docs/components/Button.md):
  choose filled, tonal, outlined or text treatment according to action emphasis.
- [Android Material shape scale](https://github.com/material-components/material-components-android/blob/1.12.0/docs/theming/Shape.md):
  8, 12 and 16dp are small, medium and large shapes. Component shape can be customized.
- [Adaptive layout guidance](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes):
  design for available window space rather than assuming a device category.
- [Accessible target sizing](https://developer.android.com/develop/ui/compose/accessibility/api-defaults#minimum-touch-target-sizes):
  maintain at least 48dp interactive targets and avoid overlapping targets.
- [Android style/theme separation](https://developer.android.com/develop/ui/views/theming/themes):
  use shared styles and semantic attributes instead of repeated hard-coded colors.

The measurements below are **Smoky Durtles conventions**, not universal requirements
attributed to Material. The JavaScript-only Material spacing/button pages could not be
read through the text fetcher; the readable official Android guidance above supplies
implementation details. No third-party design-kit rules are authoritative here.

## Layout and type conventions

| Purpose | Default | Apply it this way |
| --- | --- | --- |
| Phone content gutter | 16dp | Align headings, fields, cards and action rows to the same edge. |
| Card interior | 12dp | Allow 16dp when the content needs it; avoid nested padding stacks. |
| Related controls / grid gap | 8dp | Buttons must not touch; keep independent hit targets separate. |
| Tight label gap | 4dp | Only within a component, not between independent actions. |
| New section | 16–24dp | Use space and a title before adding another surface. |
| Controls / secondary surfaces | 12dp corners | This fork's restrained shape choice; native dialogs/menus retain their component styles. |
| Main cards / focal subject panel | 16dp corners | Do not make every row a large capsule. |
| Hit target | At least 48dp | Compact appearance must still be easy to tap. |
| Action labels | Material LabelLarge, 14sp | Sentence case, scalable; wrap instead of clipping. |
| Body / supporting information | BodyLarge 16sp / BodyMedium 14sp | Use system type for navigation; retain custom Japanese fonts where configured. |
| Section title | TitleMedium / TitleLarge | Avoid a headline-sized title for every result group. |

Domain content is different: kanji, radical images and font previews may be larger.
Preserve user-configured Japanese font and quiz-size settings. Text size uses sp;
geometry uses dp. Long translations and 2x system text must affect layout.

A responsive search grid uses measured content width and font scale. Two columns fit
ordinary phone widths; a narrow window or large text falls back to one. Group headers,
filters and unusually long subject labels can span the row. More columns on wider windows
are acceptable only while cards remain readable. Do not confuse grid columns with Android
window-size classes, and do not fix row heights around short English sample data.

## Colors and emphasis

| Element | Fill | Foreground |
| --- | --- | --- |
| Page | surface | onSurface |
| Ordinary cards / grouped content | surfaceContainerLow | onSurface |
| Supporting text | matching surface role | onSurfaceVariant |
| Primary action | primary | onPrimary |
| Tonal secondary action | secondaryContainer | onSecondaryContainer |
| Text action | transparent over surface | primary |
| Error message container | errorContainer | onErrorContainer |

Resolve these roles from the **view's themed context**, not the application theme.
Use dynamic colors when enabled, and the shared static scheme otherwise. Never use a
container color as text ink. Learning colors are now an explicit presentation preference:

- **Subtle** (modern default): 8% of the original subject color mixed into a Material
  surface, with onSurface text and small subject/stage accents. Broad saturated panels
  should not compete with the Japanese content.
- **Neutral**: Material surfaces and primary accents; stage proportions use separators.
  Preserve type/stage labels and exact counts so color is never the only encoding.
- **Original**: use the existing WaniKani/custom palette. Classic remains unchanged.

`LearningColors` transforms display colors without changing `ActiveTheme`, stored custom
palettes or subject data. Always transform the foreground and its actual parent fill
together, especially transparent custom-drawn glyphs and image radicals. Resolve roles
from the scoped view context, including the independently enabled modern dashboard.
Correct/incorrect answer feedback keeps its separate status palette. The session summary
uses one neutral accuracy panel with explicit Correct/Errors counts, without colored
outlines pretending to be extra data. Search metadata names the subject type even in
Neutral mode. Existing rich-text mnemonic emphasis remains unchanged.
Avoid legacy text shadows on modern subject glyphs; they make otherwise flat components
look pasted in from a different interface.

One dominant action per local task is the default. In a review, that is Submit or Next;
on a dashboard it is Resume or the most relevant study action. Browse, sample, dismiss,
cancel and secondary navigation should not compete with it. An import/export chooser
has equal alternatives and does not need an arbitrary dominant button.

## Patterns to apply throughout the app

### Study, details and summaries

Keep the subject panel for the subject and progress. Put answer entry and its actions
in their own aligned region. Preserve an explicit meaning/reading cue in portrait and
landscape; do not rely solely on black/white backgrounds. With the keyboard open, the
input and primary action must remain reachable by scrolling. Keep editor state, IME
options, focus behavior and grading callbacks unchanged.

Use readable section labels for mnemonics, readings, examples and metadata. Related
subjects may be compact cards. Do not surround every paragraph with another card.
Summaries show one accuracy figure, the actual item total, a small accuracy bar and a
single subject breakdown. An item with any incorrect answer counts as an item with errors;
do not relabel item accuracy as question-attempt accuracy. Hide zero-total subject rows,
and use an explicit empty state instead of inventing a percentage. Show items reveals the
existing detailed lists. Summary actions need gaps/wrapping; numeric values need enough
width independently of labels. Session logs retain restricted-mode hiding and clear expand/collapse affordances.

### Browse and search

Put keyword search before the large level index. Level buttons form a compact, evenly
spaced grid with a visible current-level state. Filters use full-width outlined fields
when side labels would squeeze them; include units in the field label. Keep result
ordering, grouping, counts, collapsed state and item IDs unchanged.

### Dashboard, settings and tools

Group level and time metadata; distinguish the main study action from Browse. Chart
labels and legends need stable alignment and wrapping, not a new card for each value.
Keep chart aggregation and search callbacks untouched. Density comes from structure:

- Level progress: label and exact passed/total count on one line when they fit; an 8dp
  segmented bar underneath. Stack the text at larger sizes and keep the 48dp row target.
  Preserve the rounded-up 90% pass target, including for small totals.
- SRS: one 12dp proportion bar, total, then a two-column tappable stage legend with exact
  counts (one column at large text/narrow widths). Do not replace counts with percentages.
  Post-60 charts expose named stage counts through a visible action and accessibility
  summary; a neutral chart must not depend on a remembered color-to-stage mapping.
- Forecast: total across the configured horizon and busiest hour first, then a compact
  plot. Reserve separate measured bands for ticks/legend/actions. Measure localized ticks
  before drawing; skip labels that cannot fit. Tap an hour for exact counts, cumulative
  load and level-up items; expose the hourly list to accessibility and keyboard clicks.
  Label the cumulative line's separate scale. Neutral segments need visible separators
  and the line needs a contrasting stroke. Never overlay every count on a dense bar chart.

Avoid increasing density by shrinking all text, disabling scaling or overlapping controls.
Use a compact overview plus reachable detail, and validate the actual populated view.

Prefer native Material preference rows, switches, sliders and dialogs. Settings already
using coherent component structure do not need cosmetic churn. A color swatch remains a
literal color sample with a visible selection marker; do not tint it to match wallpaper.
Font tools prioritize the actual preview; import has primary emphasis, sample/delete less.
Downloads place status above actions, and distinguish start/move from cancel/abort.
API-token entry uses the available width with Save below; destructive tools retain their
existing confirmation and enable/disable behavior.

## Implementation boundaries

- `ui/ModernUi.java`: scoped color resolution and stable-class button roles. Use
  `button(button, ButtonRole.PRIMARY/SECONDARY/TEXT)`; repeated styling preserves the role.
- `ui/LearningColors.java`: optional learning-color presentation; never mutate semantic palettes.
- `study/SummaryPresentation.java`: compact item-accuracy summary and adaptive breakdown.
- `ui/UtilityStyle.java`: utility gutters and constrained form/action layout corrections.
- `study/StudyStyle.java`: study arrangement and semantic surfaces, before/after binding
  as appropriate. Do not replace Session or answer-handling code to change appearance.
- `ui/SearchStyle.java`, `adapter/search`, modern search layouts: responsive result grid.
- `dashboard/DashboardStyle.java`, `DashboardOverview.kt`: existing chart Views plus Compose overview.
- `SubjectInfoButtonView.setTextShadowEnabled`: opt out for modern consumers; Classic default stays intact.
- Explicit Material components use their APIs. Retain legacy view classes and IDs where
  changing the class would break Classic/Modern saved-state restoration.

## Review checklist for every affected screen

- [ ] Compare a populated screenshot before/after, not only an empty layout or passing build.
- [ ] Identify the main task and check action emphasis, content alignment and information density.
- [ ] Check light/dark and static/dynamic role paths; preserve custom learning colors.
- [ ] Measure a normal phone, narrow window and 2x text; verify wrapping and target separation.
- [ ] For study/forms: inspect with IME visible and limited landscape height.
- [ ] Check loading, empty, error, disabled and long-content states relevant to the change.
- [ ] Keep navigation, grouping, callbacks, drafts, Classic and recreation working.
- [ ] Run appropriate regression tests; never submit real answers or run tests on the user's phone.
- [ ] Update TASKS/HANDOFF with what was actually checked and any remaining device validation.

## Refinement tracking

The current pass applies these principles to review/lesson/Anki/details/log/summary,
search grids and browse filters, dashboard spacing/action hierarchy, font tools, download
controls and utility forms. Settings rows and the color editor are retained after audit.
Record final test/capture evidence in HANDOFF rather than treating this guide as evidence
of completion. Real-phone feedback, TalkBack, actual wallpaper changes and extended study
sessions still require validation. Widgets and performance claims remain separate work.
