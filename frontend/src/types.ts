export interface StationInfo {
  stationId: string;
  nameJa: string;
  nameEn: string;
  nameKo: string | null;
  railway: string;
  railwayNameJa: string;
  railwayNameEn: string;
  railwayNameKo: string | null;
  operator: string;
  latitude: number;
  longitude: number;
}

export interface StationSearchResponse {
  stations: StationInfo[];
}

export interface Transfer {
  stationNameJa: string;
  stationNameEn: string;
  stationNameKo: string | null;
  fromRailway: string;
  toRailway: string;
  departureTime: string;
}

export interface LastTrainRoute {
  departureTime: string;
  arrivalTime: string;
  railway: string;
  railwayNameJa: string;
  railwayNameEn: string;
  railwayNameKo: string | null;
  railDirection: string;
  trainType: string;
  destinationNameJa: string;
  destinationNameEn: string;
  destinationNameKo: string | null;
  transfers: Transfer[];
  totalFare: number;
}

export interface LastTrainResponse {
  fromStation: string;
  toStation: string;
  calendarType: string;
  routes: LastTrainRoute[];
}
