# FitTrack 3.0 — hybrid fitness app starter

**Live:** https://fittrack-novotny.vercel.app (Vercel, auto from `npx vercel deploy --prod`) · Supabase project `fittrack` (ref `cxtlufcyjduqddwwnrzk`, org FitTrack).

Modern React + TypeScript + Vite app, designed as a responsive web app and Capacitor Android shell. Includes a functional local workout log, dashboard charts, Spotify Authorization Code with PKCE starter, Supabase email OTP auth setup, and a row-level-secured workout table schema.

## Requirements
- Node.js 22+
- Android Studio + Android SDK for Android builds
- Supabase project for cloud accounts and sync
- Spotify Developer app for Spotify connection

## Run locally
```bash
npm install
cp .env.example .env
npm run dev   # http://127.0.0.1:5173
```

## Supabase
1. Create a Supabase project.
2. Copy Project URL and anon/publishable key into `.env`.
3. Run `supabase/schema.sql` in Supabase SQL Editor.
4. In Authentication > URL Configuration, configure your local and production redirect URLs.
5. Enable Email OTP/magic link.

Without login, activities live in browser storage. After email login, local activities are uploaded once and all add/delete operations go to `workout_sessions` (RLS: each user sees only their rows; `anon` has no access).

## Spotify
1. Create an app at https://developer.spotify.com/dashboard.
2. Add redirect URIs `https://fittrack-novotny.vercel.app/spotify/callback` and `http://127.0.0.1:5173/spotify/callback` (default is `<origin>/spotify/callback`; Spotify does not accept `localhost`).
3. Set `VITE_SPOTIFY_CLIENT_ID` in `.env` and on Vercel (`npx vercel env add VITE_SPOTIFY_CLIENT_ID production`), then redeploy.
4. Restart Vite and use Hudba > Propojit Spotify.

This starter uses Authorization Code with PKCE and never puts a Spotify client secret in browser code. Access token is held only in session storage for this prototype; production should move token exchange/refresh to a secure backend or implement a hardened PKCE token lifecycle. Spotify API availability and playback scopes depend on Spotify's current developer policies and account/device eligibility. The app does not stream or host Spotify audio.

## Android shell
```bash
npm run android:add
npm run android:sync
npm run android:open
```
Then build/run from Android Studio. Capacitor provides the Android container; a native Health Connect module is a separate next step.

## Samsung Health / Galaxy Watch
See `android-notes/HEALTH_CONNECT.md`. Web code cannot directly access Samsung Health. Health Connect permissions and native Android code are required. Samsung Health Data SDK is another supported route and requires its SDK package and user-granted permissions.

## Current limitations
- Samsung Health is documented but not yet implemented in native Android code.
- Spotify OAuth and playlist/current-track API calls are scaffolded; production token refresh, device playback control, and policy verification remain.
- Stock photos load from Unsplash and require internet access.
- This is a starter project, not a published Play Store app.

## Safety
FitTrack is for fitness tracking, not medical diagnosis. The app keeps a visible reminder not to resume exercise after the reported presyncope/visual symptoms until medically evaluated and cleared.
