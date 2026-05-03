import { useHomeFavorite } from '../hooks/useHomeFavorite';
import { useGpsNearest } from '../hooks/useGpsNearest';
import { useTranslation } from '../i18n/LanguageContext';
import './QuickLaunch.css';

interface Props {
  onSearch: (from: string, to: string) => void;
  isLoading: boolean;
}

/**
 * 자택이 등록된 사용자에게만 노출되는 한 번-클릭 막차 검색 버튼.
 * GPS로 가장 가까운 역을 받고, 자택역을 도착지로 묶어서 즉시 검색을 트리거한다.
 */
export function QuickLaunch({ onSearch, isLoading }: Props) {
  const { t, stationName } = useTranslation();
  const { home } = useHomeFavorite();
  const gps = useGpsNearest();

  if (!home) return null;

  const errorKey =
    gps.status.kind === 'denied' ? 'gps.denied'
    : gps.status.kind === 'unsupported' ? 'gps.unsupported'
    : gps.status.kind === 'error' ? 'gps.error'
    : null;

  const handleClick = async () => {
    const from = await gps.locate();
    if (from) {
      onSearch(from.stationId, home.stationId);
    }
  };

  const label = t('quickLaunch.cta').replace('{home}', stationName(home));
  const isLocating = gps.status.kind === 'locating';

  return (
    <div className="quick-launch">
      <button
        type="button"
        className="quick-launch-button"
        disabled={isLocating || isLoading}
        onClick={handleClick}
      >
        {isLocating ? t('gps.locating') : label}
      </button>
      <p className="quick-launch-subtitle">{t('quickLaunch.subtitle')}</p>
      {errorKey && <p className="quick-launch-error">{t(errorKey)}</p>}
    </div>
  );
}
