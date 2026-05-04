package tokyo.lasttrain.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tokyo.lasttrain.config.GlobalExceptionHandler;
import tokyo.lasttrain.exception.OdptUnavailableException;
import tokyo.lasttrain.service.LastTrainService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LastTrainController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LastTrainService service;

    @Test
    void missingRequiredParam_returns400_withErrorBody() throws Exception {
        mockMvc.perform(get("/api/v1/last-train"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/last-train"));
    }

    @Test
    void odptUnavailable_returns503_withCode() throws Exception {
        when(service.findLastTrain(anyString(), anyString()))
                .thenThrow(new OdptUnavailableException("ODPT down", new RuntimeException()));

        mockMvc.perform(get("/api/v1/last-train").param("from", "X").param("to", "Y"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ODPT_UNAVAILABLE"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unexpectedError_returns500_genericMessage() throws Exception {
        when(service.findLastTrain(anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/last-train").param("from", "X").param("to", "Y"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
