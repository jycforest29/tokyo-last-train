import type { LastTrainRoute } from '../types';
import { getRailwayColor, formatTime, formatFare } from '../utils/format';
import { useTranslation } from '../i18n/LanguageContext';
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

  return (
    <div className="route-card" style={{ borderLeftColor: color }}>
      <div className="route-header">
        <span className="route-index">#{index + 1}</span>
        {route.trainType && (
          <span className="route-train-type">{route.trainType}</span>
        )}
        <span className="route-fare">{formatFare(route.totalFare)}</span>
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
    </div>
  );
}
