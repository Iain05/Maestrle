import React, { useEffect } from 'react';
import confetti from 'canvas-confetti';

interface GameStatusProps {
  won: boolean;
  composerName: string;
  pieceTitle: string;
  onClose: () => void;
}

const GameStatus: React.FC<GameStatusProps> = ({ won, composerName, pieceTitle, onClose }) => {
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
    <div
      className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="modal-card bg-white rounded-2xl p-8 w-full max-w-sm shadow-2xl text-center">
        {won ? (
          <>
            <div className="text-5xl mb-4">🎼</div>
            <h3 className="serif text-3xl font-bold mb-3 text-green-600">Bravo Maestro!</h3>
            <p className="text-slate-600">
              You correctly identified <span className="font-semibold text-slate-800">{composerName}</span>'s{' '}
              <span className="italic">{pieceTitle}</span>.
            </p>
          </>
        ) : (
          <>
            <div className="text-5xl mb-4">🎻</div>
            <h3 className="serif text-3xl font-bold mb-3 text-red-500">Encore Needed...</h3>
            <p className="text-slate-600">
              It was actually <span className="font-semibold text-slate-800">{composerName}</span>'s{' '}
              <span className="italic">{pieceTitle}</span>.
            </p>
          </>
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
  );
};

export default GameStatus;
