package tokyo.lasttrain.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tokyo.lasttrain.model.*;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class TransitDataCacheTest {

    private TransitDataCache cache;

    static final String STATION_SHIBUYA_GINZA = "odpt.Station:TokyoMetro.Ginza.Shibuya";
    static final String STATION_SHIBUYA_YAMANOTE = "odpt.Station:JR.Yamanote.Shibuya";
    static final String STATION_TOKYO_YAMANOTE = "odpt.Station:JR.Yamanote.Tokyo";
    static final String STATION_UENO = "odpt.Station:TokyoMetro.Ginza.Ueno";

    @BeforeEach
    void setUp() throws Exception {
        cache = new TransitDataCache(null);
        initializeMockData();
    }

    // === resolveCalendar ===

    @Test
    @DisplayName("평일은 Weekday 캘린더를 반환한다")
    void resolveCalendarWeekday() {
        // 2026-03-23 = Monday
        LocalDate monday = LocalDate.of(2026, 3, 23);
        assertEquals("odpt.Calendar:Weekday", cache.resolveCalendar(monday));
    }

    @Test
    @DisplayName("토요일은 SaturdayHoliday 캘린더를 반환한다")
    void resolveCalendarSaturday() {
        // 2026-03-28 = Saturday
        LocalDate saturday = LocalDate.of(2026, 3, 28);
        assertEquals("odpt.Calendar:SaturdayHoliday", cache.resolveCalendar(saturday));
    }

    @Test
    @DisplayName("일요일은 Holiday 캘린더를 반환한다")
    void resolveCalendarSunday() {
        // 2026-03-29 = Sunday
        LocalDate sunday = LocalDate.of(2026, 3, 29);
        assertEquals("odpt.Calendar:Holiday", cache.resolveCalendar(sunday));
    }

    @Test
    @DisplayName("특정 날짜가 지정된 캘린더가 있으면 우선 사용한다")
    void resolveCalendarSpecificDay() throws Exception {
        // 2026-03-23(월요일)을 Holiday로 지정
        Map<String, OdptCalendar> calendars = new ConcurrentHashMap<>();
        calendars.put("odpt.Calendar:Weekday", new OdptCalendar("odpt.Calendar:Weekday",
                "平日", Map.of(), null, null));
        calendars.put("odpt.Calendar:SpecialHoliday", new OdptCalendar("odpt.Calendar:SpecialHoliday",
                "特別休日", Map.of(), List.of("2026-03-23"), null));
        setField(cache, "calendarsById", calendars);

        LocalDate specialDay = LocalDate.of(2026, 3, 23);
        assertEquals("odpt.Calendar:SpecialHoliday", cache.resolveCalendar(specialDay));
    }

    @Test
    @DisplayName("SaturdayHoliday 캘린더가 없으면 토요일은 Holiday로 fallback")
    void resolveCalendarSaturdayFallback() throws Exception {
        Map<String, OdptCalendar> calendars = new ConcurrentHashMap<>();
        calendars.put("odpt.Calendar:Weekday", new OdptCalendar("odpt.Calendar:Weekday",
                "平日", Map.of(), null, null));
        calendars.put("odpt.Calendar:Holiday", new OdptCalendar("odpt.Calendar:Holiday",
                "休日", Map.of(), null, null));
        // SaturdayHoliday 없음
        setField(cache, "calendarsById", calendars);

        LocalDate saturday = LocalDate.of(2026, 3, 28);
        assertEquals("odpt.Calendar:Holiday", cache.resolveCalendar(saturday));
    }

    // === searchStations ===

    @Test
    @DisplayName("정확히 일치하는 역명 검색")
    void searchStationsExactMatch() {
        List<String> results = cache.searchStations("shibuya");
        assertFalse(results.isEmpty());
        assertTrue(results.contains(STATION_SHIBUYA_GINZA) || results.contains(STATION_SHIBUYA_YAMANOTE));
    }

    @Test
    @DisplayName("부분 일치 검색")
    void searchStationsPartialMatch() {
        List<String> results = cache.searchStations("shib");
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("대소문자 구분 없이 검색")
    void searchStationsCaseInsensitive() {
        List<String> upper = cache.searchStations("SHIBUYA");
        List<String> lower = cache.searchStations("shibuya");
        assertEquals(upper.size(), lower.size());
    }

    @Test
    @DisplayName("일본어 역명 검색")
    void searchStationsJapanese() {
        List<String> results = cache.searchStations("渋谷");
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 역명 검색")
    void searchStationsNotFound() {
        List<String> results = cache.searchStations("nonexistent");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("빈 문자열 검색은 전체 반환 가능")
    void searchStationsEmptyQuery() {
        List<String> results = cache.searchStations("");
        assertNotNull(results);
    }

    // === getStation ===

    @Test
    @DisplayName("존재하는 역 조회")
    void getStationExists() {
        OdptStation station = cache.getStation(STATION_SHIBUYA_GINZA);
        assertNotNull(station);
        assertEquals("渋谷", station.title());
    }

    @Test
    @DisplayName("존재하지 않는 역 조회시 null")
    void getStationNotExists() {
        assertNull(cache.getStation("odpt.Station:Fake"));
    }

    // === getRailway ===

    @Test
    @DisplayName("존재하는 노선 조회")
    void getRailwayExists() {
        OdptRailway railway = cache.getRailway("odpt.Railway:TokyoMetro.Ginza");
        assertNotNull(railway);
        assertEquals("銀座線", railway.title());
    }

    @Test
    @DisplayName("존재하지 않는 노선 조회시 null")
    void getRailwayNotExists() {
        assertNull(cache.getRailway("odpt.Railway:Fake"));
    }

    // === getTransferStations ===

    @Test
    @DisplayName("환승 가능한 역 목록 조회")
    void getTransferStations() {
        Set<String> transfers = cache.getTransferStations(STATION_SHIBUYA_GINZA);
        assertNotNull(transfers);
        assertTrue(transfers.contains(STATION_SHIBUYA_YAMANOTE));
    }

    @Test
    @DisplayName("환승이 없는 역은 빈 셋 반환")
    void getTransferStationsEmpty() {
        Set<String> transfers = cache.getTransferStations(STATION_UENO);
        assertNotNull(transfers);
        assertTrue(transfers.isEmpty());
    }

    // === getFare ===

    @Test
    @DisplayName("요금 조회")
    void getFare() {
        OdptRailwayFare fare = cache.getFare(STATION_SHIBUYA_GINZA, STATION_UENO);
        assertNotNull(fare);
        assertEquals(200, fare.ticketFare());
        assertEquals(195, fare.icCardFare());
    }

    @Test
    @DisplayName("역방향 요금은 별도 등록 필요")
    void getFareReverse() {
        // 역방향 (ueno → shibuya)은 별도 등록하지 않으면 null
        OdptRailwayFare fare = cache.getFare(STATION_UENO, STATION_SHIBUYA_GINZA);
        assertNull(fare);
    }

    // === getTrainTimetablesForStation ===

    @Test
    @DisplayName("역을 지나는 열차 시간표 조회")
    void getTrainTimetablesForStation() {
        List<OdptTrainTimetable> timetables = cache.getTrainTimetablesForStation(STATION_SHIBUYA_GINZA);
        assertFalse(timetables.isEmpty());
    }

    @Test
    @DisplayName("역을 지나는 열차 시간표 - 캘린더 필터링")
    void getTrainTimetablesForStationWithCalendar() {
        List<OdptTrainTimetable> weekday = cache.getTrainTimetablesForStation(
                STATION_SHIBUYA_GINZA, "odpt.Calendar:Weekday");
        List<OdptTrainTimetable> holiday = cache.getTrainTimetablesForStation(
                STATION_SHIBUYA_GINZA, "odpt.Calendar:Holiday");

        assertFalse(weekday.isEmpty());
        assertTrue(holiday.isEmpty());
    }

    @Test
    @DisplayName("열차가 지나지 않는 역은 빈 목록")
    void getTrainTimetablesForStationEmpty() {
        List<OdptTrainTimetable> timetables = cache.getTrainTimetablesForStation("odpt.Station:Fake");
        assertTrue(timetables.isEmpty());
    }

    // === getTrainType ===

    @Test
    @DisplayName("열차 종별 조회")
    void getTrainType() {
        OdptTrainType type = cache.getTrainType("odpt.TrainType:TokyoMetro.Local");
        assertNotNull(type);
        assertEquals("各停", type.title());
    }

    @Test
    @DisplayName("존재하지 않는 열차 종별은 null")
    void getTrainTypeNotExists() {
        assertNull(cache.getTrainType("odpt.TrainType:Fake"));
    }

    // === getAllStations / getAllRailways ===

    @Test
    @DisplayName("전체 역 목록 조회")
    void getAllStations() {
        assertEquals(4, cache.getAllStations().size());
    }

    @Test
    @DisplayName("전체 노선 목록 조회")
    void getAllRailways() {
        assertEquals(2, cache.getAllRailways().size());
    }

    // === Mock 데이터 초기화 ===

    private void initializeMockData() throws Exception {
        // 역 데이터
        Map<String, OdptStation> stations = new ConcurrentHashMap<>();
        stations.put(STATION_SHIBUYA_GINZA, new OdptStation(STATION_SHIBUYA_GINZA, "渋谷",
                Map.of("ja", "渋谷", "en", "Shibuya"),
                "odpt.Operator:TokyoMetro", "odpt.Railway:TokyoMetro.Ginza", "G01",
                35.6580, 139.7016, null,
                List.of(STATION_SHIBUYA_YAMANOTE)));
        stations.put(STATION_SHIBUYA_YAMANOTE, new OdptStation(STATION_SHIBUYA_YAMANOTE, "渋谷",
                Map.of("ja", "渋谷", "en", "Shibuya"),
                "odpt.Operator:JR", "odpt.Railway:JR.Yamanote", "JY20",
                35.6580, 139.7016, null,
                List.of(STATION_SHIBUYA_GINZA)));
        stations.put(STATION_TOKYO_YAMANOTE, new OdptStation(STATION_TOKYO_YAMANOTE, "東京",
                Map.of("ja", "東京", "en", "Tokyo"),
                "odpt.Operator:JR", "odpt.Railway:JR.Yamanote", "JY01",
                35.6812, 139.7671, null, null));
        stations.put(STATION_UENO, new OdptStation(STATION_UENO, "上野",
                Map.of("ja", "上野", "en", "Ueno"),
                "odpt.Operator:TokyoMetro", "odpt.Railway:TokyoMetro.Ginza", "G16",
                35.7141, 139.7774, null, null));
        setField(cache, "stationsById", stations);

        // 노선 데이터
        Map<String, OdptRailway> railways = new ConcurrentHashMap<>();
        railways.put("odpt.Railway:TokyoMetro.Ginza", new OdptRailway("odpt.Railway:TokyoMetro.Ginza",
                "銀座線", Map.of("ja", "銀座線", "en", "Ginza Line"),
                "odpt.Operator:TokyoMetro", "G", null, null, null,
                List.of(
                        new OdptRailway.StationOrder(STATION_SHIBUYA_GINZA, Map.of(), 1),
                        new OdptRailway.StationOrder(STATION_UENO, Map.of(), 2)
                )));
        railways.put("odpt.Railway:JR.Yamanote", new OdptRailway("odpt.Railway:JR.Yamanote",
                "山手線", Map.of("ja", "山手線", "en", "Yamanote Line"),
                "odpt.Operator:JR", "JY", null, null, null,
                List.of(
                        new OdptRailway.StationOrder(STATION_SHIBUYA_YAMANOTE, Map.of(), 1),
                        new OdptRailway.StationOrder(STATION_TOKYO_YAMANOTE, Map.of(), 2)
                )));
        setField(cache, "railwaysById", railways);

        // 캘린더
        Map<String, OdptCalendar> calendars = new ConcurrentHashMap<>();
        calendars.put("odpt.Calendar:Weekday", new OdptCalendar("odpt.Calendar:Weekday",
                "平日", Map.of("ja", "平日", "en", "Weekday"), null, null));
        calendars.put("odpt.Calendar:SaturdayHoliday", new OdptCalendar("odpt.Calendar:SaturdayHoliday",
                "土休日", Map.of("ja", "土休日", "en", "Saturday/Holiday"), null, null));
        calendars.put("odpt.Calendar:Holiday", new OdptCalendar("odpt.Calendar:Holiday",
                "休日", Map.of("ja", "休日", "en", "Holiday"), null, null));
        setField(cache, "calendarsById", calendars);

        // 열차 종별
        Map<String, OdptTrainType> trainTypes = new ConcurrentHashMap<>();
        trainTypes.put("odpt.TrainType:TokyoMetro.Local", new OdptTrainType(
                "odpt.TrainType:TokyoMetro.Local", "odpt.Operator:TokyoMetro",
                "各停", Map.of("ja", "各停", "en", "Local")));
        setField(cache, "trainTypesById", trainTypes);

        // 역명 검색 인덱스
        Map<String, List<String>> nameIndex = new ConcurrentHashMap<>();
        for (OdptStation s : stations.values()) {
            if (s.title() != null) {
                nameIndex.computeIfAbsent(s.title().toLowerCase(), k -> new ArrayList<>()).add(s.id());
            }
            if (s.stationTitle() != null && s.stationTitle().containsKey("en")) {
                nameIndex.computeIfAbsent(s.stationTitle().get("en").toLowerCase(), k -> new ArrayList<>()).add(s.id());
            }
            String[] parts = s.id().split("\\.");
            nameIndex.computeIfAbsent(parts[parts.length - 1].toLowerCase(), k -> new ArrayList<>()).add(s.id());
        }
        setField(cache, "nameIndex", nameIndex);

        // 환승 그래프 (connectingStation 기반)
        Map<String, Set<String>> transferGraph = new ConcurrentHashMap<>();
        transferGraph.put(STATION_SHIBUYA_GINZA, new HashSet<>(Set.of(STATION_SHIBUYA_YAMANOTE)));
        transferGraph.put(STATION_SHIBUYA_YAMANOTE, new HashSet<>(Set.of(STATION_SHIBUYA_GINZA)));
        setField(cache, "transferGraph", transferGraph);

        // 요금
        Map<String, OdptRailwayFare> fares = new ConcurrentHashMap<>();
        fares.put(STATION_SHIBUYA_GINZA + "::" + STATION_UENO,
                new OdptRailwayFare("fare-1", "odpt.Operator:TokyoMetro",
                        STATION_SHIBUYA_GINZA, STATION_UENO, 200, 195, 100, 98));
        setField(cache, "fares", fares);

        // 열차 시간표
        Map<String, OdptTrainTimetable> trainTimetables = new ConcurrentHashMap<>();
        trainTimetables.put("tt-ginza-1", new OdptTrainTimetable("tt-ginza-1",
                "odpt.Operator:TokyoMetro", "odpt.Railway:TokyoMetro.Ginza", null,
                "odpt.Calendar:Weekday", "A101", "odpt.TrainType:TokyoMetro.Local",
                null, null, null,
                List.of(
                        new OdptTrainTimetable.TrainStop("23:30", STATION_SHIBUYA_GINZA, null, null, null),
                        new OdptTrainTimetable.TrainStop(null, null, "23:50", STATION_UENO, null)
                )));
        setField(cache, "trainTimetables", trainTimetables);

        // 역별 열차 시간표 인덱스
        Map<String, List<String>> stationToTT = new ConcurrentHashMap<>();
        stationToTT.put(STATION_SHIBUYA_GINZA, List.of("tt-ginza-1"));
        stationToTT.put(STATION_UENO, List.of("tt-ginza-1"));
        setField(cache, "stationToTrainTimetables", stationToTT);

        // 빈 맵들
        setField(cache, "stationTimetables", new ConcurrentHashMap<>());
        setField(cache, "railwayStationOrder", new ConcurrentHashMap<>());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}