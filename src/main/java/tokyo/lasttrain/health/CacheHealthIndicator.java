package tokyo.lasttrain.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import tokyo.lasttrain.cache.TransitDataCache;

@Component
public class CacheHealthIndicator implements HealthIndicator {

    private final TransitDataCache cache;

    public CacheHealthIndicator(TransitDataCache cache) {
        this.cache = cache;
    }

    @Override
    public Health health() {
        if (!cache.isReady()) {
            return Health.down().withDetail("cache", "loading").build();
        }
        return Health.up().withDetail("stations", cache.getAllStations().size()).build();
    }
}
