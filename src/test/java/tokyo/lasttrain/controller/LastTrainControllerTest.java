package tokyo.lasttrain.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tokyo.lasttrain.dto.LastTrainResponse;
import tokyo.lasttrain.dto.LastTrainResponse.LastTrainRoute;
import tokyo.lasttrain.dto.LastTrainResponse.Transfer;
import tokyo.lasttrain.service.LastTrainService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LastTrainController.class)
class LastTrainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LastTrainService lastTrainService;

    @Test
    @DisplayName("GET /api/v1/last-train - 직통 경로")
    void findLastTrainDirect() throws Exception {
        String from = "odpt.Station:Test.LineA.StationA";
        String to = "odpt.Station:Test.LineA.StationC";

        LastTrainResponse response = new LastTrainResponse(from, to, "Weekday", List.of(
                new LastTrainRoute(
                        "23:30", "00:00",
                        "odpt.Railway:Test.LineA",
                        "テストA線", "Test Line A",
                        "odpt.RailDirection:Test.Outbound",
                        "Local",
                        "C駅", "Station C",
                        List.of(),
                        200
                )
        ));

        when(lastTrainService.findLastTrain(from, to)).thenReturn(response);

        mockMvc.perform(get("/api/v1/last-train")
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromStation").value(from))
                .andExpect(jsonPath("$.toStation").value(to))
                .andExpect(jsonPath("$.calendarType").value("Weekday"))
                .andExpect(jsonPath("$.routes[0].departureTime").value("23:30"))
                .andExpect(jsonPath("$.routes[0].arrivalTime").value("00:00"))
                .andExpect(jsonPath("$.routes[0].totalFare").value(200))
                .andExpect(jsonPath("$.routes[0].transfers").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/last-train - 환승 경로")
    void findLastTrainWithTransfer() throws Exception {
        String from = "odpt.Station:Test.LineA.StationA";
        String to = "odpt.Station:Test.LineB.StationE";

        LastTrainResponse response = new LastTrainResponse(from, to, "Weekday", List.of(
                new LastTrainRoute(
                        "23:15", "00:10",
                        "odpt.Railway:Test.LineA",
                        "テストA線", "Test Line A",
                        null, "Local",
                        "E駅", "Station E",
                        List.of(new Transfer(
                                "B駅", "Station B",
                                "odpt.Railway:Test.LineA",
                                "odpt.Railway:Test.LineB",
                                "23:40"
                        )),
                        350
                )
        ));

        when(lastTrainService.findLastTrain(from, to)).thenReturn(response);

        mockMvc.perform(get("/api/v1/last-train")
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes[0].transfers").isNotEmpty())
                .andExpect(jsonPath("$.routes[0].transfers[0].stationNameJa").value("B駅"))
                .andExpect(jsonPath("$.routes[0].totalFare").value(350));
    }

    @Test
    @DisplayName("GET /api/v1/last-train - 경로 없음")
    void findLastTrainNoRoute() throws Exception {
        String from = "odpt.Station:Test.X";
        String to = "odpt.Station:Test.Y";

        when(lastTrainService.findLastTrain(from, to))
                .thenReturn(new LastTrainResponse(from, to, "Weekday", List.of()));

        mockMvc.perform(get("/api/v1/last-train")
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/last-train - 필수 파라미터 누락")
    void missingParams() throws Exception {
        mockMvc.perform(get("/api/v1/last-train").param("from", "test"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/last-train").param("to", "test"))
                .andExpect(status().isBadRequest());
    }
}