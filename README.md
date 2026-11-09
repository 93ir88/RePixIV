# RePixIV
Twitter/X-style art feed syncing from Pixiv. Miuix-inspired dark UI.

## Build
Push to `main` → Actions tab → download APK artifact.

## Tabs
Home · Search · Profile · Settings

## Settings
- Color Mode: System / Light / Dark / MonetSystem / MonetLight / MonetDark

## Build dependency fix

BlurView is resolved from JitPack using the published `com.github.Dimezis:BlurView:version-2.0.6` coordinates. The JitPack Maven repository is configured in `settings.gradle`.

## UI refinements
- Bottom navigation icon/label spacing tightened to better match the Miuix reference.
- Color mode popup uses a taller, compact Miuix-like rounded selection panel with a selected checkmark.
- Monet context wrapping is guarded so unsupported dynamic-color overlays fall back safely instead of crashing startup.
