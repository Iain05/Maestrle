# ComposerGuesser — Architecture

## Overview

ComposerGuesser is a daily classical music identification game. Users listen to an audio excerpt and guess the composer using a hint-based feedback system (similar to Wordle). The application is fully containerized and self-hosted.

---

## Tech Stack

| Layer     | Technology                                                           |
|-----------|----------------------------------------------------------------------|
| Frontend  | React 19, TypeScript, Vite, Tailwind CSS                             |
| Backend   | Spring Boot 4, Java 17, Spring Data JPA, Liquibase, Spring Security  |
| Database  | PostgreSQL 17                                                        |
| Audio     | nginx static file server                                             |
| Infra     | Docker Compose, named volumes                                        |

---

## System Architecture

```
Browser
  │
  ├─── GET /api/**  ──────────────────► backend:8080 (Spring Boot)
  │                                          │
  │                                          ├── reads/writes ──► db:5432 (PostgreSQL)
  │                                          │                        │
  │                                          │                    [db-data volume]
  │                                          │
  │                                          └── (stores filenames, does NOT stream audio)
  │
  ├─── GET /audio/** ─────────────────► audio-server:80 (nginx)
  │                                          │
  │                                      [audio-files volume]
  │
  └─── GET /* ────────────────────────► frontend:80 (nginx serving React build)
```

### Why a separate audio container?

Browsers require **HTTP Range request** support to seek within audio tracks (`<audio>` element). nginx handles range requests natively and is highly efficient for serving large binary files. This offloads file I/O from the Spring Boot process entirely. The backend only stores the audio filename in the database and returns it as part of the API response — the frontend then fetches the audio directly from the audio container.

---

## Docker Compose Services

```yaml
services:
  frontend:
    build: ./frontend
    ports:
      - "80:80"

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/composerguesser
      - SPRING_DATASOURCE_USERNAME=...
      - SPRING_DATASOURCE_PASSWORD=...
    depends_on:
      - db

  db:
    image: postgres:17
    environment:
      POSTGRES_DB: composerguesser
      POSTGRES_USER: ...
      POSTGRES_PASSWORD: ...
    volumes:
      - db-data:/var/lib/postgresql/data

  audio-server:
    image: nginx:alpine
    volumes:
      - audio-files:/usr/share/nginx/html/audio:ro
    ports:
      - "8081:80"

volumes:
  db-data:
  audio-files:
```

The `audio-files` volume is populated manually by copying mp3 files into it on the host server. Filenames stored in the database must match the filenames in the volume exactly.

---

## Data Model

### `tbl_composer`
| Column                 | Type         | Notes                                                          |
|------------------------|--------------|----------------------------------------------------------------|
| composer_id            | BIGINT       | Primary key, auto-increment                                    |
| first_name             | VARCHAR(100) | Not null                                                       |
| last_name              | VARCHAR(100) | Not null                                                       |
| birth_year             | INT          | Not null                                                       |
| death_year             | INT          | Nullable                                                       |
| era                    | era_type     | PostgreSQL enum: `BAROQUE`, `CLASSICAL`, `EARLY_ROMANTIC`, `LATE_ROMANTIC`, `MODERN` |
| nationality            | VARCHAR(100) | ISO 3166-1 alpha-2 country code (e.g. `FR`, `DE`)             |
| number_of_compositions | INT          | Nullable                                                       |

### `tbl_user`
| Column        | Type         | Notes                        |
|---------------|--------------|------------------------------|
| user_id       | BIGINT       | Primary key, auto-increment  |
| username      | VARCHAR(50)  | Not null, unique             |
| email         | VARCHAR(255) | Not null, unique             |
| password_hash | VARCHAR(255) | BCrypt hash, not null        |
| total_points  | INT          | Not null, default 0          |
| created_at    | TIMESTAMP    | Not null                     |

### `tbl_excerpt`
| Column              | Type         | Notes                                      |
|---------------------|--------------|--------------------------------------------|
| excerpt_id          | BIGINT       | Primary key, auto-increment                |
| composer_id         | BIGINT       | FK → tbl_composer, not null                |
| uploaded_by_user_id | BIGINT       | FK → tbl_user, not null                    |
| name                | VARCHAR(255) | Not null                                   |
| filename            | VARCHAR(255) | Audio filename served by nginx, not null   |
| composition_year    | INT          | Nullable                                   |
| work_number         | INT          | Stripped opus/BWV number, nullable         |
| description         | TEXT         | Nullable                                   |
| date_uploaded       | TIMESTAMP    | Not null                                   |
| times_used          | INT          | Not null, default 0                        |

### `tbl_excerpt_day`
| Column     | Type   | Notes                          |
|------------|--------|--------------------------------|
| date       | DATE   | Primary key                    |
| excerpt_id | BIGINT | FK → tbl_excerpt, not null     |

One row per calendar day. The backend resolves today's date in `America/Vancouver` and looks up this table to find the active excerpt.

### `tbl_user_point`
| Column           | Type      | Notes                                                        |
|------------------|-----------|--------------------------------------------------------------|
| point_id         | BIGINT    | Primary key, auto-increment                                  |
| user_id          | BIGINT    | FK → tbl_user, not null                                      |
| excerpt_day_date | DATE      | FK → tbl_excerpt_day, not null                               |
| points           | INT       | Not null                                                     |
| earned_at        | TIMESTAMP | Not null                                                     |

Unique constraint on `(user_id, excerpt_day_date)` — points are awarded at most once per user per day.

### `tbl_user_guess`
| Column      | Type   | Notes                                                              |
|-------------|--------|--------------------------------------------------------------------|
| guess_id    | BIGINT | Primary key, auto-increment                                        |
| user_id     | BIGINT | FK → tbl_user, not null                                            |
| excerpt_id  | BIGINT | FK → tbl_excerpt, not null                                         |
| composer_id | BIGINT | FK → tbl_composer — the composer the user guessed, not null        |
| guess_number| INT    | 1-based index of this guess within the user's session for the day  |

Only populated for authenticated users. Guest play is fully supported but no guesses are persisted.

---

## API Design

All routes are prefixed with `/api` (configured in `application.properties`).

| Method | Route                          | Auth required | Description                                      |
|--------|--------------------------------|---------------|--------------------------------------------------|
| GET    | `/api/health`                  | No            | Health check                                     |
| GET    | `/api/composers`               | No            | List all composers (id + name only)              |
| GET    | `/api/composers/{id}`          | No            | Full details for a single composer               |
| GET    | `/api/excerpt/daily-challenge` | No            | Today's excerpt and audio URL                    |
| POST   | `/api/guess`                   | No (optional) | Submit a guess; authenticated users earn points  |
| POST   | `/api/auth/register`           | No            | Create a new account                             |
| POST   | `/api/auth/login`              | No            | Sign in, receive JWT                             |
| GET    | `/api/auth/me`                 | Yes (JWT)     | Get current user profile                         |

### `/api/composers` response shape
```json
[
  { "composerId": 1, "name": "Ludwig van Beethoven" },
  { "composerId": 2, "name": "Johann Sebastian Bach" }
]
```

### `/api/excerpt/daily-challenge` response shape
```json
{
  "excerptId": 1,
  "audioUrl": "/audio/beethoven-7.mp3"
}
```

The date is resolved server-side in `America/Vancouver` — the client has no input. `audioUrl` is a root-relative path proxied through the frontend nginx to the audio-server container.

### `/api/guess` request / response

**Request:**
```json
{
  "excerptId": 1,
  "composerId": 3
}
```

The `excerptId` must match today's active daily challenge or the request is rejected with 400.

**Response:**
```json
{
  "correct": false,
  "composerName": "Wolfgang Amadeus Mozart",
  "birthYear": 1756,
  "era": "CLASSICAL",
  "nationality": "AT",
  "composerHint": "wrong",
  "yearHint": "TOO_LOW",
  "eraHint": "close",
  "nationalityHint": "wrong",
  "pieceTitle": "Symphony No. 7 in A major - II. Allegretto",
  "targetComposerName": "Ludwig van Beethoven",
  "pointsEarned": 0
}
```

- `yearHint`: `CORRECT`, `TOO_LOW`, or `TOO_HIGH`
- `eraHint`: `correct`, `close` (one era apart), or `wrong`
- `targetComposerName` and `pieceTitle` are always returned so the end screen can show the answer regardless of win/loss
- `pointsEarned`: points awarded this guess (only non-zero on a correct guess by an authenticated user who hasn't already scored today)

### Points system

Points are awarded on a correct guess, scaled by how many guesses it took:

| Guess number | Points |
|--------------|--------|
| 1            | 10     |
| 2            | 9      |
| 3            | 8      |
| 4            | 7      |
| 5            | 6      |

Formula: `11 - guessNumber`. Points are awarded at most once per user per day (enforced by both a DB unique constraint and a service-level check).

### `/api/auth/register` request / response

**Request:**
```json
{ "username": "mozartfan", "email": "user@example.com", "password": "hunter2" }
```

**Response:**
```json
{ "token": "<jwt>", "username": "mozartfan", "email": "user@example.com", "totalPoints": 0 }
```

Returns 400 if the email is already registered.

### `/api/auth/login` request / response

**Request:**
```json
{ "email": "mozartfan", "password": "hunter2" }
```

The `email` field accepts either an email address or a username.

**Response:** same shape as `/register`. Returns 401 on bad credentials.

### `/api/auth/me` response

```json
{ "token": null, "username": "mozartfan", "email": "user@example.com", "totalPoints": 42 }
```

`token` is null — clients reuse their existing token. Returns 401 if no valid JWT is provided.

---

## Authentication

The backend uses **stateless JWT authentication** via Spring Security.

- Tokens are signed with HMAC-SHA256 using a secret from `jwt.secret` (Base64-encoded, min 32 bytes)
- Token expiry is configured via `jwt.expiration` (milliseconds, default 24 hours)
- In production, set `JWT_SECRET` in `backend/.env` to override the dev default
- The `JwtAuthFilter` runs on every request, parses the `Authorization: Bearer <token>` header, and populates the Spring Security context — all endpoints remain publicly accessible, but authenticated requests get richer behaviour (guess persistence, point tracking)

---

## Development Workflow

In development you run the frontend and backend locally (with hot reload) and only spin up the infrastructure containers.

### docker-compose.override.yml

Docker Compose automatically merges `docker-compose.override.yml` on top of `docker-compose.yml` when you run `docker compose` without `-f` flags. Use this to disable the app containers and swap the named audio volume for a bind mount:

```yaml
# docker-compose.override.yml — automatically applied in dev, not used in prod
services:
  frontend:
    profiles: ["prod"]   # excludes this service from default dev startup

  backend:
    profiles: ["prod"]   # excludes this service from default dev startup

  db:
    ports:
      - "5432:5432"      # expose to localhost so the local backend can reach it

  audio-server:
    ports:
      - "8081:80"
    volumes:
      - ./audio-files:/usr/share/nginx/html/audio:ro   # bind mount instead of named volume
```

Then in dev:
```bash
# start only db + audio-server
docker compose up -d

# drop mp3s into ./audio-files/ — they're immediately served at http://localhost:8081/audio/<filename>
```

In prod (CI or server), use `docker compose --profile prod up -d` to start all four services with the named volume.

### Local backend config

The backend needs to connect to `localhost:5432` instead of `db:5432` (the Docker hostname only resolves inside the Docker network). Use a dev application properties file that Spring Boot picks up automatically:

`backend/src/main/resources/application-dev.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/composerguesser
spring.datasource.username=...
spring.datasource.password=...
```

Run the backend with the `dev` profile active:
```bash
./mvnw
```

The frontend dev server (Vite) runs as normal on `localhost:5173`:
```bash
cd frontend && npm run dev
```

---

## File Layout (target state)

```
ComposerGuesser/
├── frontend/           # React app + Dockerfile
├── backend/            # Spring Boot app + Dockerfile
├── docker-compose.yml
├── nginx/              # nginx config for audio-server
│   └── default.conf
└── db/
    └── changelogs/     # Liquibase migration files
```
