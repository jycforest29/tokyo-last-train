import { useState, useCallback } from 'react';
import type { LastTrainResponse } from '../types';
import { findLastTrain } from '../api/client';

export function useLastTrain() {
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
    } catch {
      setError('경로 검색에 실패했습니다. 다시 시도해 주세요.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clear = useCallback(() => {
    setResult(null);
    setError(null);
  }, []);

  return { result, isLoading, error, search, clear };
}
