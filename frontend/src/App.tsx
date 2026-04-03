import { Header } from './components/Header';
import { SearchForm } from './components/SearchForm';
import { RouteList } from './components/RouteList';
import { LoadingSpinner } from './components/LoadingSpinner';
import { useLastTrain } from './hooks/useLastTrain';
import './App.css';

export default function App() {
  const { result, isLoading, error, search } = useLastTrain();

  return (
    <div className="app">
      <div className="app-container">
        <Header />
        <SearchForm onSearch={search} isLoading={isLoading} />
        {isLoading && <LoadingSpinner />}
        {error && (
          <div className="error-message">
            <p>{error}</p>
          </div>
        )}
        <RouteList result={result} />
      </div>
    </div>
  );
}
