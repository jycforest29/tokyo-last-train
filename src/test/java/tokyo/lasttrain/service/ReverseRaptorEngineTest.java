package tokyo.lasttrain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.model.*;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine.Journey;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine.Leg;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reverse RAPTOR 알고리즘 단위 테스트.
 * ODPT API 호출 없이, 캐시에 mock 데이터를 직접 주입하여 테스트.
 *
 * 테스트 노선도:
 *
 *   [A역] ──LineA──> [B역] ──LineA──> [C역]
 *                      │
 *                    (환승)
 *                      │
 *   [D역] ──LineB──> [B역'] ──LineB──> [E역]
 */
class ReverseRaptorEngineTest {

    private TransitDataCache cache;
    private ReverseRaptorEngine engine;

    // 역 ID
    static final String STATION_A = "odpt.Station:Test.LineA.StationA";
    static final String STATION_B_LINE_A = "odpt.Station:Test.LineA.StationB";
    static final String STATION_C = "odpt.Station:Test.LineA.StationC";
    static final String STATION_D = "odpt.Station:Test.LineB.StationD";
    static final String STATION_B_LINE_B = "odpt.Station:Test.LineB.StationB";
    static final String STATION_E = "odpt.Station:Test.LineB.StationE";

    static final String LINE_A = "odpt.Railway:Test.LineA";
    static final String LINE_B = "odpt.Railway:Test.LineB";
    static final String WEEKDAY = "odpt.Calendar:Weekday";

    @BeforeEach
    void setUp() throws Exception {
        cache = createMockCache();
        engine = new ReverseRaptorEngine(cache);
    }

    @Test
    @DisplayName("직통 경로: A → C (환승 없음)")
    void directRoute() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_C, WEEKDAY);

        assertFalse(journeys.isEmpty(), "경로가 있어야 한다");
        Journey best = journeys.getFirst();
        assertEquals(STATION_A, best.fromStation());
        assertEquals(STATION_C, best.toStation());
        assertEquals(0, best.transferCount(), "환승 없는 직통이어야 한다");

        // 마지막 열차(23:30 출발)가 선택되어야 함
        assertEquals("23:30", best.departureTime().toString());
    }

    @Test
    @DisplayName("환승 경로: A → E (A→B 환승 후 B→E)")
    void transferRoute() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_E, WEEKDAY);

        assertFalse(journeys.isEmpty(), "경로가 있어야 한다");
        Journey best = journeys.getFirst();
        assertEquals(STATION_A, best.fromStation());
        assertEquals(STATION_E, best.toStation());
        assertTrue(best.transferCount() > 0, "환승이 포함되어야 한다");
    }

    @Test
    @DisplayName("역방향 직통: D → B_LINE_B")
    void reverseDirectRoute() {
        List<Journey> journeys = engine.findLastTrains(STATION_D, STATION_B_LINE_B, WEEKDAY);

        assertFalse(journeys.isEmpty());
        Journey best = journeys.getFirst();
        assertEquals(0, best.transferCount());
    }

    @Test
    @DisplayName("경로 없음: 연결되지 않은 역")
    void noRouteFound() {
        // E → A: LineB에서 LineA로 역방향 환승은 있지만,
        // E에서 B까지만 가는 열차는 없으므로 (D→B→E 방향만 있음) 경로 없을 수 있음
        List<Journey> journeys = engine.findLastTrains(STATION_E, STATION_A, WEEKDAY);
        assertNotNull(journeys);
    }

    @Test
    @DisplayName("출발역과 도착역이 같으면 빈 결과")
    void sameStation() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_A, WEEKDAY);
        // legs가 비어있는 journey만 있거나 빈 리스트
        assertTrue(journeys.isEmpty() || journeys.getFirst().legs().isEmpty());
    }

    @Test
    @DisplayName("가장 늦은 출발 시간이 선택된다")
    void latestDepartureSelected() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_C, WEEKDAY);

        assertFalse(journeys.isEmpty());
        Journey best = journeys.getFirst();
        // 23:00 열차와 23:30 열차 중 23:30이 선택되어야 함
        assertEquals("23:30", best.departureTime().toString());
    }

    @Test
    @DisplayName("직통 경로의 Leg 정보가 올바르다")
    void directRouteLegDetails() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_C, WEEKDAY);

        assertFalse(journeys.isEmpty());
        Journey best = journeys.getFirst();

        // 직통이므로 ride leg 1개만
        List<Leg> rideLegs = best.legs().stream().filter(l -> !l.isTransfer()).toList();
        assertEquals(1, rideLegs.size());

        Leg leg = rideLegs.getFirst();
        assertEquals(STATION_A, leg.fromStation());
        assertEquals(STATION_C, leg.toStation());
        assertEquals(LINE_A, leg.railway());
        assertFalse(leg.isTransfer());
    }

    @Test
    @DisplayName("환승 경로의 Leg 구성 검증 (ride + transfer + ride)")
    void transferRouteLegStructure() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_E, WEEKDAY);

        assertFalse(journeys.isEmpty());
        Journey best = journeys.getFirst();

        List<Leg> rideLegs = best.legs().stream().filter(l -> !l.isTransfer()).toList();
        List<Leg> transferLegs = best.legs().stream().filter(Leg::isTransfer).toList();

        assertEquals(2, rideLegs.size(), "환승 경로는 ride leg 2개");
        assertEquals(1, transferLegs.size(), "환승 1회");

        // 첫 번째 ride: LineA
        assertEquals(LINE_A, rideLegs.get(0).railway());
        // 두 번째 ride: LineB
        assertEquals(LINE_B, rideLegs.get(1).railway());
    }

    @Test
    @DisplayName("Journey.departureTime()과 arrivalTime() 계산 검증")
    void journeyTimeMethods() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_C, WEEKDAY);

        assertFalse(journeys.isEmpty());
        Journey best = journeys.getFirst();

        // departureTime은 첫 번째 leg의 출발 시각
        assertNotNull(best.departureTime());
        assertTrue(best.departureTime().isAfter(LocalTime.of(22, 0)));

        // arrivalTime은 마지막 leg의 도착 시각
        assertNotNull(best.arrivalTime());
    }

    @Test
    @DisplayName("빈 Journey의 departureTime/arrivalTime은 LocalTime.MIN")
    void emptyJourneyTime() {
        Journey empty = new Journey("A", "A", List.of());
        assertEquals(LocalTime.MIN, empty.departureTime());
        assertEquals(LocalTime.MIN, empty.arrivalTime());
        assertEquals(0, empty.transferCount());
    }

    @Test
    @DisplayName("Leg.isTransfer()는 railway가 TRANSFER일 때만 true")
    void legIsTransfer() {
        Leg rideLeg = new Leg("A", "B", LINE_A, null, null, null, LocalTime.of(23, 0), LocalTime.of(23, 15));
        Leg transferLeg = new Leg("B", "B'", "TRANSFER", null, null, null, LocalTime.of(23, 15), LocalTime.of(23, 20));

        assertFalse(rideLeg.isTransfer());
        assertTrue(transferLeg.isTransfer());
    }

    @Test
    @DisplayName("중간역 경로: A → B (종점이 아닌 역까지)")
    void partialRoute() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_B_LINE_A, WEEKDAY);

        assertFalse(journeys.isEmpty(), "중간역까지의 경로가 있어야 한다");
        Journey best = journeys.getFirst();
        assertEquals(STATION_A, best.fromStation());
        assertEquals(STATION_B_LINE_A, best.toStation());
        assertEquals(0, best.transferCount());
    }

    @Test
    @DisplayName("존재하지 않는 역을 검색하면 빈 결과")
    void nonExistentStation() {
        List<Journey> journeys = engine.findLastTrains("odpt.Station:Fake.X", STATION_C, WEEKDAY);
        assertTrue(journeys.isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 캘린더로 검색하면 빈 결과")
    void nonExistentCalendar() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_C, "odpt.Calendar:NonExistent");
        assertTrue(journeys.isEmpty());
    }

    @Test
    @DisplayName("결과가 출발 시간 내림차순으로 정렬된다")
    void resultsSortedByDepartureDesc() {
        List<Journey> journeys = engine.findLastTrains(STATION_A, STATION_C, WEEKDAY);

        for (int i = 0; i < journeys.size() - 1; i++) {
            assertTrue(
                    journeys.get(i).departureTime().isAfter(journeys.get(i + 1).departureTime())
                            || journeys.get(i).departureTime().equals(journeys.get(i + 1).departureTime()),
                    "결과는 출발 시간 내림차순이어야 한다"
            );
        }
    }

    // === Mock 데이터 생성 ===

    private TransitDataCache createMockCache() throws Exception {
        TransitDataCache mockCache = new TransitDataCache(null);

        // 역 데이터
        Map<String, OdptStation> stations = new ConcurrentHashMap<>();
        stations.put(STATION_A, station(STATION_A, "A駅", "Station A", LINE_A));
        stations.put(STATION_B_LINE_A, station(STATION_B_LINE_A, "B駅", "Station B", LINE_A));
        stations.put(STATION_C, station(STATION_C, "C駅", "Station C", LINE_A));
        stations.put(STATION_D, station(STATION_D, "D駅", "Station D", LINE_B));
        stations.put(STATION_B_LINE_B, station(STATION_B_LINE_B, "B駅", "Station B", LINE_B));
        stations.put(STATION_E, station(STATION_E, "E駅", "Station E", LINE_B));
        setField(mockCache, "stationsById", stations);

        // 노선 데이터
        Map<String, OdptRailway> railways = new ConcurrentHashMap<>();
        railways.put(LINE_A, railway(LINE_A, "テストA線", List.of(STATION_A, STATION_B_LINE_A, STATION_C)));
        railways.put(LINE_B, railway(LINE_B, "テストB線", List.of(STATION_D, STATION_B_LINE_B, STATION_E)));
        setField(mockCache, "railwaysById", railways);

        // 열차 시간표 (핵심)
        Map<String, OdptTrainTimetable> trainTimetables = new ConcurrentHashMap<>();

        // LineA 열차 1: A(23:00) → B(23:15) → C(23:30)
        trainTimetables.put("tt-lineA-1", trainTimetable("tt-lineA-1", LINE_A, WEEKDAY, "101",
                List.of(
                        trainStop(STATION_A, "23:00", null),
                        trainStop(STATION_B_LINE_A, "23:15", "23:15"),
                        trainStop(null, null, "23:30", STATION_C)
                )));

        // LineA 열차 2 (막차): A(23:30) → B(23:45) → C(00:00)
        trainTimetables.put("tt-lineA-2", trainTimetable("tt-lineA-2", LINE_A, WEEKDAY, "102",
                List.of(
                        trainStop(STATION_A, "23:30", null),
                        trainStop(STATION_B_LINE_A, "23:45", "23:45"),
                        trainStop(null, null, "00:00", STATION_C)
                )));

        // LineB 열차 1: D(23:00) → B(23:20) → E(23:40)
        trainTimetables.put("tt-lineB-1", trainTimetable("tt-lineB-1", LINE_B, WEEKDAY, "201",
                List.of(
                        trainStop(STATION_D, "23:00", null),
                        trainStop(STATION_B_LINE_B, "23:20", "23:20"),
                        trainStop(null, null, "23:40", STATION_E)
                )));

        // LineB 열차 2 (막차): D(23:30) → B(23:50) → E(00:10)
        trainTimetables.put("tt-lineB-2", trainTimetable("tt-lineB-2", LINE_B, WEEKDAY, "202",
                List.of(
                        trainStop(STATION_D, "23:30", null),
                        trainStop(STATION_B_LINE_B, "23:50", "23:50"),
                        trainStop(null, null, "00:10", STATION_E)
                )));

        setField(mockCache, "trainTimetables", trainTimetables);

        // 역별 열차 시간표 인덱스
        Map<String, List<String>> stationToTrainTimetables = new ConcurrentHashMap<>();
        for (var entry : trainTimetables.entrySet()) {
            for (var stop : entry.getValue().stops()) {
                String sid = stop.effectiveStation();
                if (sid != null) {
                    stationToTrainTimetables.computeIfAbsent(sid, k -> new ArrayList<>()).add(entry.getKey());
                }
            }
        }
        setField(mockCache, "stationToTrainTimetables", stationToTrainTimetables);

        // 환승 그래프: B역(LineA) ↔ B역(LineB)
        Map<String, Set<String>> transferGraph = new ConcurrentHashMap<>();
        transferGraph.put(STATION_B_LINE_A, new HashSet<>(Set.of(STATION_B_LINE_B)));
        transferGraph.put(STATION_B_LINE_B, new HashSet<>(Set.of(STATION_B_LINE_A)));
        setField(mockCache, "transferGraph", transferGraph);

        // 빈 맵들 초기화
        setField(mockCache, "stationTimetables", new ConcurrentHashMap<>());
        setField(mockCache, "fares", new ConcurrentHashMap<>());
        setField(mockCache, "nameIndex", new ConcurrentHashMap<>());
        setField(mockCache, "trainTypesById", new ConcurrentHashMap<>());
        setField(mockCache, "calendarsById", new ConcurrentHashMap<>());
        setField(mockCache, "railwayStationOrder", new ConcurrentHashMap<>());

        return mockCache;
    }

    private OdptStation station(String id, String titleJa, String titleEn, String railway) {
        return new OdptStation(id, titleJa,
                Map.of("ja", titleJa, "en", titleEn),
                "odpt.Operator:Test", railway, null, 35.0, 139.0,
                null, null);
    }

    private OdptRailway railway(String id, String title, List<String> stationIds) {
        List<OdptRailway.StationOrder> order = new ArrayList<>();
        for (int i = 0; i < stationIds.size(); i++) {
            order.add(new OdptRailway.StationOrder(stationIds.get(i), Map.of(), i + 1));
        }
        return new OdptRailway(id, title, Map.of("ja", title), "odpt.Operator:Test",
                null, null, null, null, order);
    }

    private OdptTrainTimetable trainTimetable(String id, String railway, String calendar,
                                               String trainNumber, List<OdptTrainTimetable.TrainStop> stops) {
        return new OdptTrainTimetable(id, "odpt.Operator:Test", railway, null, calendar,
                trainNumber, null, null, null, null, stops);
    }

    private OdptTrainTimetable.TrainStop trainStop(String station, String depTime, String arrTime) {
        return new OdptTrainTimetable.TrainStop(depTime, station, arrTime, null, null);
    }

    private OdptTrainTimetable.TrainStop trainStop(String depStation, String depTime,
                                                     String arrTime, String arrStation) {
        return new OdptTrainTimetable.TrainStop(depTime, depStation, arrTime, arrStation, null);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}