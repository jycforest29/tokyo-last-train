package tokyo.lasttrain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdptRailwayFare(
        @JsonProperty("owl:sameAs") String id,
        @JsonProperty("odpt:operator") String operator,
        @JsonProperty("odpt:fromStation") String fromStation,
        @JsonProperty("odpt:toStation") String toStation,
        @JsonProperty("odpt:ticketFare") Integer ticketFare,
        @JsonProperty("odpt:icCardFare") Integer icCardFare,
        @JsonProperty("odpt:childTicketFare") Integer childTicketFare,
        @JsonProperty("odpt:childIcCardFare") Integer childIcCardFare
) {}