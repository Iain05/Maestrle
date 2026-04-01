# Testing

## Running tests

```bash
cd backend && ./mvnw test
```

All tests are pure unit tests — no database, no Docker, no `.env` file required.

## Philosophy

Tests use **JUnit 5 + Mockito** via `spring-boot-starter-test`. No Spring context is loaded (`@SpringBootTest` is not used in unit tests). Each service is constructed directly with mock repositories injected via constructor, keeping tests fast and fully isolated.

## Controlling time

`GuessService` resolves "today" via an injected `java.time.Clock` bean (defined in `BackendApplication`). In production this is `Clock.system("America/Vancouver")`; in tests a `Clock.fixed(...)` is passed directly to the service constructor. This makes date-sensitive logic (streaks, points, daily challenge validation) fully deterministic.

## Structure

```
backend/src/test/java/org/composerguesser/backend/
└── service/
    └── GuessServiceTest.java   — unit tests for GuessService (36 tests)
```

## What's covered

### `GuessService`

| Group | What's tested |
|-------|---------------|
| Guard conditions | No daily challenge, wrong excerpt ID, duplicate composer guess |
| Hint evaluation | All year hint boundaries (exact/±15/±16), all era hint cases (same/adjacent/far), nationality match/mismatch |
| Anonymous users | No `UserGuess` or `UserPoint` saved; `pointsEarned` and `newStreak` are zero |
| Guess persistence | `UserGuess` saved with correct fields; `guessNumber` increments correctly |
| Points | First-guess earns 10, fifth-guess earns 6; `UserPoint` record fields; `totalPoints` updated on user; own-submission and already-earned-today blocks |
| Streak logic | Increments when yesterday's point exists; resets to 1 when it doesn't; preserved when user was yesterday's submitter (ineligible for points); DTO returns updated value |
| `getGuessHistory` | Null user, no challenge, no guesses, ordered results with correct hints, `pointsEarned` populated from `UserPoint` |

## What's not yet covered

- `UserService` (register/login, password hashing, JWT issuance)
- `AdminService` / `ExcerptSubmitService`
- `DailyChallengeScheduler` (streak reset job)
- REST controller layer (request/response serialisation, auth enforcement)
- Integration tests against a real database
