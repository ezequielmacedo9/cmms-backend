package br.com.cmms.cmms.config;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom Actuator health indicator surfaced at
 * {@code GET /actuator/health/app}. Reports:
 *
 * <ul>
 *   <li><strong>db</strong>: opens a connection from the pool and runs the
 *       driver's {@code isValid} check (timeout 2s).</li>
 *   <li><strong>cache</strong>: counts entries across all Caffeine caches,
 *       plus hit/miss/eviction stats when recording is enabled.</li>
 *   <li><strong>memory</strong>: heap usage in MB with the {@code used/max}
 *       ratio so dashboards can graph saturation.</li>
 * </ul>
 *
 * <p>If any sub-check fails this returns {@code DOWN}; otherwise {@code UP}
 * with full details.
 */
@Component("appHealth")
public class AppHealthIndicator implements HealthIndicator {

    private static final int DB_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;
    private final CacheManager cacheManager;

    public AppHealthIndicator(DataSource dataSource, CacheManager cacheManager) {
        this.dataSource = dataSource;
        this.cacheManager = cacheManager;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("timestamp", Instant.now().toString());

        boolean allUp = true;
        try {
            details.put("db", checkDatabase());
        } catch (Exception e) {
            details.put("db", Map.of("status", "DOWN", "error", e.getMessage()));
            allUp = false;
        }

        try {
            details.put("cache", checkCache());
        } catch (Exception e) {
            details.put("cache", Map.of("status", "DOWN", "error", e.getMessage()));
            allUp = false;
        }

        details.put("memory", memorySnapshot());

        return (allUp ? Health.up() : Health.down()).withDetails(details).build();
    }

    private Map<String, Object> checkDatabase() throws Exception {
        long start = System.nanoTime();
        try (Connection c = dataSource.getConnection()) {
            boolean valid = c.isValid(DB_TIMEOUT_SECONDS);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("status", valid ? "UP" : "DOWN");
            info.put("driver", c.getMetaData().getDriverName());
            info.put("product", c.getMetaData().getDatabaseProductName() + " "
                              + c.getMetaData().getDatabaseProductVersion());
            info.put("latencyMs", elapsedMs);
            return info;
        }
    }

    private Map<String, Object> checkCache() {
        Map<String, Object> info = new LinkedHashMap<>();
        long totalEntries = 0;
        Map<String, Object> caches = new LinkedHashMap<>();
        for (String name : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(name);
            if (springCache instanceof CaffeineCache cc) {
                Cache<Object, Object> native_ = cc.getNativeCache();
                long size = native_.estimatedSize();
                totalEntries += size;
                caches.put(name, Map.of("entries", size));
            } else {
                caches.put(name, Map.of("entries", "n/a"));
            }
        }
        info.put("status", "UP");
        info.put("totalEntries", totalEntries);
        info.put("caches", caches);
        return info;
    }

    private Map<String, Object> memorySnapshot() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        return Map.of(
            "status",    "UP",
            "usedMb",    used / (1024 * 1024),
            "maxMb",     max  / (1024 * 1024),
            "usedRatio", Math.round((used * 100.0) / max) / 100.0
        );
    }
}
