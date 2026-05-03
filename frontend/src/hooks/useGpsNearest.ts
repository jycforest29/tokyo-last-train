import { useCallback, useState } from 'react';
import type { StationInfo } from '../types';
import { findNearestStation } from '../api/client';

export type GpsStatus =
  | { kind: 'idle' }
  | { kind: 'locating' }
  | { kind: 'denied' }
  | { kind: 'unsupported' }
  | { kind: 'error' }
  | { kind: 'success'; station: StationInfo };

/**
 * 브라우저 GPS로 현재 위치를 받고 가장 가까운 역을 백엔드에서 조회한다.
 * 좌표는 fetch 호출 직전까지 메모리에만 머무르며, hook 외부로 노출되지 않는다.
 */
export function useGpsNearest() {
  const [status, setStatus] = useState<GpsStatus>({ kind: 'idle' });

  const locate = useCallback((): Promise<StationInfo | null> => {
    if (typeof window === 'undefined' || !navigator.geolocation) {
      setStatus({ kind: 'unsupported' });
      return Promise.resolve(null);
    }
    setStatus({ kind: 'locating' });
    return new Promise<StationInfo | null>((resolve) => {
      navigator.geolocation.getCurrentPosition(
        async (pos) => {
          try {
            const station = await findNearestStation(
              pos.coords.latitude,
              pos.coords.longitude
            );
            setStatus({ kind: 'success', station });
            resolve(station);
          } catch {
            setStatus({ kind: 'error' });
            resolve(null);
          }
        },
        (err) => {
          setStatus(err.code === err.PERMISSION_DENIED ? { kind: 'denied' } : { kind: 'error' });
          resolve(null);
        },
        { enableHighAccuracy: false, timeout: 10_000, maximumAge: 60_000 }
      );
    });
  }, []);

  const reset = useCallback(() => setStatus({ kind: 'idle' }), []);

  return { status, locate, reset };
}
