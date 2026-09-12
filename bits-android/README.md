# Bits

Your external brain, living on your home screen. Android prototype v0.2.

## Get it on your phone (no Android Studio needed)

GitHub builds the app for you for free.

1. In your `bits` repository on GitHub, upload the files from this folder, replacing the old ones, and commit.
   - Delete these old files from the repository first if they're still there, or the build will fail:
     `app/src/main/java/com/bits/app/data/Wallpaper.kt`,
     `app/src/main/java/com/bits/app/ui/ListsScreen.kt`,
     `app/src/main/java/com/bits/app/ui/WidgetsScreen.kt`
2. Open the **Actions** tab. A **Build APK** run starts automatically and takes about 5 minutes.
3. When it shows a green tick, open the run and download **bits-apk** under Artifacts. It's a zip with `app-debug.apk` inside.
4. Open the APK on your phone. It installs over the previous version and keeps your lists.

## Add the widget

Long-press an empty spot on your home screen, tap **Widgets**, find **Bits**, and drag it out.
If there isn't one yet, Settings also has **Add widget to home screen**.

Tap **Bits** or the sliders icon at the bottom of the widget to open Settings.

## What's where

- **Home page:** your lists. The gear at the top right opens Settings.
- **Edit** (next to the category chips): add, rename, delete, and reorder categories. The switch on each row shows or hides that category on the widget.
- **Settings:** widget preview, clock, background opacity, daily cleanup, backup and restore, the tour, testing tools, rating, and version.

## Coming from v0.1

Your lists, widget opacity, and clock setting carry over. Categories you had unchecked for the widget become hidden.
The new starter content (Today, Tomorrow, Grocery list) only appears on a fresh install, or via Settings → Testing → Reset to starter content.

## If the build fails

Open the failed run, click the red step, copy the lines that start with `e:`, and send them to Claude.

## Before publishing

- Change `applicationId` in `app/build.gradle.kts` to one you own (for example `com.yourname.bits`). The Rate Bits link uses it.
- Remove the Testing section in `SettingsScreen.kt`.
- Create a real release signing key; the included key is for testing only.

## Project layout

- `data/` model, changes, the midnight move, JSON storage and backups
- `widget/` the home screen widget (Jetpack Glance)
- `time/` wakes the app after midnight, on reboot, and when the time zone changes
- `ui/` the app screens and the guided tour (Jetpack Compose)
