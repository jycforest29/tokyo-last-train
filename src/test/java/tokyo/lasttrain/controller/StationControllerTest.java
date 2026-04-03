package tokyo.lasttrain.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tokyo.lasttrain.dto.StationSearchResponse;
import tokyo.lasttrain.dto.StationSearchResponse.StationInfo;
import tokyo.lasttrain.service.StationSearchService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StationController.class)
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationSearchService stationSearchService;

    @Test
    @DisplayName("GET /api/v1/stations/search - 정상 응답")
    void searchStations() throws Exception {
        StationSearchResponse response = new StationSearchResponse(List.of(
                new StationInfo(
                        "odpt.Station:TokyoMetro.Ginza.Shibuya",
                        "渋谷", "Shibuya",
                        "odpt.Railway:TokyoMetro.Ginza",
                        "銀座線", "Ginza Line",
                        "odpt.Operator:TokyoMetro",
                        35.6580, 139.7016
                )
        ));

        when(stationSearchService.search("shibuya")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stations/search").param("query", "shibuya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations").isArray())
                .andExpect(jsonPath("$.stations[0].stationId").value("odpt.Station:TokyoMetro.Ginza.Shibuya"))
                .andExpect(jsonPath("$.stations[0].nameJa").value("渋谷"))
                .andExpect(jsonPath("$.stations[0].nameEn").value("Shibuya"))
                .andExpect(jsonPath("$.stations[0].railwayNameEn").value("Ginza Line"));
    }

    @Test
    @DisplayName("GET /api/v1/stations/search - query 파라미터 없으면 400")
    void searchWithoutQuery() throws Exception {
        mockMvc.perform(get("/api/v1/stations/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/stations/search - 빈 결과")
    void searchNoResults() throws Exception {
        when(stationSearchService.search("xyz")).thenReturn(new StationSearchResponse(List.of()));

        mockMvc.perform(get("/api/v1/stations/search").param("query", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations").isEmpty());
    }
}