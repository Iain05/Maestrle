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
import { ShieldCheck, Music } from 'lucide-react';
import type { ShareData } from '@src/utils/shareScore';

const DailyComposer: React.FC = () => {
  const { token, addPoints, isAdmin, user } = useAuth();
  const { showToast } = useToast();
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [excerptId, setExcerptId] = useState<number | null>(null);
  const [composers, setComposers] = useState<ComposerSummary[]>([]);
  const [challengeNumber, setChallengeNumber] = useState<number | null>(null);
  const [challengeDate, setChallengeDate] = useState<string | null>(null);
  const [ownSubmission, setOwnSubmission] = useState(false);
  const [uploaderUsername, setUploaderUsername] = useState<string | null>(null);

  useEffect(() => {
    getDailyChallenge(token)
      .then((data) => {
        setAudioUrl(data.audioUrl);
        setExcerptId(data.excerptId);
        setChallengeNumber(data.challengeNumber);
        setChallengeDate(data.date);
        setOwnSubmission(data.submittedByCurrentUser);
        setUploaderUsername(data.uploaderUsername);
      })
      .catch(console.error);
  }, [token]);

  useEffect(() => {
    getComposers().then(setComposers).catch(console.error);
  }, []);

  const { guesses, isGameOver, gameKey, won, lastGuess, submitGuess, isLoading, justFinished, clearJustFinished } =
    useGameState(excerptId, token, addPoints);

  const shareData: ShareData = {
    guesses,
    won,
    challengeNumber,
    challengeDate,
    streak: user?.streak ?? 0,
  };

  const leaderboardLink = (
    <div className="flex items-center gap-2">
      <Link
        to="/leaderboard"
        className="flex items-center gap-2 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
      >
        🏆<span className="hidden sm:inline">Leaderboard</span>
      </Link>
      <Link
        to="/challenges"
        className="flex items-center gap-2 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
      >
        📅<span className="hidden sm:inline">Past challenges</span>
      </Link>
      <Link
        to="/submit"
        className="hidden sm:flex items-center gap-2 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
      >
        🎵 Submit excerpt
      </Link>
      {isAdmin && (
        <Link
          to="/admin"
          className="flex items-center gap-2 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
          title="Admin review"
        >
          <ShieldCheck className="w-4 h-4 text-primary" />
          <span className="hidden sm:inline">Admin review</span>
        </Link>
      )}
    </div>
  );

  return (
    <PageLayout
      leftSlot={leaderboardLink}
      title="Daily Composer"
      subtitle="Identify who composed this musical excerpt!"
    >
      <main className="max-w-xl w-full flex flex-col gap-6">
        <AudioPlayer
          key={gameKey}
          audioUrl={audioUrl}
          notice={uploaderUsername
            ? ownSubmission
              ? `Submitted by you — correct guesses won't earn points.`
              : `Submitted by ${uploaderUsername}`
            : undefined}
        />

        {(!isGameOver || !lastGuess) &&
          <GuessControls
            disabled={isGameOver || isLoading}
            composers={composers}
            onSubmit={submitGuess}
            onError={showToast}
          />
        }

        {isGameOver && lastGuess && (
          <div className="bg-surface-warm border border-border rounded-2xl px-8 pt-6 pb-6 shadow-sm">
            <div className="flex items-center gap-2 text-primary mb-3">
              <Music className="w-4 h-4" />
              <span className="text-sm font-bold uppercase tracking-wider">The Piece</span>
            </div>
            <p className="serif text-xl text-ink leading-snug">
              {lastGuess.pieceTitle}
            </p>
            <p className="text-ink-muted text-sm mt-1">
              {lastGuess.targetComposerName}
              {lastGuess.compositionYear != null && (
                <> · {lastGuess.compositionYear}</>
              )}
            </p>
            {lastGuess.pieceDescription && (
              <>
                <div className="border-t border-border mt-4 mb-3" />
                <p className="text-ink-muted text-sm leading-relaxed">{lastGuess.pieceDescription}</p>
              </>
            )}
          </div>
        )}

        <GuessGrid guesses={guesses} isGameOver={isGameOver} shareData={shareData} />

        {justFinished && lastGuess && (
          <GameStatus
            won={won}
            composerName={lastGuess.targetComposerName}
            pieceTitle={lastGuess.pieceTitle}
            onClose={clearJustFinished}
            shareData={shareData}
            isLoggedIn={!!user}
          />
        )}
      </main>
    </PageLayout>
  );
};

export default DailyComposer;
