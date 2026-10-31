package vn.nhom15.preorder.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.nhom15.preorder.config.SecurityConfig;

/**
 * Cơ chế chống CSRF bằng cookie thật (double submit): cookie XSRF-TOKEN do server cấp, frontend gửi lại ở header
 * X-XSRF-TOKEN. Lớp test này không dùng {@code csrf()} của spring-security-test (nó thay kho token của cả context) và
 * có thuộc tính riêng để chạy trong context riêng.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "test.context=csrf-cookie")
class CsrfCookieTest {

    private static final String LOGIN_BODY = "{\"loginId\":\"khach@example.com\",\"password\":\"mat-khau-dung\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    /** AuthController cần thêm dịch vụ đăng ký (UC-01); test này không dùng tới. */
    @MockitoBean
    private RegistrationService registrationService;

    private String issueToken() throws Exception {
        // Phản hồi 401 đầu tiên (chưa đăng nhập) vẫn phải cấp cookie, để trang đăng nhập có token gửi lại.
        MvcResult result = mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("cookie XSRF-TOKEN").isNotNull();
        assertThat(cookie.isHttpOnly()).as("frontend phải đọc được cookie").isFalse();
        return cookie.getValue();
    }

    @Test
    void loginWithMatchingCookieAndHeaderIsAccepted() throws Exception {
        when(authService.authenticate(any(), any())).thenReturn(new CurrentUser(7, "Khách", Role.CUSTOMER));
        String token = issueToken();

        mockMvc.perform(post("/api/auth/login")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithWrongOrMissingHeaderIsRejected() throws Exception {
        String token = issueToken();

        mockMvc.perform(post("/api/auth/login")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", "gia-tri-khac")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/login")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicGetRequestsAlsoReceiveTheCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/menu")).andReturn();

        assertThat(result.getResponse().getCookie("XSRF-TOKEN")).isNotNull();
    }
}
