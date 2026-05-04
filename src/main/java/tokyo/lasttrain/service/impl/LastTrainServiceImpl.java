package tokyo.lasttrain.service.impl;

import org.springframework.stereotype.Service;
import tokyo.lasttrain.cache.TrainInformationCache;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.dto.LastTrainResponse;
import tokyo.lasttrain.dto.LastTrainResponse.Alternative;
import tokyo.lasttrain.dto.LastTrainResponse.DelayInfo;
import tokyo.lasttrain.dto.LastTrainResponse.LastTrainRoute;
import tokyo.lasttrain.dto.LastTrainResponse.TaxiEstimate;
import tokyo.lasttrain.dto.LastTrainResponse.Transfer;
import tokyo.lasttrain.model.OdptRailway;
import tokyo.lasttrain.model.OdptRailwayFare;
import tokyo.lasttrain.model.OdptStation;
import tokyo.lasttrain.model.OdptTrainInformation;
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
import java.util.Set;

@Service
public class LastTrainServiceImpl implements LastTrainService {

    private static final Logger log = LoggerFactory.getLogger(LastTrainServiceImpl.class);
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    private final ReverseRaptorEngine raptorEngine;
    private final TransitDataCache cache;
    private final TrainInformationCache trainInfo;

    public LastTrainServiceImpl(ReverseRaptorEngine raptorEngine, TransitDataCache cache,
                                TrainInformationCache trainInfo) {
        this.raptorEngine = raptorEngine;
        this.cache = cache;
        this.trainInfo = trainInfo;
    }

    @Override
    public LastTrainResponse findLastTrain(String fromStationId, String toStationId) {
        LocalDate today = LocalDate.now(TOKYO);
        Set<String> calendarIds = cache.resolveCalendars(today);
        log.info("Resolved calendars for {}: {}", today, calendarIds);

        List<LastTrainRoute> routes = raptorEngine.findLastTrains(fromStationId, toStationId, calendarIds)
                .stream().map(this::toRoute).toList();

        List<Alternative> alternatives = computeAlternatives(fromStationId, toStationId, calendarIds, routes);
        TaxiEstimate taxi = computeTaxiEstimate(fromStationId, toStationId);

        return new LastTrainResponse(fromStationId, toStationId, calendarTypeLabel(calendarIds),
                routes, alternatives, taxi);
    }

    /**
     * 도쿄 일반 택시 운임 추정. 도로 굴곡 고려해 직선거리에 1.3 보정 후 도쿄 23구 요금표 적용.
     *  - 초승: ¥500 (1.096km까지)
     *  - 가산: 255m마다 ¥100
     *  - 심야할증(22:00-05:00 JST): ×1.2
     */
    private TaxiEstimate computeTaxiEstimate(String fromId, String toId) {
        OdptStation a = cache.getStation(fromId);
        OdptStation b = cache.getStation(toId);
        if (a == null || b == null) return null;
        if (a.latitude() == null || a.longitude() == null) return null;
        if (b.latitude() == null || b.longitude() == null) return null;

        double meters = haversineMeters(a.latitude(), a.longitude(), b.latitude(), b.longitude()) * 1.3;
        int fareDay;
        if (meters <= 1096) {
            fareDay = 500;
        } else {
            fareDay = 500 + (int) Math.ceil((meters - 1096) / 255.0) * 100;
        }
        int fareNight = (int) Math.round(fareDay * 1.2);
        int hour = LocalTime.now(TOKYO).getHour();
        boolean isNight = hour >= 22 || hour < 5;
        return new TaxiEstimate(fareDay, fareNight, meters / 1000.0, isNight);
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double s = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                  * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.sqrt(s));
    }

    /**
     * 도착역 한 정거장 앞/뒤의 같은 노선 인접역에 대한 막차 검색.
     * 동일한 첫 leg(같은 열차의 같은 출발역·시각)로 이어지는 결과는 제외하여 primary와 중복되지 않게 한다.
     */
    private List<Alternative> computeAlternatives(String from, String to, Set<String> calendarIds,
                                                  List<LastTrainRoute> primaryRoutes) {
        OdptStation toStation = cache.getStation(to);
        if (toStation == null || toStation.railway() == null) return List.of();

        List<String> stationOrder = cache.getRailwayStations(toStation.railway());
        int idx = stationOrder.indexOf(to);
        if (idx < 0) return List.of();

        // primary의 첫 leg railway+departureTime 셋: 중복 판정 키
        java.util.Set<String> primaryFirstLegKeys = primaryRoutes.stream()
                .map(r -> r.railway() + "|" + r.departureTime())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<Alternative> out = new ArrayList<>();
        for (int offset : new int[]{-1, 1}) {
            int neighborIdx = idx + offset;
            if (neighborIdx < 0 || neighborIdx >= stationOrder.size()) continue;
            String neighborId = stationOrder.get(neighborIdx);
            if (neighborId.equals(from)) continue;

            List<LastTrainRoute> neighborRoutes = raptorEngine.findLastTrains(from, neighborId, calendarIds)
                    .stream().map(this::toRoute)
                    .filter(r -> !primaryFirstLegKeys.contains(r.railway() + "|" + r.departureTime()))
                    .limit(2)
                    .toList();
            if (neighborRoutes.isEmpty()) continue;

            OdptStation ns = cache.getStation(neighborId);
            String nameJa = ns != null ? ns.title() : null;
            String nameEn = ns != null ? getLocalizedName(ns.stationTitle(), "en") : null;
            String nameKo = cache.getStationNameKo(neighborId);

            out.add(new Alternative(neighborId, nameJa, nameEn, nameKo, offset, neighborRoutes));
        }
        return out;
    }

    private static String calendarTypeLabel(Set<String> ids) {
        if (ids.contains("odpt.Calendar:Weekday")) return "Weekday";
        if (ids.contains("odpt.Calendar:SaturdayHoliday") || ids.contains("odpt.Calendar:Holiday")) return "Holiday";
        if (ids.contains("odpt.Calendar:Saturday")) return "Saturday";
        return "Holiday";
    }

    private LastTrainRoute toRoute(Journey journey) {
        List<Leg> rideLegs = journey.legs().stream().filter(l -> !l.isTransfer()).toList();
        List<Leg> transferLegs = journey.legs().stream().filter(Leg::isTransfer).toList();

        Leg firstRide = rideLegs.getFirst();
        Leg lastRide = rideLegs.getLast();

        // 노선 정보
        String railwayNameJa = null;
        String railwayNameEn = null;
        String railwayNameKo = null;
        OdptRailway railway = cache.getRailway(firstRide.railway());
        if (railway != null) {
            railwayNameJa = railway.title();
            railwayNameEn = getLocalizedName(railway.railwayTitle(), "en");
            railwayNameKo = cache.getRailwayNameKo(firstRide.railway());
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
        String destNameKo = null;
        OdptStation destStation = cache.getStation(lastRide.toStation());
        if (destStation != null) {
            destNameJa = destStation.title();
            destNameEn = getLocalizedName(destStation.stationTitle(), "en");
            destNameKo = cache.getStationNameKo(lastRide.toStation());
        }

        // 환승 정보
        List<Transfer> transfers = new ArrayList<>();
        List<Leg> allLegs = journey.legs();
        for (Leg tl : transferLegs) {
            OdptStation ts = cache.getStation(tl.toStation());
            String tsNameJa = ts != null ? ts.title() : null;
            String tsNameEn = ts != null ? getLocalizedName(ts.stationTitle(), "en") : null;
            String tsNameKo = ts != null ? cache.getStationNameKo(tl.toStation()) : null;

            int idx = allLegs.indexOf(tl);
            Leg prevRide = idx > 0 ? allLegs.get(idx - 1) : null;
            Leg nextRide = idx < allLegs.size() - 1 ? allLegs.get(idx + 1) : null;

            String fromRailway = prevRide != null ? prevRide.railway() : null;
            String toRailway = nextRide != null ? nextRide.railway() : null;
            String[] fromNames = railwayNames(fromRailway);
            String[] toNames = railwayNames(toRailway);

            LocalTime arr = prevRide != null ? prevRide.arrivalTime() : null;
            LocalTime dep = nextRide != null ? nextRide.departureTime() : null;
            int waitMinutes = computeWaitMinutes(arr, dep);

            transfers.add(new Transfer(
                    tsNameJa, tsNameEn, tsNameKo,
                    fromRailway, fromNames[0], fromNames[1], fromNames[2],
                    toRailway, toNames[0], toNames[1], toNames[2],
                    arr != null ? arr.toString() : null,
                    dep != null ? dep.toString() : null,
                    waitMinutes,
                    prevRide != null ? prevRide.toPlatform() : null,
                    nextRide != null ? nextRide.fromPlatform() : null
            ));
        }

        // 요금 계산 (IC / 종이표 둘 다)
        int totalFareIc = calculateFare(journey, true);
        int totalFareTicket = calculateFare(journey, false);

        DelayInfo delay = buildDelayInfo(firstRide.railway());

        return new LastTrainRoute(
                journey.departureTime().toString(),
                journey.arrivalTime().toString(),
                firstRide.railway(),
                railwayNameJa,
                railwayNameEn,
                railwayNameKo,
                firstRide.railDirection(),
                trainType,
                destNameJa,
                destNameEn,
                destNameKo,
                transfers,
                totalFareIc,
                totalFareTicket,
                delay
        );
    }

    private DelayInfo buildDelayInfo(String railwayId) {
        OdptTrainInformation info = trainInfo.getForRailway(railwayId);
        if (info == null) return null;
        Map<String, String> status = info.trainInformationStatus();
        Map<String, String> text = info.trainInformationText();
        String statusJa = status != null ? status.get("ja") : null;
        String statusEn = status != null ? status.get("en") : null;
        String textJa = text != null ? text.get("ja") : null;
        String textEn = text != null ? text.get("en") : null;
        // 평상 운전 메시지는 disruption=false. 그 외 (지연, 운휴, 직통 중지 등)는 true.
        boolean disruption = textJa != null && !textJa.contains("平常");
        return new DelayInfo(statusJa, statusEn, textJa, textEn, disruption);
    }

    /**
     * 경로의 총 요금을 계산한다 (사업자 단위로 합산).
     * @param ic true이면 IC카드 요금, false이면 종이표(ticket) 요금. 해당 값이 null이면 다른 쪽으로 폴백.
     */
    private int calculateFare(Journey journey, boolean ic) {
        int total = 0;
        List<Leg> rideLegs = journey.legs().stream().filter(l -> !l.isTransfer()).toList();

        for (Leg leg : rideLegs) {
            OdptRailwayFare fare = cache.getFare(leg.fromStation(), leg.toStation());
            if (fare == null) continue;
            Integer primary = ic ? fare.icCardFare() : fare.ticketFare();
            Integer fallback = ic ? fare.ticketFare() : fare.icCardFare();
            if (primary != null) total += primary;
            else if (fallback != null) total += fallback;
        }

        return total;
    }

    /** 환승 도착 → 다음 출발 사이의 분 단위 대기 시간. 240분 초과는 자정 경계의 비정상 값으로 보고 0 반환. */
    private static int computeWaitMinutes(LocalTime arr, LocalTime dep) {
        if (arr == null || dep == null) return 0;
        int diff = (dep.toSecondOfDay() - arr.toSecondOfDay()) / 60;
        if (diff < 0) diff += 24 * 60;
        if (diff > 240) return 0;
        return diff;
    }

    /** {ja, en, ko} 3-tuple for railway names. null elements if unknown. */
    private String[] railwayNames(String railwayId) {
        if (railwayId == null) return new String[]{null, null, null};
        OdptRailway r = cache.getRailway(railwayId);
        if (r == null) return new String[]{null, null, null};
        return new String[]{
                r.title(),
                getLocalizedName(r.railwayTitle(), "en"),
                cache.getRailwayNameKo(railwayId)
        };
    }

    private String getLocalizedName(Map<String, String> titleMap, String lang) {
        if (titleMap == null) return null;
        return titleMap.get(lang);
    }
}