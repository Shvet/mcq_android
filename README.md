# MCQ Android app

Kotlin + Jetpack Compose reader for chapter-wise study PDFs (NEET-AIPMT style), backed by the
Laravel admin panel in [Shvet/mcq](https://github.com/Shvet/mcq).

## Features

- Splash screen with the brand badge, lab illustration and app title
- Home list of PDF categories with index numbers, "last read" button and a 300x250 ad
- Navigation drawer: Home, Pdf BookMark (with count badge), Share App, Rate Us, Feedback (mail)
- Chapter lists (any depth the backend provides) with a per-category label such as "Chapter-wise"
- PDF viewer
  - Questions / Answers section tabs (each section is its own PDF)
  - Page counter (tap to jump to a page), pinch to zoom
  - Night mode, full-text search (PdfBox-Android), bookmarks
  - Draw mode with eraser, stroke size, colour, opacity, undo / redo
  - Screenshot of the page with drawings, shared through the system share sheet
  - PDFs are cached on the device after the first download
- Bookmarks screen with delete
- MCQ tests (home screen's **MCQ Test** tab), from the admin panel's MCQ section
  - Same category → sub category → sub sub category tree as the PDFs; every leaf is a test
  - Questions and answers can be text, images (tap to zoom) or both; 4 or 5 answers
  - Timer starts when the student taps *Start test*; pause/resume, auto-pause when the app is left
  - Question grid to jump around, finish confirmation with the un-attempted count
  - Result screen: score ring, right / wrong / un-attempted counts, time taken, accuracy,
    answer review with filters and the admin's note for the right answer
  - *Share result* sends the score card as a screenshot
- "Show Your Support" rate dialog on exit
- AdMob banners and interstitials (every N PDF opens), Google test IDs by default

Titles, dialog texts, feedback email, share text, ad unit IDs and the ads switch come from the
admin panel's **App Settings** (matched by the app's `applicationId`), with built-in fallbacks.

## Configuration

| What | Where |
| --- | --- |
| Backend URL | `-Pmcq.baseUrl=https://your-domain/` or edit `BASE_URL` in `app/build.gradle.kts` (default `http://10.0.2.2:8000/`, the host machine from the emulator) |
| Package name used for content | `applicationId` in `app/build.gradle.kts` — must match the categories' *Package Name* and the App Settings entry in the admin panel |
| AdMob app ID | `-Pmcq.admobAppId=ca-app-pub-…~…` (defaults to Google's test app ID) |
| AdMob unit IDs | Admin panel → App Settings (empty = Google test ads) |

The backend serves files from the project root, so `BASE_URL` is the URL where `index.php` lives.

## Build

Requires JDK 17 and the Android SDK (API 35).

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest
```

## Project layout

```
app/src/main/java/com/onegoal/mcq/
  data/remote   Retrofit API + DTOs for the Laravel endpoints
  data/local    Room database for bookmarks
  data/prefs    DataStore: last read position, cached app settings, night mode
  data/pdf      PDF download cache
  data/quiz     In-memory hand-off of finished test results to the result screen
  ui/…          Compose screens (splash, home, list, bookmarks, pdf, quiz) and shared components
  ads           AdMob helpers
```
