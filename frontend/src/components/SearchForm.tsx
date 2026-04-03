import { useStationSearch } from '../hooks/useStationSearch';
import { StationInput } from './StationInput';
import './SearchForm.css';

interface Props {
  onSearch: (from: string, to: string) => void;
  isLoading: boolean;
}

export function SearchForm({ onSearch, isLoading }: Props) {
  const fromSearch = useStationSearch();
  const toSearch = useStationSearch();

  const canSearch = fromSearch.selectedStation && toSearch.selectedStation && !isLoading;

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

  return (
    <form className="search-form" onSubmit={handleSubmit}>
      <StationInput
        label="출발역"
        placeholder="역 이름을 입력하세요"
        query={fromSearch.query}
        setQuery={fromSearch.setQuery}
        results={fromSearch.results}
        isLoading={fromSearch.isLoading}
        selectedStation={fromSearch.selectedStation}
        selectStation={fromSearch.selectStation}
        clearSelection={fromSearch.clearSelection}
      />

      <button
        type="button"
        className="swap-button"
        onClick={handleSwap}
        disabled={!fromSearch.selectedStation || !toSearch.selectedStation}
        aria-label="Swap stations"
      >
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <path d="M6 4v12M6 16l-3-3M6 16l3-3M14 16V4M14 4l-3 3M14 4l3 3"
            stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      <StationInput
        label="도착역"
        placeholder="역 이름을 입력하세요"
        query={toSearch.query}
        setQuery={toSearch.setQuery}
        results={toSearch.results}
        isLoading={toSearch.isLoading}
        selectedStation={toSearch.selectedStation}
        selectStation={toSearch.selectStation}
        clearSelection={toSearch.clearSelection}
      />

      <button
        type="submit"
        className="search-button"
        disabled={!canSearch}
      >
        {isLoading ? (
          <span className="search-button-loading" />
        ) : (
          <>막차 검색</>

        )}
      </button>
    </form>
  );
}
