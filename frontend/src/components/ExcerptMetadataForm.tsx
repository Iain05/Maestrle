import React, { useState } from 'react';
import SearchDropdown from './SearchDropdown';
import type { ComposerSummary, ComposerWorkSummary } from '@src/api/composer';
import { getComposerWorks } from '@src/api/composer';

const DESCRIPTION_MAX = 500;

interface ExcerptMetadataFormProps {
  composers: ComposerSummary[];
  onComposerChange: (composer: ComposerSummary | null) => void;
  onWorkChange: (work: ComposerWorkSummary | null) => void;
  onTitleChange: (title: string) => void;
  onDescriptionChange: (description: string) => void;
}

const ExcerptMetadataForm: React.FC<ExcerptMetadataFormProps> = ({
  composers,
  onComposerChange,
  onWorkChange,
  onTitleChange,
  onDescriptionChange,
}) => {
  const [composerQuery, setComposerQuery] = useState('');
  const [selectedComposer, setSelectedComposer] = useState<ComposerSummary | null>(null);

  const [works, setWorks] = useState<ComposerWorkSummary[]>([]);
  const [workQuery, setWorkQuery] = useState('');
  const [selectedWork, setSelectedWork] = useState<ComposerWorkSummary | null>(null);
  const [worksLoading, setWorksLoading] = useState(false);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  async function handleComposerSelect(composer: ComposerSummary) {
    setSelectedComposer(composer);
    onComposerChange(composer);

    setSelectedWork(null);
    setWorkQuery('');
    onWorkChange(null);
    setWorksLoading(true);
    try {
      setWorks(await getComposerWorks(composer.composerId));
    } catch {
      setWorks([]);
    } finally {
      setWorksLoading(false);
    }
  }

  function handleComposerChange(value: string) {
    setComposerQuery(value);
    if (selectedComposer && value !== selectedComposer.name) {
      setSelectedComposer(null);
      setWorks([]);
      setSelectedWork(null);
      setWorkQuery('');
      onComposerChange(null);
      onWorkChange(null);
    }
  }

  function clearComposer() {
    setSelectedComposer(null);
    setComposerQuery('');
    setWorks([]);
    setSelectedWork(null);
    setWorkQuery('');
    onComposerChange(null);
    onWorkChange(null);
  }

  function handleWorkSelect(work: ComposerWorkSummary) {
    setSelectedWork(work);
    onWorkChange(work);
    setTitle(work.title);
    onTitleChange(work.title);
  }

  function handleWorkChange(value: string) {
    setWorkQuery(value);
    if (selectedWork && value !== selectedWork.title) {
      setSelectedWork(null);
      onWorkChange(null);
    }
  }

  function clearWork() {
    setSelectedWork(null);
    setWorkQuery('');
    onWorkChange(null);
  }

  function handleTitleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setTitle(e.target.value);
    onTitleChange(e.target.value);
  }

  function handleDescriptionChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    const val = e.target.value.slice(0, DESCRIPTION_MAX);
    setDescription(val);
    onDescriptionChange(val);
  }

  const workDisabled = !selectedComposer;
  const workPlaceholder = workDisabled
    ? 'Select a composer first'
    : worksLoading
      ? 'Loading works…'
      : works.length === 0
        ? 'No works on record'
        : 'Search work…';

  return (
    <div className="flex flex-col gap-4">
      {/* ── Composer + Work row ── */}
      <div className="grid grid-cols-2 gap-4">
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
            onClear={clearComposer}
            selected={!!selectedComposer}
            placeholder="Search composer…"
            icon="search"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label className={`text-sm font-semibold transition-colors ${workDisabled ? 'text-ink-subtle' : 'text-ink'}`}>
            Work <span className="text-ink-subtle font-normal">(optional)</span>
          </label>
          <SearchDropdown
            items={works}
            getKey={w => w.workId}
            getLabel={w => w.title}
            value={workQuery}
            onChange={handleWorkChange}
            onSelect={handleWorkSelect}
            onClear={clearWork}
            selected={!!selectedWork}
            disabled={workDisabled}
            placeholder={workPlaceholder}
            icon="chevron"
          />
        </div>
      </div>

      {/* ── Title ── */}
      <div className="flex flex-col gap-1.5">
        <label htmlFor="excerpt-title" className="text-sm font-semibold text-ink">
          Title <span className="text-primary">*</span>
        </label>
        <input
          id="excerpt-title"
          type="text"
          value={title}
          onChange={handleTitleChange}
          maxLength={255}
          placeholder="e.g. Symphony No. 5 in C minor, Op. 67 — I. Allegro con brio"
          className="w-full px-4 py-3 bg-surface text-ink placeholder:text-ink-subtle border-2 border-border rounded-xl focus:border-primary focus:outline-none transition-all"
        />
        <p className="text-xs text-ink-subtle">
          If this excerpt is from a specific movement or part, include it here!
        </p>
      </div>

      {/* ── Description ── */}
      <div className="flex flex-col gap-1.5">
        <label htmlFor="excerpt-description" className="text-sm font-semibold text-ink">
          Description <span className="text-ink-subtle font-normal">(optional)</span>
        </label>
        <textarea
          id="excerpt-description"
          value={description}
          onChange={handleDescriptionChange}
          rows={3}
          placeholder="A short note about this excerpt — what makes it interesting, notable performances, historical context…"
          className="w-full px-4 py-3 bg-surface text-ink placeholder:text-ink-subtle border-2 border-border rounded-xl focus:border-primary focus:outline-none transition-all resize-none"
        />
        <p className={`text-xs text-right transition-colors ${description.length >= DESCRIPTION_MAX ? 'text-red-400' : 'text-ink-subtle'}`}>
          {description.length} / {DESCRIPTION_MAX}
        </p>
      </div>
    </div>
  );
};

export default ExcerptMetadataForm;
