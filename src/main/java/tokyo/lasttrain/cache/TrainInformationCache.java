package tokyo.lasttrain.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tokyo.lasttrain.client.OdptApiClient;
import tokyo.lasttrain.model.OdptTrainInformation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 실시간 운행 정보(odpt:TrainInformation)를 90초마다 폴링하여 캐싱.
 * dump 엔드포인트가 없는 API라 일반 query를 직접 호출한다.
 */
@Component
public class TrainInformationCache {

    private static final Logger log = LoggerFactory.getLogger(TrainInformationCache.class);

    private final OdptApiClient apiClient;
    private final Map<String, OdptTrainInformation> byRailway = new ConcurrentHashMap<>();
    private volatile Instant lastUpdatedAt;
    private volatile String lastError;

    public TrainInformationCache(OdptApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Scheduled(fixedDelay = 90_000, initialDelay = 60_000)
    public void refresh() {
        try {
            List<OdptTrainInformation> list = apiClient.fetch(
                    "odpt:TrainInformation", "", new TypeReference<>() {});
            Map<String, OdptTrainInformation> next = new ConcurrentHashMap<>();
            for (OdptTrainInformation ti : list) {
                if (ti.railway() != null) next.put(ti.railway(), ti);
            }
            byRailway.clear();
            byRailway.putAll(next);
            lastUpdatedAt = Instant.now();
            lastError = null;
            log.debug("TrainInformation refreshed: {} entries", next.size());
        } catch (RuntimeException e) {
            lastError = e.getMessage();
            log.warn("TrainInformation refresh failed: {}", e.getMessage());
        }
    }

    public OdptTrainInformation getForRailway(String railwayId) {
        return byRailway.get(railwayId);
    }

    public List<OdptTrainInformation> all() {
        return List.copyOf(byRailway.values());
    }

    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public String getLastError() { return lastError; }
}
