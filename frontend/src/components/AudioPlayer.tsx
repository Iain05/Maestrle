import React, { useState, useRef } from 'react';
import { Music, Play, Pause } from 'lucide-react';

interface AudioPlayerProps {
  audioUrl: string | null;
}

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

const AudioPlayer: React.FC<AudioPlayerProps> = ({ audioUrl }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [isInPlaySession, setIsInPlaySession] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const progressBarRef = useRef<HTMLDivElement | null>(null);

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
      setIsPlaying(false);
    } else if (isInPlaySession) {
      audio.play();
      setIsPlaying(true);
    } else {
      audio.currentTime = 0;
      setCurrentTime(0);
      setIsInPlaySession(true);
      audio.play();
      setIsPlaying(true);
    }
  }

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="bg-gradient-to-br from-indigo-50 to-purple-50 p-8 rounded-2xl shadow-sm border border-indigo-200">
      <audio
        ref={audioRef}
        src={audioUrl ?? undefined}
        onTimeUpdate={(e) => setCurrentTime(e.currentTarget.currentTime)}
        onDurationChange={(e) => setDuration(e.currentTarget.duration)}
        onEnded={() => { setIsPlaying(false); setIsInPlaySession(false); }}
      />

      <div className="flex items-center gap-2 text-indigo-600 mb-4">
        <Music className="w-5 h-5" />
        <span className="text-sm font-bold uppercase tracking-wider">Listen to the Mystery</span>
      </div>

      <div className="grid grid-cols-[auto_1fr] gap-x-4">
        <button
          onClick={handlePlayPause}
          disabled={!audioUrl}
          className="row-span-2 self-center flex items-center justify-center w-14 h-14 rounded-full bg-indigo-600 text-white transition-all active:scale-95 shadow-md hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPlaying ? (
            <Pause className="w-6 h-6 fill-current" />
          ) : (
            <Play className="w-6 h-6 fill-current" />
          )}
        </button>

        <div
          ref={progressBarRef}
          className="self-end bg-white rounded-full h-2 overflow-hidden shadow-sm cursor-pointer mt-4"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
        >
          <div
            className="h-full bg-indigo-600 transition-all duration-75"
            style={{ width: `${progress}%` }}
          />
        </div>

        <div className="flex justify-between text-xs text-slate-600 mt-1">
          <span>{formatTime(currentTime)}</span>
          <span>{formatTime(duration)}</span>
        </div>
      </div>
    </div>
  );
};

export default AudioPlayer;
