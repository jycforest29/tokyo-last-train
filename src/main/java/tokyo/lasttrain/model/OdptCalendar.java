package tokyo.lasttrain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdptCalendar(
        @JsonProperty("owl:sameAs") String id,
        @JsonProperty("dc:title") String title,
        @JsonProperty("odpt:calendarTitle") Map<String, String> calendarTitle,
        @JsonProperty("odpt:day") List<String> days,
        @JsonProperty("odpt:duration") String duration
) {}