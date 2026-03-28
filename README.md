# ComposerGuesser

A daily classical music guessing game (Wordle-style). Listen to an audio excerpt and identify the composer using hint-based feedback.

## Quick start

### 1. Start infrastructure

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

This starts PostgreSQL (port 5432) and the audio file server (port 8081), with their ports exposed to the host so the locally-running backend and Vite dev server can reach them.

### 2. Start the backend

```bash
cd backend
./mvnw
```

Liquibase runs automatically on startup and applies any pending migrations.

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The app is available at http://localhost:5173. The Vite dev server proxies `/api` → `http://localhost:8080` and `/audio` → `http://localhost:8081`.

---

## Populating the database

Composer and work data comes from the [Open Opus API](https://openopus.org). Nationality data is fetched from the [MusicBrainz API](https://musicbrainz.org).

### 1. Download the data dump

```bash
curl -o dump.json https://api.openopus.org/work/dump.json
```

### 2. Install script dependencies

```bash
pip install psycopg2-binary requests
```

### 3. Run the import script

The database must be running and the backend must have started at least once (so Liquibase has applied all migrations).

```bash
python3 scripts/import_composers.py
```

The script will:
- Clear the `tbl_composer` and `tbl_composer_work` tables
- Import all 165 composers whose era maps to our enum (skipping Medieval, Renaissance, Post-War, and 21st Century)
- Look up each composer's nationality on MusicBrainz (takes ~3 minutes due to rate limiting)
- Insert all ~25,000 works

Composers where MusicBrainz returns no country are imported with nationality `XX`. A summary is printed at the end.

---

## Production

```bash
docker compose --profile prod up -d
```

Starts all four services: frontend (nginx, port 80), backend, db, and audio-server. Internal services communicate over the Docker network only — `db`, `audio-server`, and `backend` are not exposed to the host.

Or run with the observability profile as well, to access logs, traces, and metrics through grafana on port 3000.

```bash
docker compose --profile prod --profile observability up -d
```
