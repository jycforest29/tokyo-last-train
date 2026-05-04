import { useStationSearch } from '../hooks/useStationSearch';
import { useHomeFavorite } from '../hooks/useHomeFavorite';
import { useGpsNearest } from '../hooks/useGpsNearest';
import { useTranslation } from '../i18n/LanguageContext';
import { StationInput } from './StationInput';
import './SearchForm.css';

interface Props {
  onSearch: (from: string, to: string) => void;
  isLoading: boolean;
}

export function SearchForm({ onSearch, isLoading }: Props) {
  const { t, stationName } = useTranslation();
  const fromSearch = useStationSearch();
  const toSearch = useStationSearch();
  const { home, setHome, clearHome } = useHomeFavorite();
  const gps = useGpsNearest();
  const destination = toSearch.selectedStation;
  const isHome = !!destination && !!home && home.stationId === destination.stationId;

  const gpsErrorKey =
    gps.status.kind === 'denied' ? 'gps.denied'
    : gps.status.kind === 'unsupported' ? 'gps.unsupported'
    : gps.status.kind === 'error' ? 'gps.error'
    : null;

  const handleGpsClick = async () => {
    const station = await gps.locate();
    if (station) {
      fromSearch.setStationDirectly(station);
    }
  };

  const canSearch = fromSearch.selectedStation && toSearch.selectedStation && !isLoading;
  const submitDisabledHint = canSearch ? undefined : t('search.disabledHint');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (fromSearch.selectedStation && toSearch.selectedStation) {
      onSearch(fromSearch.selectedStation.stationId, toSearch.selectedStation.stationId);
    }
  };

  const handleSwap = () => {
    const fromStation = fromSearch.selectedStation;
    const toStation = toSearch.selectedStation;
    if (fromStation && toStation) {
      fromSearch.setStationDirectly(toStation);
      toSearch.setStationDirectly(fromStation);
    }
  };

  const handleHomeToggle = () => {
    if (!destination) return;
    if (isHome) clearHome();
    else setHome(destination);
  };

  return (
    <form className="search-form" onSubmit={handleSubmit}>
      <StationInput
        label={t('search.from')}
        placeholder={t('search.placeholder')}
        query={fromSearch.query}
        setQuery={fromSearch.setQuery}
        results={fromSearch.results}
        isLoading={fromSearch.isLoading}
        selectedStation={fromSearch.selectedStation}
        selectStation={fromSearch.selectStation}
        clearSelection={fromSearch.clearSelection}
      />

      {!fromSearch.selectedStation && (
        <button
          type="button"
          className="gps-button"
          onClick={handleGpsClick}
          disabled={gps.status.kind === 'locating'}
        >
          {gps.status.kind === 'locating' ? t('gps.locating') : t('gps.useCurrent')}
        </button>
      )}
      {gpsErrorKey && (
        <p className="gps-error">{t(gpsErrorKey)}</p>
      )}

      <button
        type="button"
        className="swap-button"
        onClick={handleSwap}
        disabled={!fromSearch.selectedStation || !toSearch.selectedStation}
        aria-label={t('search.swap')}
      >
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <path d="M6 4v12M6 16l-3-3M6 16l3-3M14 16V4M14 4l-3 3M14 4l3 3"
            stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      <StationInput
        label={t('search.to')}
        placeholder={t('search.placeholder')}
        query={toSearch.query}
        setQuery={toSearch.setQuery}
        results={toSearch.results}
        isLoading={toSearch.isLoading}
        selectedStation={toSearch.selectedStation}
        selectStation={toSearch.selectStation}
        clearSelection={toSearch.clearSelection}
      />

      {destination && (
        <button
          type="button"
          className={`home-toggle${isHome ? ' is-active' : ''}`}
          onClick={handleHomeToggle}
        >
          {isHome
            ? t('home.unset')
            : t('home.set').replace('{station}', stationName(destination))}
        </button>
      )}

      <button
        type="submit"
        className="search-button"
        disabled={!canSearch}
        title={submitDisabledHint}
        aria-label={submitDisabledHint ?? t('search.submit')}
      >
        {isLoading ? (
          <span className="search-button-loading" />
        ) : (
          <>{t('search.submit')}</>
        )}
      </button>
    </form>
  );
}
