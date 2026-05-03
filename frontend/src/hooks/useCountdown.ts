import { useEffect, useState } from 'react';
import { minutesUntilDeparture } from '../utils/countdown';

/**
 * 출발 시각까지 남은 분을 반환. 30초마다 갱신.
 */
export function useCountdown(departureTime: string): number {
  const [minutes, setMinutes] = useState(() => minutesUntilDeparture(departureTime));

  useEffect(() => {
    setMinutes(minutesUntilDeparture(departureTime));
    const id = setInterval(() => {
      setMinutes(minutesUntilDeparture(departureTime));
    }, 30_000);
    return () => clearInterval(id);
  }, [departureTime]);

  return minutes;
}
