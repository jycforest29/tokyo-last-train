import { useCallback, useEffect, useState } from 'react';
import type { StationInfo } from '../types';

const STORAGE_KEY = 'tokyo-last-train.home';

/**
 * 자택 즐겨찾기. localStorage에만 저장되며 서버로 전송되지 않는다.
 * 다른 탭에서 변경 시 storage 이벤트로 동기화한다.
 */
export function useHomeFavorite() {
  const [home, setHomeState] = useState<StationInfo | null>(() => readHome());

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === STORAGE_KEY) {
        setHomeState(readHome());
      }
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const setHome = useCallback((station: StationInfo) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(station));
      setHomeState(station);
    } catch {
      /* storage full / disabled — silently ignore */
    }
  }, []);

  const clearHome = useCallback(() => {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch { /* ignore */ }
    setHomeState(null);
  }, []);

  return { home, setHome, clearHome };
}

function readHome(): StationInfo | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    // 최소 필드 검증 — 손상된 항목은 무시
    if (parsed && typeof parsed.stationId === 'string') {
      return parsed as StationInfo;
    }
    return null;
  } catch {
    return null;
  }
}
