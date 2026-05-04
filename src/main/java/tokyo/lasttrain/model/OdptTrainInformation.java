package tokyo.lasttrain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdptTrainInformation(
        @JsonProperty("owl:sameAs") String id,
        @JsonProperty("odpt:operator") String operator,
        @JsonProperty("odpt:railway") String railway,
        @JsonProperty("dc:date") String date,
        @JsonProperty("dct:valid") String valid,
        @JsonProperty("odpt:trainInformationStatus") Map<String, String> trainInformationStatus,
        @JsonProperty("odpt:trainInformationText") Map<String, String> trainInformationText
) {}
