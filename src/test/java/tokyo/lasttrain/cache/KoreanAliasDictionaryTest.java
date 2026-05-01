package tokyo.lasttrain.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KoreanAliasDictionaryTest {

    private KoreanAliasDictionary dict;

    @BeforeEach
    void setUp() {
        dict = new KoreanAliasDictionary(new ObjectMapper());
        dict.load();
    }

    @Test
    @DisplayName("리소스 파일에서 사전을 로드한다")
    void loadsAliasFile() {
        assertFalse(dict.allStations().isEmpty(), "역 사전이 비어있으면 안 됨");
        assertFalse(dict.allRailways().isEmpty(), "노선 사전이 비어있으면 안 됨");
    }

    @Test
    @DisplayName("주요 역의 한국어 표기를 반환한다")
    void majorStationsHaveKorean() {
        assertEquals("시부야", dict.stationKo("渋谷"));
        assertEquals("도쿄", dict.stationKo("東京"));
        assertEquals("신주쿠", dict.stationKo("新宿"));
        assertEquals("이케부쿠로", dict.stationKo("池袋"));
        assertEquals("우에노", dict.stationKo("上野"));
    }

    @Test
    @DisplayName("주요 노선의 한국어 표기를 반환한다")
    void majorRailwaysHaveKorean() {
        assertEquals("야마노테선", dict.railwayKo("山手線"));
        assertEquals("긴자선", dict.railwayKo("銀座線"));
        assertEquals("주오선", dict.railwayKo("中央線"));
    }

    @Test
    @DisplayName("사전에 없는 역명은 null")
    void unknownStationReturnsNull() {
        assertNull(dict.stationKo("存在しない駅"));
    }

    @Test
    @DisplayName("사전에 없는 노선명은 null")
    void unknownRailwayReturnsNull() {
        assertNull(dict.railwayKo("Fake Line"));
    }

    @Test
    @DisplayName("null 입력은 null 반환")
    void nullInputReturnsNull() {
        assertNull(dict.stationKo(null));
        assertNull(dict.railwayKo(null));
    }

    @Test
    @DisplayName("allStations / allRailways는 불변 맵")
    void immutableMaps() {
        assertThrows(UnsupportedOperationException.class,
                () -> dict.allStations().put("test", "test"));
        assertThrows(UnsupportedOperationException.class,
                () -> dict.allRailways().put("test", "test"));
    }
}
