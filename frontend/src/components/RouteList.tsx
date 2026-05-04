import type { LastTrainResponse } from '../types';
import { RouteCard } from './RouteCard';
import { useTranslation } from '../i18n/LanguageContext';
import './RouteList.css';

interface Props {
  result: LastTrainResponse | null;
}

export function RouteList({ result }: Props) {
  const { t, language } = useTranslation();
  if (!result) return null;

  const calendarLabel =
    result.calendarType === 'Weekday' ? t('result.weekday')
    : result.calendarType === 'Saturday' ? t('result.saturday')
    : t('result.holiday');

  const routeCountText =
    result.routes.length === 0 ? '' :
    language === 'ja' ? `${result.routes.length}件の経路`
    : language === 'ko' ? `${result.routes.length}개 경로`
    : `${result.routes.length} route${result.routes.length > 1 ? 's' : ''}`;

  return (
    <div className="route-list">
      <div className="route-list-header">
        <span className="calendar-badge">{calendarLabel}</span>
        <span className="route-count">{routeCountText}</span>
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
          <p className="no-routes-text">{t('result.noRoutes')}</p>
        </div>
      ) : (
        result.routes.map((route, i) => (
          <RouteCard key={i} route={route} index={i} />
        ))
      )}

      {result.alternatives && result.alternatives.length > 0 && (
        <Alternatives alternatives={result.alternatives} />
      )}

      {result.taxiEstimate && <TaxiHint estimate={result.taxiEstimate} />}
    </div>
  );
}

import type { Alternative, TaxiEstimate } from '../types';

function TaxiHint({ estimate }: { estimate: TaxiEstimate }) {
  const { t } = useTranslation();
  return (
    <div className="taxi-hint">
      <div className="taxi-hint-heading">{t('taxi.heading')}</div>
      <div className="taxi-hint-fare">
        {t('taxi.estimate')
          .replace('{day}', estimate.yenDay.toLocaleString())
          .replace('{night}', estimate.yenNight.toLocaleString())}
      </div>
      <div className="taxi-hint-distance">
        {t('taxi.distance').replace('{km}', estimate.distanceKm.toFixed(1))}
      </div>
      {estimate.nightSurchargeNow && (
        <div className="taxi-hint-night">{t('taxi.nightNote')}</div>
      )}
    </div>
  );
}

function Alternatives({ alternatives }: { alternatives: Alternative[] }) {
  const { t, language } = useTranslation();
  const stationName = (a: Alternative) =>
    language === 'ko' ? (a.stationNameKo ?? a.stationNameJa)
    : language === 'en' ? a.stationNameEn
    : a.stationNameJa;

  return (
    <div className="alternatives">
      <h3 className="alternatives-heading">{t('alternatives.heading')}</h3>
      {alternatives.map(alt => {
        const labelKey = alt.offsetFromDest < 0 ? 'alternatives.before' : 'alternatives.after';
        const label = t(labelKey).replace('{station}', stationName(alt));
        return (
          <div key={alt.stationId} className="alternative-block">
            <div className="alternative-label">{label}</div>
            {alt.routes.map((route, i) => (
              <RouteCard key={i} route={route} index={i} />
            ))}
          </div>
        );
      })}
    </div>
  );
}
