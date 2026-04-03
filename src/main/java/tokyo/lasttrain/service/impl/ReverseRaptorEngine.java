package tokyo.lasttrain.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.model.OdptRailway;
import tokyo.lasttrain.model.OdptTrainTimetable;
import tokyo.lasttrain.model.OdptTrainTimetable.TrainStop;

import java.time.LocalTime;
import java.util.*;

/**
 * Reverse RAPTOR: 도착역에서 거꾸로 탐색하여
 * 출발역에서 가장 늦게 출발할 수 있는 경로를 찾는다.
 *
 * 각 라운드(round)는 환승 1회를 의미:
 *   Round 0 = 직통
 *   Round 1 = 1회 환승
 *   Round 2 = 2회 환승
 *   Round 3 = 3회 환승
 */
@Component
public class ReverseRaptorEngine {

    private static final Logger log = LoggerFactory.getLogger(ReverseRaptorEngine.class);
    private static final int MAX_ROUNDS = 4; // 0~3 = 최대 3회 환승
    private static final LocalTime LAST_TRAIN_CUTOFF = LocalTime.of(1, 30); // 새벽 1:30 이후는 다음날
    private static final int TRANSFER_WALKING_MINUTES = 5; // 환승 도보 시간

    private final TransitDataCache cache;

    public ReverseRaptorEngine(TransitDataCache cache) {
        this.cache = cache;
    }

    /**
     * 출발역에서 도착역까지 가장 늦게 출발할 수 있는 경로를 찾는다.
     *
     * @return 찾은 경로 목록 (라운드별 최적 경로). 빈 리스트면 경로 없음.
     */
    public List<Journey> findLastTrains(String fromStationId, String toStationId, String calendarId) {
        log.info("Finding last train: {} → {}, calendar={}", fromStationId, toStationId, calendarId);

        // latestDeparture[stationId] = 해당 역에서 도착역까지 갈 수 있는 가장 늦은 출발 시각
        Map<String, TimeAndJourney> bestByStation = new HashMap<>();
        // 이번 라운드에서 개선된 역 목록
        Set<String> markedStations = new HashSet<>();

        // 초기화: 도착역은 언제든 "도착 완료"
        bestByStation.put(toStationId, new TimeAndJourney(
                LocalTime.of(23, 59),
                new Journey(toStationId, toStationId, List.of())
        ));
        markedStations.add(toStationId);

        List<Journey> results = new ArrayList<>();

        for (int round = 0; round < MAX_ROUNDS; round++) {
            log.debug("Round {} - marked stations: {}", round, markedStations.size());

            Set<String> newMarked = new HashSet<>();

            // 1단계: 각 marked station에서 열차를 타고 거꾸로 갈 수 있는 역 찾기
            for (String targetStation : markedStations) {
                TimeAndJourney targetBest = bestByStation.get(targetStation);
                if (targetBest == null) continue;

                // targetStation을 지나는 모든 열차 시간표를 찾아서 역방향 스캔
                scanRoutesReverse(targetStation, targetBest, calendarId, bestByStation, newMarked);
            }

            // 2단계: 환승 확장 - 개선된 역에서 환승 가능한 역으로 전파
            Set<String> transferExpanded = new HashSet<>();
            for (String stationId : newMarked) {
                Set<String> transfers = cache.getTransferStations(stationId);
                for (String transferStation : transfers) {
                    TimeAndJourney current = bestByStation.get(stationId);
                    if (current == null) continue;

                    // 환승 도보 시간 감산
                    LocalTime afterWalk = current.latestDeparture().minusMinutes(TRANSFER_WALKING_MINUTES);
                    TimeAndJourney existing = bestByStation.get(transferStation);

                    if (existing == null || afterWalk.isAfter(existing.latestDeparture())) {
                        // 환승 leg 추가
                        List<Leg> legs = new ArrayList<>(current.journey().legs());
                        legs.addFirst(new Leg(
                                transferStation, stationId,
                                "TRANSFER", null, null, null,
                                afterWalk, current.latestDeparture()
                        ));
                        bestByStation.put(transferStation, new TimeAndJourney(
                                afterWalk,
                                new Journey(transferStation, toStationId, legs)
                        ));
                        transferExpanded.add(transferStation);
                    }
                }
            }
            newMarked.addAll(transferExpanded);

            // 출발역에 도달했는지 체크
            if (bestByStation.containsKey(fromStationId)) {
                Journey journey = bestByStation.get(fromStationId).journey();
                if (!journey.legs().isEmpty()) {
                    results.add(journey);
                }
            }

            markedStations = newMarked;
            if (markedStations.isEmpty()) break;
        }

        // 라운드별 중복 제거 후 출발 시간 내림차순 정렬
        return deduplicateAndSort(results);
    }

    /**
     * targetStation을 지나는 열차를 찾아서,
     * 그 열차의 이전 역들에 대해 "가장 늦은 출발 시각"을 갱신한다.
     */
    private void scanRoutesReverse(
            String targetStation,
            TimeAndJourney targetBest,
            String calendarId,
            Map<String, TimeAndJourney> bestByStation,
            Set<String> newMarked
    ) {
        // targetStation을 지나는 열차 시간표를 인덱스에서 조회 (풀스캔 회피)
        List<OdptTrainTimetable> timetables = cache.getTrainTimetablesForStation(targetStation, calendarId);

        for (OdptTrainTimetable tt : timetables) {
            List<TrainStop> stops = tt.stops();
            if (stops == null || stops.isEmpty()) continue;

            // targetStation이 이 열차의 몇 번째 정차역인지 찾기
            int targetIdx = -1;
            for (int i = 0; i < stops.size(); i++) {
                if (targetStation.equals(stops.get(i).effectiveStation())) {
                    targetIdx = i;
                    break;
                }
            }
            if (targetIdx < 0) continue;

            // targetStation 도착 시각
            String arrivalTimeStr = stops.get(targetIdx).effectiveTime();
            if (arrivalTimeStr == null) continue;
            LocalTime arrivalAtTarget = parseTime(arrivalTimeStr);

            // 이 열차가 targetStation에 도착하는 시각이
            // targetBest의 latestDeparture보다 늦으면 탑승 불가
            if (arrivalAtTarget.isAfter(targetBest.latestDeparture())
                    && !isAfterMidnight(arrivalAtTarget)) {
                continue;
            }

            // targetStation 이전의 모든 정차역에 대해 갱신
            for (int i = 0; i < targetIdx; i++) {
                TrainStop stop = stops.get(i);
                String stationId = stop.effectiveStation();
                String depTimeStr = stop.effectiveTime();
                if (stationId == null || depTimeStr == null) continue;

                LocalTime depTime = parseTime(depTimeStr);
                TimeAndJourney existing = bestByStation.get(stationId);

                if (existing == null || depTime.isAfter(existing.latestDeparture())) {
                    // 이 열차를 타면 더 늦게 출발 가능
                    String targetTimeStr = stops.get(targetIdx).effectiveTime();
                    Leg leg = new Leg(
                            stationId, targetStation,
                            tt.railway(), tt.railDirection(), tt.trainType(), tt.trainNumber(),
                            depTime, arrivalAtTarget
                    );

                    List<Leg> legs = new ArrayList<>();
                    legs.add(leg);
                    legs.addAll(targetBest.journey().legs());

                    bestByStation.put(stationId, new TimeAndJourney(
                            depTime,
                            new Journey(stationId, targetBest.journey().toStation(), legs)
                    ));
                    newMarked.add(stationId);
                }
            }
        }
    }

    private String findRailwayForStation(String stationId) {
        var station = cache.getStation(stationId);
        return station != null ? station.railway() : null;
    }

    private LocalTime parseTime(String timeStr) {
        // "23:45" 또는 "00:15" 형식
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        // 24시, 25시 등 심야 표기 처리
        if (hour >= 24) {
            hour -= 24;
        }
        return LocalTime.of(hour, minute);
    }

    private boolean isAfterMidnight(LocalTime time) {
        return time.isBefore(LAST_TRAIN_CUTOFF);
    }

    private List<Journey> deduplicateAndSort(List<Journey> journeys) {
        // 같은 leg 구성의 중복 제거
        Map<String, Journey> unique = new LinkedHashMap<>();
        for (Journey j : journeys) {
            String key = j.legs().stream()
                    .map(l -> l.railway() + "|" + l.fromStation() + "|" + l.toStation())
                    .reduce("", (a, b) -> a + ";" + b);
            Journey existing = unique.get(key);
            if (existing == null || j.departureTime().isAfter(existing.departureTime())) {
                unique.put(key, j);
            }
        }

        return unique.values().stream()
                .sorted(Comparator.comparing(Journey::departureTime).reversed())
                .toList();
    }

    // === 내부 데이터 클래스 ===

    public record Journey(
            String fromStation,
            String toStation,
            List<Leg> legs
    ) {
        public LocalTime departureTime() {
            if (legs.isEmpty()) return LocalTime.MIN;
            return legs.getFirst().departureTime();
        }

        public LocalTime arrivalTime() {
            if (legs.isEmpty()) return LocalTime.MIN;
            return legs.getLast().arrivalTime();
        }

        public int transferCount() {
            return (int) legs.stream().filter(l -> "TRANSFER".equals(l.railway())).count();
        }
    }

    public record Leg(
            String fromStation,
            String toStation,
            String railway,
            String railDirection,
            String trainType,
            String trainNumber,
            LocalTime departureTime,
            LocalTime arrivalTime
    ) {
        public boolean isTransfer() {
            return "TRANSFER".equals(railway);
        }
    }

    record TimeAndJourney(
            LocalTime latestDeparture,
            Journey journey
    ) {}
}