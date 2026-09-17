# Bits

Your external brain, living on your home screen. Android v1.2.1.

## New in v1.5.1

**Category rows, reworked.** A pixel pencil now sits beside each list and opens the
rename; a single tap on the name itself carries that list on the widget or takes it off.
Today and Tomorrow keep the space where the pencil would be, so every name stays on the
same left edge. Renaming no longer has a row of buttons underneath it either - the field
grew its own cross, so Save and the cancel sit together in one place.

**The preview's "tap to scroll" hint is visible again.** It was being drawn before the
widget body rather than after it, so the body painted straight over the top. It only ever
showed through at all when the background opacity was turned down.

**Bitris is swipe-only.** The pad is gone. Tap turns the piece, dragging sideways walks it
a column at a time as the finger travels, dragging down guides it, and a quick flick down
slams it home. The movement is counted off in whole cells against the well's own grid, so
the piece stays under the finger instead of jumping when the finger lifts.

One rule changed to make that work: guiding a piece down no longer locks it. Only gravity
and a flick do. A locking soft drop would have slammed a piece home the moment a finger
swept past the floor, and the rest of that same gesture would then have landed on the next
piece. As a bonus it gives the old lock-delay feel, where a landed piece can still be slid
sideways for a moment before it sets.

## New in v1.5

**Two new games, bringing the hub to eight.**

- **Spaca** - a fixed shooter. A formation of drones sways overhead, peels off to dive at
  you, and drops bombs on the way down. Drag to steer; the guns fire themselves, so there
  is no gesture competing with the steering. Divers are worth double, every third wave
  hands back a life, and a hit costs one life with a blinking grace period after it.
- **Bitris** - blocks fall, full rows clear. The pad moves and turns, swiping down drops.
  A ghost outline shows where the piece will land, and there is a Next box beside the well
  with the running line and level count.

The falling-block and fixed-shooter *rules* are not anyone's property, but the arcade
originals' look and names are, so neither is borrowed. Bitris deliberately avoids the
familiar seven four-block pieces - three of its shapes are three-block pieces and the
mirrored pair is left out - and the well is eighteen rows, in Bits' own palette. Spaca's
attackers are plain blocks rather than insects, and there is no capture-and-rescue trick.

Both games slot in exactly like the existing six: same card in the hub, same arcade frame,
same high score and easter-egg reward plumbing. The Pro page's mini-games perk now reads
six rather than four, and the easter-egg game picker offers the new pair automatically,
since both lists are built from the same enum.

**More words in Word Guess.** The answer list went from 309 to 541, which is also the list
of accepted guesses, so far more everyday words are taken. A year of consecutive days now
draws on over 300 distinct words instead of repeating.

**Pixel headings actually apply now.** The previous version put the real font on a widget
TextView, which works on some launchers and is quietly ignored on others - the widget is
drawn by the launcher, not by Bits, so the font was being swapped for plain sans-serif.
Headings are now drawn into a small bitmap here and sent across as an image, which cannot
be substituted, so the face is the same everywhere. The size steps down to fit two lines
and the result is cached. The in-app preview never consulted the setting at all, so the
toggle and press-and-hold preview both looked inert; both work now.

**"Your lists" is gone.** Renaming, reordering and deleting moved onto the category rows
inside each widget card, under Categories. The same names were previously listed twice on
one page - once to manage, once to choose from - which made it unclear which copy governed
what. The tick chooses whether a list rides on this widget; tapping the name renames it.
With pixel headings on, these names wear the pixel face too.

## New in v1.4.1

Fixed a Gradle script failure. The release signing block used `java.util.Properties()`
and `java.io.FileInputStream(...)` written out fully-qualified inline, which the Kotlin
DSL does not resolve - a build script needs those as real imports at the top of the file.
Both are now imported properly and referenced by their simple names.

## New in v1.4

**The Edit button is now a full page.** Everything that used to be the "Widget lists"
section in Settings now lives here, opened from Edit on the home page or the Customise
icon on the widget. Same behaviour, same auto-save, plus what was missing before:

- **Rename and reorder your lists.** Today and Tomorrow are marked Fixed, since renaming
  or moving them would break the midnight rollover.
- **Go Pro** banner at the top, using the same component as Settings so it carries the
  identical founder pitch.
- Add and delete lists, with an inline confirm before anything is removed.

**Pixel headings.** A toggle under the themes that draws that widget's category names in
the arcade face. Glance can't put a custom font on its own text, so these headings render
through a RemoteViews layout - the same trick the clock styles already use - which can
load the real Press Start 2P font, with the colour pushed in so it still follows the
widget's theme. Per widget, so two widgets can differ. Pro, and included in the easter
egg reward. Tap to set, press and hold to preview, same as themes and clocks.

**The widget's bottom-right icon** is now a Customise glyph opening that Edit page,
rather than a Settings gear.

**Back from the Pro page** now returns wherever you opened it from. It used to always
drop you in Settings, even if you'd come from the games hub.

**Tour** updated for the new Edit page, and the Settings step now reads "Backups, the
tour, and all that boring complicated stuff lives in here."

### Publishing preparation

- `compileSdk`/`targetSdk` raised to 36 (Android 16), which Google now requires for new
  submissions.
- A release `signingConfig` that reads from `keystore.properties`. That file is
  gitignored and must never be committed.
- R8 shrinking and obfuscation enabled for release, with rules covering the
  manifest-declared widget and activity classes.
- The Developer / Simulate Pro section is now behind `BuildConfig.DEBUG`, so a release
  build cannot expose the switch that unlocks every paid feature.
- Play Billing dependency added, ready for wiring up.

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
- `games/` pure game rules, unit-tested (505 checks pass)
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, on time zone changes
- `ui/` screens, onboarding, retro games, Pro page, tour, armed delete
- `QuickEditActivity.kt` the floating add/edit card opened from the widget
- `ReorderActivity.kt` the drag-to-reorder sheet opened from the widget
