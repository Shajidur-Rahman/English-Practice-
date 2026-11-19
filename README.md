<div align="center">

# 📱 English Practice (HSC)

**A fast, offline-first Android app for mastering HSC English 2nd Paper — _Use of Articles_ & _Use of Prepositions_.**

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Shajidur-Rahman/English-Practice-/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Passages](https://img.shields.io/badge/Passages-156%20(1%2C560%20Blanks)-0D9488?style=for-the-badge)](#-content-coverage)
[![Version](https://img.shields.io/badge/Latest-v2.3.0-1D4ED8?style=for-the-badge)](https://github.com/Shajidur-Rahman/English-Practice-/releases/latest)

### ⬇️ [Download EnglishPractice.apk (v2.3.0 — Direct Download)](https://github.com/Shajidur-Rahman/English-Practice-/raw/main/download/EnglishPractice.apk)
<sub>Direct Android APK (`v2.2.0`) • Also check <a href="https://github.com/Shajidur-Rahman/English-Practice-/releases">GitHub Releases</a> for future updates (auto-detected inside the app).</sub>

</div>

---

## ✨ Why English Practice?

Practicing from dense textbook pages and flipping back and forth to cluttered solution sheets slows down revision. **English Practice** packages **all 156 original book passages** and **1,560 verified solution keys** into a clean, zero-distraction mobile workspace with exam simulation, oral testing for parents, and an automatic SQLite spaced-repetition mistake bank.

---

## 📚 Content Coverage

| Topic | Total Passages | Total Blanks | HSC Board Questions | Practice Exercises | Book Reference |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Articles** | **76** | **760** | `#1–#24` *(2016–2019)* | `#25–#76` | Pages 49–56 |
| **Prepositions** | **80** | **800** | `#1–#56` *(2016–2025)* | `#57–#80` | Pages 84–91 |
| **Combined** | **156** | **1,560** | **80 Board Sets** | **76 Practice Sets** | **100% Offline** |

> Every answer key supports official multi-option slash variants (e.g., `a/the`, `the/×`, `of/for/about`, `before/in/in front of`) and optional strict capitalization checks (`A`/`An`/`The`, `In`/`For`/`After`/`With`).

---

## 🚀 Core Features

### 1. 💜 Mother's Test *(Read-Only Oral Testing & Verification Mode)*
Designed specifically so your mother (or a study partner) can quiz you effortlessly without squinting at tiny textbook solution lines:
- **Clean Topic & Passage Picker:** Full searchable list of all 76 Article and 80 Preposition passages.
- **100% Read-Only & Customizable (`Customize` Button):** Switch effortlessly between **5 + 5 Split** (`a–e` left, `f–j` right), **One Line Each**, **2-Line Compact**, or **Full Page (All)** continuous sheet view, plus an **Answer Text Size** slider (`Normal` → `Huge 175%`) and **Minimal Header** toggle.
- **`Before` & `Next` Navigation:** Move smoothly from passage to passage.
- **✨ Magic • Real Book Button:** Instantly reveals the **real cropped photo from the printed book solution sheet** (highlighted with a blue frame around that exact row, with pinch, double-tap, and fullscreen zoom) so you can verify every single answer with your own eyes.

### 2. 🧠 SQLite Flash Memory Database *(Spaced Repetition)*
Never lose track of the blanks you miss:
- **Auto-Capture Mistakes:** Wrong or blank answers from **both Articles and Prepositions** are automatically saved in an on-device SQLite database (`english_practice_flashcards.db`).
- **SM-2 Spaced Repetition:** Rate cards with `Again (10m)`, `Hard`, `Good`, or `Easy` to schedule optimal review intervals and track mastery streaks.
- **Smart Filters & Cram Mode:** Filter by **All / Articles / Prepositions** and **Due Now / All Mistakes / Hardest (2+ Misses) / Pinned & Custom / Mastered**.
- **Custom Cards & Notes:** Pin any blank from Practice/Results/Keys, add your own custom flashcards, and attach personal grammar memory tips.

### 3. 📝 Super-Flexible Practice & Exam Builder
- **Topics:** Practice **Articles**, **Prepositions**, or **Mixed (Both)** in one exam.
- **9 Selection Modes:** All Passages, Board Questions, Practice Exercises, Custom Range, Random N, Mistake Bank, Bookmarked, Unattempted, or Manual Pick.
- **Timers:** Untimed stopwatch, Total Exam Countdown, or Per-Passage Timer (with optional auto-submit).
- **HSC Scoring Engine:** Standard HSC `0.5` mark per blank (`5.0` per passage) or `1.0` mark per blank, Strict Capitalization toggle, and configurable Negative Marking (`None`, `-0.25x`, `-0.5x`).
- **Preposition Input Pad:** Type directly with `Next` blank auto-advance, tap quick-pick HSC preposition chips, or enable the Practice-only **Word Bank** hint.

### 4. 📱 Built for Small Phones & Reading Comfort
- **Phone-First Ergonomics:** Tuned for compact screens (`360dp` width Samsung Galaxy devices and up) with zero clipped labels or crowded toolbars.
- **Double-Tap & Fullscreen Viewer (`⛶`):** Inspect any passage or real solution photo fullscreen at up to `5.5x` zoom.
- **Dark Mode + Paper Inversion:** Comfortable night reading that automatically inverts bright white textbook scans into dark-friendly paper.
- **On-Device ML Kit OCR:** Scan passage images offline into interactive clickable inline text, plus unified JSON Export/Import backup.

### 5. 🔄 Automatic GitHub Release Updater
- Automatically checks [`Shajidur-Rahman/English-Practice-`](https://github.com/Shajidur-Rahman/English-Practice-/releases/latest) on startup (stays quiet when offline) and manually under **Keys → Settings → App Updates**.
- Shows an in-app **New Update Available** card with release notes and a 1-tap APK download button whenever a newer tag is published.

---

## 📲 How to Install on Android

1. Tap **[Download EnglishPractice.apk](https://github.com/Shajidur-Rahman/English-Practice-/raw/main/download/EnglishPractice.apk)** on your phone.
2. Open the downloaded `.apk` file from your notifications or **My Files → Downloads**.
3. If prompted, allow **Install unknown apps** for your browser or file manager, then tap **Install**.

*Minimum requirement: Android 8.0 (Oreo, API 26) or higher. Works 100% offline once installed.*

---

## 🛠️ Tech Stack & Architecture

| Layer | Technology |
| :--- | :--- |
| **UI** | Jetpack Compose + Material 3 |
| **Language** | Kotlin 2.0 + Coroutines / StateFlow |
| **Flashcard Storage** | Native Android `SQLiteOpenHelper` (`english_practice_flashcards.db`) |
| **Progress & Backup** | Local JSON persistence + Full Export/Import (stats, texts, flashcards) |
| **On-Device OCR** | Google ML Kit Text Recognition (`com.google.mlkit:text-recognition`) |
| **Updates** | GitHub Releases REST API (`HttpURLConnection`, zero extra network bloat) |
| **Media Assets** | 156 deskewed passage WebPs + 156 highlighted real solution WebPs |

```text
app/src/main/
├── assets/
│   ├── answer_keys.json              # 76 Article keys (760 blanks)
│   ├── preposition_answer_keys.json  # 80 Preposition keys (800 blanks)
│   ├── passages/                     # p_01..76.webp & prep_01..80.webp
│   └── solutions/                    # sol_art_01..76.webp & sol_prep_01..80.webp
└── java/com/articlemaster/hsc/
    ├── MainActivity.kt               # Bottom navigation & route host
    ├── data/
    │   ├── ExerciseRepository.kt     # Asset loader, progress & backup I/O
    │   ├── FlashcardDatabaseHelper.kt# SQLite Flash Memory DB + SM-2 persistence
    │   └── GitHubUpdateChecker.kt    # GitHub Releases version checker
    ├── model/Models.kt               # Evaluators, SM-2 engine & domain models
    ├── ocr/OnDeviceOcrHelper.kt      # Offline ML Kit passage scanner
    └── ui/
        ├── MainViewModel.kt          # App state, timer, session & flashcard actions
        ├── components/               # Fullscreen zoom, inline blanks & input pads
        └── screens/                  # Home, Browse, ExamBuilder, MothersTest, Flashcards, Session, Result, Reference
```

---

## 🧑‍💻 Build & Release Guide (For Maintainers)

### Build & Run Tests
```bash
./gradlew testDebugUnitTest assembleDebug
```
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Publish a New Update (Triggers In-App Updater)
1. Increment `versionCode` and `versionName` in `app/build.gradle.kts` (e.g., `versionCode = 4`, `versionName = "2.2.0"`).
2. Build the APK and name it `EnglishPractice.apk`.
3. Create a new **GitHub Release** on [Releases](https://github.com/Shajidur-Rahman/English-Practice-/releases/new) with tag `v2.2.0` and attach `EnglishPractice.apk`.
4. All installed users will automatically receive an in-app **"New Update Available"** banner with a 1-tap download button!
