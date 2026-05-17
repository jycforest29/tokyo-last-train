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
import tokyo.lasttrain.dto.LastTrainResponse.Alternative;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * 모든 (출발역, 도착역) 카타시안곱(A→A 포함)에 대해 막차 조회 응답의 invariant를 검증한다.
 *
 * 검증 항목:
 *   1. 같은 역(from==to): notice == "SAME_STATION" 이고 routes/alternatives 가 비어 있어야 한다.
 *   2. 출발 시각이 도착 시각보다 이전이어야 한다. (자정 wrap 보정 후 0 < diff <= 6h)
 *   3. 추가 invariants:
 *      - response.fromStation, response.toStation 이 요청과 일치
 *      - 정상 응답이면 notice == null
 *      - 각 route 의 railway, departureTime, arrivalTime 이 non-null/parseable
 *      - totalFare >= 0, totalFareTicket >= 0
 *      - 각 transfer 의 waitMinutes >= 0
 *      - 각 transfer 의 arrivalTime, departureTime 이 parseable (둘 다 존재하는 경우)
 *
 * 발견되는 violation/error 는 즉시 디스크에 stream 된다
 * ({@code target/all-pairs-validation/violations.tsv}, {@code errors.tsv}).
 * SIGTERM/Ctrl+C 로 중간에 죽여도 그 시점까지의 결과는 보존되며,
 * shutdown hook 이 {@code SHUTDOWN} 행을 progress 파일에 남긴다.
 *
 * 무거운 테스트라 환경변수 {@code RUN_ALL_PAIRS_VALIDATION=true} 일 때만 실행된다.
 * 추가로 {@code ODPT_API_KEY} 가 설정돼 있어야 캐시가 로드된다.
 *
 * 실행 예:
 *   RUN_ALL_PAIRS_VALIDATION=true mvn -Dtest=AllPairsValidationTest test 2>&1 | tee /tmp/all-pairs-validation.log
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_ALL_PAIRS_VALIDATION", matches = "true")
@EnabledIfEnvironmentVariable(named = "ODPT_API_KEY", matches = ".+")
class AllPairsValidationTest {

    private static final Logger log = LoggerFactory.getLogger(AllPairsValidationTest.class);

    private static final int MAX_GAP_MINUTES = 6 * 60;
    private static final int MIDNIGHT_WRAP_THRESHOLD_MINUTES = -60;
    private static final Path OUTPUT_DIR = Path.of("target", "all-pairs-validation");

    // shutdown hook 이 인스턴스 없이도 접근하기 위해 모두 static.
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
                "from\tto\trule\tdetail");
        errorsOut = openTsv(OUTPUT_DIR.resolve("errors.tsv"),
                "from\tto\texception");
        progressOut = openTsv(OUTPUT_DIR.resolve("progress.tsv"),
                "timestamp\tphase\tpairsDone\ttotalPairs\troutes\tviolations\terrors\telapsedSec");

        Runtime.getRuntime().addShutdownHook(new Thread(AllPairsValidationTest::flushOnShutdown,
                "all-pairs-validation-shutdown-hook"));
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
                true);
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
    void validateAllPairs() {
        List<String> stations = cache.getAllStations().stream()
                .filter(s -> s.id() != null && s.railway() != null)
                .filter(s -> cache.hasTimetableForRailway(s.railway()))
                .map(OdptStation::id)
                .distinct()
                .toList();

        int n = stations.size();

        // ALL_PAIRS_SAMPLE_SIZE 가 설정돼 있으면 same-station 전량 + N 무작위 cross 페어만 검증.
        // 미설정이면 카타시안곱 전체.
        List<String[]> pairs = buildPairs(stations);
        totalPairs = pairs.size();
        log.info("All-pairs validation test: {} stations, {} pairs to check", n, totalPairs);
        writeProgress("START");
        startedMillis = System.currentTimeMillis();

        pairs.parallelStream().forEach(pair -> {
            String from = pair[0];
            String to = pair[1];
            try {
                LastTrainResponse resp = lastTrainService.findLastTrain(from, to);
                validateResponse(from, to, resp);
            } catch (RuntimeException e) {
                recordError(from, to, e);
            }
            long d = pairsDone.incrementAndGet();
            if (d % 1_000 == 0) {
                writeProgress("TICK");
            }
        });

        writeProgress("DONE");
        long elapsed = (System.currentTimeMillis() - startedMillis) / 1000;
        log.info("Finished in {}s. pairs={}, routes={}, violations={}, errors={}",
                elapsed, pairsDone.get(), routesChecked.get(),
                violationCount.get(), errorCount.get());

        if (violationCount.get() > 0 || errorCount.get() > 0) {
            fail(String.format("Validation failures: %d violations, %d exceptions across %d pairs. " +
                            "See %s/violations.tsv and errors.tsv",
                    violationCount.get(), errorCount.get(), pairsDone.get(), OUTPUT_DIR));
        }
    }

    /**
     * 검증할 페어 목록 생성.
     *   - 환경변수 {@code ALL_PAIRS_SAMPLE_SIZE} 미설정: 전체 카타시안곱 (n * n)
     *   - 설정: same-station n쌍 전량 + 무작위 cross 페어 (시드 42 로 재현 가능)
     */
    private List<String[]> buildPairs(List<String> stations) {
        int n = stations.size();
        String sample = System.getenv("ALL_PAIRS_SAMPLE_SIZE");
        if (sample == null || sample.isBlank()) {
            List<String[]> all = new ArrayList<>(n * n);
            for (String from : stations) {
                for (String to : stations) {
                    all.add(new String[]{from, to});
                }
            }
            return all;
        }

        int target;
        try {
            target = Integer.parseInt(sample.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid ALL_PAIRS_SAMPLE_SIZE={}, falling back to full cartesian", sample);
            return buildPairs(stations); // 재귀하지 않도록 위에서 일찍 처리됨
        }

        log.info("Sampling mode: {} same-station + {} random cross pairs (seed=42)", n, target);
        List<String[]> out = new ArrayList<>(n + target);
        // same-station 전량
        for (String s : stations) {
            out.add(new String[]{s, s});
        }
        // 무작위 cross 페어 (중복 제거)
        Random rng = new Random(42);
        Set<Long> seen = new HashSet<>();
        int attempts = 0;
        int maxAttempts = target * 10;
        while (out.size() < n + target && attempts < maxAttempts) {
            int i = rng.nextInt(n);
            int j = rng.nextInt(n);
            if (i == j) { attempts++; continue; }
            long key = ((long) i << 32) | (j & 0xffffffffL);
            if (seen.add(key)) {
                out.add(new String[]{stations.get(i), stations.get(j)});
            }
            attempts++;
        }
        return out;
    }

    private void validateResponse(String from, String to, LastTrainResponse resp) {
        if (resp == null) {
            recordViolation(from, to, "null_response", "service returned null");
            return;
        }

        // 응답에 담긴 from/to 가 요청과 일치하는지
        if (!from.equals(resp.fromStation())) {
            recordViolation(from, to, "fromStation_mismatch", "got=" + resp.fromStation());
        }
        if (!to.equals(resp.toStation())) {
            recordViolation(from, to, "toStation_mismatch", "got=" + resp.toStation());
        }

        // (1) same-station 분기
        if (from.equals(to)) {
            if (!"SAME_STATION".equals(resp.notice())) {
                recordViolation(from, to, "same_station_missing_notice",
                        "notice=" + resp.notice());
            }
            if (resp.routes() != null && !resp.routes().isEmpty()) {
                recordViolation(from, to, "same_station_has_routes",
                        "routes=" + resp.routes().size());
            }
            if (resp.alternatives() != null && !resp.alternatives().isEmpty()) {
                recordViolation(from, to, "same_station_has_alternatives",
                        "alternatives=" + resp.alternatives().size());
            }
            return;
        }

        // from != to: notice 는 null 이어야 한다
        if (resp.notice() != null) {
            recordViolation(from, to, "unexpected_notice", "notice=" + resp.notice());
        }

        if (resp.routes() == null) return;

        for (int i = 0; i < resp.routes().size(); i++) {
            LastTrainRoute route = resp.routes().get(i);
            routesChecked.incrementAndGet();
            validateRoute(from, to, i, route);
        }

        if (resp.alternatives() != null) {
            for (Alternative alt : resp.alternatives()) {
                if (alt.offsetFromDest() != -1 && alt.offsetFromDest() != 1) {
                    recordViolation(from, to, "alternative_bad_offset",
                            "offset=" + alt.offsetFromDest());
                }
            }
        }
    }

    private void validateRoute(String from, String to, int idx, LastTrainRoute route) {
        String ctx = "route[" + idx + "]";

        // railway non-null
        if (route.railway() == null) {
            recordViolation(from, to, ctx + ".railway_null", "");
        }

        LocalTime dep = parseOrNull(route.departureTime());
        LocalTime arr = parseOrNull(route.arrivalTime());
        if (dep == null) {
            recordViolation(from, to, ctx + ".departureTime_unparseable",
                    "value=" + route.departureTime());
            return;
        }
        if (arr == null) {
            recordViolation(from, to, ctx + ".arrivalTime_unparseable",
                    "value=" + route.arrivalTime());
            return;
        }

        // (2) 출발 < 도착 (midnight wrap 보정 후 0 < diff <= MAX_GAP)
        int diff = wrappedDiffMinutes(dep, arr);
        if (diff <= 0) {
            recordViolation(from, to, ctx + ".departure_not_before_arrival",
                    "dep=" + dep + " arr=" + arr + " diffMin=" + diff);
        } else if (diff > MAX_GAP_MINUTES) {
            recordViolation(from, to, ctx + ".journey_too_long",
                    "dep=" + dep + " arr=" + arr + " diffMin=" + diff);
        }

        // 요금 음수 금지
        if (route.totalFare() < 0) {
            recordViolation(from, to, ctx + ".totalFare_negative",
                    "fare=" + route.totalFare());
        }
        if (route.totalFareTicket() < 0) {
            recordViolation(from, to, ctx + ".totalFareTicket_negative",
                    "fare=" + route.totalFareTicket());
        }

        // 환승 검증
        if (route.transfers() != null) {
            for (int t = 0; t < route.transfers().size(); t++) {
                validateTransfer(from, to, ctx + ".transfer[" + t + "]", route.transfers().get(t));
            }
        }
    }

    private void validateTransfer(String from, String to, String ctx, Transfer transfer) {
        if (transfer.waitMinutes() < 0) {
            recordViolation(from, to, ctx + ".waitMinutes_negative",
                    "wait=" + transfer.waitMinutes());
        }

        // arrival/departure 가 있으면 parseable 해야 함
        if (transfer.arrivalTime() != null && parseOrNull(transfer.arrivalTime()) == null) {
            recordViolation(from, to, ctx + ".arrivalTime_unparseable",
                    "value=" + transfer.arrivalTime());
        }
        if (transfer.departureTime() != null && parseOrNull(transfer.departureTime()) == null) {
            recordViolation(from, to, ctx + ".departureTime_unparseable",
                    "value=" + transfer.departureTime());
        }
    }

    private void recordViolation(String from, String to, String rule, String detail) {
        synchronized (writeLock) {
            if (violationsOut != null) {
                violationsOut.printf("%s\t%s\t%s\t%s%n",
                        from, to, rule, sanitize(detail));
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

    private static LocalTime parseOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalTime.parse(s);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * curr - prev (분). 자정 경계 보정: 결과가 MIDNIGHT_WRAP_THRESHOLD_MINUTES 미만이면 +24h 를 더해 다음 날로 해석.
     */
    static int wrappedDiffMinutes(LocalTime prev, LocalTime curr) {
        int diff = (curr.toSecondOfDay() - prev.toSecondOfDay()) / 60;
        if (diff < MIDNIGHT_WRAP_THRESHOLD_MINUTES) {
            diff += 24 * 60;
        }
        return diff;
    }
}
