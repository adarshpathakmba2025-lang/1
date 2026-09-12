# Bits

Your external brain, living on your home screen. Android prototype v0.3.

## Updating on your phone

1. Upload the files from this folder to your `bits` GitHub repository, replacing the old ones, and commit.
   Nothing needs deleting this time; v0.3 only adds and changes files.
2. Open **Actions**, wait for the green tick (about 5 minutes), download **bits-apk**, and install it.
   It installs over v0.2 and keeps your lists.
3. To see the new widget preview in the picker, remove the widget and add it again. Launchers cache the old preview.

## New in v0.3

- **Widget preview** in the widget picker instead of a blank grid.
- **Plus button** opens the keyboard when the field is empty, and adds the item when there's text.
- **Widget footer:** controller (bottom left) opens Games, "Bits" opens the home page, the sliders icon opens Settings.
- **Edit from the widget:** tap an item's text to edit it in a small floating card. The checkbox still ticks it off.
- **Hide categories by dimming:** in Edit, tap a category's name. Dimmed ones don't appear on the widget.
- **Games:** 2048 is free. Two more slots are shown as Pro placeholders until the games are chosen and built.
- **Pro page** with the founder message, perks, and pricing (₹179 one-time, ₹49/month).
- **Widget themes:** Classic (free), plus Midnight, Forest, Ember, Ocean, Royal (Pro). Tap any to preview it.
- **New tour** with tap zones: right 60% of the screen goes forward, left 40% goes back.

## Not live yet

- **Payments.** The Pro page shows the layout only. Play Billing needs a merchant account and a Play Console listing first.
  To test Pro features now: Settings → Developer → **Simulate Pro**.
- **A second widget list.** Listed as "coming soon" on the Pro page. It needs its own data model, so it's the next big piece.

## If the build fails

Open the failed run, click the red step, copy the lines that start with `e:`, and send them to Claude.

## Before publishing

- Change `applicationId` in `app/build.gradle.kts` to one you own. The Rate Bits link uses it.
- Delete the Developer section in `SettingsScreen.kt` (Simulate Pro, Simulate midnight, Reset).
- Create a real release signing key; the included key is for testing only.
- Wire up Play Billing so `isPro` is set by a verified purchase instead of the developer switch.

## Project layout

- `data/` model, changes, the midnight move, JSON storage and backups, widget themes
- `games/` game rules (2048), kept free of UI code so they can be tested directly
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, and when the time zone changes
- `ui/` app screens, games, Pro page, and the guided tour (Jetpack Compose)
- `QuickEditActivity.kt` the floating editor opened from the widget
