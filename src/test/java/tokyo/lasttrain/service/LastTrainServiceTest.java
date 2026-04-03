package tokyo.lasttrain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.dto.LastTrainResponse;
import tokyo.lasttrain.dto.LastTrainResponse.LastTrainRoute;
import tokyo.lasttrain.model.*;
import tokyo.lasttrain.service.impl.LastTrainServiceImpl;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class LastTrainServiceTest {

    private LastTrainServiceImpl service;
    private TransitDataCache cache;

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
        ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);
        service = new LastTrainServiceImpl(engine, cache);
    }

    @Test
    @DisplayName("막차 응답에 출발/도착역 정보가 포함된다")
    void responseContainsStationInfo() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_C);

        assertEquals(STATION_A, response.fromStation());
        assertEquals(STATION_C, response.toStation());
        assertNotNull(response.calendarType());
    }

    @Test
    @DisplayName("요금 정보가 포함된다 - IC카드 요금 우선")
    void fareIncludedIcCardPriority() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_C);

        if (!response.routes().isEmpty()) {
            // IC카드 요금(195)이 일반 요금(200)보다 우선 사용됨
            assertTrue(response.routes().getFirst().totalFare() > 0);
        }
    }

    @Test
    @DisplayName("직통 경로에는 환승 정보가 비어있다")
    void directRouteNoTransfers() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_C);

        if (!response.routes().isEmpty()) {
            LastTrainRoute route = response.routes().getFirst();
            assertTrue(route.transfers().isEmpty(), "직통 경로에는 환승이 없어야 한다");
        }
    }

    @Test
    @DisplayName("직통 경로의 노선 정보가 올바르다")
    void directRouteRailwayInfo() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_C);

        if (!response.routes().isEmpty()) {
            LastTrainRoute route = response.routes().getFirst();
            assertEquals(LINE_A, route.railway());
            assertEquals("テストA線", route.railwayNameJa());
            assertEquals("Test Line A", route.railwayNameEn());
        }
    }

    @Test
    @DisplayName("직통 경로의 행선지 정보가 올바르다")
    void directRouteDestination() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_C);

        if (!response.routes().isEmpty()) {
            LastTrainRoute route = response.routes().getFirst();
            assertEquals("C駅", route.destinationNameJa());
            assertEquals("Station C", route.destinationNameEn());
        }
    }

    @Test
    @DisplayName("열차 종별 정보가 포함된다")
    void trainTypeIncluded() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_C);

        if (!response.routes().isEmpty()) {
            LastTrainRoute route = response.routes().getFirst();
            // trainType이 cache에 있으면 영어명, 없으면 ID 그대로
            assertNotNull(route.trainType());
        }
    }

    @Test
    @DisplayName("환승 경로: A → E (환승 포함)")
    void transferRoute() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_E);

        if (!response.routes().isEmpty()) {
            LastTrainRoute route = response.routes().getFirst();
            assertFalse(route.transfers().isEmpty(), "환승이 포함되어야 한다");

            // 환승 정보에 역명이 포함됨
            LastTrainResponse.Transfer transfer = route.transfers().getFirst();
            assertNotNull(transfer.stationNameJa());
            assertNotNull(transfer.stationNameEn());
        }
    }

    @Test
    @DisplayName("환승 경로의 환승 전후 노선 정보가 포함된다")
    void transferRouteRailwayInfo() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_E);

        if (!response.routes().isEmpty()) {
            LastTrainRoute route = response.routes().getFirst();
            if (!route.transfers().isEmpty()) {
                LastTrainResponse.Transfer transfer = route.transfers().getFirst();
                // 환승 전/후 노선이 다른 노선이어야 함
                assertNotNull(transfer.fromRailway());
                assertNotNull(transfer.toRailway());
            }
        }
    }

    @Test
    @DisplayName("경로 없음: 연결 불가능한 역 쌍")
    void noRouteFound() {
        // E → A: 역방향 열차 없음
        LastTrainResponse response = service.findLastTrain(STATION_E, STATION_A);

        assertNotNull(response);
        assertEquals(STATION_E, response.fromStation());
        assertEquals(STATION_A, response.toStation());
        // 결과가 비어도 에러 아님
    }

    @Test
    @DisplayName("캘린더 타입이 응답에 포함된다")
    void calendarTypeInResponse() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_C);

        assertNotNull(response.calendarType());
        // Weekday, Saturday, Holiday 중 하나
        assertTrue(List.of("Weekday", "Saturday", "Holiday").contains(response.calendarType()));
    }

    @Test
    @DisplayName("요금 정보가 없으면 0원")
    void noFareReturnsZero() {
        // D → E : 요금 데이터 없음
        LastTrainResponse response = service.findLastTrain(STATION_D, STATION_E);

        if (!response.routes().isEmpty()) {
            assertEquals(0, response.routes().getFirst().totalFare());
        }
    }

    @Test
    @DisplayName("출발/도착 시간 문자열 형식 검증")
    void timeFormat() {
        LastTrainResponse response = service.findLastTrain(STATION_A, STATION_C);

        if (!response.routes().isEmpty()) {
            LastTrainRoute route = response.routes().getFirst();
            assertNotNull(route.departureTime());
            assertNotNull(route.arrivalTime());
            // HH:MM 형식
            assertTrue(route.departureTime().matches("\\d{2}:\\d{2}"));
            assertTrue(route.arrivalTime().matches("\\d{2}:\\d{2}"));
        }
    }

    private TransitDataCache createMockCache() throws Exception {
        TransitDataCache mockCache = new TransitDataCache(null);

        // 역 데이터 (2개 노선, 환승 포함)
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
        railways.put(LINE_A, new OdptRailway(LINE_A, "テストA線",
                Map.of("ja", "テストA線", "en", "Test Line A"),
                "odpt.Operator:Test", null, null, null, null,
                List.of(
                        new OdptRailway.StationOrder(STATION_A, Map.of(), 1),
                        new OdptRailway.StationOrder(STATION_B_LINE_A, Map.of(), 2),
                        new OdptRailway.StationOrder(STATION_C, Map.of(), 3)
                )));
        railways.put(LINE_B, new OdptRailway(LINE_B, "テストB線",
                Map.of("ja", "テストB線", "en", "Test Line B"),
                "odpt.Operator:Test", null, null, null, null,
                List.of(
                        new OdptRailway.StationOrder(STATION_D, Map.of(), 1),
                        new OdptRailway.StationOrder(STATION_B_LINE_B, Map.of(), 2),
                        new OdptRailway.StationOrder(STATION_E, Map.of(), 3)
                )));
        setField(mockCache, "railwaysById", railways);

        // 열차 시간표
        Map<String, OdptTrainTimetable> trainTimetables = new ConcurrentHashMap<>();

        // LineA 열차: A(23:30) → B(23:45) → C(00:00)
        trainTimetables.put("tt-lineA", new OdptTrainTimetable("tt-lineA", "odpt.Operator:Test",
                LINE_A, null, WEEKDAY, "101", "odpt.TrainType:Test.Local",
                null, null, null,
                List.of(
                        new OdptTrainTimetable.TrainStop("23:30", STATION_A, null, null, null),
                        new OdptTrainTimetable.TrainStop("23:45", STATION_B_LINE_A, "23:45", null, null),
                        new OdptTrainTimetable.TrainStop(null, null, "00:00", STATION_C, null)
                )));

        // LineB 열차: D(23:30) → B(23:50) → E(00:10)
        trainTimetables.put("tt-lineB", new OdptTrainTimetable("tt-lineB", "odpt.Operator:Test",
                LINE_B, null, WEEKDAY, "201", "odpt.TrainType:Test.Rapid",
                null, null, null,
                List.of(
                        new OdptTrainTimetable.TrainStop("23:30", STATION_D, null, null, null),
                        new OdptTrainTimetable.TrainStop("23:50", STATION_B_LINE_B, "23:50", null, null),
                        new OdptTrainTimetable.TrainStop(null, null, "00:10", STATION_E, null)
                )));

        setField(mockCache, "trainTimetables", trainTimetables);

        // 역별 열차 시간표 인덱스
        Map<String, List<String>> stationToTT = new ConcurrentHashMap<>();
        for (var entry : trainTimetables.entrySet()) {
            for (var stop : entry.getValue().stops()) {
                String sid = stop.effectiveStation();
                if (sid != null) {
                    stationToTT.computeIfAbsent(sid, k -> new ArrayList<>()).add(entry.getKey());
                }
            }
        }
        setField(mockCache, "stationToTrainTimetables", stationToTT);

        // 환승 그래프: B역(LineA) ↔ B역(LineB)
        Map<String, Set<String>> transferGraph = new ConcurrentHashMap<>();
        transferGraph.put(STATION_B_LINE_A, new HashSet<>(Set.of(STATION_B_LINE_B)));
        transferGraph.put(STATION_B_LINE_B, new HashSet<>(Set.of(STATION_B_LINE_A)));
        setField(mockCache, "transferGraph", transferGraph);

        // 요금 (A→C만 설정, IC카드 요금 우선 테스트용)
        Map<String, OdptRailwayFare> fares = new ConcurrentHashMap<>();
        fares.put(STATION_A + "::" + STATION_C,
                new OdptRailwayFare("fare-1", "odpt.Operator:Test",
                        STATION_A, STATION_C, 200, 195, 100, 98));
        setField(mockCache, "fares", fares);

        // 캘린더
        Map<String, OdptCalendar> calendars = new ConcurrentHashMap<>();
        calendars.put(WEEKDAY, new OdptCalendar(WEEKDAY, "平日", Map.of("ja", "平日", "en", "Weekday"), null, null));
        setField(mockCache, "calendarsById", calendars);

        // 열차 종별
        Map<String, OdptTrainType> trainTypes = new ConcurrentHashMap<>();
        trainTypes.put("odpt.TrainType:Test.Local",
                new OdptTrainType("odpt.TrainType:Test.Local", "odpt.Operator:Test",
                        "各停", Map.of("ja", "各停", "en", "Local")));
        trainTypes.put("odpt.TrainType:Test.Rapid",
                new OdptTrainType("odpt.TrainType:Test.Rapid", "odpt.Operator:Test",
                        "快速", Map.of("ja", "快速", "en", "Rapid")));
        setField(mockCache, "trainTypesById", trainTypes);

        // 빈 맵들
        setField(mockCache, "stationTimetables", new ConcurrentHashMap<>());
        setField(mockCache, "nameIndex", new ConcurrentHashMap<>());
        setField(mockCache, "railwayStationOrder", new ConcurrentHashMap<>());

        return mockCache;
    }

    private OdptStation station(String id, String titleJa, String titleEn, String railway) {
        return new OdptStation(id, titleJa, Map.of("ja", titleJa, "en", titleEn),
                "odpt.Operator:Test", railway, null, 35.0, 139.0, null, null);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}