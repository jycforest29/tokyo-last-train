import { useCallback, useEffect, useRef, useState } from 'react';
import { minutesUntilDeparture } from '../utils/countdown';

export type AlertStatus =
  | { kind: 'idle' }
  | { kind: 'scheduled'; minutesBefore: number; firesAt: number }
  | { kind: 'denied' }
  | { kind: 'unsupported' }
  | { kind: 'too-late' };

/**
 * 출발 X분 전에 브라우저 알림을 띄우는 훅.
 * 탭이 열려 있어야 알림이 발화한다 (서비스워커 푸시 미사용).
 */
export function useLastTrainAlert(departureTime: string) {
  const [status, setStatus] = useState<AlertStatus>({ kind: 'idle' });
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const cancel = useCallback(() => {
    if (timerRef.current !== null) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    setStatus({ kind: 'idle' });
  }, []);

  useEffect(() => () => cancel(), [cancel]);

  const schedule = useCallback(
    async (minutesBefore: number, title: string, body: string) => {
      if (typeof window === 'undefined' || !('Notification' in window)) {
        setStatus({ kind: 'unsupported' });
        return;
      }

      const minsUntil = minutesUntilDeparture(departureTime);
      const delayMinutes = minsUntil - minutesBefore;
      if (delayMinutes <= 0) {
        setStatus({ kind: 'too-late' });
        return;
      }

      let perm = Notification.permission;
      if (perm === 'default') {
        perm = await Notification.requestPermission();
      }
      if (perm !== 'granted') {
        setStatus({ kind: 'denied' });
        return;
      }

      if (timerRef.current !== null) clearTimeout(timerRef.current);
      const delayMs = delayMinutes * 60 * 1000;
      const firesAt = Date.now() + delayMs;
      timerRef.current = setTimeout(() => {
        try {
          new Notification(title, { body, tag: `last-train-${departureTime}`, requireInteraction: true });
        } catch {
          /* iOS Safari 등 일부 환경에서 throw — 무시 */
        }
        setStatus({ kind: 'idle' });
        timerRef.current = null;
      }, delayMs);
      setStatus({ kind: 'scheduled', minutesBefore, firesAt });
    },
    [departureTime]
  );

  return { status, schedule, cancel };
}
