import { supabase } from './lib';

export type Session = { id: string; kind: string; title: string; minutes: number; date: string };
type Row = { id: string; kind: string; title: string; minutes: number; performed_at: string };

const toSession = (r: Row): Session => ({ id: r.id, kind: r.kind, title: r.title, minutes: r.minutes, date: r.performed_at });

export async function fetchSessions(): Promise<Session[]> {
  if (!supabase) return [];
  const { data, error } = await supabase
    .from('workout_sessions')
    .select('id,kind,title,minutes,performed_at')
    .order('performed_at', { ascending: false })
    .order('created_at', { ascending: false });
  if (error) throw error;
  return (data as Row[]).map(toSession);
}

export async function insertSessions(userId: string, sessions: Session[]) {
  if (!supabase || sessions.length === 0) return;
  const rows = sessions.map(s => ({ id: s.id, user_id: userId, kind: s.kind, title: s.title, minutes: s.minutes, performed_at: s.date }));
  const { error } = await supabase.from('workout_sessions').upsert(rows, { onConflict: 'id', ignoreDuplicates: true });
  if (error) throw error;
}

export async function deleteSession(id: string) {
  if (!supabase) return;
  const { error } = await supabase.from('workout_sessions').delete().eq('id', id);
  if (error) throw error;
}
