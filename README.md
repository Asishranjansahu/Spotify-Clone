# Spotify Clone

Spring Boot 3 REST API (`backend/`) + React/Vite frontend (`frontend/`), implementing the SRS.

## Quick start (no PostgreSQL needed)

```bash
# terminal 1 – API on :8081 (H2 file DB, seeded demo data)
cd backend && SERVER_PORT=8081 CORS_ORIGIN=http://localhost:5180 SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# terminal 2 – UI on :5180 (proxies /api to :8081; ports set in frontend/vite.config.js)
cd frontend && npm install && npm run dev
```

Open http://localhost:5180. Demo admin: `admin@example.com` / `admin12345` (dev profile only).
Register a new account for a regular user. The Admin page (admin only) uploads songs/albums/artists.

## Production configuration (PostgreSQL)

Environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (32+ chars, required),
`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `CORS_ORIGIN`, `STORAGE_DIR`, `SEED_DEMO`, `YOUTUBE_API_KEY` (optional, see below).
Docker: `JWT_SECRET=... ADMIN_PASSWORD=... docker compose up --build`.
Set `VITE_API_URL` when building the frontend for a separate API origin.

## YouTube source

A second music source next to local files. Search uses the YouTube Data API v3 (server side);
playback uses **only the official YouTube IFrame Player**. No YouTube audio is downloaded, extracted or proxied.

- Set the key as an environment variable, never in code or the frontend:
  `export YOUTUBE_API_KEY=<your key>` before starting the API (docker compose passes it through as a variable).
  Without it, YouTube search returns a clear `503` JSON error; local music is unaffected.
- Endpoints (JWT required): `GET /api/youtube/search?q=` and `POST /api/youtube/songs` `{ "videoId": "..." }`.
- Search results are cached in memory for a few minutes (`youtube-cache-seconds`, default 300) to save quota.
  A search costs ~100 quota units (default 10,000/day). Quota exhaustion returns `429` JSON.
- Only video ID, title, thumbnail and duration are stored, and only once a user plays/likes/adds a video to a playlist.
- The YouTube player is shown in a small corner video while a YouTube song is current; it must stay visible.
- **Compliance notes:** ads may appear in the YouTube player; background playback on mobile may pause
  (the embed is controlled by the browser/YouTube); embedding-disabled or removed videos are skipped.
  Review the [YouTube API Services Terms of Service](https://developers.google.com/youtube/terms/api-services-terms-of-service)
  and Developer Policies before using this beyond personal use. The UI shows a "Powered by YouTube" credit.

## Tests

`cd backend && mvn test`

## Notes

- Streaming: `GET /api/songs/{id}/stream` supports HTTP Range (206). The `<audio>` element authenticates with `?token=` on this endpoint only.
- Play count increments after 30 s of listening; history is recorded on play.
- Shortcuts: Space play/pause, ←/→ seek, ↑/↓ volume.
