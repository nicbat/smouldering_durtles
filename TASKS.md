# Smoky Durtles working tasks

Start here for current work. Read [HANDOFF.md](HANDOFF.md) for setup and session handoff;
[FORK_PLAN.md](FORK_PLAN.md) holds architecture findings and the longer backlog.

## Current milestone

Rounded toolbar overflow menus and modern dashboard chart/level panels.
Development and emulator testing are authorized. The settings milestone was installed on the user's Pixel 9 Pro.
The overflow/chart build b074d72 was installed on the phone with adb install -r; user validation is pending.
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

## Next

Start with the first item below. No build/test blocker remains; emulator is stopped at handoff.

- [ ] Test actual review/lesson navigation and populated session state using controlled offline fixtures (no real account submissions). Chart fixtures now cover populated statistics without database/account data.
- [x] Verify modern-to-Classic dashboard switching across real activity recreation.
- [ ] Verify API 21/30 fallback, real wallpaper changes on API 31+, and Classic-to-modern round trips during a study session.
- [ ] Refine dashboard subject lists, table density, and configurable section ordering. Current charts retain their original data models and drawing logic.

- [ ] Session fixtures and recovery tests: interruption, process death, pending offline submissions.
- [ ] Reproduce offline audio delays and audit cancellation/focus behavior.
- [ ] Modern widgets: inspect upstream PR #153, then implement and test one useful widget.
- [ ] Further settings/search redesign: grouping, Material sliders, custom two-field layouts, and contextual menus beyond toolbar overflow.
- [ ] Measured startup/scroll/audio optimizations.
- [ ] Review/lesson presentation migration after behavior coverage is established.
- [x] Install the dashboard milestone on the Pixel 9 Pro; user confirmed launcher visibility.
- [x] Install Material settings build 4472ee8 on the Pixel 9 Pro as an update, preserving app data.
- [x] Install overflow/chart build b074d72 on the Pixel 9 Pro, preserving app data.
- [ ] Physical-device validation of settings, search keyboard, study flows, and launcher widgets.
- [ ] Durable release signing and update delivery; do not commit signing keys.

## Open decisions

- Phone is Pixel 9 Pro; Android version/launcher behavior and minimum supported SDK (currently 21) remain to confirm.
- Permanent Classic support versus transition-only fallback.
- Public distribution and release signing. The fork already exists at `nicbat/smouldering_durtles`.

Update checkboxes and evidence after each milestone. Record partial work honestly;
do not mark a passing focused test as a passing full suite.
