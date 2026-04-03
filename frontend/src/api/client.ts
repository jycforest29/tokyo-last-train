import type { StationSearchResponse, LastTrainResponse } from '../types';

export async function searchStations(
  query: string,
  signal?: AbortSignal
): Promise<StationSearchResponse> {
  const res = await fetch(
    `/api/v1/stations/search?query=${encodeURIComponent(query)}`,
    { signal }
  );
  if (!res.ok) throw new Error(`Station search failed: ${res.status}`);
  return res.json();
}

export async function findLastTrain(
  from: string,
  to: string,
  signal?: AbortSignal
): Promise<LastTrainResponse> {
  const res = await fetch(
    `/api/v1/last-train?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
    { signal }
  );
  if (!res.ok) throw new Error(`Last train search failed: ${res.status}`);
  return res.json();
}
