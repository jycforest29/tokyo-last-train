package tokyo.lasttrain.dto;

import java.util.List;

public record LastTrainResponse(
        String fromStation,
        String toStation,
        String calendarType,        // "Weekday" or "Holiday"
        List<LastTrainRoute> routes
) {
    public record LastTrainRoute(
            String departureTime,       // e.g. "23:45"
            String arrivalTime,         // e.g. "00:12"
            String railway,
            String railwayNameJa,
            String railwayNameEn,
            String railDirection,
            String trainType,           // e.g. "Local", "Rapid"
            String destinationNameJa,   // 행선지
            String destinationNameEn,
            List<Transfer> transfers,   // 환승 정보
            int totalFare               // IC카드 기준 총 요금 (엔)
    ) {}

    public record Transfer(
            String stationNameJa,
            String stationNameEn,
            String fromRailway,
            String toRailway,
            String departureTime
    ) {}
}