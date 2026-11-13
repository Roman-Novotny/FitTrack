import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL as string | undefined;
const anon = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined;
export const supabase = url && anon ? createClient(url, anon) : null;

const clientId = import.meta.env.VITE_SPOTIFY_CLIENT_ID as string | undefined;
// Default: <current origin>/spotify/callback, so the same build works locally (http://127.0.0.1:5173) and in production.
const redirectUri = import.meta.env.VITE_SPOTIFY_REDIRECT_URI || `${location.origin}/spotify/callback`;
const scopes = ['user-read-private','user-read-email','playlist-read-private','playlist-read-collaborative','user-read-currently-playing','user-read-playback-state','user-modify-playback-state','user-read-recently-played'];
const b64url = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes)).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
const randomString = (n:number) => { const a=new Uint8Array(n); crypto.getRandomValues(a); return b64url(a); };
export async function spotifyConnect(){
  if(!clientId) throw new Error('Spotify zatím není nastavené (chybí VITE_SPOTIFY_CLIENT_ID).');
  const verifier=randomString(64); const digest=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(verifier));
  const challenge=b64url(new Uint8Array(digest)); const state=randomString(24);
  sessionStorage.setItem('spotify_verifier',verifier); sessionStorage.setItem('spotify_state',state);
  const url=new URL('https://accounts.spotify.com/authorize');
  url.searchParams.set('client_id',clientId); url.searchParams.set('response_type','code'); url.searchParams.set('redirect_uri',redirectUri); url.searchParams.set('code_challenge_method','S256'); url.searchParams.set('code_challenge',challenge); url.searchParams.set('state',state); url.searchParams.set('scope',scopes.join(' '));
  location.assign(url.toString());
}
export async function spotifyCallback(){
  const u=new URL(location.href); const code=u.searchParams.get('code'); const state=u.searchParams.get('state');
  if(!code) return false;
  const expected=sessionStorage.getItem('spotify_state'); const verifier=sessionStorage.getItem('spotify_verifier');
  if(!state || state!==expected || !verifier || !clientId || !redirectUri) throw new Error('Spotify přihlášení se nepodařilo ověřit. Zkus propojení znovu.');
  const body=new URLSearchParams({client_id:clientId,grant_type:'authorization_code',code,redirect_uri:redirectUri,code_verifier:verifier});
  const r=await fetch('https://accounts.spotify.com/api/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body});
  const data=await r.json(); if(!r.ok) throw new Error(data.error_description||'Spotify token se nepodařilo získat.');
  sessionStorage.removeItem('spotify_state');sessionStorage.removeItem('spotify_verifier');
  // Prototype only: keep short-lived access token in session memory, never persist a client secret.
  sessionStorage.setItem('spotify_access_token',data.access_token); sessionStorage.setItem('spotify_expires_at',String(Date.now()+data.expires_in*1000));
  history.replaceState({},'','/'); return true;
}
export async function spotifyApi(path:string, method='GET'){
  let token=sessionStorage.getItem('spotify_access_token'); const exp=Number(sessionStorage.getItem('spotify_expires_at')||0);
  if(!token || Date.now()>exp) throw new Error('Spotify relace vypršela. Propoj Spotify znovu.');
  const r=await fetch(`https://api.spotify.com/v1/${path}`,{method,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'}});
  const data=await r.json(); if(!r.ok) throw new Error(data.error?.message||'Spotify API vrátilo chybu.'); return data;
}
export const spotifyConnected=()=>!!sessionStorage.getItem('spotify_access_token') && Date.now()<Number(sessionStorage.getItem('spotify_expires_at')||0);
