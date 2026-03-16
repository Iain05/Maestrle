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

  const { guesses, isGameOver, gameKey, won, lastGuess, submitGuess, isLoading, justFinished } =
    useGameState(excerptId, token, addPoints);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    if (justFinished) setShowModal(true);
  }, [justFinished]);

  const leaderboardLink = (
    <Link
      to="/leaderboard"
      className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-200 text-slate-700 text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-slate-300 transition-all"
    >
      🏆<span className="hidden sm:inline">Leaderboard</span>
    </Link>
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

        {showModal && lastGuess && (
          <GameStatus
            won={won}
            composerName={lastGuess.targetComposerName}
            pieceTitle={lastGuess.pieceTitle}
            onClose={() => setShowModal(false)}
          />
        )}
      </main>
    </PageLayout>
  );
};

export default DailyComposer;
