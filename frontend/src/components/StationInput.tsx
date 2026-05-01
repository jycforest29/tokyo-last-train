import { useState, useRef, useEffect } from 'react';
import type { StationInfo } from '../types';
import { useTranslation } from '../i18n/LanguageContext';
import { getRailwayColor } from '../utils/format';
import './StationInput.css';

interface Props {
  label: string;
  placeholder: string;
  query: string;
  setQuery: (q: string) => void;
  results: StationInfo[];
  isLoading: boolean;
  selectedStation: StationInfo | null;
  selectStation: (s: StationInfo) => void;
  clearSelection: () => void;
}

export function StationInput({
  label, placeholder, query, setQuery,
  results, isLoading, selectedStation,
  selectStation, clearSelection,
}: Props) {
  const { stationName, stationNameSecondary, railwayName } = useTranslation();
  const [activeIndex, setActiveIndex] = useState(-1);
  const [isOpen, setIsOpen] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  useEffect(() => {
    setIsOpen(results.length > 0 && !selectedStation);
    setActiveIndex(-1);
  }, [results, selectedStation]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) return;
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setActiveIndex(i => Math.min(i + 1, results.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setActiveIndex(i => Math.max(i - 1, 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (activeIndex >= 0 && activeIndex < results.length) {
          selectStation(results[activeIndex]);
        }
        break;
      case 'Escape':
        setIsOpen(false);
        break;
    }
  };

  useEffect(() => {
    if (activeIndex >= 0 && listRef.current) {
      const item = listRef.current.children[activeIndex] as HTMLElement;
      item?.scrollIntoView({ block: 'nearest' });
    }
  }, [activeIndex]);

  if (selectedStation) {
    const primary = stationName(selectedStation);
    const secondary = stationNameSecondary(selectedStation);
    return (
      <div className="station-input-wrapper">
        <label className="station-label">{label}</label>
        <div className="station-chip">
          <span
            className="station-chip-dot"
            style={{ backgroundColor: getRailwayColor(selectedStation.railway) }}
          />
          <span className="station-chip-name">
            {primary}
            {secondary && secondary !== primary && (
              <span className="station-chip-name-en">{secondary}</span>
            )}
          </span>
          <span className="station-chip-line">{railwayName(selectedStation)}</span>
          <button
            type="button"
            className="station-chip-clear"
            onClick={clearSelection}
            aria-label="Clear"
          >&times;</button>
        </div>
      </div>
    );
  }

  return (
    <div className="station-input-wrapper">
      <label className="station-label">{label}</label>
      <div className="station-input-container">
        <input
          ref={inputRef}
          className="station-input"
          type="text"
          value={query}
          onChange={e => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => results.length > 0 && setIsOpen(true)}
          onBlur={() => setTimeout(() => setIsOpen(false), 200)}
          placeholder={placeholder}
          autoComplete="off"
        />
        {isLoading && <span className="station-input-spinner" />}
      </div>
      {isOpen && (
        <ul className="station-dropdown" ref={listRef} role="listbox">
          {results.map((station, i) => {
            const primary = stationName(station);
            const secondary = stationNameSecondary(station);
            return (
              <li
                key={station.stationId}
                className={`station-dropdown-item ${i === activeIndex ? 'active' : ''}`}
                onMouseDown={() => selectStation(station)}
                role="option"
                aria-selected={i === activeIndex}
              >
                <span
                  className="station-dropdown-dot"
                  style={{ backgroundColor: getRailwayColor(station.railway) }}
                />
                <div className="station-dropdown-info">
                  <div className="station-dropdown-name">
                    {primary}
                    {secondary && secondary !== primary && (
                      <span className="station-dropdown-name-en">{secondary}</span>
                    )}
                  </div>
                  <div className="station-dropdown-line">
                    {railwayName(station)}
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
