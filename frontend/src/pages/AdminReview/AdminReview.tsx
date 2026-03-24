import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Link } from 'react-router-dom';
import PageLayout from '@src/components/PageLayout';
import SearchDropdown from '@src/components/SearchDropdown';
import { useAuth } from '@src/context/AuthContext';
import {
  getExcerpts, approveExcerpt, updateExcerptStatus, getDailyChallenges, scheduleTomorrow,
  type DraftExcerpt, type ExcerptsPage, type ExcerptStatus, type SortOption, type DailyChallengesResponse,
} from '@src/api/admin';
import { getComposers, getComposerWorks, type ComposerSummary, type ComposerWorkSummary } from '@src/api/composer';
import { ChevronDown, ChevronUp, Check, X, ChevronLeft, ChevronRight, Trash2, RotateCcw, Undo2 } from 'lucide-react';

const DESCRIPTION_MAX = 500;
const PAGE_SIZE = 10;

const STATUS_LABELS: Record<ExcerptStatus, string> = {
  DRAFT:    'Pending',
  ACTIVE:   'Active',
  REJECTED: 'Rejected',
  DELETED:  'Deleted',
};

const STATUS_BADGE: Record<ExcerptStatus, string> = {
  DRAFT:    'bg-amber-400 text-white dark:bg-amber-900/30 dark:text-amber-400',
  ACTIVE:   'bg-green-500 text-white dark:bg-green-900/30 dark:text-green-400',
  REJECTED: 'bg-red-500 text-white dark:bg-red-900/30 dark:text-red-400',
  DELETED:  'bg-zinc-400 text-white dark:bg-zinc-800 dark:text-zinc-400',
};

const ALL_STATUSES: ExcerptStatus[] = ['DRAFT', 'ACTIVE', 'REJECTED', 'DELETED'];

const SORT_LABELS: Record<SortOption, string> = {
  timesUsed_asc:    'Times used ↑',
  timesUsed_desc:   'Times used ↓',
  dateUploaded_asc: 'Date uploaded ↑',
  dateUploaded_desc:'Date uploaded ↓',
};

// ── Confirmation modal ─────────────────────────────────────────────────────

type PendingAction = 'approve' | 'reject' | 'unreject' | 'delete' | 'restore';

interface ActionConfig {
  title: string;
  message: string;
  confirmLabel: string;
  confirmStyle: string;
}

const ACTION_CONFIG: Record<PendingAction, ActionConfig> = {
  approve:  {
    title: 'Approve submission?',
    message: 'This excerpt will become active and eligible for the daily challenge. Your edits will be saved.',
    confirmLabel: 'Approve',
    confirmStyle: 'bg-green-500 hover:bg-green-600 text-white',
  },
  reject:   {
    title: 'Reject submission?',
    message: 'This excerpt will be marked as rejected. You can unreject it later.',
    confirmLabel: 'Reject',
    confirmStyle: 'bg-red-500 hover:bg-red-600 text-white',
  },
  unreject: {
    title: 'Move back to pending?',
    message: 'This excerpt will be moved back to draft for re-review.',
    confirmLabel: 'Unreject',
    confirmStyle: 'bg-amber-500 hover:bg-amber-600 text-white',
  },
  delete:   {
    title: 'Delete excerpt?',
    message: 'This excerpt will be removed from the active pool. You can restore it later.',
    confirmLabel: 'Delete',
    confirmStyle: 'bg-red-500 hover:bg-red-600 text-white',
  },
  restore:  {
    title: 'Restore excerpt?',
    message: 'This excerpt will be restored to active status and become eligible for the daily challenge again.',
    confirmLabel: 'Restore',
    confirmStyle: 'bg-green-500 hover:bg-green-600 text-white',
  },
};

interface ConfirmModalProps {
  action: PendingAction;
  loading: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

const ConfirmModal: React.FC<ConfirmModalProps> = ({ action, loading, onConfirm, onCancel }) => {
  const cfg = ACTION_CONFIG[action];
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 backdrop-blur-sm">
      <div className="modal-card bg-canvas border border-border rounded-2xl shadow-2xl p-6 w-full max-w-sm mx-4">
        <h2 className="text-lg font-bold text-ink mb-2">{cfg.title}</h2>
        <p className="text-ink-muted text-sm mb-6">{cfg.message}</p>
        <div className="flex gap-3 justify-end">
          <button
            onClick={onCancel}
            disabled={loading}
            className="px-4 py-2 bg-surface border border-border text-ink-muted text-sm font-semibold rounded-xl hover:border-border-hover hover:text-ink transition-all disabled:opacity-40"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className={`px-4 py-2 text-sm font-semibold rounded-xl transition-all active:scale-95 disabled:opacity-40 disabled:active:scale-100 ${cfg.confirmStyle}`}
          >
            {loading ? 'Working…' : cfg.confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

// ── DraftCard ──────────────────────────────────────────────────────────────

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

  async function handleConfirmAction() {
    if (!pendingAction) return;
    setActionLoading(true);
    setError('');
    try {
      if (pendingAction === 'approve') {
        await approveExcerpt(
          excerpt.excerptId,
          {
            composerId: selectedComposer!.composerId,
            workId: selectedWork?.workId ?? null,
            name: title.trim(),
            compositionYear: parseYear(),
            description,
          },
          token,
        );
      } else {
        const statusMap: Record<Exclude<PendingAction, 'approve'>, ExcerptStatus> = {
          reject:   'REJECTED',
          unreject: 'DRAFT',
          delete:   'DELETED',
          restore:  'ACTIVE',
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
            <div className="hidden sm:flex flex-col items-end gap-0.5">
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

              <div className="grid grid-cols-6 gap-4">
                <div className="col-span-5 flex flex-col gap-1.5">
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
                <div className="col-span-1 flex flex-col gap-1.5">
                  <label className="text-sm font-semibold text-ink">
                    Year <span className="text-ink-subtle font-normal">(opt.)</span>
                  </label>
                  <input
                    type="text"
                    inputMode="numeric"
                    value={yearStr}
                    onChange={e => setYearStr(e.target.value.replace(/\D/g, '').slice(0, 4))}
                    maxLength={4}
                    placeholder="e.g. 1808"
                    className="w-full px-4 py-3 bg-surface text-ink placeholder:text-ink-subtle border-2 border-border rounded-xl focus:border-primary focus:outline-none transition-all"
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

// ── AdminReview ────────────────────────────────────────────────────────────

const AdminReview: React.FC = () => {
  const { token, isAdmin } = useAuth();

  const [selectedStatuses, setSelectedStatuses] = useState<Set<ExcerptStatus>>(new Set(['DRAFT']));
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const statusDropdownRef = useRef<HTMLDivElement>(null);
  const [sortDropdownOpen, setSortDropdownOpen] = useState(false);
  const sortDropdownRef = useRef<HTMLDivElement>(null);
  const [composerQuery, setComposerQuery] = useState('');
  const [filterComposer, setFilterComposer] = useState<ComposerSummary | null>(null);
  const [sort, setSort] = useState<SortOption>('timesUsed_asc');

  const [dailyChallenges, setDailyChallenges] = useState<DailyChallengesResponse | null>(null);

  const [resultPage, setResultPage] = useState<ExcerptsPage | null>(null);
  const [composers, setComposers] = useState<ComposerSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState('');
  const [currentPage, setCurrentPage] = useState(0);

  const loadPage = useCallback((statuses: ExcerptStatus[], composerId: number | null, sortOption: SortOption, pageIndex: number) => {
    if (!token) return;
    setLoading(true);
    setFetchError('');
    getExcerpts(token, statuses, composerId, sortOption, pageIndex, PAGE_SIZE)
      .then(p => setResultPage(p))
      .catch(e => setFetchError(e instanceof Error ? e.message : 'Failed to load'))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    if (!token) return;
    getComposers().then(setComposers).catch(() => {});
    getDailyChallenges(token).then(setDailyChallenges).catch(() => {});
    loadPage(['DRAFT'], null, 'timesUsed_asc', 0);
  }, [token, loadPage]);

  // Close dropdowns on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (statusDropdownRef.current && !statusDropdownRef.current.contains(e.target as Node)) {
        setStatusDropdownOpen(false);
      }
      if (sortDropdownRef.current && !sortDropdownRef.current.contains(e.target as Node)) {
        setSortDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function toggleStatus(s: ExcerptStatus) {
    if (selectedStatuses.has(s) && selectedStatuses.size === 1) return;
    const next = new Set(selectedStatuses);
    next.has(s) ? next.delete(s) : next.add(s);
    setSelectedStatuses(next);
    setCurrentPage(0);
    loadPage([...next], filterComposer?.composerId ?? null, sort, 0);
  }

  function handleFilterComposerSelect(c: ComposerSummary) {
    setFilterComposer(c);
    setComposerQuery(c.name);
    setCurrentPage(0);
    loadPage([...selectedStatuses], c.composerId, sort, 0);
  }

  function handleFilterComposerChange(val: string) {
    setComposerQuery(val);
    if (filterComposer && val !== filterComposer.name) {
      setFilterComposer(null);
      setCurrentPage(0);
      loadPage([...selectedStatuses], null, sort, 0);
    }
  }

  function clearFilterComposer() {
    setFilterComposer(null);
    setComposerQuery('');
    setCurrentPage(0);
    loadPage([...selectedStatuses], null, sort, 0);
  }

  function handleSortChange(newSort: SortOption) {
    setSort(newSort);
    setCurrentPage(0);
    loadPage([...selectedStatuses], filterComposer?.composerId ?? null, newSort, 0);
  }

  function handleStatusChanged() {
    loadPage([...selectedStatuses], filterComposer?.composerId ?? null, sort, currentPage);
  }

  function handleScheduled() {
    if (token) getDailyChallenges(token).then(setDailyChallenges).catch(() => {});
  }

  function goToPage(index: number) {
    setCurrentPage(index);
    loadPage([...selectedStatuses], filterComposer?.composerId ?? null, sort, index);
  }

  const statusDropdownLabel = selectedStatuses.size === ALL_STATUSES.length
    ? 'All statuses'
    : selectedStatuses.size === 1
      ? STATUS_LABELS[[...selectedStatuses][0]]
      : `${selectedStatuses.size} statuses`;

  const totalPages = resultPage?.totalPages ?? 0;
  const totalElements = resultPage?.totalElements ?? 0;

  const backLink = (
    <Link
      to="/"
      className="px-3 py-2 bg-surface border border-border text-ink-muted text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
    >
      ← Back to game
    </Link>
  );

  if (!isAdmin) {
    return (
      <PageLayout leftSlot={backLink} title="Admin Review" subtitle="Excerpt management">
        <p className="text-ink-muted text-sm mt-8">You don't have permission to view this page.</p>
      </PageLayout>
    );
  }

  return (
    <PageLayout leftSlot={backLink} title="Admin Review" subtitle="Manage submitted excerpts">
      <main className="max-w-3xl w-full flex flex-col gap-4">

        {/* Daily challenge banner */}
        {dailyChallenges && (
          <div className="grid grid-cols-2 gap-3">
            {(['today', 'tomorrow'] as const).map(day => {
              const entry = dailyChallenges[day];
              return (
                <div key={day} className="p-3 bg-surface border border-border rounded-2xl flex flex-col gap-0.5">
                  <span className="text-xs font-semibold text-ink-muted uppercase tracking-wide">{day === 'today' ? 'Today' : 'Tomorrow'}</span>
                  {entry ? (
                    <>
                      <span className="text-sm font-semibold text-ink truncate">{entry.excerptName}</span>
                      <span className="text-xs text-ink-muted">{entry.composerName}</span>
                      <span className="text-xs text-ink-subtle mt-0.5">Challenge #{entry.challengeNumber}</span>
                    </>
                  ) : (
                    <span className="text-sm text-ink-subtle italic">Not scheduled</span>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Filters */}
        <div className="flex items-stretch gap-3 p-3 bg-surface border border-border rounded-2xl">
          {/* Status multi-select dropdown */}
          <div className="relative" ref={statusDropdownRef}>
            <button
              onClick={() => setStatusDropdownOpen(v => !v)}
              className="flex items-center gap-2 px-4 h-full bg-canvas border-2 border-border text-ink text-sm font-semibold rounded-xl hover:border-border-hover transition-all"
            >
              <span>{statusDropdownLabel}</span>
              <ChevronDown className="w-3.5 h-3.5 text-ink-muted" />
            </button>
            {statusDropdownOpen && (
              <div className="absolute z-20 top-full left-0 mt-1 bg-canvas border border-border rounded-xl shadow-lg py-1 min-w-[160px]">
                {ALL_STATUSES.map(s => (
                  <label
                    key={s}
                    className="flex items-center gap-2.5 px-3 py-2 cursor-pointer hover:bg-surface transition-colors"
                    onClick={e => { e.preventDefault(); toggleStatus(s); }}
                  >
                    <div className={`w-4 h-4 rounded border-2 flex items-center justify-center flex-shrink-0 transition-all ${
                      selectedStatuses.has(s) ? 'bg-primary border-primary' : 'border-border'
                    }`}>
                      {selectedStatuses.has(s) && <Check className="w-2.5 h-2.5 text-white" />}
                    </div>
                    <span className="text-sm text-ink">{STATUS_LABELS[s]}</span>
                  </label>
                ))}
              </div>
            )}
          </div>

          {/* Composer search */}
          <div className="flex-1">
            <SearchDropdown
              items={composers}
              getKey={c => c.composerId}
              getLabel={c => c.name}
              value={composerQuery}
              onChange={handleFilterComposerChange}
              onSelect={handleFilterComposerSelect}
              onClear={clearFilterComposer}
              selected={!!filterComposer}
              placeholder="All composers"
              icon="search"
            />
          </div>
        </div>

        {/* Sort selector */}
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-ink-muted">Sort:</span>
          <div className="relative" ref={sortDropdownRef}>
            <button
              onClick={() => setSortDropdownOpen(v => !v)}
              className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-lg bg-primary/15 text-primary hover:bg-primary/25 transition-all"
            >
              {SORT_LABELS[sort]}
              <ChevronDown className="w-3 h-3" />
            </button>
            {sortDropdownOpen && (
              <div className="absolute z-20 top-full left-0 mt-1 bg-canvas border border-border rounded-xl shadow-lg py-1 min-w-[160px]">
                {(Object.keys(SORT_LABELS) as SortOption[]).map(opt => (
                  <button
                    key={opt}
                    onClick={() => { handleSortChange(opt); setSortDropdownOpen(false); }}
                    className={`w-full text-left px-3 py-2 text-sm transition-colors hover:bg-surface ${
                      sort === opt ? 'text-primary font-semibold' : 'text-ink'
                    }`}
                  >
                    {SORT_LABELS[opt]}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {loading && (
          <div className="flex justify-center py-16">
            <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {!loading && fetchError && (
          <p className="text-red-500 text-sm text-center py-8">{fetchError}</p>
        )}

        {!loading && !fetchError && totalElements === 0 && (
          <div className="text-center py-16">
            <p className="text-2xl mb-2 text-green-600 dark:text-green-400">✓</p>
            <p className="text-ink font-semibold">No results</p>
          </div>
        )}

        {!loading && !fetchError && resultPage?.content.map(excerpt => (
          <DraftCard
            key={excerpt.excerptId}
            excerpt={excerpt}
            composers={composers}
            token={token!}
            tomorrowExcerptId={dailyChallenges?.tomorrow?.excerptId ?? null}
            onStatusChanged={handleStatusChanged}
            onScheduled={handleScheduled}
          />
        ))}

        {!loading && !fetchError && totalPages > 1 && (
          <div className="flex items-center justify-between pt-2">
            <span className="text-ink-muted text-sm">
              Page {currentPage + 1} of {totalPages}
              <span className="text-ink-subtle ml-2">({totalElements} total)</span>
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => goToPage(currentPage - 1)}
                disabled={currentPage === 0}
                className="flex items-center gap-1 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <ChevronLeft className="w-4 h-4" /> Prev
              </button>
              <button
                onClick={() => goToPage(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
                className="flex items-center gap-1 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Next <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </main>
    </PageLayout>
  );
};

export default AdminReview;
