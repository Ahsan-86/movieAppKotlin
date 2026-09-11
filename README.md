# Movie App

A movie browsing app built on TMDB, rewritten from scratch on modern Android: Jetpack Compose, Kotlin Coroutines/Flow, Hilt, Room, Retrofit, WorkManager, and Navigation-Compose.

## What's built so far

- **Offline-first everywhere** — every screen reads from Room, not the network directly. TMDB data is cached locally, shown instantly, and refreshed live in the background (WorkManager syncs every 6 hours). No crash or blank screen when offline.
- **Navigation** — bottom nav with Explore / Trending / Favorites, plus Search and Account.
- **Explore/Home** — horizontally-scrolling carousels: Trending Today, Popular, For You (based on your favorite genres), Now Playing, Top Rated, Upcoming.
- **Favorites** — tap the heart on any poster, fully offline (just a Room table).
- **Search** — debounced search (fires after you stop typing, not on every keystroke) by title, cast, or description, with cast results linking to a full filmography. Search results are paginated and cached.
- **Movie details** — cascaded redesign with cast & crew (director, full credits), an Information section (status, runtime, budget/revenue, production companies, official site), Similar and Recommended sections, and franchise/collection links where applicable.
- **Person screen** — bio, age/gender/profession, and their filmography sorted by release date (newest first), shown as a list with favorite toggles.
- **Accounts** — Guest mode, Sign Up, and Log In via Firebase Authentication.
- **Pagination** — Paging 3 + a Room `RemoteMediator`, applied across Trending, Genre browsing, Search, and filtered Discover results.

## Coming next

- **TV show support** — TV titles in search results (grouped separately from movies) and full TV browsing/detail screens.

## Setup

### 1. TMDB API key
The old version of this app had a TMDB key hardcoded in source, and that repo history is public — treat that key as compromised if you're reusing this codebase.

1. Get a free key at [themoviedb.org/settings/api](https://www.themoviedb.org/settings/api) (the "API Key (v3 auth)" value).
2. Copy `local.properties.example` to `local.properties` (gitignored, never committed).
3. Add `TMDB_API_KEY=your_key_here` to `local.properties`.

### 2. Open in Android Studio
Requires **Android Studio Ladybug (2024.2)** or newer, and **JDK 17** (bundled with recent Android Studio). Open the project root, let Gradle sync, add your TMDB key, then Run. Min SDK 24 (Android 7.0), target/compile SDK 35.

### 3. Firebase (for sign-up/login)
1. Create a free project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app with application ID `com.ahsan.movieapp`.
3. Download `google-services.json` and place it in `app/`.

Guest mode works without this step.

## Architecture

```
data/
  remote/      TMDB Retrofit API + DTOs
  local/       Room database, entities, DAOs (offline source of truth)
  mapper/      DTO ↔ Entity ↔ domain model conversions
  repository/  Offline-first repositories (network-bound-resource pattern)
domain/
  model/       UI-facing models (Movie, MovieDetails, CastMember, Person)
di/            Hilt modules
work/          WorkManager background sync
ui/
  theme/       Material 3 theme (dark-first, dynamic color on Android 12+)
  navigation/  NavHost, bottom nav, routes
  components/  Reusable composables
  home/ trending/ favorites/ search/ detail/ person/ account/   One package per screen
```

Every ViewModel talks only to its repository — never directly to Retrofit or Firebase — so the backend can change without touching the UI.

## A note on this being an AI-assisted rewrite

Large portions of this rewrite were generated with Claude Code, developed in phases and reviewed/tested locally in Android Studio after each phase.
