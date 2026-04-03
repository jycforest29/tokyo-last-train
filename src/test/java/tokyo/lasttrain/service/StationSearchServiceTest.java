package tokyo.lasttrain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.dto.StationSearchResponse;
import tokyo.lasttrain.model.OdptRailway;
import tokyo.lasttrain.model.OdptStation;
import tokyo.lasttrain.service.impl.StationSearchServiceImpl;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class StationSearchServiceTest {

    private StationSearchServiceImpl service;
    private TransitDataCache cache;

    @BeforeEach
    void setUp() throws Exception {
        cache = createMockCache();
        service = new StationSearchServiceImpl(cache);
    }

    @Test
    @DisplayName("영어 이름으로 검색")
    void searchByEnglishName() {
        StationSearchResponse response = service.search("shibuya");

        assertFalse(response.stations().isEmpty());
        assertTrue(response.stations().stream()
                .anyMatch(s -> "Shibuya".equals(s.nameEn())));
    }

    @Test
    @DisplayName("일본어 이름으로 검색")
    void searchByJapaneseName() {
        StationSearchResponse response = service.search("渋谷");

        assertFalse(response.stations().isEmpty());
        assertTrue(response.stations().stream()
                .anyMatch(s -> "渋谷".equals(s.nameJa())));
    }

    @Test
    @DisplayName("부분 일치 검색")
    void partialMatch() {
        StationSearchResponse response = service.search("shib");

        assertFalse(response.stations().isEmpty());
    }

    @Test
    @DisplayName("대소문자 구분 없이 검색")
    void caseInsensitive() {
        StationSearchResponse response1 = service.search("SHIBUYA");
        StationSearchResponse response2 = service.search("shibuya");

        assertEquals(response1.stations().size(), response2.stations().size());
    }

    @Test
    @DisplayName("검색 결과 없음")
    void noResults() {
        StationSearchResponse response = service.search("nonexistent");

        assertTrue(response.stations().isEmpty());
    }

    @Test
    @DisplayName("결과 최대 20건 제한")
    void maxResults() {
        StationSearchResponse response = service.search("station");

        assertTrue(response.stations().size() <= 20);
    }

    @Test
    @DisplayName("같은 역명이 여러 노선에 있으면 모두 반환")
    void sameNameDifferentLines() {
        StationSearchResponse response = service.search("tokyo");

        // Tokyo역은 Yamanote, Marunouchi 2개 노선에 있음
        assertTrue(response.stations().size() >= 2);
    }

    private TransitDataCache createMockCache() throws Exception {
        TransitDataCache mockCache = new TransitDataCache(null);

        Map<String, OdptStation> stations = new ConcurrentHashMap<>();
        stations.put("odpt.Station:Test.Ginza.Shibuya",
                new OdptStation("odpt.Station:Test.Ginza.Shibuya", "渋谷",
                        Map.of("ja", "渋谷", "en", "Shibuya"),
                        "odpt.Operator:Test", "odpt.Railway:Test.Ginza", "G01",
                        35.6580, 139.7016, null, null));

        stations.put("odpt.Station:Test.Yamanote.Shibuya",
                new OdptStation("odpt.Station:Test.Yamanote.Shibuya", "渋谷",
                        Map.of("ja", "渋谷", "en", "Shibuya"),
                        "odpt.Operator:Test", "odpt.Railway:Test.Yamanote", "JY20",
                        35.6580, 139.7016, null, null));

        stations.put("odpt.Station:Test.Yamanote.Tokyo",
                new OdptStation("odpt.Station:Test.Yamanote.Tokyo", "東京",
                        Map.of("ja", "東京", "en", "Tokyo"),
                        "odpt.Operator:Test", "odpt.Railway:Test.Yamanote", "JY01",
                        35.6812, 139.7671, null, null));

        stations.put("odpt.Station:Test.Marunouchi.Tokyo",
                new OdptStation("odpt.Station:Test.Marunouchi.Tokyo", "東京",
                        Map.of("ja", "東京", "en", "Tokyo"),
                        "odpt.Operator:Test", "odpt.Railway:Test.Marunouchi", "M17",
                        35.6812, 139.7671, null, null));

        stations.put("odpt.Station:Test.Ginza.Ueno",
                new OdptStation("odpt.Station:Test.Ginza.Ueno", "上野",
                        Map.of("ja", "上野", "en", "Ueno"),
                        "odpt.Operator:Test", "odpt.Railway:Test.Ginza", "G16",
                        35.7141, 139.7774, null, null));

        setField(mockCache, "stationsById", stations);

        // 노선 데이터
        Map<String, OdptRailway> railways = new ConcurrentHashMap<>();
        railways.put("odpt.Railway:Test.Ginza",
                new OdptRailway("odpt.Railway:Test.Ginza", "銀座線",
                        Map.of("ja", "銀座線", "en", "Ginza Line"),
                        "odpt.Operator:Test", "G", null, null, null, List.of()));
        railways.put("odpt.Railway:Test.Yamanote",
                new OdptRailway("odpt.Railway:Test.Yamanote", "山手線",
                        Map.of("ja", "山手線", "en", "Yamanote Line"),
                        "odpt.Operator:Test", "JY", null, null, null, List.of()));
        railways.put("odpt.Railway:Test.Marunouchi",
                new OdptRailway("odpt.Railway:Test.Marunouchi", "丸ノ内線",
                        Map.of("ja", "丸ノ内線", "en", "Marunouchi Line"),
                        "odpt.Operator:Test", "M", null, null, null, List.of()));
        setField(mockCache, "railwaysById", railways);

        // 역명 인덱스 구축
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
        setField(mockCache, "nameIndex", nameIndex);

        // 빈 맵들
        setField(mockCache, "stationTimetables", new ConcurrentHashMap<>());
        setField(mockCache, "trainTimetables", new ConcurrentHashMap<>());
        setField(mockCache, "fares", new ConcurrentHashMap<>());
        setField(mockCache, "trainTypesById", new ConcurrentHashMap<>());
        setField(mockCache, "calendarsById", new ConcurrentHashMap<>());
        setField(mockCache, "transferGraph", new ConcurrentHashMap<>());
        setField(mockCache, "railwayStationOrder", new ConcurrentHashMap<>());
        setField(mockCache, "stationToTrainTimetables", new ConcurrentHashMap<>());

        return mockCache;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
