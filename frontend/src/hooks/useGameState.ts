import { useState, useCallback, useEffect, useRef } from 'react';
import { MAX_GUESSES } from '@src/data/gameData';
import { submitGuess as submitGuessApi, getMyGuesses, type GuessResult } from '@src/api/guess';

const HISTORY_STAGGER_MS = 500;

export function useGameState(excerptId: number | null, token: string | null, onPointsEarned: (points: number) => void) {
  const [guesses, setGuesses] = useState<GuessResult[]>([]);
  const [gameKey, setGameKey] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [justFinished, setJustFinished] = useState(false);
  const guessedIdsRef = useRef<Set<number>>(new Set());
  const timeoutsRef = useRef<ReturnType<typeof setTimeout>[]>([]);

  const won = guesses.some((g) => g.correct);
  const isGameOver = won || guesses.length >= MAX_GUESSES;

  useEffect(() => {
    if (!excerptId || !token) return;

    setIsLoading(true);

    getMyGuesses(token)
      .then((history) => {
        if (history.length === 0) {
          setIsLoading(false);
          return;
        }
        history.forEach((guess, i) => {
          const t = setTimeout(() => {
            setGuesses((prev) => [...prev, guess]);
            if (i === history.length - 1) setIsLoading(false);
          }, i * HISTORY_STAGGER_MS);
          timeoutsRef.current.push(t);
        });
      })
      .catch(() => setIsLoading(false));

    return () => {
      timeoutsRef.current.forEach(clearTimeout);
      timeoutsRef.current = [];
    };
  }, [excerptId, token]);

  // Returns null on success, or an error message string on failure.
  const submitGuess = useCallback(async (composerId: number): Promise<string | null> => {
    if (!excerptId || isGameOver) return null;

    if (guessedIdsRef.current.has(composerId)) {
      return "You've already guessed that composer";
    }

    try {
      const result = await submitGuessApi(excerptId, composerId, token);
      guessedIdsRef.current.add(composerId);
      setGuesses((prev) => {
        const next = [...prev, result];
        const gameOver = next.some((g) => g.correct) || next.length >= MAX_GUESSES;
        if (gameOver) setJustFinished(true);
        return next;
      });
      if (result.pointsEarned > 0) onPointsEarned(result.pointsEarned);
      return null;
    } catch (err) {
      return err instanceof Error ? err.message : 'Something went wrong';
    }
  }, [excerptId, isGameOver, token, onPointsEarned]);

  const resetGame = useCallback(() => {
    timeoutsRef.current.forEach(clearTimeout);
    timeoutsRef.current = [];
    guessedIdsRef.current = new Set();
    setGuesses([]);
    setJustFinished(false);
    setGameKey((k) => k + 1);
  }, []);

  const lastGuess = guesses[guesses.length - 1];

  return { guesses, isGameOver, gameKey, won, lastGuess, submitGuess, resetGame, isLoading, justFinished };
}
