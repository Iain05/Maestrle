import React, { useEffect, useState } from 'react';
import confetti from 'canvas-confetti';
import { Copy, Check } from 'lucide-react';
import { buildShareText, copyToClipboard, type ShareData } from '@src/utils/shareScore';
import AuthModal from './AuthModal';

interface GameStatusProps {
  won: boolean;
  composerName: string;
  pieceTitle: string;
  onClose: () => void;
  shareData: ShareData;
  isLoggedIn: boolean;
}

const GameStatus: React.FC<GameStatusProps> = ({ won, composerName, pieceTitle, onClose, shareData, isLoggedIn }) => {
  const [closing, setClosing] = useState(false);
  const [copied, setCopied] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register' | null>(null);

  function handleClose() {
    setClosing(true);
    setTimeout(onClose, 200);
  }

  async function handleShare() {
    try {
      await copyToClipboard(buildShareText(shareData));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (e) {
      console.error('Share failed:', e);
    }
  }

  useEffect(() => {
    if (!won) return;
    confetti({
      particleCount: 120,
      spread: 70,
      origin: { y: 0.6 },
      colors: ['#6366f1', '#22c55e', '#eab308', '#f472b6', '#38bdf8'],
    });
  }, []);

  return (
    <>
    <div
      className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
      onMouseDown={(e) => { if (e.target === e.currentTarget) handleClose(); }}
    >
      <div className={`modal-card bg-surface rounded-2xl p-8 w-full max-w-sm shadow-2xl text-center ${closing ? 'closing' : ''}`}>
        {won ? (
          <>
            <div className="text-5xl mb-4">🎼</div>
            <h3 className="serif text-3xl mb-3 text-green-600">Bravo Maestro!</h3>
            <p className="text-ink-muted">
              You correctly identified <span className="font-semibold text-ink">{composerName}</span>'s{' '}
              <span className="italic">{pieceTitle}</span>.
            </p>
          </>
        ) : (
          <>
            <div className="text-5xl mb-4">🎻</div>
            <h3 className="serif text-3xl mb-3 text-red-500">Encore Needed...</h3>
            <p className="text-ink-muted">
              It was actually <span className="font-semibold text-ink">{composerName}</span>'s{' '}
              <span className="italic">{pieceTitle}</span>.
            </p>
          </>
        )}

        <button
          onClick={handleShare}
          className="mt-6 flex items-center justify-center gap-2 w-full px-4 py-2.5 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
        >
          {copied ? <Check className="w-4 h-4 text-green-500" /> : <Copy className="w-4 h-4" />}
          {copied ? 'Copied!' : 'Share your score'}
        </button>

        {!isLoggedIn && (
          <div className="mt-4 p-4 bg-primary/10 border border-primary/20 rounded-xl text-center">
            <p className="text-sm text-ink-muted mb-3">
              Sign in to save your score, track your streak, and appear on the leaderboard.
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setAuthMode('register')}
                className="flex-1 py-2 bg-primary text-primary-text text-sm font-semibold rounded-xl hover:bg-primary-hover transition-colors"
              >
                Create account
              </button>
              <button
                onClick={() => setAuthMode('login')}
                className="flex-1 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl hover:border-border-hover transition-colors"
              >
                Sign in
              </button>
            </div>
          </div>
        )}

        {/* This button is commented out because i want to add it back but as a 'view daily leaderboard' button or something */}
        {/* <button */}
        {/*   onClick={onPlayAgain} */}
        {/*   className="px-8 py-2.5 bg-slate-900 text-white font-semibold rounded-xl hover:bg-slate-800 transition-colors" */}
        {/* > */}
        {/*   Play Again */}
        {/* </button> */}
      </div>
    </div>

    {authMode && <AuthModal initialMode={authMode} onClose={() => setAuthMode(null)} />}
    </>
  );
};

export default GameStatus;
