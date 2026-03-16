import React, { useState, useEffect } from 'react';
import AudioPlayer from '@src/components/AudioPlayer';
import GuessControls from '@src/components/GuessControls';
import GuessGrid from '@src/components/GuessGrid';
import GameStatus from '@src/components/GameStatus';
import AuthModal from '@src/components/AuthModal';
import { useGameState } from '@src/hooks/useGameState';
import { getDailyChallenge } from '@src/api/excerpt';
import { getComposers, type ComposerSummary } from '@src/api/composer';
import { useAuth } from '@src/context/AuthContext';
import { useToast } from '@src/context/ToastContext';

const DailyComposer: React.FC = () => {
  const { user, token, logout, addPoints } = useAuth();
  const { showToast } = useToast();
  const [authModal, setAuthModal] = useState<'login' | 'register' | null>(null);
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

  return (
    <>
      <div className="fixed top-4 right-4 flex items-center gap-3">
        {user ? (
          <>
            <span className="text-slate-800 font-semibold text-lg">{user.username}</span>
            <span className="text-indigo-600 font-bold text-lg">{user.totalPoints} pts</span>
            <button
              onClick={logout}
              className="px-3 py-1.5 border-2 border-slate-300 text-slate-600 text-sm font-medium rounded-lg hover:border-slate-400 transition-colors"
            >
              Sign out
            </button>
          </>
        ) : (
          <>
            <button
              onClick={() => setAuthModal('login')}
              className="px-4 py-1.5 border-2 border-slate-300 text-slate-700 font-medium rounded-lg hover:border-slate-400 transition-colors"
            >
              Sign in
            </button>
            <button
              onClick={() => setAuthModal('register')}
              className="px-4 py-1.5 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition-colors"
            >
              Create account
            </button>
          </>
        )}
      </div>

      {authModal && (
        <AuthModal initialMode={authModal} onClose={() => setAuthModal(null)} />
      )}

      <header className="text-center mb-8 max-w-2xl w-full">
        <h1 className="serif text-4xl font-bold mb-2 text-slate-900">Daily Composer</h1>
        <p className="text-slate-600 italic">Identify who composed this musical excerpt!</p>
      </header>

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
    </>
  );
};

export default DailyComposer;
