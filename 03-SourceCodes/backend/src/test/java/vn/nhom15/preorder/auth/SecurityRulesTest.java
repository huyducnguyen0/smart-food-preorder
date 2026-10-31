package vn.nhom15.preorder.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import vn.nhom15.preorder.config.SecurityConfig;

/**
 * Bảng quyền theo đường dẫn (BR-33, NFR-03; UC-02 mục "Yêu cầu phân quyền chung"). Các đường dẫn của chức năng chưa
 * viết (menu, đơn, bếp) vẫn kiểm tra được: lớp bảo mật trả lời trước khi có controller. Test phân quyền đầy đủ cho
 * từng API thật bổ sung ở T-14.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    /** Bộ lọc tài khoản bị khóa (BR-32) đọc tài khoản ở mỗi yêu cầu; mock trả false: không bị khóa. */
    @MockitoBean
    AccountRepository accountRepository;

    @MockitoBean
    private AuthService authService;

    /** AuthController cần thêm dịch vụ đăng ký (UC-01); test này không dùng tới. */
    @MockitoBean
    private RegistrationService registrationService;

    private static RequestPostProcessor as(Role role) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new CurrentUser(1, "Thử", role), null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    @Test
    void anonymousCanReachHealthAndMenuButNotCustomerOrKitchenAreas() throws Exception {
        // BR-34: xem menu không cần đăng nhập. Chưa có controller menu nên là 404, quan trọng là không phải 401/403.
        mockMvc.perform(get("/api/menu")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/health")).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/slots")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/kitchen/queue")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotUseKitchenAreaButCanUseCustomerArea() throws Exception {
        mockMvc.perform(get("/api/kitchen/queue").with(as(Role.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(post("/api/kitchen/orders/1/start")
                        .with(as(Role.CUSTOMER))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders").with(as(Role.CUSTOMER))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/slots").with(as(Role.CUSTOMER))).andExpect(status().isNotFound());
    }

    @Test
    void kitchenStaffCannotUseCustomerArea() throws Exception {
        // Ví dụ của UC-02 E5: nhân viên bếp gọi thao tác đặt đơn.
        mockMvc.perform(post("/api/orders").with(as(Role.KITCHEN)).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/kitchen/queue").with(as(Role.KITCHEN))).andExpect(status().isNotFound());
    }

    @Test
    void counterAndManagerCannotUseCustomerOrKitchenAreasInSprint1() throws Exception {
        for (Role role : List.of(Role.COUNTER, Role.MANAGER)) {
            mockMvc.perform(get("/api/orders").with(as(role))).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/kitchen/queue").with(as(role))).andExpect(status().isForbidden());
        }
    }

    @Test
    void unknownApiPathsAreDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/chua-khai-bao")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/chua-khai-bao").with(as(Role.MANAGER))).andExpect(status().isForbidden());
    }

    @Test
    void writeRequestsWithoutCsrfTokenAreRejectedEvenWhenAuthorized() throws Exception {
        mockMvc.perform(post("/api/orders").with(as(Role.CUSTOMER))).andExpect(status().isForbidden());
    }
}
