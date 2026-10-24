package vn.nhom15.preorder.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Luồng đăng nhập chạy qua toàn bộ ứng dụng thật: bộ lọc bảo mật, controller, service, repository và PostgreSQL thật
 * với dữ liệu mẫu. Chỉ có cổng HTTP là giả lập (MockMvc).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final HikariDataSource DATA_SOURCE = TestDatabase.create(true);

    /** Mật khẩu chung của tài khoản mẫu (03-SourceCodes/README.md). */
    private static final String SEED_PASSWORD = "Demo@2026";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATA_SOURCE::getJdbcUrl);
        registry.add("spring.datasource.username", DATA_SOURCE::getUsername);
        registry.add("spring.datasource.password", DATA_SOURCE::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    /** Mô phỏng trình duyệt: lấy cookie chống CSRF, rồi gửi lại cả cookie lẫn header cho yêu cầu ghi. */
    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, String token) {
        return request.cookie(new Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", token);
    }

    private String csrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me")).andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    private MvcResult login(String loginId, String password) throws Exception {
        String body = "{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}";
        return mockMvc.perform(withCsrf(post("/api/auth/login"), csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Test
    void customerLoginThenMeThenLogout() throws Exception {
        MvcResult result = login("  KHACH1@example.com ", SEED_PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .contains("\"role\":\"CUSTOMER\"")
                .doesNotContain("password")
                .doesNotContain("$2");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Khách Một"));

        mockMvc.perform(withCsrf(post("/api/auth/logout"), csrfToken()).session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordUnknownAccountAndLockedAccountBehaveAsSpecified() throws Exception {
        // E1: sai mật khẩu và định danh không tồn tại cho cùng một phản hồi.
        String wrongPassword =
                login("khach1@example.com", "sai-mat-khau").getResponse().getContentAsString();
        String unknownAccount =
                login("khong-co@example.com", "sai-mat-khau").getResponse().getContentAsString();
        assertThat(wrongPassword).isEqualTo(unknownAccount).contains("INVALID_CREDENTIALS");

        // E2: chỉ khi mật khẩu đúng mới báo tài khoản bị khóa.
        MvcResult locked = login("khoa@example.com", SEED_PASSWORD);
        assertThat(locked.getResponse().getStatus()).isEqualTo(403);
        assertThat(locked.getResponse().getContentAsString()).contains("ACCOUNT_LOCKED");
        assertThat(login("khoa@example.com", "sai-mat-khau").getResponse().getContentAsString())
                .contains("INVALID_CREDENTIALS");
    }

    @Test
    void roleDecidesWhichAreasAreReachable() throws Exception {
        MockHttpSession customer = (MockHttpSession)
                login("khach1@example.com", SEED_PASSWORD).getRequest().getSession(false);
        MockHttpSession kitchen = (MockHttpSession)
                login("bep1@example.com", SEED_PASSWORD).getRequest().getSession(false);

        // Cùng một đường dẫn: khách bị từ chối vùng của bếp, bếp bị từ chối vùng của khách (UC-02 E5).
        mockMvc.perform(get("/api/kitchen/queue").session(customer)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/kitchen/queue").session(kitchen)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/1").session(kitchen)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/1").session(customer)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsRejectedWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"khach1@example.com\",\"password\":\"" + SEED_PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEndpointStaysPublicAndReportsDatabaseUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.schemaVersion").value("5"));
    }
}
