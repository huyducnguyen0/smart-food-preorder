package vn.nhom15.preorder.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Ma trận phân quyền (NFR-03, BR-33, PB-01 AC2): với từng vai trò, gọi trực tiếp từng API và kiểm tra bị từ chối hay
 * được qua cổng bảo mật. Chạy trên ứng dụng thật với phiên đăng nhập thật, không dùng người dùng giả lập.
 *
 * <p>Quy ước kết quả: được phép = không phải 401 hoặc 403 (API chưa viết trả 404 hoặc mã thành công, đều tính là qua
 * cổng bảo mật, nên test vẫn đúng khi API thật được thêm vào); không được phép = 401 nếu chưa đăng nhập, 403 nếu đã
 * đăng nhập sai vai trò. Bảng quyền khai báo ở {@link #ENDPOINTS} phải khớp UC-02 mục "Yêu cầu phân quyền chung" và
 * Architecture mục 5.1: thêm API mới thì thêm một dòng ở đây. Kiểm tra "đơn của khách khác" cần API đơn nên bổ sung ở
 * T-20 (UC-05 E1).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationMatrixTest {

    private static final HikariDataSource DATA_SOURCE = TestDatabase.create(true);
    private static final String SEED_PASSWORD = "Demo@2026";

    /** Người gọi API: chưa đăng nhập hoặc một vai trò, kèm tài khoản mẫu dùng để đăng nhập. */
    private enum Actor {
        ANONYMOUS(null, null),
        CUSTOMER(Role.CUSTOMER, "khach1@example.com"),
        KITCHEN(Role.KITCHEN, "bep1@example.com"),
        COUNTER(Role.COUNTER, "quay1@example.com"),
        MANAGER(Role.MANAGER, "quanly@example.com");

        final Role role;
        final String loginId;

        Actor(Role role, String loginId) {
            this.role = role;
            this.loginId = loginId;
        }
    }

    private record Endpoint(HttpMethod method, String path, Set<Role> allowedRoles, boolean anonymousAllowed) {}

    private static Endpoint open(HttpMethod method, String path) {
        return new Endpoint(method, path, EnumSet.allOf(Role.class), true);
    }

    private static Endpoint loggedIn(HttpMethod method, String path) {
        return new Endpoint(method, path, EnumSet.allOf(Role.class), false);
    }

    private static Endpoint only(Role role, HttpMethod method, String path) {
        return new Endpoint(method, path, EnumSet.of(role), false);
    }

    private static final List<Endpoint> ENDPOINTS = List.of(
            // Công khai (BR-34).
            open(HttpMethod.GET, "/api/health"),
            open(HttpMethod.GET, "/api/menu"),
            open(HttpMethod.GET, "/api/menu/items/1"),
            // Cần đăng nhập, mọi vai trò (UC-02).
            loggedIn(HttpMethod.GET, "/api/auth/me"),
            loggedIn(HttpMethod.POST, "/api/auth/logout"),
            // Chỉ khách (UC-04, UC-05).
            only(Role.CUSTOMER, HttpMethod.GET, "/api/slots"),
            only(Role.CUSTOMER, HttpMethod.POST, "/api/orders"),
            only(Role.CUSTOMER, HttpMethod.GET, "/api/orders"),
            only(Role.CUSTOMER, HttpMethod.GET, "/api/orders/1"),
            // Chỉ nhân viên bếp (UC-12).
            only(Role.KITCHEN, HttpMethod.GET, "/api/kitchen/queue"),
            only(Role.KITCHEN, HttpMethod.POST, "/api/kitchen/orders/1/start"),
            only(Role.KITCHEN, HttpMethod.POST, "/api/kitchen/orders/1/complete"),
            // Đường dẫn chưa khai báo: mặc định từ chối với mọi người dùng.
            new Endpoint(HttpMethod.GET, "/api/chua-khai-bao", EnumSet.noneOf(Role.class), false));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATA_SOURCE::getJdbcUrl);
        registry.add("spring.datasource.username", DATA_SOURCE::getUsername);
        registry.add("spring.datasource.password", DATA_SOURCE::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    private String csrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me")).andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN").getValue();
    }

    /** Đăng nhập mới mỗi lần, vì ca gọi đăng xuất sẽ hủy phiên và làm hỏng các ca sau nếu dùng chung. */
    private MockHttpSession sessionOf(Actor actor) throws Exception {
        if (actor == Actor.ANONYMOUS) {
            return null;
        }
        String token = csrfToken();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + actor.loginId + "\",\"password\":\"" + SEED_PASSWORD + "\"}"))
                .andReturn();
        assertThat(login.getResponse().getStatus()).as("đăng nhập " + actor).isEqualTo(200);
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    /** Gọi API như trình duyệt: kèm cookie phiên và token CSRF, để kết quả phản ánh quyền chứ không do thiếu token. */
    private int call(Actor actor, HttpMethod method, String path) throws Exception {
        String token = csrfToken();
        MockHttpServletRequestBuilder request =
                method == HttpMethod.GET ? get(path) : post(path).contentType(MediaType.APPLICATION_JSON);
        request.cookie(new Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", token);
        MockHttpSession session = sessionOf(actor);
        if (session != null) {
            request.session(session);
        }
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }

    static Stream<Arguments> everyActorOnEveryEndpoint() {
        return ENDPOINTS.stream()
                .flatMap(endpoint -> java.util.Arrays.stream(Actor.values()).map(actor -> {
                    boolean allowed = actor == Actor.ANONYMOUS
                            ? endpoint.anonymousAllowed()
                            : endpoint.allowedRoles().contains(actor.role);
                    return Arguments.of(actor, endpoint.method(), endpoint.path(), allowed);
                }));
    }

    @ParameterizedTest(name = "{0} {1} {2} → được phép = {3}")
    @MethodSource("everyActorOnEveryEndpoint")
    void eachRoleReachesExactlyItsOwnEndpoints(Actor actor, HttpMethod method, String path, boolean allowed)
            throws Exception {
        int status = call(actor, method, path);

        if (allowed) {
            assertThat(status).as("được phép nhưng bị chặn").isNotIn(401, 403);
        } else if (actor == Actor.ANONYMOUS) {
            assertThat(status).as("chưa đăng nhập phải bị từ chối").isEqualTo(401);
        } else {
            assertThat(status).as("sai vai trò phải bị từ chối").isEqualTo(403);
        }
    }

    @Test
    void forgedOrExpiredSessionCookieIsTreatedAsNotLoggedIn() throws Exception {
        String token = csrfToken();

        int status = mockMvc.perform(get("/api/kitchen/queue")
                        .cookie(new Cookie("JSESSIONID", "phien-gia-mao-0123456789"))
                        .cookie(new Cookie("XSRF-TOKEN", token)))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isEqualTo(401);
    }

    @Test
    void roleCannotBeEscalatedThroughRequestParametersOrHeaders() throws Exception {
        MockHttpSession customer = sessionOf(Actor.CUSTOMER);

        // Vai trò chỉ lấy từ phiên phía server; tham số hoặc header do người gọi tự đặt không có tác dụng.
        int status = mockMvc.perform(get("/api/kitchen/queue")
                        .session(customer)
                        .param("role", "KITCHEN")
                        .header("X-Role", "KITCHEN")
                        .header("X-User-Role", "KITCHEN"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isEqualTo(403);
    }

    @Test
    void deniedResponsesNeverRevealWhetherTheTargetExists() throws Exception {
        // Khách bị từ chối vùng bếp với cùng một phản hồi cho đơn có thật và đơn không có thật.
        MockHttpSession customer = sessionOf(Actor.CUSTOMER);
        String token = csrfToken();
        String forExisting = body(customer, token, "/api/kitchen/orders/1/start");
        String forMissing = body(customer, token, "/api/kitchen/orders/987654/start");

        assertThat(forExisting).isEqualTo(forMissing).contains("FORBIDDEN");
    }

    private String body(MockHttpSession session, String token, String path) throws Exception {
        return mockMvc.perform(post(path)
                        .session(session)
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
