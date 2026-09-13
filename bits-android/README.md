# Bits

Your external brain, living on your home screen. Android prototype v0.8.

## Updating

1. Upload these files to your repo, replacing the old ones. Commit.
2. **Actions** -> green tick -> download **bits-apk** -> install over v0.7.
3. Remove and re-add the widget.

Settings -> About -> Version should read **0.8.0**.

## New in v0.8

**Settings** - the two duplicate widget sections are gone. Everything now lives under
**Widget lists**, using the richer swatch tiles from the old theme card. Free users see one
card that drives all their widgets; Pro users get one card per widget, each with its own
categories, clock, theme and opacity, plus a "Match the app" option.

**Word Guess**
- **No try limit.** Keep guessing until you solve it; the end screen reports how many
  tries it took.
- **A hint point for every completed row**, not just for solving.
- **Full-screen instructions** on opening, in pixel styling, impossible to miss.
  An **INSTRUCTIONS** button in the header reopens them any time.
- **Keyboard fixed.** Every row now spans the same ten key-widths, so the middle row is no
  longer wider than the others, and every key is the same fixed height.
- Bigger letter cells with thin borders, matching your reference layout.

**Widget card** - long text is capped and scrolls, so Save/Delete/Cancel can never be
pushed off screen. Undo and the "item isn't there anymore" message are gone from the
widget entirely; deleting just closes the card.

**Memory Match** - emoji faces are much larger and easier to read at a glance.

**Snake** - the D-pad is considerably bigger and more spread out.

## Protecting the Pro perks

The easter egg is one atomic all-or-nothing claim. Restoring a backup never grants Pro or
unlocks, because entitlements belong to the device.

**The honest limit:** Bits is fully offline, so all state is a file on the user's device.
Anyone willing to root their phone or decompile the APK can change it. Only server-side or
Play Billing verification prevents that.

## Not live yet

**Payments.** Test Pro via Settings -> Developer -> **Simulate Pro**.

## The hidden thing

Four taps on the "Bits" title. One theme, one game, one clock style, once per device.

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
