# Bits

Your external brain, living on your home screen. Android prototype v0.6.

## Updating

1. Upload these files to your `bits` repo, replacing the old ones. Commit.
2. **Actions** → green tick → download **bits-apk** → install over v0.5.
3. **Remove and re-add the widget** so the launcher refreshes its cached preview.
   The launcher icon may also take a reboot or a launcher restart to update.

Settings → About → Version should read **0.6.0**.

## New in v0.6

**Logo** — the new bold amber tile with the dark pixel checkmark, as an adaptive icon
(plus raster icons for Android 7 and older).

**Widget picker preview** — the picker prefers `previewLayout` over `previewImage` on
Android 12+, and the old layout used a live `TextClock`, which the picker draws blank.
It's now a plain image, which the picker can always render.

**Separate widget lists (Pro)** — each widget now has a full **Customise** panel:
its own categories, clock style, theme and background opacity, or "Match app" to follow
your main settings. Reset returns any widget to the shared look.

**Word Guess — rebuilt as a daily puzzle**
- One word per day, the same for the whole day, drawn from **223 words**.
- The free letter can no longer be typed over: typing only ever fills the other four
  slots, so the hint is structurally protected rather than just visually.
- The free letter's position moves every day.
- Solve it and you're done until tomorrow. Your streak carries over; skipping a day
  resets it. Progress survives closing the app, so a loss can't be retried.

**Two-tap delete, everywhere** — the Delete button sits dim and inert. One tap arms it
and turns it red for 3 seconds; a second tap inside that window deletes. Miss the window
and it disarms. Works the same in the app and in the widget's floating card. A deletion
then offers **Undo** for 5 seconds, and restores the task to its exact old position.

**Snake** — added a retro direction pad. Swiping still works.

**Settings** — widget previews no longer steal your scroll. They show a "Tap to scroll"
badge and only become scrollable once tapped.

**Long-press previews** — now works for unlocked themes and clock styles too, not just
locked ones, and the hint text says so.

**Easter egg** — now **4 taps** on the "Bits" title, and it grants **one theme, one game
and one clock style**, picked together.

**Pricing** — ₹229 lifetime, ₹49 monthly.

## About protecting the Pro perks

The easter egg itself is airtight: it's one atomic all-or-nothing claim, it refuses if any
slot is already filled or if any pick is a free item, and 223 tests include spamming the
claim 50 times and confirming only one set is ever granted.

One hole is now closed that was open before: **restoring a backup no longer grants Pro or
easter-egg unlocks.** Entitlements belong to the device, so a hand-edited backup file
can't be used to unlock anything. Lists and settings still restore normally.

**The honest limit:** Bits is fully offline, so all state lives in a file on the user's
own device. Anyone willing to root their phone or decompile the APK can change it. No
offline app can prevent that — only server-side or Play Billing verification can, which is
what wiring up real billing will give you. The above stops casual sharing and
backup-editing, which is the realistic threat.

## Not live yet

**Payments.** Test Pro via Settings → Developer → **Simulate Pro**.

## Before publishing

- Change `applicationId` to one you own. Rate Bits uses it.
- Delete the Developer section in `SettingsScreen.kt`.
- Make a real release signing key.
- Wire Play Billing so `isPro` comes from a verified purchase, and use Play Console
  **License Testing** to comp yourself and close friends.
- Keep the font licences: Press Start 2P, Chakra Petch, Atkinson Hyperlegible.

## Project layout

- `data/` model, midnight move, storage, backups, themes, clocks, per-widget boards
- `games/` pure game rules, unit-tested (223 checks pass)
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, on time zone changes
- `ui/` screens, onboarding, retro games, Pro page, tour, armed delete
- `QuickEditActivity.kt` the floating add/edit card opened from the widget
