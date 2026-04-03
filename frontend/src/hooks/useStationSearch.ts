import { useState, useEffect, useRef, useCallback } from 'react';
import type { StationInfo } from '../types';
import { searchStations } from '../api/client';

export function useStationSearch() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<StationInfo[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedStation, setSelectedStation] = useState<StationInfo | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    if (selectedStation || query.length < 1) {
      setResults([]);
      return;
    }

    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(async () => {
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;

      setIsLoading(true);
      try {
        const res = await searchStations(query, controller.signal);
        setResults(res.stations);
      } catch (e) {
        if (e instanceof DOMException && e.name === 'AbortError') return;
        setResults([]);
      } finally {
        setIsLoading(false);
      }
    }, 300);

    return () => {
      clearTimeout(timerRef.current);
      abortRef.current?.abort();
    };
  }, [query, selectedStation]);

  const selectStation = useCallback((station: StationInfo) => {
    setSelectedStation(station);
    setQuery(station.nameEn || station.nameJa);
    setResults([]);
  }, []);

  const clearSelection = useCallback(() => {
    setSelectedStation(null);
    setQuery('');
    setResults([]);
  }, []);

  const setStationDirectly = useCallback((station: StationInfo) => {
    setSelectedStation(station);
    setQuery(station.nameEn || station.nameJa);
  }, []);

  return { query, setQuery, results, isLoading, selectedStation, selectStation, clearSelection, setStationDirectly };
}
