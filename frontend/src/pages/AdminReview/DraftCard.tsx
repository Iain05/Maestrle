import React, { useState, useEffect } from 'react';
import SearchDropdown from '@src/components/SearchDropdown';
import {
  approveExcerpt, saveExcerptMetadata, updateExcerptStatus, scheduleTomorrow,
  type DraftExcerpt, type ExcerptStatus,
} from '@src/api/admin';
import { getComposerWorks, type ComposerSummary, type ComposerWorkSummary } from '@src/api/composer';
import { ChevronDown, ChevronUp, Check, X, Trash2, RotateCcw, Undo2 } from 'lucide-react';
import { STATUS_LABELS, STATUS_BADGE } from './adminConstants';
import ConfirmModal, { type PendingAction } from './ConfirmModal';

const DESCRIPTION_MAX = 500;

interface DraftCardProps {
  excerpt: DraftExcerpt;
  composers: ComposerSummary[];
  token: string;
  tomorrowExcerptId: number | null;
  onStatusChanged: () => void;
  onScheduled: () => void;
}

const DraftCard: React.FC<DraftCardProps> = ({ excerpt, composers, token, tomorrowExcerptId, onStatusChanged, onScheduled }) => {
  const [expanded, setExpanded] = useState(false);

  const [composerQuery, setComposerQuery] = useState(excerpt.composerName);
  const [selectedComposer, setSelectedComposer] = useState<ComposerSummary | null>(
    () => composers.find(c => c.composerId === excerpt.composerId) ?? null,
  );
  const [works, setWorks] = useState<ComposerWorkSummary[]>([]);
  const [workQuery, setWorkQuery] = useState('');
  const [selectedWork, setSelectedWork] = useState<ComposerWorkSummary | null>(null);
  const [worksLoading, setWorksLoading] = useState(false);
  const [title, setTitle] = useState(excerpt.name);
  const [yearStr, setYearStr] = useState(excerpt.compositionYear?.toString() ?? '');
  const [description, setDescription] = useState(excerpt.description ?? '');

  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!selectedComposer && composers.length > 0) {
      const match = composers.find(c => c.composerId === excerpt.composerId);
      if (match) { setSelectedComposer(match); setComposerQuery(match.name); }
    }
  }, [composers, excerpt.composerId, selectedComposer]);

  useEffect(() => {
    if (!expanded || !selectedComposer) return;
    setWorksLoading(true);
    getComposerWorks(selectedComposer.composerId)
      .then(ws => {
        setWorks(ws);
        if (excerpt.workId) {
          const match = ws.find(w => w.workId === excerpt.workId);
          if (match) { setSelectedWork(match); setWorkQuery(match.title); }
        }
      })
      .catch(() => setWorks([]))
      .finally(() => setWorksLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expanded]);

  function handleComposerSelect(c: ComposerSummary) {
    setSelectedComposer(c);
    setComposerQuery(c.name);
    setSelectedWork(null);
    setWorkQuery('');
    setWorksLoading(true);
    getComposerWorks(c.composerId)
      .then(setWorks).catch(() => setWorks([]))
      .finally(() => setWorksLoading(false));
  }

  function handleComposerChange(val: string) {
    setComposerQuery(val);
    if (selectedComposer && val !== selectedComposer.name) {
      setSelectedComposer(null); setWorks([]); setSelectedWork(null); setWorkQuery('');
    }
  }

  function parseYear(): number | null {
    const n = parseInt(yearStr);
    return yearStr.length === 4 && n >= 600 && n <= new Date().getFullYear() ? n : null;
  }

  const canApprove = !!selectedComposer && title.trim().length > 0;

  const workChanged = !worksLoading && (selectedWork?.workId ?? null) !== (excerpt.workId ?? null);
  const hasChanges =
    selectedComposer?.composerId !== excerpt.composerId ||
    workChanged ||
    title.trim() !== excerpt.name ||
    yearStr !== (excerpt.compositionYear?.toString() ?? '') ||
    description !== (excerpt.description ?? '');

  async function handleConfirmAction() {
    if (!pendingAction) return;
    setActionLoading(true);
    setError('');
    const metadataPayload = {
      composerId: selectedComposer!.composerId,
      workId: selectedWork?.workId ?? null,
      name: title.trim(),
      compositionYear: parseYear(),
      description,
    };

    try {
      if (pendingAction === 'save') {
        await saveExcerptMetadata(excerpt.excerptId, metadataPayload, token);
      } else if (pendingAction === 'approve') {
        await approveExcerpt(excerpt.excerptId, metadataPayload, token);
      } else {
        const statusMap: Record<Exclude<PendingAction, 'approve' | 'save'>, ExcerptStatus> = {
          reject: 'REJECTED',
          unreject: 'DRAFT',
          delete: 'DELETED',
          restore: 'ACTIVE',
        };
        await updateExcerptStatus(excerpt.excerptId, statusMap[pendingAction], token);
      }
      setPendingAction(null);
      onStatusChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong.');
      setPendingAction(null);
      setActionLoading(false);
    }
  }

  const formattedDate = excerpt.dateUploaded
    ? new Date(excerpt.dateUploaded).toLocaleDateString('en-CA', { year: 'numeric', month: 'short', day: 'numeric' })
    : '—';

  const workPlaceholder = !selectedComposer
    ? 'Select a composer first'
    : worksLoading ? 'Loading works…'
      : works.length === 0 ? 'No works on record'
        : 'Search work…';

  return (
    <>
      {pendingAction && (
        <ConfirmModal
          action={pendingAction}
          loading={actionLoading}
          onConfirm={handleConfirmAction}
          onCancel={() => setPendingAction(null)}
        />
      )}

      <div className="bg-surface border border-border rounded-2xl overflow-hidden shadow-sm">
        {/* Header row */}
        <button
          onClick={() => setExpanded(v => !v)}
          className="w-full flex items-center justify-between px-5 py-4 hover:bg-surface-warm transition-colors text-left"
        >
          <div className="flex flex-col gap-0.5 min-w-0">
            <div className="flex items-baseline gap-2">
              <span className="font-semibold text-ink truncate">{excerpt.name}</span>
              {excerpt.status === 'ACTIVE' && (
                <span className="text-xs text-ink-subtle font-medium flex-shrink-0">{excerpt.timesUsed}×</span>
              )}
            </div>
            <span className="text-ink-muted text-sm">{excerpt.composerName}</span>
          </div>
          <div className="flex items-center gap-4 flex-shrink-0 ml-4">
            <div className="hidden md:flex flex-col items-end gap-0.5">
              <span className="text-ink-subtle text-xs">by {excerpt.uploaderUsername}</span>
              <span className="text-ink-subtle text-xs">{formattedDate}</span>
            </div>
            <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${STATUS_BADGE[excerpt.status]}`}>
              {STATUS_LABELS[excerpt.status]}
            </span>
            {expanded
              ? <ChevronUp className="w-4 h-4 text-ink-subtle flex-shrink-0" />
              : <ChevronDown className="w-4 h-4 text-ink-subtle flex-shrink-0" />}
          </div>
        </button>

        {/* Expanded panel */}
        {expanded && (
          <div className="border-t border-border px-5 py-5 flex flex-col gap-5">
            {/* Audio */}
            <div className="flex flex-col gap-1.5">
              <p className="text-sm font-semibold text-ink">Audio</p>
              {/* eslint-disable-next-line jsx-a11y/media-has-caption */}
              <audio controls src={excerpt.audioUrl} className="w-full h-10" />
            </div>

            {/* Edit form */}
            <div className="flex flex-col gap-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="flex flex-col gap-1.5">
                  <label className="text-sm font-semibold text-ink">
                    Composer <span className="text-primary">*</span>
                  </label>
                  <SearchDropdown
                    items={composers}
                    getKey={c => c.composerId}
                    getLabel={c => c.name}
                    value={composerQuery}
                    onChange={handleComposerChange}
                    onSelect={handleComposerSelect}
                    onClear={() => { setSelectedComposer(null); setComposerQuery(''); setWorks([]); setSelectedWork(null); setWorkQuery(''); }}
                    selected={!!selectedComposer}
                    placeholder="Search composer…"
                    icon="search"
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className={`text-sm font-semibold transition-colors ${!selectedComposer ? 'text-ink-subtle' : 'text-ink'}`}>
                    Work <span className="text-ink-subtle font-normal">(optional)</span>
                  </label>
                  <SearchDropdown
                    items={works}
                    getKey={w => w.workId}
                    getLabel={w => w.title}
                    value={workQuery}
                    onChange={val => { setWorkQuery(val); if (selectedWork && val !== selectedWork.title) setSelectedWork(null); }}
                    onSelect={w => { setSelectedWork(w); setWorkQuery(w.title); }}
                    onClear={() => { setSelectedWork(null); setWorkQuery(''); }}
                    selected={!!selectedWork}
                    disabled={!selectedComposer}
                    placeholder={workPlaceholder}
                    icon="chevron"
                  />
                </div>
              </div>

              <div className="flex gap-4 items-start">
                <div className="flex-1 min-w-0 flex flex-col gap-1.5">
                  <label className="text-sm font-semibold text-ink">
                    Title <span className="text-primary">*</span>
                  </label>
                  <input
                    type="text"
                    value={title}
                    onChange={e => setTitle(e.target.value)}
                    maxLength={255}
                    placeholder="e.g. Symphony No. 5 in C minor, Op. 67 — I. Allegro con brio"
                    className="w-full px-4 py-3 bg-surface text-ink placeholder:text-ink-subtle border-2 border-border rounded-xl focus:border-primary focus:outline-none transition-all"
                  />
                </div>
                <div className="shrink-0 flex flex-col gap-1.5">
                  <label className="text-sm font-semibold text-ink">
                    Year <span className="text-ink-subtle font-normal">(opt.)</span>
                  </label>
                  <input
                    type="text"
                    inputMode="numeric"
                    value={yearStr}
                    onChange={e => setYearStr(e.target.value.replace(/\D/g, '').slice(0, 4))}
                    maxLength={4}
                    placeholder="1808"
                    className="w-20 px-4 py-3 bg-surface text-ink placeholder:text-ink-subtle border-2 border-border rounded-xl focus:border-primary focus:outline-none transition-all"
                  />
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-semibold text-ink">
                  Description <span className="text-ink-subtle font-normal">(optional)</span>
                </label>
                <textarea
                  value={description}
                  onChange={e => setDescription(e.target.value.slice(0, DESCRIPTION_MAX))}
                  rows={3}
                  placeholder="A short note about this excerpt…"
                  className="w-full px-4 py-3 bg-surface text-ink placeholder:text-ink-subtle border-2 border-border rounded-xl focus:border-primary focus:outline-none transition-all resize-none"
                />
                <p className={`text-xs text-right transition-colors ${description.length >= DESCRIPTION_MAX ? 'text-red-400' : 'text-ink-subtle'}`}>
                  {description.length} / {DESCRIPTION_MAX}
                </p>
              </div>
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            {/* Action buttons — depend on current status */}
            <div className="flex items-center gap-3 pt-1 flex-wrap">
              {excerpt.status === 'DRAFT' && (
                <>
                  <button
                    onClick={() => setPendingAction('approve')}
                    disabled={!canApprove}
                    className="flex items-center gap-2 px-5 py-2.5 bg-green-500 hover:bg-green-600 text-white font-semibold rounded-xl transition-all shadow-sm disabled:opacity-40 disabled:cursor-not-allowed active:scale-95 disabled:active:scale-100"
                  >
                    <Check className="w-4 h-4" /> Approve
                  </button>
                  <button
                    onClick={() => setPendingAction('reject')}
                    className="flex items-center gap-2 px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white font-semibold rounded-xl transition-all shadow-sm active:scale-95"
                  >
                    <X className="w-4 h-4" /> Reject
                  </button>
                </>
              )}

              {excerpt.status === 'REJECTED' && (
                <button
                  onClick={() => setPendingAction('unreject')}
                  className="flex items-center gap-2 px-5 py-2.5 bg-amber-500 hover:bg-amber-600 text-white font-semibold rounded-xl transition-all shadow-sm active:scale-95"
                >
                  <Undo2 className="w-4 h-4" /> Unreject
                </button>
              )}

              {excerpt.status === 'ACTIVE' && (
                <>
                  <button
                    onClick={async () => {
                      setScheduleLoading(true);
                      setError('');
                      try {
                        await scheduleTomorrow(excerpt.excerptId, token);
                        onScheduled();
                      } catch (e) {
                        setError(e instanceof Error ? e.message : 'Failed to schedule');
                      } finally {
                        setScheduleLoading(false);
                      }
                    }}
                    disabled={scheduleLoading || tomorrowExcerptId === excerpt.excerptId}
                    className="flex items-center gap-2 px-5 py-2.5 bg-primary hover:bg-primary-hover text-white font-semibold rounded-xl transition-all shadow-sm active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
                  >
                    {scheduleLoading ? 'Scheduling…' : tomorrowExcerptId === excerpt.excerptId ? 'Scheduled tomorrow' : 'Schedule tomorrow'}
                  </button>
                  <button
                    onClick={() => setPendingAction('save')}
                    disabled={!hasChanges || !canApprove}
                    className={`flex items-center gap-2 px-5 py-2.5 font-semibold rounded-xl transition-all shadow-sm active:scale-95 disabled:cursor-not-allowed disabled:active:scale-100 ${hasChanges && canApprove
                      ? 'bg-green-500 hover:bg-green-600 text-white'
                      : 'bg-surface border border-border text-ink-subtle opacity-50'
                      }`}
                  >
                    <Check className="w-4 h-4" /> Save
                  </button>
                  <button
                    onClick={() => setPendingAction('delete')}
                    className="flex items-center gap-2 px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white font-semibold rounded-xl transition-all shadow-sm active:scale-95"
                  >
                    <Trash2 className="w-4 h-4" /> Delete
                  </button>
                </>
              )}

              {excerpt.status === 'DELETED' && (
                <button
                  onClick={() => setPendingAction('restore')}
                  className="flex items-center gap-2 px-5 py-2.5 bg-green-500 hover:bg-green-600 text-white font-semibold rounded-xl transition-all shadow-sm active:scale-95"
                >
                  <RotateCcw className="w-4 h-4" /> Restore
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default DraftCard;
