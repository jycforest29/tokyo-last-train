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
  hasTimetable: boolean;
}

export interface StationSearchResponse {
  stations: StationInfo[];
}

export interface Transfer {
  stationNameJa: string;
  stationNameEn: string;
  stationNameKo: string | null;
  fromRailway: string;
  fromRailwayNameJa: string | null;
  fromRailwayNameEn: string | null;
  fromRailwayNameKo: string | null;
  toRailway: string;
  toRailwayNameJa: string | null;
  toRailwayNameEn: string | null;
  toRailwayNameKo: string | null;
  arrivalTime: string | null;
  departureTime: string | null;
  waitMinutes: number;
  fromPlatform: string | null;
  toPlatform: string | null;
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
  totalFareTicket: number;
  delay: DelayInfo | null;
}

export interface DelayInfo {
  statusJa: string | null;
  statusEn: string | null;
  textJa: string | null;
  textEn: string | null;
  isDisruption: boolean;
}

export interface Alternative {
  stationId: string;
  stationNameJa: string;
  stationNameEn: string;
  stationNameKo: string | null;
  offsetFromDest: number;
  routes: LastTrainRoute[];
}

export interface TaxiEstimate {
  yenDay: number;
  yenNight: number;
  distanceKm: number;
  nightSurchargeNow: boolean;
}

export interface LastTrainResponse {
  fromStation: string;
  toStation: string;
  calendarType: string;
  routes: LastTrainRoute[];
  alternatives: Alternative[];
  taxiEstimate: TaxiEstimate | null;
}

