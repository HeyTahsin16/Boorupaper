# Boorupaper (Android — Kotlin & Jetpack Compose)

**Boorupaper** (`com.degenk.boorusama`, v4.6.1+188) is a native Android client and wallpaper studio for browsing, searching, organizing, downloading, and setting wallpapers across 14+ booru engines, built with **Kotlin** and **Jetpack Compose** (Material 3).

## Key Features
- **Interactive Wallpaper Studio**:
  - Set any viewing post directly as your **Home Screen**, **Lock Screen**, or **Both** wallpapers from the post viewer.
  - Interactive device-aspect-ratio preview with **touch drag/pan** and **pinch-to-zoom** plus fine-grained sliders for **Zoom/Scale (`0.4x`–`4.0x`)**, **Horizontal & Vertical Position (`X`/`Y`)**, **Rotation (`0°`, `90°`, `180°`, `270°`)**, **Resize/Fit Modes (`Crop to Fill`, `Fit Center`, `Stretch to Screen`, `Center 1:1`)**, and **Darken/Dim Overlay** for home screen icon legibility.
  - Also supports launching the native Android System Wallpaper Cropper (`WallpaperManager.getCropAndSetWallpaperIntent`).
- **Clean Start — User-Configured Booru Profiles**:
  - Starts cleanly without pre-loaded default profiles so you only add and manage the Booru profiles you want.
  - Configure unlimited profiles with custom URLs, API credentials, WebView Cookie Grabber (`NativeAndroidBridge`), granular rating filters, and custom download filename formats.
- **Supported Booru Engines**:
  - **Danbooru**, **Gelbooru 0.2.5 / 0.2 / 0.1.x**, **Moebooru**, **e621 / e926**, **Philomena**, **Zerochan**, **Sankaku**, **Szurubooru**, **Hydrus Network**, **Anime Pictures**, **Shimmie2**, **Hybooru**, **E-Shuushuu**, and **Nozomi.la**.
- **Kurumi Color Schemes & Tag Highlighting**:
  - Includes Danbooru Dark, Danbooru Light, AMOLED Midnight, Forest Green, Dark Emerald, Coral Blossom, Terminal Matrix, Neon Cyberpunk, and Android 12+ Material You Dynamic Colors, plus semantic category colors for Artist, Copyright, Character, General, and Meta tags.
- **Masonry & Standard Grids**:
  - Infinite scroll or paginated browsing, multi-select batch bookmarking and downloading, and live blacklisted post breakdown.
- **Interactive Post Viewer**:
  - Horizontal swipe pager, pinch/double-tap zoom, Media3 ExoPlayer video playback, proportional translation note (`PostNote`) overlays with DText rendering, wiki lookup, and comments sheet.
- **Tag Expression & Blacklist Engine**:
  - Supports multi-tag rules, negation (`-tag`), wildcards (`*`), `rating:`, `score:>N`, `downvotes:`, `favcount:`, `uploader:`, and `status:`.
- **Custom Filename Token Generator**:
  - Supports `{id}`, `{md5:maxlength=8}`, `{artist}`, `{character}`, `{copyright}`, `{tags}`, `{rating}`, `{width}`, `{height}`, `{score}`, `{date}`, `{search}`, and `{ext}`.
- **Local Room Persistence & Backup/Restore**:
  - Stores Booru profiles, bookmarks, blacklisted tags, favorite tags, search history, and bulk download tasks in Room SQLite with full JSON backup export/import.

## Build & Test
- **Compile Debug APK**: `gradle assembleDebug` (outputs `app/build/outputs/apk/debug/app-debug.apk`)
- **Run Unit Tests**: `gradle :app:testDebugUnitTest`
