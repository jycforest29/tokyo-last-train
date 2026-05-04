package tokyo.lasttrain.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import tokyo.lasttrain.cache.TransitDataCache;

import java.time.Duration;
import java.time.Instant;

@Component
public class CacheHealthIndicator implements HealthIndicator {

    /** 마지막 refresh 이후 이 시간이 지나면 stale로 판단 (스케줄: 매주 일요일 03:00 JST). */
    private static final Duration STALE_THRESHOLD = Duration.ofDays(8);

    private final TransitDataCache cache;

    public CacheHealthIndicator(TransitDataCache cache) {
        this.cache = cache;
    }

    @Override
    public Health health() {
        if (!cache.isReady()) {
            return Health.down()
                    .withDetail("cache", "loading")
                    .withDetail("consecutiveFailures", cache.getConsecutiveFailures())
                    .withDetail("lastError", cache.getLastRefreshError())
                    .build();
        }
        Instant lastRefresh = cache.getLastRefreshAt();
        boolean stale = lastRefresh != null && Duration.between(lastRefresh, Instant.now()).compareTo(STALE_THRESHOLD) > 0;
        Health.Builder b = stale ? Health.status("STALE") : Health.up();
        return b
                .withDetail("stations", cache.getAllStations().size())
                .withDetail("trainTimetables", cache.getAllTrainTimetables().size())
                .withDetail("lastRefreshAt", lastRefresh)
                .withDetail("lastRefreshDurationMs", cache.getLastRefreshDurationMs())
                .withDetail("consecutiveFailures", cache.getConsecutiveFailures())
                .build();
    }
}
