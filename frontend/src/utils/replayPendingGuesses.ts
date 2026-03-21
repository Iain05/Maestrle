const PENDING_KEY = 'cg_pending_guesses';

interface PendingGuess {
  date: string;
  excerptId: number;
  composerId: number;
}

export function storePendingGuess(excerptId: number, composerId: number): void {
  const today = new Date().toISOString().slice(0, 10);
  const existing = loadPendingGuesses();
  existing.push({ date: today, excerptId, composerId });
  localStorage.setItem(PENDING_KEY, JSON.stringify(existing));
}

export function clearPendingGuesses(): void {
  localStorage.removeItem(PENDING_KEY);
}

function loadPendingGuesses(): PendingGuess[] {
  try {
    const raw = localStorage.getItem(PENDING_KEY);
    return raw ? (JSON.parse(raw) as PendingGuess[]) : [];
  } catch {
    return [];
  }
}

export async function replayPendingGuesses(token: string): Promise<void> {
  const today = new Date().toISOString().slice(0, 10);
  const pending = loadPendingGuesses().filter((g) => g.date === today);

  clearPendingGuesses();

  if (pending.length === 0) return;

  // If the account already has guesses today, server takes precedence — don't replay.
  const historyRes = await fetch('/api/guess', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!historyRes.ok) return;
  const history: unknown[] = await historyRes.json();
  if (history.length > 0) return;

  for (const guess of pending) {
    const res = await fetch('/api/guess', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ excerptId: guess.excerptId, composerId: guess.composerId }),
    });
    if (!res.ok) break;
  }
}
