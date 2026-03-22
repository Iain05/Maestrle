import React, { useState, useRef, useEffect } from 'react';
import { Music, Play, Pause } from 'lucide-react';

interface AudioPlayerProps {
  audioUrl: string | null;
  notice?: string;
}

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

const AudioPlayer: React.FC<AudioPlayerProps> = ({ audioUrl, notice }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [isInPlaySession, setIsInPlaySession] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const progressBarRef = useRef<HTMLDivElement | null>(null);
  const rafRef = useRef<number | null>(null);

  function startRaf() {
    function tick() {
      if (audioRef.current) {
        setCurrentTime(audioRef.current.currentTime);
      }
      rafRef.current = requestAnimationFrame(tick);
    }
    rafRef.current = requestAnimationFrame(tick);
  }

  function stopRaf() {
    if (rafRef.current !== null) {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
  }

  useEffect(() => stopRaf, []);

  function seek(clientX: number) {
    const audio = audioRef.current;
    const bar = progressBarRef.current;
    if (!audio || !bar || duration === 0) return;
    const rect = bar.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    audio.currentTime = ratio * duration;
    setCurrentTime(ratio * duration);
  }

  function handlePointerDown(e: React.PointerEvent<HTMLDivElement>) {
    e.currentTarget.setPointerCapture(e.pointerId);
    seek(e.clientX);
  }

  function handlePointerMove(e: React.PointerEvent<HTMLDivElement>) {
    if (e.buttons === 0) return;
    seek(e.clientX);
  }

  function handlePlayPause() {
    const audio = audioRef.current;
    if (!audio) return;

    if (isPlaying) {
      audio.pause();
      stopRaf();
      setIsPlaying(false);
    } else if (isInPlaySession) {
      audio.play();
      startRaf();
      setIsPlaying(true);
    } else {
      audio.currentTime = 0;
      setCurrentTime(0);
      setIsInPlaySession(true);
      audio.play();
      startRaf();
      setIsPlaying(true);
    }
  }

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="bg-surface-warm p-8 pt-6 pb-6 rounded-2xl shadow-sm border border-border">
      <audio
        ref={audioRef}
        src={audioUrl ?? undefined}
        onDurationChange={(e) => setDuration(e.currentTarget.duration)}
        onEnded={() => { stopRaf(); setIsPlaying(false); setIsInPlaySession(false); }}
      />

      <div className="flex items-center gap-2 text-primary mb-4">
        <Music className="w-5 h-5" />
        <span className="text-sm font-bold uppercase tracking-wider">Listen to the Mystery</span>
      </div>

      <div className="grid grid-cols-[auto_1fr] gap-x-4">
        <button
          onClick={handlePlayPause}
          disabled={!audioUrl}
          className="row-span-2 self-center flex items-center justify-center w-14 h-14 rounded-full bg-primary text-primary-text transition-all active:scale-95 shadow-md hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPlaying ? (
            <Pause className="w-6 h-6 fill-current" />
          ) : (
            <Play className="w-6 h-6 fill-current" />
          )}
        </button>

        <div
          ref={progressBarRef}
          className="self-end bg-border rounded-full h-2 overflow-hidden shadow-sm cursor-pointer mt-4"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
        >
          <div
            className="h-full bg-primary"
            style={{ width: `${progress}%` }}
          />
        </div>

        <div className="flex justify-between text-xs text-ink-muted mt-1">
          <span>{formatTime(currentTime)}</span>
          <span>{formatTime(duration)}</span>
        </div>
      </div>

      {notice && (
        <p className="text-xs text-ink-muted italic -mt-2 text-center">{notice}</p>
      )}
    </div>
  );
};

export default AudioPlayer;
