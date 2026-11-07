# RePixIV

A Twitter/X-style art feed app that syncs artwork from Pixiv.

## Build

Automatic via GitHub Actions — push to `main` and grab the APK from the **Actions** tab.

For local builds, install [Gradle 8.0.2](https://gradle.org/releases/) then:

```bash
gradle assembleDebug
```

## Status

- [x] Base feed UI with mock artwork cards
- [x] Bottom navigation (Home / Search / Profile)
- [ ] Pixiv OAuth login
- [ ] Live artwork sync from Pixiv API
- [ ] Like / Bookmark actions
- [ ] Search by tag / artist

## Stack

- Kotlin · AndroidX · Material Components
- Navigation Component · RecyclerView · Glide
- Coroutines + LiveData (ready for Pixiv API layer)
