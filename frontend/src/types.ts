export interface StationInfo {
  stationId: string;
  nameJa: string;
  nameEn: string;
  railway: string;
  railwayNameJa: string;
  railwayNameEn: string;
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
  railDirection: string;
  trainType: string;
  destinationNameJa: string;
  destinationNameEn: string;
  transfers: Transfer[];
  totalFare: number;
}

export interface LastTrainResponse {
  fromStation: string;
  toStation: string;
  calendarType: string;
  routes: LastTrainRoute[];
}
