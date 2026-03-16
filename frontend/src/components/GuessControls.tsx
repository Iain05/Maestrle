import React, { useState } from 'react';
import ComposerSearch from './ComposerSearch';
import type { ComposerSummary } from '@src/api/composer';

interface GuessControlsProps {
  disabled: boolean;
  composers: ComposerSummary[];
  onSubmit: (composerId: number) => Promise<string | null>;
  onError: (message: string) => void;
}

const GuessControls: React.FC<GuessControlsProps> = ({ disabled, composers, onSubmit, onError }) => {
  const [composerInput, setComposerInput] = useState('');
  const [selectedComposer, setSelectedComposer] = useState<ComposerSummary | null>(null);
  const [shake, setShake] = useState(false);

  function handleInput(value: string) {
    setComposerInput(value);
    setSelectedComposer(null);
  }

  async function handleSubmit() {
    if (disabled) return;
    if (!selectedComposer) {
      setShake(true);
      setTimeout(() => setShake(false), 500);
      return;
    }
    const error = await onSubmit(selectedComposer.composerId);
    if (!error) {
      setComposerInput('');
      setSelectedComposer(null);
    } else {
      setShake(true);
      setTimeout(() => setShake(false), 500);
      onError(error);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <ComposerSearch
        value={composerInput}
        onChange={handleInput}
        onSelect={setSelectedComposer}
        disabled={disabled}
        composers={composers}
      />
      <button
        onClick={handleSubmit}
        disabled={disabled}
        className={`w-full py-4 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl transition-all shadow-md active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed ${shake ? 'bg-red-500 hover:bg-red-500' : ''}`}
      >
        Submit Guess
      </button>
    </div>
  );
};

export default GuessControls;
