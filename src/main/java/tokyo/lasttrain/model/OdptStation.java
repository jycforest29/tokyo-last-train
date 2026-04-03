package tokyo.lasttrain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdptStation(
        @JsonProperty("owl:sameAs") String id,
        @JsonProperty("dc:title") String title,
        @JsonProperty("odpt:stationTitle") Map<String, String> stationTitle,
        @JsonProperty("odpt:operator") String operator,
        @JsonProperty("odpt:railway") String railway,
        @JsonProperty("odpt:stationCode") String stationCode,
        @JsonProperty("geo:lat") Double latitude,
        @JsonProperty("geo:long") Double longitude,
        @JsonProperty("odpt:connectingRailway") List<String> connectingRailway,
        @JsonProperty("odpt:connectingStation") List<String> connectingStation
) {}