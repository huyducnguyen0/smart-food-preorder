package vn.nhom15.preorder.health;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Kiểm tra lớp web: mã HTTP và JSON trả về, không cần database thật. */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-12T09:00:00+07:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    void returns200WhenDatabaseIsUp() throws Exception {
        when(healthService.check()).thenReturn(new HealthResponse("UP", "UP", "1", NOW));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.schemaVersion").value("1"))
                .andExpect(jsonPath("$.serverTime").value("2026-10-12T09:00:00+07:00"));
    }

    @Test
    void returns503WhenDatabaseIsDown() throws Exception {
        when(healthService.check()).thenReturn(new HealthResponse("DOWN", "DOWN", null, NOW));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("DOWN"));
    }
}
