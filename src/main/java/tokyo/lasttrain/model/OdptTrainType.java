package tokyo.lasttrain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdptTrainType(
        @JsonProperty("owl:sameAs") String id,
        @JsonProperty("odpt:operator") String operator,
        @JsonProperty("dc:title") String title,
        @JsonProperty("odpt:trainTypeTitle") Map<String, String> trainTypeTitle
) {}
