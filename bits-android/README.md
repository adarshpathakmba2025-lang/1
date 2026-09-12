# Bits

Your external brain, living on your home screen. Android prototype v0.4.

## Updating on your phone

1. Upload these files to your `bits` GitHub repository, replacing the old ones, and commit.
2. Open **Actions**, wait for the green tick, download **bits-apk**, install it over v0.3.
3. Remove and re-add the widget so the launcher picks up the new preview and bigger icons.

Check Settings → About → Version says **0.4.0**. If it doesn't, you installed an older artifact.

## New in v0.4

**Lists**
- New items go to the **top** by default. Settings → Lists has a toggle for the bottom.
- Items moving from Tomorrow at midnight land on **top** of Today.
- **Swipe sideways** on a list to move between categories.
- Back button now saves: leaving Edit, or a task you were typing, keeps the change.
- Hiding a category just dims it. A one-time message explains what dimming means.

**Widget**
- Bigger controller, "Bits", and settings icons, and bigger checkboxes.
- Long text no longer runs under the scrollbar.
- Tapping a **category heading** opens a small add box, instead of the whole app.
- Six clock styles (two free, four Pro).
- Eight themes with full palettes and descriptive names, not just accent swaps.

**Games** — now its own retro pixel section with a high score per game.
- Free: 2048, Snake
- Pro: Memory Match, X and O, Word Guess, Flappy

**Pro**
- Redesigned page with perks, and separate Lifetime (₹179) and Monthly (₹49) cards.
- The founder note appears once before the page. Pro users get a thank-you instead of a pitch.
- Restore purchases sits in Settings → About.

**Settings** — rebuilt into labelled sections with cards.

## Not live yet

- **Payments.** Play Billing needs a merchant account and a Play Console listing first.
  To test Pro features: Settings → Developer → **Simulate Pro**.
- **A second widget list.** Shown as "coming soon" on the Pro page; it needs its own data model.

## There's a hidden thing

Tap the "Bits" title on the home page six times in a row. One free theme, once per device.
Don't tell anyone. The developer switch does not reset it, so test it deliberately.

## If the build fails

Open the failed run, click the red step, copy the lines starting with `e:`, and send them to Claude.

## Before publishing

- Change `applicationId` in `app/build.gradle.kts` to one you own. Rate Bits uses it.
- Delete the Developer section in `SettingsScreen.kt`.
- Create a real release signing key; the bundled one is for testing.
- Wire Play Billing so `isPro` comes from a verified purchase.
- Press Start 2P, Chakra Petch and Atkinson Hyperlegible are all open-licensed; keep their licences with the app.

## Project layout

- `data/` model, the midnight move, storage, backups, themes and clock styles
- `games/` pure game rules (2048, Snake, Memory, X and O, Word Guess, Flappy), all unit-tested
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, and on time zone changes
- `ui/` app screens, retro games chrome, Pro page, tour
- `QuickEditActivity.kt` the floating add/edit card opened from the widget
