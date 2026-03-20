import type { GuessResult } from '@src/api/guess';
import { MAX_GUESSES } from '@src/data/gameData';

export interface ShareData {
  guesses: GuessResult[];
  won: boolean;
  challengeNumber: number | null;
  challengeDate: string | null;
  streak: number;
}

function hintToEmoji(hint: string): string {
  if (hint === 'correct' || hint === 'CORRECT') return '🟩';
  if (hint === 'close') return '🟨';
  return '⬜';
}

function formatDate(iso: string): string {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

export async function copyToClipboard(text: string): Promise<void> {
  // Modern clipboard API
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }
  // Fallback for browsers/contexts where clipboard API isn't available
  const el = document.createElement('textarea');
  el.value = text;
  el.style.cssText = 'position:fixed;top:0;left:0;opacity:0;pointer-events:none';
  document.body.appendChild(el);
  el.focus();
  el.select();
  const ok = document.execCommand('copy');
  document.body.removeChild(el);
  if (!ok) throw new Error('Copy failed');
}

export function buildShareText(data: ShareData): string {
  const { guesses, won, challengeNumber, challengeDate, streak } = data;

  const points = guesses.find((g) => g.pointsEarned > 0)?.pointsEarned ?? 0;
  const guessStr = won ? `${guesses.length}/${MAX_GUESSES}` : `X/${MAX_GUESSES}`;
  const dateStr = challengeDate ? formatDate(challengeDate) : null;

  const header =
    challengeNumber != null && dateStr != null
      ? `ComposerGuesser #${challengeNumber} — ${dateStr}`
      : 'ComposerGuesser';

  const scoreParts = won ? [`🎼 ${points} pts`, `(${guessStr})`] : [`🎻 ${guessStr}`];
  if (streak > 0) scoreParts.push(`🔥 ${streak}`);
  const scoreLine = scoreParts.join(' ');

  const grid = guesses.map((g) =>
    [
      hintToEmoji(g.composerHint),
      hintToEmoji(g.yearHint),
      hintToEmoji(g.eraHint),
      hintToEmoji(g.nationalityHint),
    ].join(''),
  ).join('\n');

  return `${header}\n${scoreLine}\n${grid}\n\nhttps://maestrle.com`;
}
