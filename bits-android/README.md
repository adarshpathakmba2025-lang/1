# Bits

Your external brain, living on your home screen. Android v1.2.1.

## New in v1.3.5

Removed the Snake glide animation. It drew each segment sliding from its previous cell to
its current one, which meant the head on screen was up to a full cell behind where the
game logic actually had it. That is why fruit looked eaten a tile early and collisions
looked like they happened before the head reached anything - and on death the animation
froze mid-slide, leaving the snake permanently stranded a tile short of what it hit.

No amount of tuning fixes that, because the mismatch is the concept itself: an
interpolated head can never be in the same place as the logical head. Snake now draws at
exact grid positions, so what is on screen is always exactly what the collision checks
use. The improved turn responsiveness from 1.3.4 is kept.

## New in v1.3.4

Snake turns respond properly again. Two things I added in 1.1 were fighting each other:

- The input buffer only applied a queued turn on the next tick, so a swipe could sit
  unused for most of a tick - up to 260ms at low speed.
- The glide animation stretched each move across that whole tick, so even a correctly
  handled turn *looked* like it was creeping.

The loop now polls in 8ms slices and lets a queued turn cut the wait short, dropping
worst-case turn latency from 260ms to about 117ms (55ms at high speed). The glide now
finishes in 55% of the tick, so motion reads crisp rather than floaty. A floor stops
repeated swiping from cutting every tick short and racing the snake forward - the most it
can ever be sped up is about 2x. The reversal protection from 1.1 is untouched.

## New in v1.3.3

Fixed the LinkRow build failure properly. The root problem: `LinkRow` is called two ways -
some call sites pass `onClick` positionally, others use a trailing lambda. A trailing
lambda always binds to the LAST parameter, so no position for the optional `color`
parameter satisfies both styles at once. v1.3.2 moved `color` last, which fixed the
positional callers and broke all five trailing-lambda ones.

The fix: `onClick` is last (so trailing lambdas work, which is the common style), and the
two call sites that passed it positionally now name it explicitly. Every call site was
checked individually rather than assuming.

## New in v1.3.2

Fixed the build. Two real bugs, both from the same root cause:

- `Celebration.kt` used `Modifier.size(...)` in two new tiles without importing the
  `size` extension.
- `LinkRow` gained an optional `color` parameter inserted *before* `onClick`. Every
  existing call site passes `onClick` positionally, so the lambda meant for it was
  landing on `color` instead, leaving `onClick` empty. Moving `color` to the very end
  fixed every other call site; the one call that actually needs a custom colour now uses
  fully named arguments, since a trailing lambda requires the last parameter to be the
  function type, and that one call also needed to set `color` explicitly.

Also swept the whole project for the same two mistake-shapes rather than just fixing
the two reported lines: every `.size(`/`.height(`/`.width(`/`.padding(`/`.fillMaxWidth(`/
`.fillMaxHeight(`/`.fillMaxSize(`/`.background(`/`.clip(` call now has a matching import
across every file, and every function with a defaulted parameter sitting before a
required one was checked against its actual call sites for the same silent-misbinding
risk. Nothing else was affected.

## New in v1.3.1

Fixed a build failure: `SettingsScreen.kt` used `ProPlan` (for the subscription-plan
selector) in three places without importing it. One missing import line, now added.
Verified this time with a targeted grep for `ProPlan` across the full compiler output,
not just the general error filter, so this exact class of miss can't slip through again.

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
