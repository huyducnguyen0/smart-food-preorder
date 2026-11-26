package vn.nhom15.preorder.auth;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.SecurityConfig;

/** Lớp web của UC-02: mã HTTP, JSON lỗi, phiên đăng nhập và CSRF. Không cần database. */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String LOGIN_BODY = "{\"loginId\":\"khach@example.com\",\"password\":\"mat-khau-dung\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    /** AuthController cần thêm dịch vụ đăng ký (UC-01); test này không dùng tới. */
    @MockitoBean
    private RegistrationService registrationService;

    @Test
    void loginSuccessReturnsUserAndCreatesSessionThatMeAccepts() throws Exception {
        when(authService.authenticate("khach@example.com", "mat-khau-dung"))
                .thenReturn(new CurrentUser(7, "Khách", Role.CUSTOMER));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.displayName").value("Khách"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void wrongCredentialsReturn401WithCode() throws Exception {
        when(authService.authenticate(any(), any()))
                .thenThrow(new ApiException(ErrorCode.INVALID_CREDENTIALS, "Thông tin đăng nhập không đúng"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Thông tin đăng nhập không đúng"));
    }

    @Test
    void lockedAccountReturn403WithCode() throws Exception {
        when(authService.authenticate(any(), any()))
                .thenThrow(new ApiException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản đã bị khóa, liên hệ quản lý"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void missingFieldsReturn400ListingEachField() throws Exception {
        // UC-02 E3: chỉ ra trường còn thiếu, không gọi xác thực.
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"  \",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void unreadableBodyReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("khong phai json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void meWithoutSessionReturns401() throws Exception {
        // UC-02 E4: chưa đăng nhập hoặc phiên hết hạn.
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        when(authService.authenticate(any(), any())).thenReturn(new CurrentUser(7, "Khách", Role.CUSTOMER));
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session)).andExpect(status().isNoContent());

        // UC-02 bước 8: phiên cũ không dùng lại được.
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void errorResponsesAreProblemJsonInUtf8() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.detail").value("Vui lòng đăng nhập"));
    }

    @Test
    void authenticatedPrincipalIsExposedToTheController() throws Exception {
        var token = UsernamePasswordAuthenticationToken.authenticated(
                new CurrentUser(9, "Bếp", Role.KITCHEN), null, List.of(new SimpleGrantedAuthority("ROLE_KITCHEN")));

        mockMvc.perform(get("/api/auth/me").with(authentication(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.role").value("KITCHEN"));
    }
}
