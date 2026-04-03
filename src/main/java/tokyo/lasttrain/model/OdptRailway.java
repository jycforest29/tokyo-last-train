package tokyo.lasttrain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdptRailway(
        @JsonProperty("owl:sameAs") String id,
        @JsonProperty("dc:title") String title,
        @JsonProperty("odpt:railwayTitle") Map<String, String> railwayTitle,
        @JsonProperty("odpt:operator") String operator,
        @JsonProperty("odpt:lineCode") String lineCode,
        @JsonProperty("odpt:color") String color,
        @JsonProperty("odpt:ascendingRailDirection") String ascendingRailDirection,
        @JsonProperty("odpt:descendingRailDirection") String descendingRailDirection,
        @JsonProperty("odpt:stationOrder") List<StationOrder> stationOrder
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StationOrder(
            @JsonProperty("odpt:station") String station,
            @JsonProperty("odpt:stationTitle") Map<String, String> stationTitle,
            @JsonProperty("odpt:index") int index
    ) {}
}