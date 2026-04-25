import type { LastTrainResponse } from '../types';
import { RouteCard } from './RouteCard';
import './RouteList.css';

interface Props {
  result: LastTrainResponse | null;
}

export function RouteList({ result }: Props) {
  if (!result) return null;

  return (
    <div className="route-list">
      <div className="route-list-header">
        <span className="calendar-badge">
          {result.calendarType === 'Weekday' ? '平日' : result.calendarType === 'Saturday' ? '土曜' : '休日'}
        </span>
        <span className="route-count">
          {result.routes.length > 0
            ? `${result.routes.length}件の経路`
            : ''}
        </span>
      </div>

      {result.routes.length === 0 ? (
        <div className="no-routes">
          <div className="no-routes-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="20" stroke="var(--text-secondary)" strokeWidth="2" />
              <path d="M16 28c2-3 6-5 8-5s6 2 8 5" stroke="var(--text-secondary)" strokeWidth="2" strokeLinecap="round" transform="scale(1,-1) translate(0,-56)" />
              <circle cx="18" cy="20" r="2" fill="var(--text-secondary)" />
              <circle cx="30" cy="20" r="2" fill="var(--text-secondary)" />
            </svg>
          </div>
          <p className="no-routes-text">終電が見つかりませんでした</p>
          <p className="no-routes-text-en">検索条件をご確認ください</p>
        </div>
      ) : (
        result.routes.map((route, i) => (
          <RouteCard key={i} route={route} index={i} />
        ))
      )}
    </div>
  );
}
