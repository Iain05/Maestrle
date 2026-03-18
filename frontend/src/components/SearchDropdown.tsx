import React, { useState, useRef, useEffect } from 'react';
import { Search, ChevronDown, X } from 'lucide-react';

interface SearchDropdownProps<T> {
  items: T[];
  getKey: (item: T) => string | number;
  getLabel: (item: T) => string;
  value: string;
  onChange: (value: string) => void;
  onSelect: (item: T) => void;
  onClear?: () => void;
  selected?: boolean;
  disabled?: boolean;
  placeholder?: string;
  icon?: 'search' | 'chevron';
  id?: string;
  label?: string;
}

function SearchDropdown<T>({
  items,
  getKey,
  getLabel,
  value,
  onChange,
  onSelect,
  onClear,
  selected = false,
  disabled = false,
  placeholder = 'Search…',
  icon = 'search',
  id,
  label,
}: SearchDropdownProps<T>) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const filtered = value.trim()
    ? items.filter(item => getLabel(item).toLowerCase().includes(value.toLowerCase()))
    : items;

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleInput(e: React.ChangeEvent<HTMLInputElement>) {
    onChange(e.target.value);
    setOpen(true);
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && filtered.length > 0 && !disabled) {
      e.preventDefault();
      handleSelect(filtered[0]);
    }
  }

  function handleSelect(item: T) {
    onChange(getLabel(item));
    onSelect(item);
    setOpen(false);
  }

  function handleClear() {
    onClear?.();
    setOpen(false);
  }

  const showDropdown = open && filtered.length > 0 && !disabled;

  return (
    <div className="relative" ref={containerRef}>
      {label && (
        <label htmlFor={id} className="sr-only">
          {label}
        </label>
      )}
      <input
        id={id}
        type="text"
        value={value}
        onChange={handleInput}
        onKeyDown={handleKeyDown}
        onFocus={() => !disabled && setOpen(true)}
        disabled={disabled}
        placeholder={placeholder}
        autoComplete="off"
        className="w-full px-4 py-3 bg-surface text-ink placeholder:text-ink-subtle border-2 border-border rounded-xl focus:border-primary focus:outline-none transition-all pr-10 disabled:opacity-40 disabled:cursor-not-allowed disabled:bg-surface-warm"
      />

      {selected && onClear ? (
        <button
          type="button"
          onMouseDown={handleClear}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-subtle hover:text-ink transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      ) : icon === 'chevron' ? (
        <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-ink-subtle pointer-events-none" />
      ) : (
        <Search className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-ink-subtle pointer-events-none" />
      )}

      {showDropdown && (
        <div className="absolute z-50 w-full mt-2 bg-surface border border-border rounded-xl shadow-xl max-h-48 overflow-y-auto custom-scrollbar">
          {filtered.map(item => (
            <button
              key={getKey(item)}
              type="button"
              onMouseDown={() => handleSelect(item)}
              className="w-full text-left px-4 py-2 hover:bg-canvas text-ink text-sm border-b border-border last:border-0"
            >
              {getLabel(item)}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default SearchDropdown;
