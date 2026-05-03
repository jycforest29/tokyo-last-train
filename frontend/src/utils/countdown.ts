/**
 * 도쿄 시간 기준으로 "HH:MM" 출발까지 남은 분을 계산한다.
 * 새벽 시간(24~28시 표기)도 자정 다음 날로 자연스럽게 처리.
 */
export function minutesUntilDeparture(departureTime: string, now: Date = new Date()): number {
  const [h, m] = departureTime.split(':').map(Number);
  const depMinutes = h * 60 + m;

  const tokyoNowStr = now.toLocaleString('en-US', {
    timeZone: 'Asia/Tokyo',
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
  // "HH:MM:SS" 또는 "24:MM:SS"가 일부 환경에서 나오므로 첫 두 토큰만 사용
  const [nh, nm] = tokyoNowStr.replace(/[^0-9:]/g, '').split(':').map(Number);
  const nowMinutes = (nh % 24) * 60 + nm;

  let diff = depMinutes - nowMinutes;
  // 출발 시각이 새벽(0~3시)이고 현재가 저녁이면 diff가 음수 → 다음 날로 보정
  if (diff < -180) diff += 24 * 60;
  return diff;
}
