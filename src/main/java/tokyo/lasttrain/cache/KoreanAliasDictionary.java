package tokyo.lasttrain.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * ODPT가 제공하지 않는 한국어 역명/노선명 별칭 사전.
 * 키는 일본어 title (예: "渋谷"), 값은 한글 표기 (예: "시부야").
 * 같은 title을 가진 모든 역(여러 노선의 渋谷)에 동일 한글이 적용된다.
 */
@Component
public class KoreanAliasDictionary {

    private static final Logger log = LoggerFactory.getLogger(KoreanAliasDictionary.class);
    private static final String RESOURCE_PATH = "station-aliases-ko.json";

    private Map<String, String> stations = Collections.emptyMap();
    private Map<String, String> railways = Collections.emptyMap();

    private final ObjectMapper objectMapper;

    public KoreanAliasDictionary(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            AliasFile file = objectMapper.readValue(in, AliasFile.class);
            this.stations = file.stations() != null ? file.stations() : Collections.emptyMap();
            this.railways = file.railways() != null ? file.railways() : Collections.emptyMap();
            log.info("Loaded Korean aliases: {} stations, {} railways", stations.size(), railways.size());
        } catch (IOException e) {
            log.warn("Failed to load Korean alias dictionary from {}: {}", RESOURCE_PATH, e.getMessage());
        }
    }

    public String stationKo(String japaneseTitle) {
        if (japaneseTitle == null) return null;
        return stations.get(japaneseTitle);
    }

    public String railwayKo(String japaneseTitle) {
        if (japaneseTitle == null) return null;
        return railways.get(japaneseTitle);
    }

    public Map<String, String> allStations() {
        return Collections.unmodifiableMap(stations);
    }

    public Map<String, String> allRailways() {
        return Collections.unmodifiableMap(railways);
    }

    record AliasFile(Map<String, String> stations, Map<String, String> railways) {}
}
