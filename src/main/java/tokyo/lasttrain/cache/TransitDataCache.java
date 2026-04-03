package tokyo.lasttrain.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tokyo.lasttrain.client.OdptApiClient;
import tokyo.lasttrain.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 서버 시작 시 ODPT dump API로 전체 데이터를 캐싱한다.
 * 모든 조회는 이 캐시에서 이루어지며 ODPT API에 실시간 의존하지 않는다.
 */
@Component
public class TransitDataCache {

    private static final Logger log = LoggerFactory.getLogger(TransitDataCache.class);

    private final OdptApiClient apiClient;

    // 기본 데이터
    private final Map<String, OdptStation> stationsById = new ConcurrentHashMap<>();
    private final Map<String, OdptRailway> railwaysById = new ConcurrentHashMap<>();
    private final Map<String, OdptTrainType> trainTypesById = new ConcurrentHashMap<>();
    private final Map<String, OdptCalendar> calendarsById = new ConcurrentHashMap<>();

    // 시간표: key = "stationId::railDirection::calendarId"
    private final Map<String, OdptStationTimetable> stationTimetables = new ConcurrentHashMap<>();

    // 열차 시간표: key = trainTimetableId
    private final Map<String, OdptTrainTimetable> trainTimetables = new ConcurrentHashMap<>();

    // 요금: key = "fromStationId::toStationId"
    private final Map<String, OdptRailwayFare> fares = new ConcurrentHashMap<>();

    // 역명 검색용 인덱스: 소문자 영어/일본어 → stationId 목록
    private final Map<String, List<String>> nameIndex = new ConcurrentHashMap<>();

    // 역별 열차 시간표 인덱스: stationId → 해당 역을 지나는 trainTimetableId 목록
    private final Map<String, List<String>> stationToTrainTimetables = new ConcurrentHashMap<>();

    // 환승 그래프: stationId → 환승 가능한 stationId 목록
    private final Map<String, Set<String>> transferGraph = new ConcurrentHashMap<>();

    // 노선별 역 순서: railwayId → 정렬된 stationId 리스트
    private final Map<String, List<String>> railwayStationOrder = new ConcurrentHashMap<>();

    public TransitDataCache(OdptApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @PostConstruct
    public void initialize() {
        log.info("=== Starting ODPT data cache initialization ===");
        long start = System.currentTimeMillis();

        loadStations();
        loadRailways();
        loadTrainTypes();
        loadCalendars();
        loadStationTimetables();
        loadTrainTimetables();
        loadFares();
        buildNameIndex();
        buildTransferGraph();
        buildStationTrainTimetableIndex();

        long elapsed = System.currentTimeMillis() - start;
        log.info("=== ODPT data cache initialized in {}ms ===", elapsed);
        log.info("Stations: {}, Railways: {}, StationTimetables: {}, TrainTimetables: {}, Fares: {}",
                stationsById.size(), railwaysById.size(),
                stationTimetables.size(), trainTimetables.size(), fares.size());
    }

    private void loadStations() {
        List<OdptStation> list = apiClient.fetchDump("odpt:Station", new TypeReference<>() {});
        list.forEach(s -> stationsById.put(s.id(), s));
    }

    private void loadRailways() {
        List<OdptRailway> list = apiClient.fetchDump("odpt:Railway", new TypeReference<>() {});
        for (OdptRailway r : list) {
            railwaysById.put(r.id(), r);
            if (r.stationOrder() != null) {
                List<String> ordered = r.stationOrder().stream()
                        .sorted(Comparator.comparingInt(OdptRailway.StationOrder::index))
                        .map(OdptRailway.StationOrder::station)
                        .toList();
                railwayStationOrder.put(r.id(), ordered);
            }
        }
    }

    private void loadTrainTypes() {
        List<OdptTrainType> list = apiClient.fetchDump("odpt:TrainType", new TypeReference<>() {});
        list.forEach(t -> trainTypesById.put(t.id(), t));
    }

    private void loadCalendars() {
        List<OdptCalendar> list = apiClient.fetchDump("odpt:Calendar", new TypeReference<>() {});
        list.forEach(c -> calendarsById.put(c.id(), c));
    }

    private void loadStationTimetables() {
        List<OdptStationTimetable> list = apiClient.fetchDump("odpt:StationTimetable", new TypeReference<>() {});
        for (OdptStationTimetable tt : list) {
            String key = timetableKey(tt.station(), tt.railDirection(), tt.calendar());
            stationTimetables.put(key, tt);
        }
    }

    private void loadTrainTimetables() {
        List<OdptTrainTimetable> list = apiClient.fetchDump("odpt:TrainTimetable", new TypeReference<>() {});
        list.forEach(tt -> trainTimetables.put(tt.id(), tt));
    }

    private void loadFares() {
        List<OdptRailwayFare> list = apiClient.fetchDump("odpt:RailwayFare", new TypeReference<>() {});
        for (OdptRailwayFare f : list) {
            fares.put(fareKey(f.fromStation(), f.toStation()), f);
        }
    }

    private void buildNameIndex() {
        for (OdptStation station : stationsById.values()) {
            // 일본어 역명
            if (station.title() != null) {
                nameIndex.computeIfAbsent(station.title().toLowerCase(), k -> new ArrayList<>())
                        .add(station.id());
            }
            // 영어 역명
            if (station.stationTitle() != null && station.stationTitle().containsKey("en")) {
                nameIndex.computeIfAbsent(station.stationTitle().get("en").toLowerCase(), k -> new ArrayList<>())
                        .add(station.id());
            }
            // stationId에서 역명 부분 추출 (e.g. "odpt.Station:JR-East.Yamanote.Tokyo" → "tokyo")
            String[] parts = station.id().split("\\.");
            if (parts.length > 0) {
                String shortName = parts[parts.length - 1].toLowerCase();
                nameIndex.computeIfAbsent(shortName, k -> new ArrayList<>())
                        .add(station.id());
            }
        }
    }

    private void buildStationTrainTimetableIndex() {
        for (OdptTrainTimetable tt : trainTimetables.values()) {
            if (tt.stops() == null) continue;
            for (var stop : tt.stops()) {
                String stationId = stop.effectiveStation();
                if (stationId != null) {
                    stationToTrainTimetables
                            .computeIfAbsent(stationId, k -> new ArrayList<>())
                            .add(tt.id());
                }
            }
        }
        log.info("Station-TrainTimetable index: {} stations indexed", stationToTrainTimetables.size());
    }

    private void buildTransferGraph() {
        for (OdptStation station : stationsById.values()) {
            if (station.connectingStation() != null) {
                for (String connected : station.connectingStation()) {
                    transferGraph.computeIfAbsent(station.id(), k -> new HashSet<>()).add(connected);
                    transferGraph.computeIfAbsent(connected, k -> new HashSet<>()).add(station.id());
                }
            }
        }

        // connectingStation이 없는 경우 같은 이름의 역을 환승으로 연결
        Map<String, List<OdptStation>> byName = stationsById.values().stream()
                .filter(s -> s.title() != null)
                .collect(Collectors.groupingBy(OdptStation::title));

        for (List<OdptStation> sameNameStations : byName.values()) {
            if (sameNameStations.size() > 1) {
                for (int i = 0; i < sameNameStations.size(); i++) {
                    for (int j = i + 1; j < sameNameStations.size(); j++) {
                        String a = sameNameStations.get(i).id();
                        String b = sameNameStations.get(j).id();
                        transferGraph.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                        transferGraph.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                    }
                }
            }
        }
    }

    // === 조회 메서드 ===

    public OdptStation getStation(String stationId) {
        return stationsById.get(stationId);
    }

    public OdptRailway getRailway(String railwayId) {
        return railwaysById.get(railwayId);
    }

    public OdptTrainType getTrainType(String trainTypeId) {
        return trainTypesById.get(trainTypeId);
    }

    public List<String> searchStations(String query) {
        String q = query.toLowerCase().trim();
        Set<String> results = new LinkedHashSet<>();

        // 정확히 일치
        if (nameIndex.containsKey(q)) {
            results.addAll(nameIndex.get(q));
        }

        // 부분 일치
        for (Map.Entry<String, List<String>> entry : nameIndex.entrySet()) {
            if (entry.getKey().contains(q)) {
                results.addAll(entry.getValue());
            }
        }

        return new ArrayList<>(results);
    }

    public OdptStationTimetable getStationTimetable(String stationId, String railDirection, String calendarId) {
        return stationTimetables.get(timetableKey(stationId, railDirection, calendarId));
    }

    /**
     * 특정 역에서 출발하는 모든 시간표를 가져온다 (모든 방향, 특정 캘린더).
     */
    public List<OdptStationTimetable> getStationTimetables(String stationId, String calendarId) {
        return stationTimetables.values().stream()
                .filter(tt -> stationId.equals(tt.station()) && calendarId.equals(tt.calendar()))
                .toList();
    }

    public OdptTrainTimetable getTrainTimetable(String trainTimetableId) {
        return trainTimetables.get(trainTimetableId);
    }

    /**
     * 특정 노선+방향+캘린더+열차번호로 열차 시간표를 찾는다.
     */
    public OdptTrainTimetable findTrainTimetable(String railway, String trainNumber, String calendarId) {
        return trainTimetables.values().stream()
                .filter(tt -> railway.equals(tt.railway())
                        && trainNumber.equals(tt.trainNumber())
                        && calendarId.equals(tt.calendar()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 특정 역을 지나는 열차 시간표 목록을 인덱스에서 빠르게 조회한다.
     */
    public List<OdptTrainTimetable> getTrainTimetablesForStation(String stationId) {
        List<String> ids = stationToTrainTimetables.getOrDefault(stationId, List.of());
        return ids.stream()
                .map(trainTimetables::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 특정 역을 지나는 열차 시간표 중 특정 캘린더에 해당하는 것만 조회.
     */
    public List<OdptTrainTimetable> getTrainTimetablesForStation(String stationId, String calendarId) {
        return getTrainTimetablesForStation(stationId).stream()
                .filter(tt -> calendarId.equals(tt.calendar()))
                .toList();
    }

    public Set<String> getTransferStations(String stationId) {
        return transferGraph.getOrDefault(stationId, Set.of());
    }

    public List<String> getRailwayStations(String railwayId) {
        return railwayStationOrder.getOrDefault(railwayId, List.of());
    }

    public OdptRailwayFare getFare(String fromStation, String toStation) {
        return fares.get(fareKey(fromStation, toStation));
    }

    /**
     * 오늘 날짜에 맞는 캘린더 ID를 반환한다.
     * 평일/휴일/토요일 판별.
     */
    public String resolveCalendar(java.time.LocalDate date) {
        java.time.DayOfWeek dow = date.getDayOfWeek();
        String dateStr = date.toString();

        // 특정 날짜가 지정된 캘린더가 있으면 우선
        for (OdptCalendar cal : calendarsById.values()) {
            if (cal.days() != null && cal.days().contains(dateStr)) {
                return cal.id();
            }
        }

        // 요일 기반 판별
        return switch (dow) {
            case SATURDAY -> calendarsById.containsKey("odpt.Calendar:SaturdayHoliday")
                    ? "odpt.Calendar:SaturdayHoliday"
                    : "odpt.Calendar:Holiday";
            case SUNDAY -> "odpt.Calendar:Holiday";
            default -> "odpt.Calendar:Weekday";
        };
    }

    public Collection<OdptStation> getAllStations() {
        return stationsById.values();
    }

    public Collection<OdptRailway> getAllRailways() {
        return railwaysById.values();
    }

    public Collection<OdptTrainTimetable> getAllTrainTimetables() {
        return trainTimetables.values();
    }

    private static String timetableKey(String station, String direction, String calendar) {
        return station + "::" + direction + "::" + calendar;
    }

    private static String fareKey(String from, String to) {
        return from + "::" + to;
    }
}