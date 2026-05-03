import { useEffect, useState } from 'react';
import { Header } from './components/Header';
import { SearchForm } from './components/SearchForm';
import { QuickLaunch } from './components/QuickLaunch';
import { RouteList } from './components/RouteList';
import { LoadingSpinner } from './components/LoadingSpinner';
import { Footer } from './components/Footer';
import { PrivacyPage } from './pages/PrivacyPage';
import { useLastTrain } from './hooks/useLastTrain';
import './App.css';

function useCurrentPath(): string {
  const [path, setPath] = useState(() =>
    typeof window !== 'undefined' ? window.location.pathname : '/'
  );
  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname);
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, []);
  return path;
}

export default function App() {
  const path = useCurrentPath();
  const { result, isLoading, error, search } = useLastTrain();

  if (path === '/privacy') {
    return <PrivacyPage />;
  }

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
