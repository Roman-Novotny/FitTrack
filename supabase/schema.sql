-- Run this in Supabase SQL Editor. RLS ensures users can only access their own records.
create table if not exists public.workout_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in ('strength','cardio','mobility')),
  title text not null,
  minutes integer not null check (minutes between 1 and 1440),
  performed_at date not null default current_date,
  created_at timestamptz not null default now()
);
alter table public.workout_sessions enable row level security;
drop policy if exists "read own sessions" on public.workout_sessions;
create policy "read own sessions" on public.workout_sessions for select using (auth.uid() = user_id);
drop policy if exists "insert own sessions" on public.workout_sessions;
create policy "insert own sessions" on public.workout_sessions for insert with check (auth.uid() = user_id);
drop policy if exists "update own sessions" on public.workout_sessions;
create policy "update own sessions" on public.workout_sessions for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists "delete own sessions" on public.workout_sessions;
create policy "delete own sessions" on public.workout_sessions for delete using (auth.uid() = user_id);
create index if not exists workout_sessions_user_date_idx on public.workout_sessions (user_id, performed_at desc);
grant select, insert, update, delete on public.workout_sessions to authenticated;
revoke all on public.workout_sessions from anon;
