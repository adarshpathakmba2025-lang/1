# Bits

Your external brain, living on your home screen. Android prototype v0.9.

## Updating

1. Upload these files to your repo, replacing the old ones. Commit.
2. **Actions** -> green tick -> download **bits-apk** -> install over v0.8.
3. Remove and re-add the widget. The **app icon has changed**, so a reboot or launcher
   restart may be needed before the new one shows.

Settings -> About -> Version should read **0.9.0**.

## New in v0.9

**Your logo is in.** Background removed, trimmed, and fitted inside the adaptive-icon safe
zone on a cream tile. Checked against circular, rounded and square launcher masks: nothing
is cropped, including the pencil tip and the creature's legs.

**Move bits between categories.** Tap a bit, then use the arrows at bottom-left to send it
to the category either side. Works in the app and on the widget, follows your own category
order, picks up new categories automatically, and the arrow dims at each end. 47 tests
cover the edges, reordering, category deletion and bad input.

**Themes** - **Midnight Black** added (free, true black with crisp white), plus **Neon
Nocturne** (Pro, electric cyan on ink) to make ten and keep the grid even. Two free now.

**Checkbox** - a proper wide rectangle, matching your reference.

**Category chips** - now in the arcade style, square with an offset shadow.

**Tap a category title** in the app to add straight into it, exactly like the widget.

**The widget card now wears the widget's theme** instead of always being navy and amber.

**Widget** - category names are noticeably larger.

**Memory Match** - "Score" is now **TRIES**, and **BEST** holds the fewest tries you have
ever cleared a round in, with a small pixel reset beside it.

**Flappy** - ramps up past 20, and again past 47, with a floor on the gap and a ceiling on
speed so a high score stays hard rather than impossible. An autopilot test reaches 60.

**Settings** - opacity now sits directly under the preview; long-pressing a theme or clock
scrolls back to the preview automatically; "Match the app" is gone, and the matching theme
simply shows as selected.

**Tour** - every card is now in the retro pixel frame, step five reworded, and a new step
covers the category arrows.

**About** - Rate Bits now reads "Tell us what to improve - every review is read".

## Not live yet

**Payments.** Test Pro via Settings -> Developer -> **Simulate Pro**.

## Before publishing

- Change `applicationId` to one you own. Rate Bits uses it.
- Delete the Developer section in `SettingsScreen.kt`.
- Make a real release signing key.
- Wire Play Billing, and use Play Console **License Testing** to comp yourself.
- Keep the font licences: Press Start 2P, Chakra Petch, Atkinson Hyperlegible.

## Project layout

- `data/` model, midnight move, storage, backups, themes, clocks, boards, hints, shifting
- `games/` pure game rules, unit-tested (323 checks pass)
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, on time zone changes
- `ui/` screens, onboarding, retro games, Pro page, tour, armed delete
- `QuickEditActivity.kt` the floating add/edit card opened from the widget
