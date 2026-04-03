import type { LastTrainRoute } from '../types';
import { getRailwayColor, formatTime, formatFare } from '../utils/format';
import { TransferStep } from './TransferStep';
import './RouteCard.css';

interface Props {
  route: LastTrainRoute;
  index: number;
}

export function RouteCard({ route, index }: Props) {
  const dep = formatTime(route.departureTime);
  const arr = formatTime(route.arrivalTime);
  const color = getRailwayColor(route.railway);

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
          {dep.isNextDay && <span className="next-day">익일</span>}
          <span className="route-time-label">출발</span>
        </div>
        <div className="route-arrow">
          <svg width="32" height="16" viewBox="0 0 32 16">
            <line x1="0" y1="8" x2="26" y2="8" stroke={color} strokeWidth="2" />
            <polygon points="24,3 32,8 24,13" fill={color} />
          </svg>
        </div>
        <div className="route-time-block">
          <span className="route-time">{arr.display}</span>
          {arr.isNextDay && <span className="next-day">익일</span>}
          <span className="route-time-label">도착</span>
        </div>
      </div>

      <div className="route-line-info">
        <span className="route-line-dot" style={{ backgroundColor: color }} />
        <span className="route-line-name">
          {route.railwayNameJa}
          {route.railwayNameEn && (
            <span className="route-line-name-en"> {route.railwayNameEn}</span>
          )}
        </span>
      </div>

      {route.destinationNameJa && (
        <div className="route-destination">
          행선지: {route.destinationNameJa}
          {route.destinationNameEn && (
            <span className="route-destination-en"> / {route.destinationNameEn}</span>
          )}
        </div>
      )}

      {route.transfers.length > 0 && (
        <div className="route-transfers">
          <div className="route-transfers-title">
            환승 {route.transfers.length}회
          </div>
          {route.transfers.map((t, i) => (
            <TransferStep key={i} transfer={t} />
          ))}
        </div>
      )}
    </div>
  );
}
