package tokyo.lasttrain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tokyo.lasttrain.cache.TransitDataCache;
import tokyo.lasttrain.model.*;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine.Journey;
import tokyo.lasttrain.service.impl.ReverseRaptorEngine.Leg;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reverse RAPTOR 알고리즘 정확성 테스트.
 *
 * 다음 시나리오를 검증한다:
 * - 여러 후보 경로 중 가장 늦게 출발하는 경로 선택
 * - 2회 환승 (multi-transfer) 시나리오
 * - 자정 넘김 (24:30 → 익일 00:30) 처리
 * - 환승 도보 시간(5분) 강제
 * - 환승 시각이 따라잡을 수 없으면 후보에서 제외
 * - 같은 이름 자동 환승 (connectingStation 없을 때)
 *
 * "최단거리"의 의미: 막차 검색에서는 출발지에서 가장 늦게 출발 가능한 경로 = 우리가 찾는 답.
 */
class ReverseRaptorAlgorithmTest {

    static final String WEEKDAY = "odpt.Calendar:Weekday";

    // ====== 시나리오 1: 두 후보 경로 중 늦은 출발 선택 ======

    @Nested
    @DisplayName("여러 후보 경로 중 가장 늦은 출발이 선택된다")
    class LatestDepartureSelection {

        @Test
        @DisplayName("같은 노선의 다른 시각 열차 — 마지막 열차가 선택된다")
        void picksLastTrainOfDay() throws Exception {
            // A → B 직통, 22:00 / 23:00 / 23:50 세 편
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "Line1");
            fb.addStation("B", "B", "Line1");
            fb.addRailway("Line1", "1선", "A", "B");
            fb.addTrain("Line1", "t1", "A:22:00", "B:22:30");
            fb.addTrain("Line1", "t2", "A:23:00", "B:23:30");
            fb.addTrain("Line1", "t3", "A:23:50", "B:24:20");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("B"), WEEKDAY);

            assertFalse(journeys.isEmpty());
            // 23:50 출발이 선택되어야 함 (마지막 열차)
            assertEquals(23 * 60 + 50, journeys.getFirst().departureMinutes());
        }

        @Test
        @DisplayName("환승 1회 vs 직통 — 더 늦게 출발하는 쪽이 선택된다")
        void prefersLaterDepartureRegardlessOfTransferCount() throws Exception {
            // A → C 직통: 22:00 출발
            // A → B → C 환승: 23:00 출발
            // 막차 = 더 늦게 출발하는 환승 경로
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B-L1", "B", "L1");
            fb.addStation("B-L2", "B", "L2");
            fb.addStation("C-L1", "C", "L1");
            fb.addStation("C-L2", "C", "L2");
            fb.addRailway("L1", "1선", "A", "B-L1", "C-L1");
            fb.addRailway("L2", "2선", "B-L2", "C-L2");
            // 직통 (이른 출발)
            fb.addTrain("L1", "direct", "A:22:00", "B-L1:22:30", "C-L1:23:00");
            // 환승 시나리오: A→B는 L1 막차 23:00, B에서 L2로 23:30 출발
            fb.addTrain("L1", "ride1", "A:23:00", "B-L1:23:30");
            fb.addTrain("L2", "ride2", "B-L2:23:35", "C-L2:23:55");
            fb.connect("B-L1", "B-L2");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("C-L2"), WEEKDAY);

            assertFalse(journeys.isEmpty());
            Journey best = journeys.getFirst();
            // 가장 늦게 출발하는 경로 = 환승 23:00
            assertEquals(23 * 60, best.departureMinutes());
            assertTrue(best.transferCount() > 0, "환승이 더 늦게 출발하면 환승이 선택됨");
        }
    }

    // ====== 시나리오 2: 2회 환승 ======

    @Nested
    @DisplayName("2회 환승 시나리오")
    class MultiTransfer {

        @Test
        @DisplayName("A→B→C→D, B/C 두 번 환승 가능한 경로를 찾는다")
        void twoTransfersChain() throws Exception {
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B-L1", "B", "L1");
            fb.addStation("B-L2", "B", "L2");
            fb.addStation("C-L2", "C", "L2");
            fb.addStation("C-L3", "C", "L3");
            fb.addStation("D", "D", "L3");
            fb.addRailway("L1", "1선", "A", "B-L1");
            fb.addRailway("L2", "2선", "B-L2", "C-L2");
            fb.addRailway("L3", "3선", "C-L3", "D");
            fb.addTrain("L1", "t1", "A:22:30", "B-L1:23:00");
            fb.addTrain("L2", "t2", "B-L2:23:10", "C-L2:23:30");
            fb.addTrain("L3", "t3", "C-L3:23:40", "D:23:55");
            fb.connect("B-L1", "B-L2");
            fb.connect("C-L2", "C-L3");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("D"), WEEKDAY);

            assertFalse(journeys.isEmpty(), "2회 환승 경로를 찾아야 함");
            Journey best = journeys.getFirst();

            // 2회 환승 = ride 3개 + transfer 2개
            List<Leg> rides = best.legs().stream().filter(l -> !l.isTransfer()).toList();
            List<Leg> transfers = best.legs().stream().filter(Leg::isTransfer).toList();
            assertEquals(3, rides.size(), "ride leg 3개");
            assertEquals(2, transfers.size(), "transfer leg 2개");

            // 노선 순서: L1 → L2 → L3
            assertEquals("L1-fixture", rides.get(0).railway().substring(rides.get(0).railway().length() - 10));
            // 시간 순서 일관성: service-day 분 단위라 단순 비교로 충분
            for (int i = 0; i < best.legs().size() - 1; i++) {
                Leg cur = best.legs().get(i);
                Leg next = best.legs().get(i + 1);
                assertTrue(cur.arrivalMinutes() <= next.departureMinutes() + 1,
                        "leg 간 시간 순서가 정상이어야 함 (cur.arr=" + cur.arrivalMinutes()
                                + ", next.dep=" + next.departureMinutes() + ")");
            }
        }
    }

    // ====== 시나리오 3: 환승 도보 시간 ======

    @Nested
    @DisplayName("환승 도보 시간 5분이 강제된다")
    class TransferWalkTime {

        @Test
        @DisplayName("환승 가능: 도착 23:30, 환승 후 다음 열차 23:35 이상이면 OK")
        void transferFeasibleWithEnoughBuffer() throws Exception {
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B-L1", "B", "L1");
            fb.addStation("B-L2", "B", "L2");
            fb.addStation("C", "C", "L2");
            fb.addRailway("L1", "1선", "A", "B-L1");
            fb.addRailway("L2", "2선", "B-L2", "C");
            // L1: A 22:55 → B 23:30 도착
            fb.addTrain("L1", "ride1", "A:22:55", "B-L1:23:30");
            // L2: B 23:35 출발 (5분 버퍼 = 환승 가능)
            fb.addTrain("L2", "ride2", "B-L2:23:35", "C:23:55");
            fb.connect("B-L1", "B-L2");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("C"), WEEKDAY);
            assertFalse(journeys.isEmpty(), "5분 버퍼 환승은 가능해야 함");
        }

        @Test
        @DisplayName("환승 leg에 도보 시간이 반영되어 출발/도착 5분 차이")
        void transferLegHas5MinuteSpan() throws Exception {
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B-L1", "B", "L1");
            fb.addStation("B-L2", "B", "L2");
            fb.addStation("C", "C", "L2");
            fb.addRailway("L1", "1선", "A", "B-L1");
            fb.addRailway("L2", "2선", "B-L2", "C");
            fb.addTrain("L1", "ride1", "A:23:00", "B-L1:23:20");
            fb.addTrain("L2", "ride2", "B-L2:23:30", "C:23:50");
            fb.connect("B-L1", "B-L2");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("C"), WEEKDAY);
            assertFalse(journeys.isEmpty());

            Journey best = journeys.getFirst();
            Leg transferLeg = best.legs().stream().filter(Leg::isTransfer).findFirst().orElseThrow();

            // 환승 leg는 5분 차이
            int minutes = transferLeg.arrivalMinutes() - transferLeg.departureMinutes();
            assertEquals(5, minutes, "환승 leg는 5분 도보로 모델링됨");
        }
    }

    // ====== 시나리오 4: 자정 넘김 ======

    @Nested
    @DisplayName("심야 시각 표기 (24:30 → 00:30) 처리")
    class MidnightCrossing {

        @Test
        @DisplayName("ODPT 24시 표기를 00시로 정상 파싱")
        void parses24HourNotation() throws Exception {
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B", "B", "L1");
            fb.addRailway("L1", "1선", "A", "B");
            // ODPT는 익일 0:30을 "24:30"으로 표기
            fb.addTrain("L1", "lateNight", "A:23:50", "B:24:30");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("B"), WEEKDAY);
            assertFalse(journeys.isEmpty(), "24:30 표기 열차도 찾아야 함");

            Journey best = journeys.getFirst();
            assertEquals(23 * 60 + 50, best.departureMinutes());
            // 도착 시각은 24:30 (service-day 분으로 1470)으로 보존된다.
            assertEquals(24 * 60 + 30, best.arrivalMinutes());
        }
    }

    // ====== 시나리오 5: 같은 이름 자동 환승 ======

    @Nested
    @DisplayName("같은 이름 역의 자동 환승")
    class SameNameTransfer {

        @Test
        @DisplayName("connectingStation 없어도 같은 title이면 환승 가능")
        void sameTitleAutoConnects() throws Exception {
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B-L1", "B", "L1");      // L1의 B역
            fb.addStation("B-L2", "B", "L2");      // L2의 B역, 같은 이름
            fb.addStation("C", "C", "L2");
            fb.addRailway("L1", "1선", "A", "B-L1");
            fb.addRailway("L2", "2선", "B-L2", "C");
            fb.addTrain("L1", "ride1", "A:23:00", "B-L1:23:20");
            fb.addTrain("L2", "ride2", "B-L2:23:30", "C:23:50");
            // ※ connect() 호출 안 함 — connectingStation 없음

            TransitDataCache cache = fb.buildWithSameNameAutoTransfer();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("C"), WEEKDAY);
            assertFalse(journeys.isEmpty(), "같은 title이면 자동 환승되어 경로가 있어야 함");
        }
    }

    // ====== 시나리오 6: 환승 따라잡기 불가 ======

    @Nested
    @DisplayName("환승 따라잡기 불가능한 경우 후보에서 제외")
    class InfeasibleTransfer {

        @Test
        @DisplayName("환승 도보 시간을 빼면 음수 출발 — 후보 아님")
        void notEnoughBufferRejected() throws Exception {
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B-L1", "B", "L1");
            fb.addStation("B-L2", "B", "L2");
            fb.addStation("C", "C", "L2");
            fb.addRailway("L1", "1선", "A", "B-L1");
            fb.addRailway("L2", "2선", "B-L2", "C");
            // L1 도착 23:30, L2 출발 23:32 — 버퍼 2분 < 5분 도보
            fb.addTrain("L1", "ride1", "A:23:00", "B-L1:23:30");
            fb.addTrain("L2", "ride2", "B-L2:23:32", "C:23:55");
            // 추가로 가능한 경로: L1 22:00 도착, L2 23:32 출발 — 환승 가능
            fb.addTrain("L1", "early", "A:21:30", "B-L1:22:00");
            fb.connect("B-L1", "B-L2");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("C"), WEEKDAY);

            // 23:00 출발 ride1을 타면 23:30 도착인데, L2가 23:32 출발이라 5분 버퍼 부족.
            // 알고리즘은 더 이른 ride (early, 21:30)를 골라야 함.
            assertFalse(journeys.isEmpty(), "이른 열차로라도 도달 가능해야 함");
            Journey best = journeys.getFirst();
            assertTrue(best.departureMinutes() < 22 * 60
                            || best.legs().stream().noneMatch(Leg::isTransfer),
                    "버퍼 부족 환승은 채택되지 않아야 함 (실제: dep="
                            + best.departureMinutes() + ", legs=" + best.legs().size() + ")");
        }
    }

    // ====== 시나리오 7: 단일 leg 시간 일관성 ======

    @Nested
    @DisplayName("Journey 시간 일관성 검증")
    class TimeConsistency {

        @Test
        @DisplayName("모든 ride leg의 출발 < 도착 (자정 넘김 제외)")
        void rideLegsHaveValidTimeOrder() throws Exception {
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B", "B", "L1");
            fb.addStation("C", "C", "L1");
            fb.addRailway("L1", "1선", "A", "B", "C");
            fb.addTrain("L1", "t1", "A:23:00", "B:23:15", "C:23:30");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("C"), WEEKDAY);
            assertFalse(journeys.isEmpty());

            Journey best = journeys.getFirst();
            for (Leg leg : best.legs()) {
                if (leg.isTransfer()) continue;
                // service-day 분 표현에서는 24:30 같은 익일 도착도 단조 증가이므로 단순 비교로 충분.
                assertTrue(leg.departureMinutes() < leg.arrivalMinutes(),
                        "ride leg는 dep < arr: " + leg);
            }
        }

        @Test
        @DisplayName("Journey의 첫 leg가 fromStation에서 시작, 마지막 leg가 toStation에서 끝남")
        void endpointsMatchJourneyStations() throws Exception {
            FixtureBuilder fb = new FixtureBuilder();
            fb.addStation("A", "A", "L1");
            fb.addStation("B-L1", "B", "L1");
            fb.addStation("B-L2", "B", "L2");
            fb.addStation("C", "C", "L2");
            fb.addRailway("L1", "1선", "A", "B-L1");
            fb.addRailway("L2", "2선", "B-L2", "C");
            fb.addTrain("L1", "ride1", "A:23:00", "B-L1:23:20");
            fb.addTrain("L2", "ride2", "B-L2:23:30", "C:23:50");
            fb.connect("B-L1", "B-L2");

            TransitDataCache cache = fb.build();
            ReverseRaptorEngine engine = new ReverseRaptorEngine(cache);

            List<Journey> journeys = engine.findLastTrains(fb.id("A"), fb.id("C"), WEEKDAY);
            assertFalse(journeys.isEmpty());

            Journey best = journeys.getFirst();
            assertEquals(fb.id("A"), best.legs().getFirst().fromStation());
            assertEquals(fb.id("C"), best.legs().getLast().toStation());
        }
    }

    // ====== Fixture 빌더 ======

    /**
     * 테스트용 캐시를 빌드하는 헬퍼.
     * - addStation(key, title, lineKey): 역 추가
     * - addRailway(key, title, stationKeys...): 노선 추가
     * - addTrain(lineKey, trainId, "stationKey:HH:mm", ...): 열차 시간표 추가
     * - connect(stationKeyA, stationKeyB): 환승 추가
     * - id(key): 내부에서 만든 stationId 반환
     */
    static class FixtureBuilder {
        private final Map<String, String> ids = new HashMap<>();
        private final Map<String, OdptStation> stations = new ConcurrentHashMap<>();
        private final Map<String, OdptRailway> railways = new ConcurrentHashMap<>();
        private final Map<String, OdptTrainTimetable> trainTimetables = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> transfers = new ConcurrentHashMap<>();

        void addStation(String key, String title, String lineKey) {
            String stationId = "odpt.Station:Test." + lineKey + "." + key + "-fixture";
            String railwayId = "odpt.Railway:Test." + lineKey + "-fixture";
            ids.put(key, stationId);
            stations.put(stationId, new OdptStation(stationId, title,
                    Map.of("ja", title, "en", title),
                    "odpt.Operator:Test", railwayId, null,
                    35.0, 139.0, null, null));
        }

        void addRailway(String key, String title, String... stationKeys) {
            String railwayId = "odpt.Railway:Test." + key + "-fixture";
            List<OdptRailway.StationOrder> order = new ArrayList<>();
            for (int i = 0; i < stationKeys.length; i++) {
                order.add(new OdptRailway.StationOrder(id(stationKeys[i]), Map.of(), i + 1));
            }
            railways.put(railwayId, new OdptRailway(railwayId, title,
                    Map.of("ja", title, "en", title),
                    "odpt.Operator:Test", null, null, null, null, order));
        }

        /**
         * 열차 추가. stops는 "stationKey:HH:mm" 형태.
         * 첫 정차역은 출발만, 마지막은 도착만, 중간은 출발+도착(같은 시각).
         */
        void addTrain(String lineKey, String trainId, String... stops) {
            String railwayId = "odpt.Railway:Test." + lineKey + "-fixture";
            List<OdptTrainTimetable.TrainStop> trainStops = new ArrayList<>();
            for (int i = 0; i < stops.length; i++) {
                String[] parts = stops[i].split(":", 2);
                String stationKey = parts[0];
                String time = parts[1];
                String stationId = id(stationKey);
                if (i == 0) {
                    // 출발만
                    trainStops.add(new OdptTrainTimetable.TrainStop(time, stationId, null, null, null));
                } else if (i == stops.length - 1) {
                    // 도착만
                    trainStops.add(new OdptTrainTimetable.TrainStop(null, null, time, stationId, null));
                } else {
                    // 출발+도착 모두
                    trainStops.add(new OdptTrainTimetable.TrainStop(time, stationId, time, stationId, null));
                }
            }
            String ttId = "tt-" + lineKey + "-" + trainId;
            trainTimetables.put(ttId, new OdptTrainTimetable(ttId, "odpt.Operator:Test",
                    railwayId, null, WEEKDAY, trainId, "odpt.TrainType:Test.Local",
                    null, null, null, trainStops));
        }

        void connect(String stationKeyA, String stationKeyB) {
            String a = id(stationKeyA);
            String b = id(stationKeyB);
            transfers.computeIfAbsent(a, k -> new HashSet<>()).add(b);
            transfers.computeIfAbsent(b, k -> new HashSet<>()).add(a);
        }

        String id(String key) {
            String stationId = ids.get(key);
            if (stationId == null) {
                throw new IllegalArgumentException("Unknown station key: " + key);
            }
            return stationId;
        }

        TransitDataCache build() throws Exception {
            return buildInternal(false);
        }

        TransitDataCache buildWithSameNameAutoTransfer() throws Exception {
            return buildInternal(true);
        }

        private TransitDataCache buildInternal(boolean autoTransferByName) throws Exception {
            TransitDataCache cache = new TransitDataCache(null, null);
            setField(cache, "stationsById", stations);
            setField(cache, "railwaysById", railways);
            setField(cache, "trainTimetables", trainTimetables);

            // 역별 열차 시간표 인덱스
            Map<String, List<String>> stationToTT = new ConcurrentHashMap<>();
            for (var entry : trainTimetables.entrySet()) {
                for (var stop : entry.getValue().stops()) {
                    String sid = stop.effectiveStation();
                    if (sid != null) {
                        stationToTT.computeIfAbsent(sid, k -> new ArrayList<>()).add(entry.getKey());
                    }
                }
            }
            setField(cache, "stationToTrainTimetables", stationToTT);

            // 환승
            Map<String, Set<String>> transferGraph = new ConcurrentHashMap<>(transfers);
            if (autoTransferByName) {
                Map<String, List<OdptStation>> byTitle = new HashMap<>();
                for (OdptStation s : stations.values()) {
                    byTitle.computeIfAbsent(s.title(), k -> new ArrayList<>()).add(s);
                }
                for (List<OdptStation> sameName : byTitle.values()) {
                    if (sameName.size() > 1) {
                        for (int i = 0; i < sameName.size(); i++) {
                            for (int j = i + 1; j < sameName.size(); j++) {
                                String a = sameName.get(i).id();
                                String b = sameName.get(j).id();
                                transferGraph.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                                transferGraph.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                            }
                        }
                    }
                }
            }
            setField(cache, "transferGraph", transferGraph);

            // 빈 맵들
            setField(cache, "stationTimetables", new ConcurrentHashMap<>());
            setField(cache, "fares", new ConcurrentHashMap<>());
            setField(cache, "nameIndex", new ConcurrentHashMap<>());
            setField(cache, "trainTypesById", new ConcurrentHashMap<>());
            setField(cache, "calendarsById", new ConcurrentHashMap<>());
            setField(cache, "railwayStationOrder", new ConcurrentHashMap<>());

            return cache;
        }

        private void setField(Object target, String fieldName, Object value) throws Exception {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
    }
}
