export interface GuessResult {
  correct: boolean;
  composerName: string;
  birthYear: number;
  era: string;
  nationality: string;
  composerHint: 'correct' | 'wrong';
  yearHint: 'CORRECT' | 'TOO_LOW' | 'TOO_HIGH';
  eraHint: 'correct' | 'close' | 'wrong';
  nationalityHint: 'correct' | 'wrong';
  pieceTitle: string;
  targetComposerName: string;
  pointsEarned: number;
}

export async function getMyGuesses(token: string): Promise<GuessResult[]> {
  const res = await fetch('/api/guess', {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('Failed to fetch guess history');
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
