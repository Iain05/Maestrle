import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import PageLayout from '@src/components/PageLayout';
import WaveformTrimmer, { fmt, MIN_CLIP_SECS } from '@src/components/WaveformTrimmer';
import ExcerptMetadataForm from '@src/components/ExcerptMetadataForm';
import { getComposers } from '@src/api/composer';
import type { ComposerSummary, ComposerWorkSummary } from '@src/api/composer';

const SubmitExcerpt: React.FC = () => {
  const [audioBuffer, setAudioBuffer] = useState<AudioBuffer | null>(null);
  const [fileName, setFileName] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [startTime, setStartTime] = useState(0);
  const [endTime, setEndTime] = useState(0);
  const [isDragOver, setIsDragOver] = useState(false);

  const [composers, setComposers] = useState<ComposerSummary[]>([]);
  const [selectedComposer, setSelectedComposer] = useState<ComposerSummary | null>(null);
  const [selectedWork, setSelectedWork] = useState<ComposerWorkSummary | null>(null);
  const [excerptTitle, setExcerptTitle] = useState('');
  const [excerptDescription, setExcerptDescription] = useState('');

  useEffect(() => {
    getComposers().then(setComposers).catch(() => {});
  }, []);

  async function loadFile(file: File) {
    if (!file.type.startsWith('audio/') && !file.name.match(/\.(mp3|wav|flac|ogg|m4a|aac)$/i)) {
      setLoadError('Please upload an audio file (MP3, WAV, FLAC, M4A, OGG).');
      return;
    }
    setLoading(true);
    setLoadError('');
    setFileName(file.name);
    try {
      const arrayBuffer = await file.arrayBuffer();
      const ctx = new AudioContext();
      const buffer = await ctx.decodeAudioData(arrayBuffer);
      await ctx.close();

      if (buffer.duration < MIN_CLIP_SECS) {
        setLoadError(`File is only ${fmt(buffer.duration)} long — please upload audio that is at least 1:00.`);
        setFileName('');
        return;
      }

      setAudioBuffer(buffer);
      setStartTime(0);
      setEndTime(buffer.duration);
    } catch {
      setLoadError('Could not decode this file. Try MP3 or WAV format.');
    } finally {
      setLoading(false);
    }
  }

  function handleFileInput(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) loadFile(file);
    e.target.value = '';
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) loadFile(file);
  }

  const duration = audioBuffer?.duration ?? 0;

  const backLink = (
    <Link
      to="/"
      className="px-3 py-2 bg-surface border border-border text-ink-muted text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
    >
      ← Back to game
    </Link>
  );

  return (
    <PageLayout
      leftSlot={backLink}
      title="Submit an Excerpt"
      subtitle="Share a piece for the community to identify"
    >
      {/* Mobile fallback */}
      <div className="sm:hidden text-center text-ink-muted text-sm px-6 py-10">
        Excerpt submission is only available on desktop.
      </div>

      <main className="hidden sm:flex max-w-3xl w-full flex-col gap-8">
        {/* ── Upload drop zone ── */}
        {!audioBuffer && (
          <div>
            <label
              className={`flex flex-col items-center justify-center gap-4 h-52 rounded-2xl border-2 border-dashed cursor-pointer transition-all ${
                isDragOver
                  ? 'border-primary bg-primary/8 scale-[1.015]'
                  : 'border-border hover:border-primary/50 hover:bg-surface'
              }`}
              onDragOver={e => { e.preventDefault(); setIsDragOver(true); }}
              onDragEnter={e => { e.preventDefault(); setIsDragOver(true); }}
              onDragLeave={() => setIsDragOver(false)}
              onDrop={handleDrop}
            >
              <input
                type="file"
                accept="audio/*,.mp3,.wav,.flac,.ogg,.m4a,.aac"
                className="sr-only"
                onChange={handleFileInput}
              />
              {loading ? (
                <div className="flex flex-col items-center gap-3">
                  <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                  <p className="text-ink-muted text-sm">Decoding audio…</p>
                </div>
              ) : (
                <>
                  <div className="w-14 h-14 rounded-full bg-primary/10 flex items-center justify-center text-3xl">
                    🎵
                  </div>
                  <div className="text-center">
                    <p className="text-ink font-semibold">Drop an audio file here</p>
                    <p className="text-ink-muted text-sm mt-1">
                      or{' '}
                      <span className="text-primary font-semibold underline underline-offset-2 decoration-primary/40">
                        browse to upload
                      </span>
                    </p>
                  </div>
                  <p className="text-ink-subtle text-xs">MP3 · WAV · FLAC · M4A · OGG · minimum 1:00</p>
                </>
              )}
            </label>
            {loadError && (
              <p className="text-red-500 text-sm mt-3 px-1">{loadError}</p>
            )}
          </div>
        )}

        {/* ── Trimmer ── */}
        {audioBuffer && (
          <div className="flex flex-col gap-6">
            {/* File info bar */}
            <div className="flex items-center justify-between pb-5 border-b border-border">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center text-lg flex-shrink-0">
                  🎵
                </div>
                <div>
                  <p className="font-semibold text-ink text-sm leading-tight truncate max-w-xs">{fileName}</p>
                  <p className="text-ink-subtle text-xs mt-0.5">{fmt(duration)} total</p>
                </div>
              </div>
              <button
                onClick={() => { setAudioBuffer(null); setFileName(''); setLoadError(''); }}
                className="px-3 py-1.5 text-sm text-ink-muted border border-border rounded-lg hover:border-border-hover hover:text-ink transition-all"
              >
                Replace file
              </button>
            </div>

            {/* Waveform trimmer */}
            <div>
              <h2 className="font-semibold text-ink mb-1">Trim to excerpt</h2>
              <p className="text-ink-muted text-sm mb-4">
                Select at least 1:00 of audio. Drag the handles, or click the timestamps to enter a precise time.
              </p>
              <WaveformTrimmer
                audioBuffer={audioBuffer}
                startTime={startTime}
                endTime={endTime}
                duration={duration}
                onStartChange={setStartTime}
                onEndChange={setEndTime}
              />
            </div>

            {/* Divider */}
            <div className="border-t border-border" />

            {/* Metadata form */}
            <ExcerptMetadataForm
              composers={composers}
              onComposerChange={setSelectedComposer}
              onWorkChange={setSelectedWork}
              onTitleChange={setExcerptTitle}
              onDescriptionChange={setExcerptDescription}
            />
          </div>
        )}
      </main>
    </PageLayout>
  );
};

export default SubmitExcerpt;
