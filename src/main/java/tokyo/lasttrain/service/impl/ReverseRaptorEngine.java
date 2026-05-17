package tokyo.lasttrain.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.model.OdptTrainTimetable;
import tokyo.lasttrain.model.OdptTrainTimetable.TrainStop;

import java.util.*;

/**
 * Reverse RAPTOR: 도착역에서 거꾸로 탐색하여
 * 출발역에서 가장 늦게 출발할 수 있는 경로를 찾는다.
 *
 * 시간은 모두 "service-day 기준 00:00부터의 분(int)"으로 표현한다.
 * 25:30(다음날 01:30) 같은 ODPT 심야 표기도 그대로 1530분으로 보존되며,
 * LocalTime의 mod-24h 한계 때문에 자정 경계에서 발생하던 비교 오류를 피한다.
 *
 * 각 라운드(round)는 환승 1회를 의미:
 *   Round 0 = 직통, Round 1 = 1회 환승, ... , Round 3 = 3회 환승
 */
@Component
public class ReverseRaptorEngine {

    private static final Logger log = LoggerFactory.getLogger(ReverseRaptorEngine.class);
    private static final int MAX_ROUNDS = 4; // 0~3 = 최대 3회 환승
    private static final int TRANSFER_WALKING_MINUTES = 5; // 환승 도보 시간

    /** 도착역의 초기 deadline. 어떤 도착 시각도 허용한다는 의미. */
    private static final int NO_DEADLINE = Integer.MAX_VALUE / 2;

    private final TransitDataCache cache;

    public ReverseRaptorEngine(TransitDataCache cache) {
        this.cache = cache;
    }

    public List<Journey> findLastTrains(String fromStationId, String toStationId, String calendarId) {
        return findLastTrains(fromStationId, toStationId, Set.of(calendarId));
    }

    public List<Journey> findLastTrains(String fromStationId, String toStationId, Set<String> calendarIds) {
        log.info("Finding last train: {} → {}, calendars={}", fromStationId, toStationId, calendarIds);

        Map<String, TimeAndJourney> bestByStation = new HashMap<>();
        Set<String> markedStations = new HashSet<>();

        // 도착역은 도착 deadline 없음.
        bestByStation.put(toStationId, new TimeAndJourney(
                NO_DEADLINE,
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
                scanRoutesReverse(targetStation, targetBest, calendarIds, bestByStation, newMarked);
            }

            // 2단계: 환승 확장 - 개선된 역에서 환승 가능한 역으로 전파
            Set<String> transferExpanded = new HashSet<>();
            for (String stationId : newMarked) {
                Set<String> transfers = cache.getTransferStations(stationId);
                for (String transferStation : transfers) {
                    TimeAndJourney current = bestByStation.get(stationId);
                    if (current == null) continue;

                    int afterWalk = current.latestDeparture() - TRANSFER_WALKING_MINUTES;
                    TimeAndJourney existing = bestByStation.get(transferStation);

                    if (existing == null || afterWalk > existing.latestDeparture()) {
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

        return deduplicateAndSort(results);
    }

    /**
     * targetStation을 지나는 열차를 찾아서,
     * 그 열차의 이전 역들에 대해 "가장 늦은 출발 시각"을 갱신한다.
     */
    private void scanRoutesReverse(
            String targetStation,
            TimeAndJourney targetBest,
            Set<String> calendarIds,
            Map<String, TimeAndJourney> bestByStation,
            Set<String> newMarked
    ) {
        List<OdptTrainTimetable> timetables = cache.getTrainTimetablesForStation(targetStation, calendarIds);

        for (OdptTrainTimetable tt : timetables) {
            List<TrainStop> stops = tt.stops();
            if (stops == null || stops.isEmpty()) continue;

            int targetIdx = -1;
            for (int i = 0; i < stops.size(); i++) {
                if (targetStation.equals(stops.get(i).effectiveStation())) {
                    targetIdx = i;
                    break;
                }
            }
            if (targetIdx < 0) continue;

            String arrivalTimeStr = stops.get(targetIdx).effectiveTime();
            if (arrivalTimeStr == null) continue;
            int arrivalAtTarget = parseTime(arrivalTimeStr);

            // 환승 deadline을 못 맞추면 탑승 불가.
            // service-day 분 단위라 자정 경계에서도 단순 부등식이 정확하다.
            if (arrivalAtTarget > targetBest.latestDeparture()) {
                continue;
            }

            for (int i = 0; i < targetIdx; i++) {
                TrainStop stop = stops.get(i);
                String stationId = stop.effectiveStation();
                String depTimeStr = stop.effectiveTime();
                if (stationId == null || depTimeStr == null) continue;

                int depTime = parseTime(depTimeStr);
                TimeAndJourney existing = bestByStation.get(stationId);

                if (existing == null || depTime > existing.latestDeparture()) {
                    Leg leg = new Leg(
                            stationId, targetStation,
                            tt.railway(), tt.railDirection(), tt.trainType(), tt.trainNumber(),
                            depTime, arrivalAtTarget,
                            stop.platformNumber(),
                            stops.get(targetIdx).platformNumber()
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

    /**
     * "HH:mm" 또는 "HH:mm:ss" 표기를 00:00 기준 분(int)으로 변환.
     * ODPT 심야 표기인 24+시간(예: "25:30")도 그대로 살린다.
     */
    private static int parseTime(String timeStr) {
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour * 60 + minute;
    }

    private List<Journey> deduplicateAndSort(List<Journey> journeys) {
        Map<String, Journey> unique = new LinkedHashMap<>();
        for (Journey j : journeys) {
            String key = j.legs().stream()
                    .map(l -> l.railway() + "|" + l.fromStation() + "|" + l.toStation())
                    .reduce("", (a, b) -> a + ";" + b);
            Journey existing = unique.get(key);
            if (existing == null || j.departureMinutes() > existing.departureMinutes()) {
                unique.put(key, j);
            }
        }

        return unique.values().stream()
                .sorted(Comparator.comparingInt(Journey::departureMinutes).reversed())
                .toList();
    }

    // === 내부 데이터 클래스 ===

    public record Journey(
            String fromStation,
            String toStation,
            List<Leg> legs
    ) {
        public int departureMinutes() {
            if (legs.isEmpty()) return 0;
            return legs.getFirst().departureMinutes();
        }

        public int arrivalMinutes() {
            if (legs.isEmpty()) return 0;
            return legs.getLast().arrivalMinutes();
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
            int departureMinutes,
            int arrivalMinutes,
            String fromPlatform,
            String toPlatform
    ) {
        public Leg(String fromStation, String toStation, String railway, String railDirection,
                   String trainType, String trainNumber, int departureMinutes, int arrivalMinutes) {
            this(fromStation, toStation, railway, railDirection, trainType, trainNumber,
                    departureMinutes, arrivalMinutes, null, null);
        }

        public boolean isTransfer() {
            return "TRANSFER".equals(railway);
        }
    }

    record TimeAndJourney(
            int latestDeparture,
            Journey journey
    ) {}
}
