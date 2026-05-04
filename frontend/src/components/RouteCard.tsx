import type { LastTrainRoute } from '../types';
import { getRailwayColor, formatTime, formatFare } from '../utils/format';
import { useTranslation } from '../i18n/LanguageContext';
import { useCountdown } from '../hooks/useCountdown';
import { useLastTrainAlert } from '../hooks/useLastTrainAlert';
import { TransferStep } from './TransferStep';
import './RouteCard.css';

interface Props {
  route: LastTrainRoute;
  index: number;
}

export function RouteCard({ route, index }: Props) {
  const { t, railwayName, destinationName, language } = useTranslation();
  const dep = formatTime(route.departureTime);
  const arr = formatTime(route.arrivalTime);
  const color = getRailwayColor(route.railway);
  const lineName = railwayName(route);
  const destName = destinationName(route);
  const nextDayLabel = language === 'ja' ? '翌日' : language === 'ko' ? '익일' : 'next day';

  const minutesLeft = useCountdown(route.departureTime);
  const { status, schedule, cancel } = useLastTrainAlert(route.departureTime);

  const countdownLabel = formatCountdown(minutesLeft, t);
  const countdownTone = toneForMinutes(minutesLeft);

  const handleNotify = (minutesBefore: number) => {
    const body = format(t('alert.notifBody'), {
      line: lineName,
      dest: destName ?? '',
      time: dep.display,
      n: String(minutesBefore),
    });
    void schedule(minutesBefore, t('app.title'), body);
  };

  const alertMessage = (() => {
    switch (status.kind) {
      case 'scheduled':
        return `${t('alert.scheduled')} · ${t('alert.keepTabOpen')}`;
      case 'denied':
        return t('alert.denied');
      case 'too-late':
        return t('alert.tooLate');
      case 'unsupported':
        return null;
      default:
        return null;
    }
  })();

  return (
    <div className="route-card" style={{ borderLeftColor: color }}>
      <div className="route-header">
        <span className="route-index">#{index + 1}</span>
        {route.trainType && (
          <span className="route-train-type">{route.trainType}</span>
        )}
        <span
          className={`route-countdown route-countdown--${countdownTone}`}
          aria-live="polite"
          aria-atomic="true"
        >{countdownLabel}</span>
        <span className="route-fare">
          <span className="route-fare-ic">
            <span className="route-fare-tag">{t('route.fareIc')}</span>{formatFare(route.totalFare)}
          </span>
          {route.totalFareTicket > 0 && route.totalFareTicket !== route.totalFare && (
            <span className="route-fare-ticket">
              <span className="route-fare-tag">{t('route.fareTicket')}</span>{formatFare(route.totalFareTicket)}
            </span>
          )}
        </span>
      </div>

      <div className="route-times">
        <div className="route-time-block">
          <span className="route-time">{dep.display}</span>
          {dep.isNextDay && <span className="next-day">{nextDayLabel}</span>}
          <span className="route-time-label">{t('route.depart')}</span>
        </div>
        <div className="route-arrow">
          <svg width="32" height="16" viewBox="0 0 32 16">
            <line x1="0" y1="8" x2="26" y2="8" stroke={color} strokeWidth="2" />
            <polygon points="24,3 32,8 24,13" fill={color} />
          </svg>
        </div>
        <div className="route-time-block">
          <span className="route-time">{arr.display}</span>
          {arr.isNextDay && <span className="next-day">{nextDayLabel}</span>}
          <span className="route-time-label">{t('route.arrive')}</span>
        </div>
      </div>

      <div className="route-line-info">
        <span className="route-line-dot" style={{ backgroundColor: color }} />
        <span className="route-line-name">{lineName}</span>
      </div>

      {route.delay && route.delay.isDisruption && (
        <div className="route-delay" role="alert">
          <span className="route-delay-icon">⚠</span>
          <span className="route-delay-text">
            {language === 'en'
              ? (route.delay.textEn ?? route.delay.textJa ?? '')
              : (route.delay.textJa ?? route.delay.textEn ?? '')}
          </span>
        </div>
      )}

      {destName && (
        <div className="route-destination">
          {destName} {t('route.bound')}
        </div>
      )}

      {route.transfers.length > 0 && (
        <div className="route-transfers">
          <div className="route-transfers-title">
            {language === 'ja'
              ? `乗換 ${route.transfers.length}回`
              : language === 'ko'
              ? `${route.transfers.length}회 환승`
              : `${route.transfers.length} transfer${route.transfers.length > 1 ? 's' : ''}`}
          </div>
          {route.transfers.map((tr, i) => (
            <TransferStep key={i} transfer={tr} />
          ))}
        </div>
      )}

      {minutesLeft > 5 && (
        <div className="route-alert">
          <span className="route-alert-label">🔔 {t('alert.title')}</span>
          {[30, 15, 5].map((mins) => {
            const disabled = minutesLeft <= mins;
            const isSelected = status.kind === 'scheduled' && status.minutesBefore === mins;
            return (
              <button
                key={mins}
                type="button"
                className={`route-alert-btn${isSelected ? ' is-active' : ''}`}
                disabled={disabled}
                onClick={() => (isSelected ? cancel() : handleNotify(mins))}
              >
                {t(mins === 30 ? 'alert.notify30' : mins === 15 ? 'alert.notify15' : 'alert.notify5')}
              </button>
            );
          })}
          {alertMessage && <span className="route-alert-msg">{alertMessage}</span>}
        </div>
      )}
    </div>
  );
}

function formatCountdown(minutes: number, t: (k: 'countdown.departed' | 'countdown.soon' | 'countdown.minutes' | 'countdown.hourMinutes') => string): string {
  if (minutes < 0) return t('countdown.departed');
  if (minutes < 1) return t('countdown.soon');
  if (minutes < 60) return format(t('countdown.minutes'), { n: String(minutes) });
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return format(t('countdown.hourMinutes'), { h: String(h), m: String(m) });
}

function toneForMinutes(minutes: number): 'gone' | 'urgent' | 'soon' | 'ok' {
  if (minutes < 0) return 'gone';
  if (minutes <= 10) return 'urgent';
  if (minutes <= 30) return 'soon';
  return 'ok';
}

function format(template: string, vars: Record<string, string>): string {
  return template.replace(/\{(\w+)\}/g, (_, k) => vars[k] ?? '');
}
