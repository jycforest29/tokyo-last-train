package tokyo.lasttrain.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransitDataRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransitDataRefreshScheduler.class);

    private final TransitDataCache cache;

    public TransitDataRefreshScheduler(TransitDataCache cache) {
        this.cache = cache;
    }

    // 매주 일요일 새벽 3시 (JST). 다이어 개정·시간표 변경 반영 목적.
    @Scheduled(cron = "0 0 3 ? * SUN", zone = "Asia/Tokyo")
    public void refreshCache() {
        try {
            cache.refresh();
        } catch (Exception e) {
            log.error("Scheduled cache refresh failed", e);
        }
    }
}
