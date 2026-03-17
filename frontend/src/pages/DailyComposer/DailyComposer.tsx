import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import AudioPlayer from '@src/components/AudioPlayer';
import GuessControls from '@src/components/GuessControls';
import GuessGrid from '@src/components/GuessGrid';
import GameStatus from '@src/components/GameStatus';
import PageLayout from '@src/components/PageLayout';
import { useGameState } from '@src/hooks/useGameState';
import { getDailyChallenge } from '@src/api/excerpt';
import { getComposers, type ComposerSummary } from '@src/api/composer';
import { useAuth } from '@src/context/AuthContext';
import { useToast } from '@src/context/ToastContext';

const DailyComposer: React.FC = () => {
  const { token, addPoints } = useAuth();
  const { showToast } = useToast();
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [excerptId, setExcerptId] = useState<number | null>(null);
  const [composers, setComposers] = useState<ComposerSummary[]>([]);

  useEffect(() => {
    getDailyChallenge()
      .then((data) => {
        setAudioUrl(data.audioUrl);
        setExcerptId(data.excerptId);
      })
      .catch(console.error);

    getComposers().then(setComposers).catch(console.error);
  }, []);

  const { guesses, isGameOver, gameKey, won, lastGuess, submitGuess, isLoading, justFinished, clearJustFinished } =
    useGameState(excerptId, token, addPoints);

  const leaderboardLink = (
    <div className="flex items-center gap-2">
      <Link
        to="/leaderboard"
        className="flex items-center gap-2 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
      >
        🏆<span className="hidden sm:inline">Leaderboard</span>
      </Link>
      <Link
        to="/submit"
        className="hidden sm:flex items-center gap-2 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
      >
        🎵 Submit excerpt
      </Link>
    </div>
  );

  return (
    <PageLayout
      leftSlot={leaderboardLink}
      title="Daily Composer"
      subtitle="Identify who composed this musical excerpt!"
    >
      <main className="max-w-xl w-full flex flex-col gap-6">
        <AudioPlayer key={gameKey} audioUrl={audioUrl} />

        <GuessControls
          disabled={isGameOver || isLoading}
          composers={composers}
          onSubmit={submitGuess}
          onError={showToast}
        />

        <GuessGrid guesses={guesses} />

        {justFinished && lastGuess && (
          <GameStatus
            won={won}
            composerName={lastGuess.targetComposerName}
            pieceTitle={lastGuess.pieceTitle}
            onClose={clearJustFinished}
          />
        )}
      </main>
    </PageLayout>
  );
};

export default DailyComposer;
