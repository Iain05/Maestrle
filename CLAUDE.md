# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Spring Boot — run from `backend/`)
```bash
./mvnw                  # Run backend (default goal: spring-boot:run)
./mvnw test             # Run tests
./mvnw package          # Build JAR
```

### Frontend (run from `frontend/`)
```bash
npm run dev             # Start Vite dev server with HMR
npm run build           # Type-check + production build
npm run lint            # ESLint
```

### Infrastructure
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d  # Start db + audio-server (dev, ports exposed to host)
docker compose --profile prod up -d                                    # Start all four services (prod, no extra ports exposed)
```

## Architecture

ComposerGuesser is a daily classical music guessing game (Wordle-style). Users listen to an audio excerpt and guess the composer using hint-based feedback.

### Services
- **frontend** — React 19 SPA (Vite), served by nginx in prod
- **backend** — Spring Boot 4, Java 17 (`backend/`)
- **db** — PostgreSQL 17 (always running via Docker)
- **audio-server** — nginx serving mp3 files from `./audio-files/`

In dev, only `db` and `audio-server` run in Docker. Frontend and backend run locally. The Vite dev server proxies `/api` → `http://localhost:8080` and `/audio` → `http://localhost:8081`. In prod, the frontend nginx handles both proxies.

### Backend package structure (`backend/src/main/java/org/composerguesser/backend/`)
- `controller/` — REST controllers (`/api` prefix set in `application.properties`)
- `service/` — Business logic (`GuessService`, `UserService`, `ExcerptSubmitService`, `AdminService`, `DailyChallengeScheduler`)
- `model/` — JPA entities (`Composer`, `ComposerWork`, `Excerpt`, `ExcerptDay`, `User`, `UserGuess`, `UserPoint`); enums (`Era`, `ExcerptStatus`, `Role`)
- `repository/` — Spring Data JPA repositories
- `dto/` — Request/response DTOs
- `security/` — JWT auth (`JwtUtil`, `JwtAuthFilter`, `SecurityConfig`)

**Config:** `application.properties` imports `backend/.env` via `spring.config.import`. The `.env` file holds datasource URL, credentials, and `audio.base-url`. The JVM timezone is forced to `America/Vancouver` via `pom.xml` `jvmArguments` — this is required because PostgreSQL 17 rejects the legacy `Canada/Pacific` system timezone.

**Database migrations:** Liquibase runs automatically on startup. Changelogs live in `backend/src/main/resources/db/changelog/changes/` and are registered in `master.xml`. Always add new changesets as new numbered files.

**Daily challenge date:** Resolved server-side using `America/Vancouver` timezone — never from the client.

**Authentication:** JWT-based, stateless. Token is returned on register/login as `Authorization: Bearer <token>`. All endpoints are publicly accessible; the token is optional but enables point tracking, streaks, and leaderboard ranking. `@AuthenticationPrincipal User user` will be `null` for anonymous requests.

**Streak system:** `current_streak` is stored on `tbl_user` and updated in `GuessService` on each correct guess (checks for a `tbl_user_point` entry for yesterday; increments if found, resets to 1 otherwise). `DailyChallengeScheduler` resets streaks to 0 at 00:01 each night for users who missed the previous day, and also runs this check on startup in case the backend was down at midnight.

### API endpoints (`/api` prefix)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | — | Create account; returns JWT + profile |
| POST | `/auth/login` | — | Authenticate; returns JWT + profile |
| GET | `/auth/me` | Required | Current user's profile |
| GET | `/composers` | — | All composers (id + name) for the guess dropdown |
| GET | `/composers/{id}` | — | Single composer full details |
| GET | `/composers/{id}/works` | — | All works for a composer (id + title), sorted by title; used to populate the work dropdown on the submit form |
| GET | `/excerpt/daily-challenge` | Optional | Today's challenge: `{ excerptId, audioUrl, challengeNumber, date, submittedByCurrentUser, uploaderUsername }` |
| GET | `/excerpt/archive` | Optional | All past challenges (before today, newest first): `[{ date, challengeNumber, guessCount, correct }]`; `guessCount`/`correct` are `null` when unauthenticated or not played |
| GET | `/excerpt/challenge/{date}` | Optional | Past challenge for a specific date (YYYY-MM-DD); 400 if date is today or future, 404 if no challenge was scheduled |
| POST | `/excerpt/submit` | Required | Upload a trimmed WAV + metadata as `multipart/form-data`; creates a `DRAFT` excerpt |
| GET | `/guess` | Optional | Authenticated user's guess history for a challenge; accepts optional `?date=YYYY-MM-DD` for archive dates (empty array if anon) |
| POST | `/guess` | Optional | Submit a guess for today's challenge; returns hint feedback and `pointsEarned`/`newStreak` on win |
| POST | `/guess/archive` | Optional | Submit a guess for a past challenge (`{ excerptId, composerId, date }`); no points or streak changes |
| GET | `/guess/archive/statuses` | Optional | Map of `{ [date]: { guessCount, correct } }` for every archive date the user has played; returns `{}` if unauthenticated |
| GET | `/leaderboard/daily` | — | Today's leaderboard, paginated (`?page=0&size=20`) |
| GET | `/leaderboard/all-time` | — | All-time leaderboard by total points, paginated |
| GET | `/leaderboard/my-rank` | Required | Caller's all-time rank, daily rank, points, and streak |
| GET | `/admin/excerpts` | ADMIN | Paginated excerpts filtered by `?status=DRAFT` (default), `ACTIVE`, `REJECTED`, or `DELETED`; optionally filtered by `?composerId=` |
| PATCH | `/admin/excerpts/{id}/status` | ADMIN | Flip an excerpt's status (reject, unreject, delete, restore) |
| PATCH | `/admin/excerpts/{id}/approve` | ADMIN | Update excerpt metadata and set status to `ACTIVE` |
| GET | `/health` | — | `{ "status": "UP" }` — used by Docker health checks |

### Database schema

| Table | Key columns | Notes |
|-------|-------------|-------|
| `tbl_user` | `user_id`, `username`, `email`, `password_hash`, `total_points`, `current_streak`, `created_at`, `role` | `email` is the JWT subject; `role` is a PostgreSQL enum (`USER`, `MODERATOR`, `ADMIN`) |
| `tbl_composer` | `composer_id`, `complete_name`, `last_name`, `birth_year`, `death_year`, `era`, `nationality` | `era` is a PostgreSQL enum |
| `tbl_composer_work` | `work_id`, `composer_id`, `title`, `genre`, `year` | Optional metadata |
| `tbl_excerpt` | `excerpt_id`, `composer_id`, `uploaded_by_user_id`, `name`, `filename`, `times_used`, `status`, `composition_year`, `work_id`, `description`, `date_uploaded` | `filename` maps to a file in `./audio-files/`; `times_used` drives round-robin daily selection; `status` is a PostgreSQL enum (`DRAFT`, `ACTIVE`, `REJECTED`, `DELETED`) — only `ACTIVE` excerpts are eligible for the daily challenge |
| `tbl_excerpt_day` | `date` (PK), `excerpt_id`, `challenge_number` | One row per calendar day; `challenge_number` is auto-assigned via a sequence and is unique |
| `tbl_user_guess` | `guess_id`, `user_id`, `excerpt_id`, `composer_id`, `guess_number`, `date` | Max 5 guesses per user per day; `date` is used to scope queries to today |
| `tbl_user_point` | `point_id`, `user_id`, `excerpt_day_date`, `points`, `earned_at` | Unique per (user, date); `points = 11 - guess_number` (range 6–10) |

### Frontend structure (`frontend/src/`)
- `api/` — All fetch calls (`composer.ts`, `excerpt.ts`, `guess.ts`, `leaderboard.ts`). Add new endpoints here.
- `pages/DailyComposer/` — Main game page. Fetches the daily challenge and composer list on mount, owns `excerptId` and `composers` state, passes them down.
- `pages/Leaderboard/` — Leaderboard page. Toggles between today/all-time views. Shows a "my rank" card for logged-in users.
- `pages/SubmitExcerpt/` — Desktop-only excerpt submission page with drag-and-drop audio upload and a waveform trimmer.
- `pages/PastChallenges/` — Archive index page (`/challenges`). Fetches `GET /excerpt/archive` for the full list of past challenges (with user play status if logged in), groups them by month, and renders a card grid. Each card links to `/:date` and shows a Wordle-style guess bar if the user has played.
- `pages/ArchiveChallenge/` — Archive game page (`/:date`). Loads a past challenge via `GET /excerpt/challenge/{date}` and plays it using the same `useGameState` hook in archive mode (no points, no streak).
- `hooks/useGameState.ts` — Manages `guesses: GuessResult[]` state. Accepts an optional `archiveDate` param; when set, routes guesses through `POST /api/guess/archive` instead of `POST /api/guess`, and fetches history with `?date=`. `isGameOver` and `won` are derived from guesses.
- `components/` — `AudioPlayer`, `ComposerSearch`, `GuessControls`, `GuessGrid`, `HintCard`, `GameStatus`, `WaveformTrimmer`, `PageLayout`
- `context/` — `AuthContext` (JWT token, user profile, `addPoints`, anonymous guess replay on login), `ToastContext` (error toasts)
- `utils/replayPendingGuesses.ts` — Stores anonymous guesses to `localStorage` and replays them against the API on login/register
- `data/gameData.ts` — Only `MAX_PLAYS` and `MAX_GUESSES` constants remain
- `types/game.ts` — Only `HintStatus = 'CORRECT' | 'CLOSE' | 'WRONG'`

**Path alias:** `@src/*` → `src/*`

**React Compiler** is enabled — avoid manual `useMemo`/`useCallback`.

### Guess flow (daily)
1. `GET /api/excerpt/daily-challenge` → `{ excerptId, audioUrl, submittedByCurrentUser, uploaderUsername, ... }`
2. `GET /api/composers` → `[{ composerId, name }]` (populates search dropdown)
3. User selects a composer → `ComposerSearch` calls `onSelect(ComposerSummary)` to give `GuessControls` the `composerId`
4. `POST /api/guess` with `{ excerptId, composerId }` → `GuessResult` with hint fields (`composerHint`, `yearHint`, `eraHint`, `nationalityHint`) and always includes `targetComposerName` + `pieceTitle` for the end screen

### Guess flow (archive)
1. `GET /api/excerpt/challenge/{date}` → same `DailyChallengeDto` shape as daily challenge
2. `GET /api/composers` → composer list (same as daily)
3. `POST /api/guess/archive` with `{ excerptId, composerId, date }` → same `GuessResult` shape; `pointsEarned` and `newStreak` are always 0
4. Guess history is restored via `GET /api/guess?date={date}` on page load

### Enums
- **`era_type`** (PostgreSQL): `BAROQUE`, `CLASSICAL`, `EARLY_ROMANTIC`, `ROMANTIC`, `LATE_ROMANTIC`, `_20TH_CENTURY`, `MODERN`. Java `Era` enum matches. Era adjacency (one step apart = `CLOSE`) is computed by `ordinal()` comparison in `GuessService`.
- **`excerpt_status_type`** (PostgreSQL): `DRAFT`, `ACTIVE`, `REJECTED`, `DELETED`. Java `ExcerptStatus` enum matches. Only `ACTIVE` excerpts are scheduled for the daily challenge.
- **`user_role_type`** (PostgreSQL): `USER`, `MODERATOR`, `ADMIN`. Java `Role` enum matches. Admin endpoints are restricted to `ADMIN` role via `SecurityConfig`.
