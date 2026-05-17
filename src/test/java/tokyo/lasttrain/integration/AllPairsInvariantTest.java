package tokyo.lasttrain.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.dto.LastTrainResponse;
import tokyo.lasttrain.dto.LastTrainResponse.LastTrainRoute;
import tokyo.lasttrain.dto.LastTrainResponse.Transfer;
import tokyo.lasttrain.model.OdptStation;
import tokyo.lasttrain.service.LastTrainService;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * 모든 (출발역, 도착역) 쌍에 대해 막차 조회를 돌려, 반환되는 모든 route에 대해
 * "시간이 단조 증가하는가"(midnight wrap 보정 후)를 검증한다.
 *
 * 시간 시퀀스: route.departure → t0.arrival → t0.departure → ... → route.arrival
 * 각 인접 쌍의 diff는 [-60min(wrap 임계값), 6h] 안에 들어와야 한다.
 *
 * 발견되는 violation/error는 즉시 디스크에 stream된다
 * ({@code target/all-pairs-results/violations.tsv}, {@code errors.tsv}).
 * SIGTERM/Ctrl+C로 중간에 죽여도 그 시점까지의 결과는 보존되며,
 * shutdown hook이 {@code SHUTDOWN} 행을 progress 파일에 남긴다.
 *
 * 무거운 테스트라 환경변수 {@code RUN_ALL_PAIRS_INVARIANT=true}일 때만 실행된다.
 * 추가로 {@code ODPT_API_KEY}가 설정돼 있어야 캐시가 로드된다.
 *
 * 실행 예:
 *   RUN_ALL_PAIRS_INVARIANT=true mvn -Dtest=AllPairsInvariantTest test 2>&1 | tee /tmp/all-pairs.log
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_ALL_PAIRS_INVARIANT", matches = "true")
@EnabledIfEnvironmentVariable(named = "ODPT_API_KEY", matches = ".+")
class AllPairsInvariantTest {

    private static final Logger log = LoggerFactory.getLogger(AllPairsInvariantTest.class);

    private static final int MAX_GAP_MINUTES = 6 * 60;
    private static final int MIDNIGHT_WRAP_THRESHOLD_MINUTES = -60;
    private static final Path OUTPUT_DIR = Path.of("target", "all-pairs-results");

    // 모두 static — shutdown hook이 인스턴스 없이도 접근하기 위해.
    private static final Object writeLock = new Object();
    private static PrintWriter violationsOut;
    private static PrintWriter errorsOut;
    private static PrintWriter progressOut;
    private static final AtomicLong violationCount = new AtomicLong();
    private static final AtomicLong errorCount = new AtomicLong();
    private static final AtomicLong pairsDone = new AtomicLong();
    private static final AtomicLong routesChecked = new AtomicLong();
    private static volatile long totalPairs = 0;
    private static volatile long startedMillis = 0;
    private static volatile boolean shutdownFlushDone = false;

    @Autowired
    private LastTrainService lastTrainService;

    @Autowired
    private TransitDataCache cache;

    @BeforeAll
    static void openOutputs() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        violationsOut = openTsv(OUTPUT_DIR.resolve("violations.tsv"),
                "from\tto\trouteDep\trouteArr\tprevLabel\tprev\tcurrLabel\tcurr\tdiffMin");
        errorsOut = openTsv(OUTPUT_DIR.resolve("errors.tsv"),
                "from\tto\texception");
        progressOut = openTsv(OUTPUT_DIR.resolve("progress.tsv"),
                "timestamp\tphase\tpairsDone\ttotalPairs\troutes\tviolations\terrors\telapsedSec");

        Runtime.getRuntime().addShutdownHook(new Thread(AllPairsInvariantTest::flushOnShutdown,
                "all-pairs-shutdown-hook"));
    }

    @AfterAll
    static void closeOutputs() {
        flushOnShutdown();
        synchronized (writeLock) {
            if (violationsOut != null) { violationsOut.close(); violationsOut = null; }
            if (errorsOut != null) { errorsOut.close(); errorsOut = null; }
            if (progressOut != null) { progressOut.close(); progressOut = null; }
        }
    }

    private static PrintWriter openTsv(Path path, String header) throws IOException {
        PrintWriter w = new PrintWriter(
                Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE),
                true /* autoFlush */);
        w.println(header);
        return w;
    }

    private static void flushOnShutdown() {
        if (shutdownFlushDone) return;
        synchronized (writeLock) {
            if (shutdownFlushDone) return;
            shutdownFlushDone = true;
            if (progressOut != null) {
                long elapsed = startedMillis > 0 ? (System.currentTimeMillis() - startedMillis) / 1000 : 0;
                progressOut.printf("%s\tSHUTDOWN\t%d\t%d\t%d\t%d\t%d\t%d%n",
                        Instant.now(), pairsDone.get(), totalPairs,
                        routesChecked.get(), violationCount.get(), errorCount.get(), elapsed);
                progressOut.flush();
            }
            if (violationsOut != null) violationsOut.flush();
            if (errorsOut != null) errorsOut.flush();
        }
    }

    @Test
    void timeMonotonicAcrossAllPairs() {
        List<String> stations = cache.getAllStations().stream()
                .filter(s -> s.id() != null && s.railway() != null)
                .filter(s -> cache.hasTimetableForRailway(s.railway()))
                .map(OdptStation::id)
                .distinct()
                .toList();

        int n = stations.size();
        totalPairs = (long) n * (n - 1);
        log.info("All-pairs invariant test: {} stations, {} ordered pairs", n, totalPairs);
        writeProgress("START");
        startedMillis = System.currentTimeMillis();

        stations.parallelStream().forEach(from -> {
            for (String to : stations) {
                if (from.equals(to)) continue;
                try {
                    LastTrainResponse resp = lastTrainService.findLastTrain(from, to);
                    if (resp != null && resp.routes() != null) {
                        for (LastTrainRoute route : resp.routes()) {
                            routesChecked.incrementAndGet();
                            checkRoute(from, to, route);
                        }
                    }
                } catch (RuntimeException e) {
                    recordError(from, to, e);
                }
                long d = pairsDone.incrementAndGet();
                if (d % 10_000 == 0) {
                    writeProgress("TICK");
                }
            }
        });

        writeProgress("DONE");
        long elapsed = (System.currentTimeMillis() - startedMillis) / 1000;
        log.info("Finished in {}s. pairs={}, routes={}, violations={}, errors={}",
                elapsed, pairsDone.get(), routesChecked.get(),
                violationCount.get(), errorCount.get());

        if (violationCount.get() > 0 || errorCount.get() > 0) {
            fail(String.format("Invariant failures: %d violations, %d exceptions across %d pairs. " +
                            "See %s/violations.tsv and errors.tsv",
                    violationCount.get(), errorCount.get(), pairsDone.get(), OUTPUT_DIR));
        }
    }

    private void checkRoute(String from, String to, LastTrainRoute route) {
        List<TimedPoint> seq = buildTimeSequence(route);
        if (seq.size() < 2) return;

        for (int i = 1; i < seq.size(); i++) {
            TimedPoint prev = seq.get(i - 1);
            TimedPoint curr = seq.get(i);
            int diff = wrappedDiffMinutes(prev.time(), curr.time());
            if (diff < MIDNIGHT_WRAP_THRESHOLD_MINUTES || diff > MAX_GAP_MINUTES) {
                recordViolation(from, to, route, prev, curr, diff);
                return;
            }
        }
    }

    private void recordViolation(String from, String to, LastTrainRoute route,
                                 TimedPoint prev, TimedPoint curr, int diff) {
        synchronized (writeLock) {
            if (violationsOut != null) {
                violationsOut.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%d%n",
                        from, to, route.departureTime(), route.arrivalTime(),
                        prev.label(), prev.time(), curr.label(), curr.time(), diff);
            }
        }
        violationCount.incrementAndGet();
    }

    private void recordError(String from, String to, RuntimeException e) {
        String msg = e.getClass().getSimpleName() + ": " + sanitize(e.getMessage());
        synchronized (writeLock) {
            if (errorsOut != null) {
                errorsOut.printf("%s\t%s\t%s%n", from, to, msg);
            }
        }
        errorCount.incrementAndGet();
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static void writeProgress(String phase) {
        long elapsed = startedMillis > 0 ? (System.currentTimeMillis() - startedMillis) / 1000 : 0;
        synchronized (writeLock) {
            if (progressOut != null) {
                progressOut.printf("%s\t%s\t%d\t%d\t%d\t%d\t%d\t%d%n",
                        Instant.now(), phase, pairsDone.get(), totalPairs,
                        routesChecked.get(), violationCount.get(), errorCount.get(), elapsed);
            }
        }
        if (totalPairs > 0) {
            log.info("Progress[{}]: {}/{} pairs ({}%), routes={}, violations={}, errors={}, elapsed {}s",
                    phase, pairsDone.get(), totalPairs,
                    (pairsDone.get() * 100) / totalPairs,
                    routesChecked.get(), violationCount.get(), errorCount.get(), elapsed);
        } else {
            log.info("Progress[{}]: pairs={}, elapsed {}s", phase, pairsDone.get(), elapsed);
        }
    }

    private static List<TimedPoint> buildTimeSequence(LastTrainRoute route) {
        List<TimedPoint> seq = new ArrayList<>();
        LocalTime dep = parseOrNull(route.departureTime());
        LocalTime arr = parseOrNull(route.arrivalTime());
        if (dep == null || arr == null) return seq;

        seq.add(new TimedPoint("route.departure", dep));
        if (route.transfers() != null) {
            for (int i = 0; i < route.transfers().size(); i++) {
                Transfer t = route.transfers().get(i);
                LocalTime tArr = parseOrNull(t.arrivalTime());
                LocalTime tDep = parseOrNull(t.departureTime());
                if (tArr != null) seq.add(new TimedPoint("transfer[" + i + "].arrival", tArr));
                if (tDep != null) seq.add(new TimedPoint("transfer[" + i + "].departure", tDep));
            }
        }
        seq.add(new TimedPoint("route.arrival", arr));
        return seq;
    }

    private static LocalTime parseOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalTime.parse(s);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * curr - prev (분). 자정 경계 보정: 결과가 -60분 미만이면 +24h를 더해 다음 날로 해석.
     * (-60~0 사이는 보정하지 않아 진짜 위반으로 잡힌다.)
     */
    static int wrappedDiffMinutes(LocalTime prev, LocalTime curr) {
        int diff = (curr.toSecondOfDay() - prev.toSecondOfDay()) / 60;
        if (diff < MIDNIGHT_WRAP_THRESHOLD_MINUTES) {
            diff += 24 * 60;
        }
        return diff;
    }

    private record TimedPoint(String label, LocalTime time) {}
}
