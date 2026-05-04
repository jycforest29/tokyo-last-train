package tokyo.lasttrain.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tokyo.lasttrain.cache.TransitDataCache;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final TransitDataCache cache;
    private final String expectedToken;

    public AdminController(TransitDataCache cache,
                           @Value("${admin.token:}") String expectedToken) {
        this.cache = cache;
        this.expectedToken = expectedToken;
    }

    /**
     * Manually trigger a transit data cache refresh (for diagram-change windows).
     * Auth: HTTP header {@code X-Admin-Token} must match {@code admin.token} property
     * (which reads {@code ADMIN_TOKEN} env var). If the property is empty, refresh is disabled.
     */
    @PostMapping("/refresh-transit")
    public ResponseEntity<Map<String, Object>> refresh(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (expectedToken == null || expectedToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "admin endpoint disabled (ADMIN_TOKEN not set)"));
        }
        if (token == null || !expectedToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Admin refresh requested");
        triggerAsyncRefresh();
        return ResponseEntity.accepted().body(Map.of(
                "status", "refresh started (async)",
                "previousLastRefreshAt", String.valueOf(cache.getLastRefreshAt())
        ));
    }

    @Async
    public void triggerAsyncRefresh() {
        try {
            cache.refresh();
        } catch (RuntimeException e) {
            log.error("Async admin refresh failed", e);
        }
    }
}
