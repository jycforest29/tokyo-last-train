package tokyo.lasttrain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdptTrainTimetable(
        @JsonProperty("owl:sameAs") String id,
        @JsonProperty("odpt:operator") String operator,
        @JsonProperty("odpt:railway") String railway,
        @JsonProperty("odpt:railDirection") String railDirection,
        @JsonProperty("odpt:calendar") String calendar,
        @JsonProperty("odpt:trainNumber") String trainNumber,
        @JsonProperty("odpt:trainType") String trainType,
        @JsonProperty("odpt:trainName") List<Map<String, String>> trainName,
        @JsonProperty("odpt:originStation") List<String> originStation,
        @JsonProperty("odpt:destinationStation") List<String> destinationStation,
        @JsonProperty("odpt:trainTimetableObject") List<TrainStop> stops
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrainStop(
            @JsonProperty("odpt:departureTime") String departureTime,
            @JsonProperty("odpt:departureStation") String departureStation,
            @JsonProperty("odpt:arrivalTime") String arrivalTime,
            @JsonProperty("odpt:arrivalStation") String arrivalStation,
            @JsonProperty("odpt:platformNumber") String platformNumber
    ) {
        public String effectiveTime() {
            return departureTime != null ? departureTime : arrivalTime;
        }

        public String effectiveStation() {
            return departureStation != null ? departureStation : arrivalStation;
        }
    }
}