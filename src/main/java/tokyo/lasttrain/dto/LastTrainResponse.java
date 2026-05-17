package tokyo.lasttrain.dto;

import java.util.List;

public record LastTrainResponse(
        String fromStation,
        String toStation,
        String calendarType,        // "Weekday" or "Holiday"
        List<LastTrainRoute> routes,
        List<Alternative> alternatives,    // 같은 노선의 인접 역으로 가는 대안 (없으면 빈 리스트)
        TaxiEstimate taxiEstimate,         // 두 역 모두 위경도 데이터가 있을 때만 채워짐 (없으면 null)
        String notice                      // 출발=도착 등 경로 조회가 불가능한 사유 코드 (정상 응답이면 null). 예: "SAME_STATION"
) {
    /**
     * 도쿄 택시 운임 추정. 막차를 놓쳤거나 환승이 빠듯할 때 참고용.
     * 도로 거리가 아닌 직선 거리 기반이라 실제 운임보다 저평가될 수 있음.
     */
    public record TaxiEstimate(
            int yenDay,         // 평시 운임 추정
            int yenNight,       // 심야할증(22:00-05:00) 운임 추정
            double distanceKm,
            boolean nightSurchargeNow   // 현재 시각이 심야할증 시간대인가
    ) {}

    /**
     * 도착역 한 정거장 전까지의 막차 등 대안. 사용자가 막차를 놓치거나
     * 시간을 살짝 벌고 싶을 때 도보/택시 거리에서 대신 내릴 수 있는 옵션.
     */
    public record Alternative(
            String stationId,
            String stationNameJa,
            String stationNameEn,
            String stationNameKo,
            int offsetFromDest,         // -1 = 한 정거장 전, +1 = 한 정거장 후
            List<LastTrainRoute> routes
    ) {}

    public record LastTrainRoute(
            String departureTime,       // e.g. "23:45"
            String arrivalTime,         // e.g. "00:12"
            String railway,
            String railwayNameJa,
            String railwayNameEn,
            String railwayNameKo,
            String railDirection,
            String trainType,           // e.g. "Local", "Rapid"
            String destinationNameJa,   // 행선지
            String destinationNameEn,
            String destinationNameKo,
            List<Transfer> transfers,   // 환승 정보
            int totalFare,              // IC카드 기준 총 요금 (엔)
            int totalFareTicket,        // 종이표 기준 총 요금 (엔)
            DelayInfo delay             // 첫 leg 노선의 실시간 운행 정보 (없으면 null)
    ) {}

    /** ODPT TrainInformation을 단순화한 운행 정보. */
    public record DelayInfo(
            String statusJa,
            String statusEn,
            String textJa,
            String textEn,
            boolean isDisruption    // "平常" 외 메시지 여부
    ) {}

    public record Transfer(
            String stationNameJa,
            String stationNameEn,
            String stationNameKo,
            String fromRailway,
            String fromRailwayNameJa,
            String fromRailwayNameEn,
            String fromRailwayNameKo,
            String toRailway,
            String toRailwayNameJa,
            String toRailwayNameEn,
            String toRailwayNameKo,
            String arrivalTime,        // 환승역 도착 시각
            String departureTime,      // 환승 후 다음 열차 출발 시각
            int waitMinutes,           // 환승 대기 시간(분)
            String fromPlatform,       // 도착 플랫폼 번호 (없으면 null)
            String toPlatform          // 출발 플랫폼 번호 (없으면 null)
    ) {}
}
