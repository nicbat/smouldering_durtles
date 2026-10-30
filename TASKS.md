# Smoky Durtles working tasks

Start here for current work. Read [HANDOFF.md](HANDOFF.md) for setup and session handoff;
[FORK_PLAN.md](FORK_PLAN.md) holds architecture findings and the longer backlog.

## Current milestone

Reliability baseline, then an optional Compose Material 3 dashboard overview.
Development and emulator testing are authorized; the user will test on a phone later.
Push tested milestones to the user's fork on the current feature branch.

## Completed

- [x] Separate Smoky Durtles app IDs, authorities, links, labels, and upstream credits.
- [x] Reproduce and fix toolbar search requiring two taps.
- [x] Three foundation instrumentation tests; debug/release builds; side-by-side install.
- [x] Push foundation to `origin/smoky-durtles/foundation` (`0b6b531`).

## In progress

- [x] Restore original Room schemas 48–67 from `b36c696`; full suite passes (25 tests, Android 16).
- [x] Fix DARK and NORD Guru customization slots; all-theme palette regression test passes.
- [ ] Add a Compose dashboard overview with real counts and existing session actions.
- [ ] Keep Classic selectable and semantic subject colors unchanged.
- [ ] Verify loading, no-work, vacation, active-session, large-text, light/dark states.
- [ ] Build APK, update these documents, commit and push the milestone.

## Next

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
