# Bits

Your external brain, living on your home screen. Android prototype v0.7.

## Updating

1. Upload these files to your repo, replacing the old ones. Commit.
2. **Actions** -> green tick -> download **bits-apk** -> install over v0.6.
3. Remove and re-add the widget. The **app icon** has changed, so a reboot or launcher
   restart may be needed before the new one appears.

Settings -> About -> Version should read **0.7.0**.

## New in v0.7

**New logo** - a cream tile with a navy pixel checklist and an amber tick on the completed
row. Chosen over the darker and bolder options because it was the only one that kept its
structure at real launcher size (48px), where dim rows on dark tiles turn to mush.

**Delete** - the armed state now reads **Confirm?**. Deleting from the widget keeps the
floating card open for a few seconds to offer **Undo**, since the app's own undo bar can
never be seen when the action came from the widget.

**Second and later widgets** - each now has a full **Customise** panel: its own
categories, its own clock style and theme, its own opacity, and a field to create a brand
new category that appears on that widget only.

**Word Guess**
- **309 words**, deduplicated.
- Free letters are now properly scattered: position varies day to day, and the count is
  usually one, sometimes two, so it never settles into a pattern.
- **Hint points.** Solve the daily puzzle to earn one. They accumulate.
  Spend **1** to be told a letter that's in the word; spend **5** to reveal a whole box of
  your choosing. Bought letters are protected from typing exactly like free ones.
- A short pixel-styled instruction panel on first open, dismissable.

**Memory Match** - now 12 deck types: colours, numbers, letters, shapes, fruit, food,
animals, flags, space, weather, codes (2U / 9A style pairs), and a jumbled round that
mixes decks together. Decks are picked at random per round, and the level tag is gone.

**Checkboxes** - squared off rather than rounded, and aligned to the first line of text so
multi-line items stay tidy.

**Widget previews in Settings** - hand scrolling back automatically 15 seconds after being
tapped, so the settings page never stays awkward to navigate.

**Founder note** - rebuilt in the arcade styling used by the games section, with the text
broken into short readable paragraphs.

## Protecting the Pro perks

The easter egg is one atomic all-or-nothing claim and refuses if any slot is filled or any
pick is a free item. Restoring a backup never grants Pro or unlocks, because entitlements
belong to the device.

**The honest limit:** Bits is fully offline, so all state is a file on the user's device.
Anyone willing to root their phone or decompile the APK can change it. Only server-side or
Play Billing verification prevents that.

## Not live yet

**Payments.** Test Pro via Settings -> Developer -> **Simulate Pro**.

## Before publishing

- Change `applicationId` to one you own. Rate Bits uses it.
- Delete the Developer section in `SettingsScreen.kt`.
- Make a real release signing key.
- Wire Play Billing, and use Play Console **License Testing** to comp yourself.
- Keep the font licences: Press Start 2P, Chakra Petch, Atkinson Hyperlegible.

## Project layout

- `data/` model, midnight move, storage, backups, themes, clocks, per-widget boards, hints
- `games/` pure game rules, unit-tested (261 checks pass)
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, on time zone changes
- `ui/` screens, onboarding, retro games, Pro page, tour, armed delete
- `QuickEditActivity.kt` the floating add/edit card opened from the widget
