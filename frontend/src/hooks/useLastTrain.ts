import { useState, useCallback } from 'react';
import type { LastTrainResponse } from '../types';
import { findLastTrain } from '../api/client';
import { useTranslation } from '../i18n/LanguageContext';

export function useLastTrain() {
  const { t } = useTranslation();
  const [result, setResult] = useState<LastTrainResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const search = useCallback(async (from: string, to: string) => {
    setIsLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await findLastTrain(from, to);
      setResult(res);
      window.gtag?.('event', 'last_train_search', {
        from_station: from,
        to_station: to,
        route_count: res.routes.length,
      });
    } catch {
      setError(t('error.searchFailed'));
    } finally {
      setIsLoading(false);
    }
  }, [t]);

  const clear = useCallback(() => {
    setResult(null);
    setError(null);
  }, []);

  return { result, isLoading, error, search, clear };
}
