# Bits

Your external brain, living on your home screen. Android v1.2.1.

## New in v1.3

**Package name locked in:** `com.bits.todoandgames`. Permanent from here on.

**Save and Delete swapped** in both the app's item editor and the widget's popup card:
Delete now sits on the left, Save on the right.

**A real bug found and fixed in Flappy.** Each pipe's collision gap was being
recalculated every tick from the *current* score rather than the score it was actually
spawned at, so a pipe's safe zone could shrink while you were already flying through it \u2014
and the rendering never reflected the real difficulty either, always drawing the easiest
gap regardless of the true one. Both are now fixed: every pipe permanently remembers its
own gap the moment it spawns, and rendering draws exactly that. Verified with a 6000-tick
autopilot run spanning both difficulty ramps.

**Snake input fixed and smoothed.** Two quick swipes in the same tick window used to be
able to compose into a reversal the player never saw coming, since the second swipe was
validated against the first swipe's *unconfirmed* result rather than the snake's true
heading. Turning now buffers exactly one queued direction, always validated against the
confirmed direction. Movement between ticks now glides continuously instead of teleporting
cell to cell.

**Cancel a monthly subscription.** A new field remembers whether Pro came from a lifetime
purchase or a monthly plan, since the two need very different treatment. A "Cancel
membership" link appears \u2014 only for monthly subscribers \u2014 in Settings near Restore
purchases, and on the Pro page's unlocked view. Both open Play's own subscription
management page, which is where Google requires the actual cancel control to live.

**The 4-tap easter egg dialog** now matches the Settings styling: real game icons on the
game picker, the same little clock-layout preview used for clock styles.

## Updating

1. Upload these files to your repo, replacing the old ones. Commit.
2. **Actions** -> green tick -> download **bits-apk** -> install over v1.2.
3. Remove and re-add the widget so it picks up the new reorder button.

Settings -> About -> Version should read **1.2.1**.

## New in v1.2

**A thirty-day signpost.** Thirty days after first launch, a bit appears in Today pointing
the user at the hidden easter egg they might otherwise never find. It grants nothing by
itself: the reward remains the single set every user has always had, claimable once.

If the user already found the egg, no note appears at all, since it would point at nothing.
The allowance is fixed at one and can never rise. Restoring a backup grants nothing, the
install clock included, so the thirty days cannot be rewound.

**Memory Match** - Tries and Best are now identical in size, centred, with a proper pixel
refresh icon beneath the Best figure.

**Widget footer icons** share one size and inset, so the row is even.

**Long bits scroll as you type**, in both the app and the widget card, so the caret is
never left behind.

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
- `games/` pure game rules, unit-tested (478 checks pass)
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, on time zone changes
- `ui/` screens, onboarding, retro games, Pro page, tour, armed delete
- `QuickEditActivity.kt` the floating add/edit card opened from the widget
- `ReorderActivity.kt` the drag-to-reorder sheet opened from the widget
