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

## Android app
Native project lives in `android/` (Capacitor 7, compileSdk 36, minSdk 26). Requires **JDK 21**.
```bash
npm run android:sync          # web build + cap sync
cd android && ./gradlew assembleDebug   # -> app/build/outputs/apk/debug/app-debug.apk
```
CI (`.github/workflows/android.yml`) builds the debug APK on every push to `main` and uploads it as the run artifact **FitTrack-debug-apk**.

- **Login in the app:** the e-mail link redirects to `cz.fittrack.app://login#access_token=…` (allowed in Supabase Redirect URLs); `@capacitor/app` `appUrlOpen` hands the tokens to `supabase.auth.setSession`.
- **Samsung Health / Galaxy Watch:** `android/app/src/main/java/cz/fittrack/app/HealthPlugin.kt` (Health Connect, read-only: steps, heart rate, sleep, exercise). JS wrapper `src/health.ts`. In Samsung Health enable *Settings → Health Connect* sharing, then in FitTrack *Propojení → Samsung Health → Propojit*. Watch workouts can be imported into the diary (deduplicated by Health Connect record id).
- **Spotify** is web-only (PKCE redirect cannot return into the app).

## Keep-alive
`.github/workflows/keep-alive.yml` calls `rpc/keep_alive` daily so the free Supabase project is not paused.

## Current limitations
- Health Connect integration is not yet tested on a physical device.
- Spotify OAuth and playlist/current-track API calls are scaffolded; production token refresh, device playback control, and policy verification remain.
- Stock photos load from Unsplash and require internet access.
- Debug APK only (no release signing / Play Store). Publishing to Play requires a Health Connect data declaration.

## Safety
FitTrack is for fitness tracking, not medical diagnosis. The app keeps a visible reminder not to resume exercise after the reported presyncope/visual symptoms until medically evaluated and cleared.
