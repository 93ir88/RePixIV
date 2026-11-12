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

## Player
The Player tab plays music inside the app from your **Google / YouTube Music account**. Tap *Sign in with Google*:
the sign-in page opens in your device's browser (OAuth 2.0 with PKCE, never inside the app) and returns to RePixIV.
Music comes from the official **YouTube Data API v3** (YouTube Music has no public API; its songs are YouTube videos
in the Music category) and plays through YouTube's official embedded player (a tiny view kept in the main window, so playback
continues across tabs). Lists load more as you reach the end. "For You" shows what you play most in RePixIV, then music you liked on YouTube, then popular music
for your region; "Favorites" are the tracks you starred (saved on-device); the search bar searches music.
Tap a track to play it; tap it again for the glass control panel (stop, repeat, speed, restart, volume), whose
liquid-glass controls are adapted from Kyant's [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (Apache-2.0).
Some tracks can't be embedded (the owner disabled it); those are skipped automatically.

## Build
Push to `main` → Actions tab → download APK artifact.
Toolchain mirrors Miuix: JDK 21, Gradle 9.9.0, AGP 9.5.1, Kotlin 2.4.21, compileSdk 37, Miuix 0.9.4 (sources ported from tag v0.9.4).

## Features
- **Log in with pixiv** (OAuth + PKCE through the official web login page, opened in your device browser and returned to the app
  via `pixiv://account/login`), or paste a refresh token. If the official pixiv app is installed it may claim that redirect.
- **Home** (pixiv-style tabs): *Home* (latest works for you + recommendations with comment previews and
  "Collections by" mosaics), *Illustrations* (daily ranking + following), *Manga*, *Discover* (trending tags
  + rising this week). Pull to refresh with the Miuix indicator.
- **Likes and comments**: heart any post (optimistic, with a pop animation), open and write comments. **Search**: tag/keyword search.
  **Profile**: your account stats and public bookmarks, with an account-settings button (top right).
- **Post page**: swipeable pages, like/share/menu, caption, tags, stats and date, the artist's works strip
  (scrolls endlessly), comments with replies, and related works.
- **Artist page**: cover, premium badge, follow, links, Home / Illustrations / Bookmarks tabs with an endless grid
  and a swipeable featured carousel.
- **Search**: history while the bar is focused, favorite tags (add / remove, on-device), illustration and user search.
- Everything opens inside the app: tap a post for the full-screen zoomable viewer, tap an artist's
  avatar for their page (follow / unfollow).
- Per-post menu: Download (original quality, saved to Pictures/RePixIV), View full screen,
  Not interested (hides the post and similar ones, stored on-device) and Share (link).
- Account settings: restricted mode and AI-generated works (synced with pixiv), account info.
- Tabs: Home, Following, Search, Profile (swipe between them like the Miuix example app). Screens opened on top of
  each other form a real stack, so going back never reloads the screen underneath.
- **Following**: new works from people you follow (All) and who you follow (Users), with a public/private filter.
- **Settings** live behind the gear icon on the Profile tab: account settings first, then a "RePixIV settings"
  category with the app options, including **Low-res mode** (Disabled / 144p / 480p / 720p).

## Settings
Color Mode (System / Light / Dark / MonetSystem / MonetLight / MonetDark), Key Color, Palette Style,
Color Spec, Squircle, Blur, Scroll End Haptic, Page User Scroll, Pager Gesture Mode, Show TopAppBar,
TopAppBar Blur Style, Show NavigationBar, Navigation Badge, NavigationBar Mode and
**Use FloatingNavigationBar** (+ position Center / Start / End).
Fresh-install defaults live in `AppState` (`AppState.kt`): Progressive TopAppBar blur and the floating navigation bar (Center) are on.

## Notes
This uses pixiv's unofficial mobile app API, which can change without notice. Tokens are stored in
app-private SharedPreferences. Android backup is turned off so saved settings and tokens are never restored onto a fresh install.
