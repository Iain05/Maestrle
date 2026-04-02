export interface ArchiveStatus {
  guessCount: number;
  correct: boolean;
}

export async function getArchiveStatuses(token: string | null): Promise<Record<string, ArchiveStatus>> {
  if (!token) return {};
  const res = await fetch('/api/guess/archive/statuses', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) return {};
  return res.json();
}

export interface GuessResult {
  correct: boolean;
  composerName: string;
  birthYear: number;
  era: string;
  nationality: string;
  composerHint: 'CORRECT' | 'WRONG';
  yearHint: 'CORRECT' | 'TOO_LOW' | 'TOO_HIGH' | 'CLOSE_LOW' | 'CLOSE_HIGH';
  eraHint: 'CORRECT' | 'CLOSE' | 'WRONG';
  nationalityHint: 'CORRECT' | 'WRONG';
  pieceTitle: string;
  targetComposerName: string;
  compositionYear: number | null;
  pieceDescription: string | null;
  pointsEarned: number;
  newStreak: number;
}

export async function getMyGuesses(token: string, date?: string): Promise<GuessResult[]> {
  const url = date ? `/api/guess?date=${date}` : '/api/guess';
  const res = await fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('Failed to fetch guess history');
  return res.json();
}

export async function submitArchiveGuess(excerptId: number, composerId: number, date: string, token: string | null): Promise<GuessResult> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch('/api/guess/archive', {
    method: 'POST',
    headers,
    body: JSON.stringify({ excerptId, composerId, date }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? 'Failed to submit guess');
  }
  return res.json();
}

export async function submitGuess(excerptId: number, composerId: number, token: string | null): Promise<GuessResult> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch('/api/guess', {
    method: 'POST',
    headers,
    body: JSON.stringify({ excerptId, composerId }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? 'Failed to submit guess');
  }
  return res.json();
}
