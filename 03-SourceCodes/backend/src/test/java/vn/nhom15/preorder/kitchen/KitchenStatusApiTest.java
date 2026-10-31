package vn.nhom15.preorder.kitchen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.order.OrderStatusService;
import vn.nhom15.preorder.order.PlaceOrderCommand;
import vn.nhom15.preorder.order.PlaceOrderService;
import vn.nhom15.preorder.order.RequestedLine;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Chuyển trạng thái của bếp qua HTTP trên PostgreSQL thật (UC-12.2; PB-13 AC1–AC3; SRS B.5): chỉ hai bước hợp lệ, mọi
 * chuyển khác bị từ chối và không đổi gì, chỉ bếp làm được, hai bếp cùng bấm thì một người thành công.
 */
@SpringBootTest
@AutoConfigureMockMvc
// Đồng hồ cố định 09:00 mà đơn ở khung 12:00: nới cửa sổ chuẩn bị để đơn đã "tới giờ chuẩn bị" (BR-21). Test này
// kiểm tra chuyển trạng thái; mốc giờ bắt đầu kiểm ở ReviewFixesBlackBoxTest (TC-FIX-07) và TC-KIT-25.
@TestPropertySource(properties = "app.ordering.prep-window-minutes=180")
class KitchenStatusApiTest {

    private static final HikariDataSource DATA_SOURCE = TestDatabase.create(true);
    private static final LocalDate TODAY = LocalDate.of(2031, 3, 5);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SEED_PASSWORD = "Demo@2026";

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(TODAY.atTime(9, 0).atZone(ZONE).toInstant(), ZONE);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATA_SOURCE::getJdbcUrl);
        registry.add("spring.datasource.username", DATA_SOURCE::getUsername);
        registry.add("spring.datasource.password", DATA_SOURCE::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlaceOrderService placeOrderService;

    @Autowired
    private OrderStatusService orderStatusService;

    private long customer;
    private long kitchenStaff;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE order_lines, orders, time_slots RESTART IDENTITY CASCADE");
        customer =
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'khach1@example.com'", Long.class);
        kitchenStaff =
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'bep1@example.com'", Long.class);
    }

    private long newOrder() {
        return newOrder(12, 0);
    }

    private long newOrder(int hour, int minute) {
        long itemId = jdbc.queryForObject("SELECT id FROM menu_items WHERE name = 'Burger bò'", Long.class);
        PlaceOrderCommand command = new PlaceOrderCommand(
                UUID.randomUUID(), TODAY.atTime(hour, minute), List.of(new RequestedLine(itemId, 1, 45000)));
        return placeOrderService.placeOrder(customer, command).order().id();
    }

    private String statusOf(long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    private int heldPoints() {
        return jdbc.queryForObject("SELECT held_points FROM time_slots", Integer.class);
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        String token = mockMvc.perform(get("/api/auth/me"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN")
                .getValue();
        return request.cookie(new Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", token);
    }

    private MockHttpSession login(String loginId) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + SEED_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MvcResult act(String action, long orderId, MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder request = withCsrf(post("/api/kitchen/orders/" + orderId + "/" + action));
        return mockMvc.perform(session == null ? request : request.session(session))
                .andReturn();
    }

    @Test
    void kitchenStartsThenCompletesAnOrderAndRecordsWhoAndWhen() throws Exception {
        long id = newOrder();
        MockHttpSession kitchen = login("bep1@example.com");

        // BR-21: bếp được bắt đầu cả đơn sắp tới (đơn 12:00 khi bây giờ mới 09:00).
        mockMvc.perform(withCsrf(post("/api/kitchen/orders/" + id + "/start")).session(kitchen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PREPARING"));
        assertThat(statusOf(id)).isEqualTo("PREPARING");
        assertThat(jdbc.queryForObject("SELECT preparing_by FROM orders WHERE id = ?", Long.class, id))
                .isEqualTo(kitchenStaff);
        assertThat(jdbc.queryForObject("SELECT preparing_at IS NOT NULL FROM orders WHERE id = ?", Boolean.class, id))
                .isTrue();

        mockMvc.perform(withCsrf(post("/api/kitchen/orders/" + id + "/complete"))
                        .session(kitchen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
        assertThat(statusOf(id)).isEqualTo("READY");
        assertThat(jdbc.queryForObject("SELECT ready_by FROM orders WHERE id = ?", Long.class, id))
                .isEqualTo(kitchenStaff);

        // BR-14: quota vẫn giữ sau khi chuyển trạng thái. Đơn READY rời hàng chờ bếp.
        assertThat(heldPoints()).isEqualTo(5);
        mockMvc.perform(get("/api/kitchen/queue").session(kitchen))
                .andExpect(jsonPath("$.preparing.length()").value(0))
                .andExpect(jsonPath("$.dueSoon.length()").value(0))
                .andExpect(jsonPath("$.upcoming.length()").value(0));
    }

    @Test
    void customerSeesTheNewStatusInMyOrders() throws Exception {
        long id = newOrder();
        MockHttpSession kitchen = login("bep1@example.com");
        act("start", id, kitchen);
        act("complete", id, kitchen);

        mockMvc.perform(get("/api/orders/" + id).session(login("khach1@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void everyOtherTransitionIsRejectedAndChangesNothing() throws Exception {
        MockHttpSession kitchen = login("bep1@example.com");

        // Không thể hoàn tất đơn chưa bắt đầu (CONFIRMED → READY không có trong SRS B.5).
        long confirmed = newOrder();
        mockMvc.perform(withCsrf(post("/api/kitchen/orders/" + confirmed + "/complete"))
                        .session(kitchen))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.currentStatus").value("CONFIRMED"));
        assertThat(statusOf(confirmed)).isEqualTo("CONFIRMED");

        // Không bắt đầu lại đơn đang làm; không làm gì với đơn đã xong hoặc đã kết thúc.
        int index = 0;
        for (String state : List.of("PREPARING", "READY", "PICKED_UP", "CANCELLED", "NO_SHOW")) {
            // Mỗi đơn một khung riêng để không đụng quota của khung (5 điểm × 5 đơn > quota 20).
            long id = newOrder(13 + index / 4, (index % 4) * 15);
            index++;
            forceStatus(id, state);
            for (String action : List.of("start", "complete")) {
                boolean legal = (state.equals("PREPARING") && action.equals("complete"));
                if (legal) {
                    continue;
                }
                mockMvc.perform(withCsrf(post("/api/kitchen/orders/" + id + "/" + action))
                                .session(kitchen))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
                        .andExpect(jsonPath("$.currentStatus").value(state));
                assertThat(statusOf(id)).as("%s sau %s", state, action).isEqualTo(state);
            }
            // Dọn để không vượt giới hạn số đơn chưa kết thúc của khách ở vòng sau.
            jdbc.update("UPDATE orders SET status = 'PICKED_UP' WHERE id = ?", id);
        }
    }

    /** Đặt trạng thái trực tiếp trong database, giữ đúng các ràng buộc thời điểm/người thực hiện. */
    private void forceStatus(long orderId, String state) {
        switch (state) {
            case "PREPARING" ->
                jdbc.update(
                        "UPDATE orders SET status = 'PREPARING', preparing_at = now(), preparing_by = ? WHERE id = ?",
                        kitchenStaff,
                        orderId);
            case "READY" ->
                jdbc.update(
                        "UPDATE orders SET status = 'READY', preparing_at = now(), preparing_by = ?, ready_at = now(),"
                                + " ready_by = ? WHERE id = ?",
                        kitchenStaff,
                        kitchenStaff,
                        orderId);
            default -> jdbc.update("UPDATE orders SET status = ? WHERE id = ?", state, orderId);
        }
    }

    @Test
    void unknownOrderIsNotFound() throws Exception {
        MockHttpSession kitchen = login("bep1@example.com");

        for (String action : List.of("start", "complete")) {
            mockMvc.perform(withCsrf(post("/api/kitchen/orders/987654/" + action))
                            .session(kitchen))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        }
        mockMvc.perform(withCsrf(post("/api/kitchen/orders/abc/start")).session(kitchen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void onlyKitchenStaffCanChangeStatus() throws Exception {
        long id = newOrder();

        // Chưa đăng nhập, khách, quầy, quản lý: đều bị từ chối, đơn không đổi (UC-12 E2, PB-13 AC3, NFR-03).
        assertThat(act("start", id, null).getResponse().getStatus()).isEqualTo(401);
        for (String loginId : List.of("khach1@example.com", "quay1@example.com", "quanly@example.com")) {
            MvcResult result = act("start", id, login(loginId));
            assertThat(result.getResponse().getStatus()).as(loginId).isEqualTo(403);
            assertThat(result.getResponse().getContentAsString()).contains("FORBIDDEN");
            assertThat(act("complete", id, login(loginId)).getResponse().getStatus())
                    .isEqualTo(403);
        }
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");

        // Thiếu token CSRF: bị chặn trước khi tới controller.
        mockMvc.perform(post("/api/kitchen/orders/" + id + "/start").session(login("bep1@example.com")))
                .andExpect(status().isForbidden());
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    void twoKitchenStaffPressingStartTogetherOnlyOneSucceeds() throws Exception {
        long id = newOrder();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<ErrorCode>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    go.await();
                    try {
                        orderStatusService.startPreparing(id, kitchenStaff);
                        return null;
                    } catch (ApiException e) {
                        return e.code();
                    }
                }));
            }
            go.countDown();
            List<ErrorCode> outcomes = new ArrayList<>();
            for (Future<ErrorCode> future : results) {
                outcomes.add(future.get());
            }

            assertThat(outcomes.stream().filter(code -> code == null)).hasSize(1);
            assertThat(outcomes.stream().filter(code -> code == ErrorCode.INVALID_STATUS_TRANSITION))
                    .hasSize(threads - 1);
            assertThat(statusOf(id)).isEqualTo("PREPARING");
        } finally {
            pool.shutdownNow();
        }
    }
}
