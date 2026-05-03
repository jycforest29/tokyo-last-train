import { Header } from './components/Header';
import { SearchForm } from './components/SearchForm';
import { QuickLaunch } from './components/QuickLaunch';
import { RouteList } from './components/RouteList';
import { LoadingSpinner } from './components/LoadingSpinner';
import { Footer } from './components/Footer';
import { useLastTrain } from './hooks/useLastTrain';
import './App.css';

export default function App() {
  const { result, isLoading, error, search } = useLastTrain();

  return (
    <div className="app">
      <div className="app-container">
        <Header />
        <QuickLaunch onSearch={search} isLoading={isLoading} />
        <SearchForm onSearch={search} isLoading={isLoading} />
        {isLoading && <LoadingSpinner />}
        {error && (
          <div className="error-message">
            <p>{error}</p>
          </div>
        )}
        <RouteList result={result} />
        <Footer />
      </div>
    </div>
  );
}
