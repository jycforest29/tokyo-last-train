package tokyo.lasttrain.service.impl;

import org.springframework.stereotype.Service;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.dto.LastTrainResponse;
import tokyo.lasttrain.dto.LastTrainResponse.LastTrainRoute;
import tokyo.lasttrain.dto.LastTrainResponse.Transfer;
import tokyo.lasttrain.model.OdptRailway;
import tokyo.lasttrain.model.OdptRailwayFare;
import tokyo.lasttrain.model.OdptStation;
import tokyo.lasttrain.model.OdptTrainType;
import tokyo.lasttrain.service.LastTrainService;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine.Journey;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine.Leg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LastTrainServiceImpl implements LastTrainService {

    private static final Logger log = LoggerFactory.getLogger(LastTrainServiceImpl.class);
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final List<String> CALENDAR_FALLBACKS = List.of(
            "odpt.Calendar:Weekday",
            "odpt.Calendar:SaturdayHoliday",
            "odpt.Calendar:Holiday"
    );

    private final ReverseRaptorEngine raptorEngine;
    private final TransitDataCache cache;

    public LastTrainServiceImpl(ReverseRaptorEngine raptorEngine, TransitDataCache cache) {
        this.raptorEngine = raptorEngine;
        this.cache = cache;
    }

    @Override
    public LastTrainResponse findLastTrain(String fromStationId, String toStationId) {
        LocalDate today = LocalDate.now(TOKYO);
        final String calendarId = resolveCalendarWithFallback(today, fromStationId, toStationId);

        List<Journey> journeys = raptorEngine.findLastTrains(fromStationId, toStationId, calendarId);

        List<LastTrainRoute> routes = journeys.stream()
                .map(j -> toRoute(j, calendarId))
                .toList();

        String calendarType = calendarId.contains("Weekday") ? "Weekday"
                : calendarId.contains("Saturday") ? "Saturday" : "Holiday";

        return new LastTrainResponse(fromStationId, toStationId, calendarType, routes);
    }

    private String resolveCalendarWithFallback(LocalDate date, String from, String to) {
        String resolved = cache.resolveCalendar(date);
        log.info("Resolved calendar: {} for date: {}", resolved, date);

        if (!raptorEngine.findLastTrains(from, to, resolved).isEmpty()) {
            return resolved;
        }

        for (String fallback : CALENDAR_FALLBACKS) {
            if (!fallback.equals(resolved)) {
                log.info("No routes with {}. Trying fallback: {}", resolved, fallback);
                if (!raptorEngine.findLastTrains(from, to, fallback).isEmpty()) {
                    return fallback;
                }
            }
        }

        return resolved;
    }

    private LastTrainRoute toRoute(Journey journey, String calendarId) {
        List<Leg> rideLegs = journey.legs().stream().filter(l -> !l.isTransfer()).toList();
        List<Leg> transferLegs = journey.legs().stream().filter(Leg::isTransfer).toList();

        Leg firstRide = rideLegs.getFirst();
        Leg lastRide = rideLegs.getLast();

        // 노선 정보
        String railwayNameJa = null;
        String railwayNameEn = null;
        OdptRailway railway = cache.getRailway(firstRide.railway());
        if (railway != null) {
            railwayNameJa = railway.title();
            railwayNameEn = getLocalizedName(railway.railwayTitle(), "en");
        }

        // 열차 종별
        String trainType = null;
        if (firstRide.trainType() != null) {
            OdptTrainType tt = cache.getTrainType(firstRide.trainType());
            trainType = tt != null ? getLocalizedName(tt.trainTypeTitle(), "en") : firstRide.trainType();
        }

        // 행선지 (마지막 ride leg의 도착역)
        String destNameJa = null;
        String destNameEn = null;
        OdptStation destStation = cache.getStation(lastRide.toStation());
        if (destStation != null) {
            destNameJa = destStation.title();
            destNameEn = getLocalizedName(destStation.stationTitle(), "en");
        }

        // 환승 정보
        List<Transfer> transfers = new ArrayList<>();
        for (Leg tl : transferLegs) {
            OdptStation ts = cache.getStation(tl.toStation());
            String tsNameJa = ts != null ? ts.title() : null;
            String tsNameEn = ts != null ? getLocalizedName(ts.stationTitle(), "en") : null;

            // 환승 전후 노선
            String fromRailway = findAdjacentRailway(journey.legs(), tl, true);
            String toRailway = findAdjacentRailway(journey.legs(), tl, false);

            transfers.add(new Transfer(
                    tsNameJa, tsNameEn,
                    fromRailway, toRailway,
                    tl.arrivalTime().toString()
            ));
        }

        // 요금 계산
        int totalFare = calculateFare(journey);

        return new LastTrainRoute(
                journey.departureTime().toString(),
                journey.arrivalTime().toString(),
                firstRide.railway(),
                railwayNameJa,
                railwayNameEn,
                firstRide.railDirection(),
                trainType,
                destNameJa,
                destNameEn,
                transfers,
                totalFare
        );
    }

    /**
     * 경로의 총 요금을 계산한다.
     * 같은 사업자 내에서는 통산, 사업자가 바뀌면 별도 요금.
     */
    private int calculateFare(Journey journey) {
        int total = 0;
        List<Leg> rideLegs = journey.legs().stream().filter(l -> !l.isTransfer()).toList();

        for (Leg leg : rideLegs) {
            OdptRailwayFare fare = cache.getFare(leg.fromStation(), leg.toStation());
            if (fare != null && fare.icCardFare() != null) {
                total += fare.icCardFare();
            } else if (fare != null && fare.ticketFare() != null) {
                total += fare.ticketFare();
            }
        }

        return total;
    }

    private String findAdjacentRailway(List<Leg> legs, Leg transferLeg, boolean before) {
        int idx = legs.indexOf(transferLeg);
        if (before && idx > 0) return legs.get(idx - 1).railway();
        if (!before && idx < legs.size() - 1) return legs.get(idx + 1).railway();
        return null;
    }

    private boolean isAfterMidnight(LocalTime time) {
        return time.isBefore(LocalTime.of(1, 30));
    }

    private String getLocalizedName(Map<String, String> titleMap, String lang) {
        if (titleMap == null) return null;
        return titleMap.get(lang);
    }
}