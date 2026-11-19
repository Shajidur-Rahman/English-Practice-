# English Practice (HSC Articles & Prepositions)

An offline-first Android app built with Jetpack Compose for practicing **HSC English 2nd Paper — Articles (76 passages, 760 blanks)** and **Prepositions (80 passages, 800 blanks)**, complete with an oral **Mother's Test** viewer, **SQLite Flash Memory** spaced-repetition mistake bank, and **GitHub Releases** update checker.

## Key Features
- **Articles & Prepositions Menu**: Full library of 76 Article passages (`1–24` HSC Board Qs, `25–76` Practice) and 80 Preposition passages (`1–56` HSC Board Qs, `57–80` Practice).
- **Mother's Test (Oral Testing Mode)**:
  - Clean full list of all Articles and Prepositions.
  - Ultra-readable **5 Left `(a)–(e)` + 5 Right `(f)–(j)`** answer grid with optional tap-to-mark (`✗` / `✓`) and 1-tap save oral mistakes to Flash Memory.
  - **`Before` & `Next`** navigation buttons.
  - **✨ Magic Button**: Reveals the real cropped photo from the printed book solution sheet (with pinch, double-tap, and fullscreen zoom) to verify every answer directly.
- **Flexible Exam & Practice Builder**: Choose Articles, Prepositions, or Mixed (Both); filter by Board/Practice/Range/Random/Mistakes/Bookmarked/Unattempted/Manual Pick; configure Timers, HSC `0.5`-mark scale, Strict Capitalization, Negative Marking, Instant Feedback, and Practice-only Preposition Word Bank hints.
- **SQLite Flash Memory Database**: Automatically saves missed blanks from both Articles and Prepositions with SM-2 Spaced Repetition (`Again`, `Hard`, `Good`, `Easy`), Cram Mode, custom card creation, personal memory notes, and unified JSON Export/Import backup.
- **Compact Phone Ergonomics**: Edge-to-edge layout tuned for small Samsung Galaxy screens (`360dp` width) with single-line segmented controls, double-tap zoom, and a 1-tap fullscreen image viewer.
- **In-App GitHub Release Updater**: Automatically checks `Shajidur-Rahman/English-Practice-` latest release on startup (and manually in **Keys → Settings → App Updates**).

## Building Locally
```bash
./gradlew testDebugUnitTest assembleDebug
```
The built APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Publishing an App Update (Triggers In-App Update Banner)
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` (e.g., `2.2.0`).
2. Build your APK (`./gradlew assembleDebug` or `assembleRelease`) and rename it `EnglishPractice.apk`.
3. Create a new **GitHub Release** on `https://github.com/Shajidur-Rahman/English-Practice-/releases` with tag `v2.2.0` and attach `EnglishPractice.apk`.
4. Existing users will automatically see **"New Update Available (v2.2.0)"** on the Home screen with a 1-tap download button!
