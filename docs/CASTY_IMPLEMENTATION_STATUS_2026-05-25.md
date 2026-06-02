# Casty implementation status - 2026-05-25

## Build results

- `:composeApp:compileFullDebugKotlinAndroid`: passed.
- `:composeApp:assembleFullDebug`: passed.
- `:composeApp:assembleFullRelease`: passed.
- Debug APK: `composeApp/build/outputs/apk/full/debug/casty-full-debug.apk` - 42.32 MB.
- Release APK: `composeApp/build/outputs/apk/full/release/casty-full-release.apk` - 7.93 MB.

## Completed in this pass

- Library now shows Casty-backed songs as a first-class filter instead of only downloads/playlists/albums/artists.
- Home content now folds in Casty charts and YouTube Music history before fallback searches.
- Search All now includes podcast and episode coverage, with a dedicated Podcasts filter.
- Podcast detail is wired to `YouTube.podcast`, episode queue playback, save/unsave, and real artwork.
- Audiobook/About mock routes and fake screens were removed from Casty runtime.
- Fake cast-device UI, unused branded toast, unused custom notification manager, and legacy Glance widget XML were removed.
- Artist and playlist track taps now preserve queue context through Casty player paths.
- Queue sheet now preserves the current track when clearing queue, separates Up next from Previously played, and uses stable row keys.
- Startup animation uses Casty's `app_icon` asset instead of the old launcher/Casty-style foreground.
- Casty/database source trees are excluded from Android Kotlin compilation; Room KSP and legacy-only dependencies were removed from the Casty Android path.

## Static regression checks

- No active Casty/Casty Kotlin references found for `it.fast4x.Casty`, `PlayerServiceModern`, or `LocalPlayerServiceBinder`.
- No active Casty/Casty Kotlin references found for StrideO, `yt-dlp`, localhost relay, or Go bridge playback.
- No visible Casty routes remain for audiobook/about-song mock surfaces.
- Removed Glance widget descriptors no longer require `glance_default_loading_layout`.

## Remaining real-device checks

- Search a song, tap play, verify first-audio latency, duration, progress, pause, next, previous.
- Open a playlist/album/artist and verify selected item starts at the correct queue index.
- Sign in through Account and verify YouTube Music library, liked songs, playlists, and history populate.
- Verify podcast save/play on a logged-in account.
- Verify notification controls and background playback on a real phone.
