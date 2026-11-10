# RePixIV
Twitter/X-style art feed syncing from Pixiv, built with Jetpack Compose and the **Miuix** UI library.

## Design credits
The entire UI design (Settings page, bottom NavigationBar, top bars, blur, squircle shapes and
Monet / Material You color engine) comes from **[Miuix](https://github.com/compose-miuix-ui/miuix)**
by the compose-miuix-ui contributors, licensed under Apache-2.0 (see `LICENSE-miuix` and `NOTICE`).
Files derived from the Miuix example app carry the original copyright header.

The **floating navigation bar** (Settings → Use FloatingNavigationBar) is the liquid-glass bar from
**[BakaSU](https://github.com/Baka-SU/BakaSU)** (GPL-3.0, see `LICENSE-bakasu`), which adapted it from the Miuix
example app and Kyant0's [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (both Apache-2.0).
Because it is GPL-3.0 code, distributing RePixIV with it means following the GPL-3.0 terms
(for example, offering the source of your build).

## Build
Push to `main` → Actions tab → download APK artifact.
Toolchain mirrors Miuix: JDK 21, Gradle 9.9.0, AGP 9.5.1, Kotlin 2.4.21, compileSdk 37, Miuix 0.9.4 (sources ported from tag v0.9.4).

## Features
- **Log in with pixiv** (OAuth + PKCE through the official web login page), or paste a refresh token.
- **Home** (pixiv-style tabs): *Home* (latest works for you + recommendations with comment previews and
  "Collections by" mosaics), *Illustrations* (daily ranking + following), *Manga*, *Discover* (trending tags
  + rising this week). Pull to refresh with the Miuix indicator.
- **Likes and comments**: heart any post (optimistic, with a pop animation), open and write comments. **Search**: tag/keyword search.
  **Profile**: your account stats and public bookmarks, with an account-settings button (top right).
- Everything opens inside the app: tap a post for the full-screen zoomable viewer, tap an artist's
  avatar for their page (follow / unfollow).
- Per-post menu: Download (original quality, saved to Pictures/RePixIV), View full screen,
  Not interested (hides the post and similar ones, stored on-device) and Share (link).
- Account settings: restricted mode and AI-generated works (synced with pixiv), account info.
- Tabs swipe like the Miuix example app.

## Settings
Color Mode (System / Light / Dark / MonetSystem / MonetLight / MonetDark), Key Color, Palette Style,
Color Spec, Squircle, Blur, Scroll End Haptic, Page User Scroll, Pager Gesture Mode, Show TopAppBar,
TopAppBar Blur Style, Show NavigationBar, Navigation Badge, NavigationBar Mode and
**Use FloatingNavigationBar** (+ position Center / Start / End).

## Notes
This uses pixiv's unofficial mobile app API, which can change without notice. Tokens are stored in
app-private SharedPreferences.
