# Bits

Your external brain, living on your home screen. Android v1.1.1.

## Updating

1. Upload these files to your repo, replacing the old ones. Commit.
2. **Actions** -> green tick -> download **bits-apk** -> install over v1.1.
3. Remove and re-add the widget so it picks up the new reorder button.

Settings -> About -> Version should read **1.1.1**.

## New in v1.1.1

- The **reorder button moved to the widget footer**, beside the games controller, so the
  top of the widget is given back entirely to your lists.
- **Checkboxes nudged down** in both the app and the widget, so they sit on the line of
  the text rather than above it.
- **A bit can no longer be dragged above the first category.** The drop is refused in the
  sheet, and the data layer refuses any arrangement that would leave a bit ahead of every
  heading, so nothing can ever be stranded without a category.

## New in v1.1

**Drag to reorder from the widget.** A reorder button (the stacked-bars symbol) sits at
the top right of the widget. Tapping it opens a sheet covering most of the widget area,
showing that widget's exact list with its category headings. Drag any bit anywhere,
including past a heading to move it into that category. Tap Done to save, or tap outside
to dismiss.

The sheet takes its colours strictly from **that widget's own theme**, so two widgets with
different themes never show each other's look.

**The up/down arrows are gone from the widget**, replaced by the drag sheet.
**The left/right arrows remain in the app only**, as you asked.

### Why a sheet and not dragging in the widget itself

A home screen widget is drawn from RemoteViews and runs inside the launcher's process, so
an app can never attach touch listeners to it; a widget can only respond to taps through a
PendingIntent. Every drag-reorder mechanism Android offers needs real Views in the app's
own process. No widget on Android supports in-place dragging. The sheet is the standard
workaround, and it opens over the home screen without ever showing the full app.

## Not live yet

**Payments.** Test Pro via Settings -> Developer -> **Simulate Pro**.

## Before publishing

- Change `applicationId` to one you own. Rate Bits uses it.
- Delete the Developer section in `SettingsScreen.kt`.
- Make a real release signing key.
- Wire Play Billing, and use Play Console **License Testing** to comp yourself.
- Keep the font licences: Press Start 2P, Chakra Petch, Atkinson Hyperlegible.

## Project layout

- `data/` model, midnight move, storage, backups, themes, clocks, boards, hints, ordering
- `games/` pure game rules, unit-tested (383 checks pass)
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, on time zone changes
- `ui/` screens, onboarding, retro games, Pro page, tour, armed delete
- `QuickEditActivity.kt` the floating add/edit card opened from the widget
- `ReorderActivity.kt` the drag-to-reorder sheet opened from the widget
