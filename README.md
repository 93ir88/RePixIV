# RePixIV
Twitter/X-style art feed syncing from Pixiv, built with Jetpack Compose and the **Miuix** UI library.

## Design credits
The entire UI design (Settings page, bottom NavigationBar, top bars, blur, squircle shapes and
Monet / Material You color engine) comes from **[Miuix](https://github.com/compose-miuix-ui/miuix)**
by the compose-miuix-ui contributors, licensed under Apache-2.0 (see `LICENSE-miuix` and `NOTICE`).
Files derived from the Miuix example app carry the original copyright header.

## Build
Push to `main` → Actions tab → download APK artifact.
Toolchain mirrors Miuix: JDK 21, Gradle 9.9.0, AGP 9.5.1, Kotlin 2.4.21, compileSdk 37, Miuix 0.9.4 (sources ported from tag v0.9.4).

## Tabs
Home · Search · Profile · Settings (pager with swipe between tabs, like the Miuix example)

## Settings
Show FPS Monitor, Color Mode (System / Light / Dark / MonetSystem / MonetLight / MonetDark),
Key Color, Palette Style, Color Spec, Squircle, Blur, Scroll End Haptic, Page User Scroll,
Pager Gesture Mode, Show TopAppBar, Show NavigationBar, Navigation Badge, NavigationBar Mode.
