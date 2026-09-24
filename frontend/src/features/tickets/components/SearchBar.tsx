import { useEffect, useState } from 'react';

interface SearchBarProps {
  value: string;
  onChange: (value: string) => void;
  debounceMs?: number;
}

export function SearchBar({ value, onChange, debounceMs = 300 }: SearchBarProps) {
  const [localValue, setLocalValue] = useState(value);

  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  useEffect(() => {
    const timer = window.setTimeout(() => onChange(localValue), debounceMs);
    return () => window.clearTimeout(timer);
  }, [localValue, debounceMs, onChange]);

  return (
    <input
      type="search"
      placeholder="Search tickets…"
      value={localValue}
      onChange={(e) => setLocalValue(e.target.value)}
      aria-label="Search tickets"
    />
  );
}
