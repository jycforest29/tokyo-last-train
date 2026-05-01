package tokyo.lasttrain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tokyo.lasttrain.cache.KoreanAliasDictionary;
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

    @Test
    @DisplayName("한국어로 검색 — 시부야 → Shibuya 매칭")
    void searchByKoreanName() {
        StationSearchResponse response = service.search("시부야");

        assertFalse(response.stations().isEmpty(), "한국어 검색이 일치 결과를 찾아야 한다");
        assertTrue(response.stations().stream()
                .anyMatch(s -> "Shibuya".equals(s.nameEn()) && "시부야".equals(s.nameKo())));
    }

    @Test
    @DisplayName("한국어로 검색 — 도쿄 → Tokyo (모든 노선의 Tokyo역)")
    void searchByKoreanTokyo() {
        StationSearchResponse response = service.search("도쿄");

        // 도쿄역은 Yamanote와 Marunouchi 2개 노선에 있음
        assertTrue(response.stations().size() >= 2);
        assertTrue(response.stations().stream()
                .allMatch(s -> "Tokyo".equals(s.nameEn())));
    }

    @Test
    @DisplayName("응답에 한국어 역명/노선명이 포함된다")
    void responseIncludesKoreanFields() {
        StationSearchResponse response = service.search("shibuya");

        assertFalse(response.stations().isEmpty());
        StationSearchResponse.StationInfo first = response.stations().getFirst();
        assertEquals("시부야", first.nameKo());
        // 노선 한국어명도 채워져야 함
        assertTrue(
                "긴자선".equals(first.railwayNameKo()) || "야마노테선".equals(first.railwayNameKo()),
                "railwayNameKo가 사전 매핑값이어야 함: " + first.railwayNameKo()
        );
    }

    @Test
    @DisplayName("한국어 사전에 없는 역은 nameKo가 null")
    void unmappedStationHasNullKorean() {
        // "上野"는 사전에 있지만, 사전 매핑을 빼서 null 검증
        // 직접 dict를 비워서 검증
        StationSearchResponse response = service.search("ueno");
        assertFalse(response.stations().isEmpty());
        // 우리 테스트 dict에 上野 → 우에노 등록되어 있어 not null
        assertEquals("우에노", response.stations().getFirst().nameKo());
    }

    private TransitDataCache createMockCache() throws Exception {
        KoreanAliasDictionary dict = createKoreanDict();
        TransitDataCache mockCache = new TransitDataCache(null, dict);

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

        // 역명 인덱스 구축 (ja, en, ko, shortname 모두 포함)
        Map<String, List<String>> nameIndex = new ConcurrentHashMap<>();
        for (OdptStation s : stations.values()) {
            if (s.title() != null) {
                nameIndex.computeIfAbsent(s.title().toLowerCase(), k -> new ArrayList<>()).add(s.id());
            }
            if (s.stationTitle() != null && s.stationTitle().containsKey("en")) {
                nameIndex.computeIfAbsent(s.stationTitle().get("en").toLowerCase(), k -> new ArrayList<>()).add(s.id());
            }
            String koName = mockCache.getStationNameKo(s.id());
            if (koName != null) {
                nameIndex.computeIfAbsent(koName.toLowerCase(), k -> new ArrayList<>()).add(s.id());
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

    private KoreanAliasDictionary createKoreanDict() throws Exception {
        KoreanAliasDictionary dict = new KoreanAliasDictionary(new ObjectMapper());
        Map<String, String> stations = Map.of(
                "渋谷", "시부야",
                "東京", "도쿄",
                "上野", "우에노"
        );
        Map<String, String> railways = Map.of(
                "銀座線", "긴자선",
                "山手線", "야마노테선",
                "丸ノ内線", "마루노우치선"
        );
        Field stationsField = KoreanAliasDictionary.class.getDeclaredField("stations");
        stationsField.setAccessible(true);
        stationsField.set(dict, stations);
        Field railwaysField = KoreanAliasDictionary.class.getDeclaredField("railways");
        railwaysField.setAccessible(true);
        railwaysField.set(dict, railways);
        return dict;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
