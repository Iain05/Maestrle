import React from 'react';
import SearchDropdown from './SearchDropdown';
import type { ComposerSummary } from '@src/api/composer';

interface ComposerSearchProps {
  value: string;
  onChange: (value: string) => void;
  onSelect: (composer: ComposerSummary) => void;
  onClear?: () => void;
  selected?: boolean;
  disabled: boolean;
  composers: ComposerSummary[];
}

const ComposerSearch: React.FC<ComposerSearchProps> = ({
  value,
  onChange,
  onSelect,
  onClear,
  selected,
  disabled,
  composers,
}) => (
  <SearchDropdown
    id="composer-search"
    label="Search Composer"
    items={composers}
    getKey={c => c.composerId}
    getLabel={c => c.name}
    value={value}
    onChange={onChange}
    onSelect={onSelect}
    onClear={onClear}
    selected={selected}
    disabled={disabled}
    placeholder="Search composer..."
    icon="search"
  />
);

export default ComposerSearch;
