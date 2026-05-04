package tokyo.lasttrain.dto;

import java.util.List;

public record StationSearchResponse(
        List<StationInfo> stations
) {
    public record StationInfo(
            String stationId,       // e.g. "odpt.Station:TokyoMetro.Ginza.Shibuya"
            String nameJa,          // e.g. "渋谷"
            String nameEn,          // e.g. "Shibuya"
            String nameKo,          // e.g. "시부야"
            String railway,         // e.g. "odpt.Railway:TokyoMetro.Ginza"
            String railwayNameJa,   // e.g. "銀座線"
            String railwayNameEn,   // e.g. "Ginza Line"
            String railwayNameKo,   // e.g. "긴자선"
            String operator,        // e.g. "odpt.Operator:TokyoMetro"
            Double latitude,
            Double longitude,
            boolean hasTimetable    // ODPT TrainTimetable 데이터 보유 여부
    ) {}
}
