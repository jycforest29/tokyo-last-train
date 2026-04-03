package tokyo.lasttrain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdptStationTimetable(
        @JsonProperty("owl:sameAs") String id,
        @JsonProperty("odpt:operator") String operator,
        @JsonProperty("odpt:railway") String railway,
        @JsonProperty("odpt:railwayTitle") Map<String, String> railwayTitle,
        @JsonProperty("odpt:station") String station,
        @JsonProperty("odpt:stationTitle") Map<String, String> stationTitle,
        @JsonProperty("odpt:railDirection") String railDirection,
        @JsonProperty("odpt:calendar") String calendar,
        @JsonProperty("odpt:stationTimetableObject") List<TimetableEntry> entries
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimetableEntry(
            @JsonProperty("odpt:departureTime") String departureTime,
            @JsonProperty("odpt:arrivalTime") String arrivalTime,
            @JsonProperty("odpt:destinationStation") List<String> destinationStation,
            @JsonProperty("odpt:trainType") String trainType,
            @JsonProperty("odpt:trainNumber") String trainNumber,
            @JsonProperty("odpt:isLast") Boolean isLast,
            @JsonProperty("odpt:isOrigin") Boolean isOrigin,
            @JsonProperty("odpt:platformNumber") String platformNumber,
            @JsonProperty("odpt:note") Map<String, String> note
    ) {
        public String effectiveTime() {
            return departureTime != null ? departureTime : arrivalTime;
        }
    }
}