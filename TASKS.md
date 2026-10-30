# Smoky Durtles working tasks

Start here for current work. Read [HANDOFF.md](HANDOFF.md) for setup and session handoff;
[FORK_PLAN.md](FORK_PLAN.md) holds architecture findings and the longer backlog.

## Current milestone

Optional Compose Material 3 dashboard overview; reliability baseline is complete.
Development and emulator testing are authorized; the user will test on a phone later.
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

## Next

Start with the first item below. No build/test blocker remains; emulator is stopped at handoff.

- [ ] Test a fully populated dashboard and actual review/lesson navigation using controlled offline fixtures (no real account submissions).
- [ ] Verify Classic switching across recreation, API 21/30 fallback, and wallpaper changes on API 31+.
- [ ] Continue Material styling into toolbar, dashboard statistics, and configurable sections; the current change modernizes the overview only.

- [ ] Session fixtures and recovery tests: interruption, process death, pending offline submissions.
- [ ] Reproduce offline audio delays and audit cancellation/focus behavior.
- [ ] Modern widgets: inspect upstream PR #153, then implement and test one useful widget.
- [ ] Settings/search redesign and measured startup/scroll/audio optimizations.
- [ ] Review/lesson presentation migration after behavior coverage is established.
- [ ] Physical-device and launcher tests when the user connects their phone.
- [ ] Durable release signing and update delivery; do not commit signing keys.

## Open decisions

- Phone/Android version/launcher and minimum supported SDK (currently 21).
- Permanent Classic support versus transition-only fallback.
- Public distribution and release signing. The fork already exists at `nicbat/smouldering_durtles`.

Update checkboxes and evidence after each milestone. Record partial work honestly;
do not mark a passing focused test as a passing full suite.
