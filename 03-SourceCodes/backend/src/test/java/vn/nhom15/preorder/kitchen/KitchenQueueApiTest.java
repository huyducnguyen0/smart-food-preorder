package vn.nhom15.preorder.kitchen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.nhom15.preorder.order.PlaceOrderCommand;
import vn.nhom15.preorder.order.PlaceOrderService;
import vn.nhom15.preorder.order.RequestedLine;
import vn.nhom15.preorder.support.MutableClock;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Hàng chờ bếp qua HTTP trên PostgreSQL thật (UC-12.1): chỉ đơn hôm nay, đang chờ hoặc đang làm, chia ba nhóm, đúng thứ
 * tự, có cờ trễ, không lộ giá hay thanh toán, và chỉ bếp xem được.
 */
@SpringBootTest
@AutoConfigureMockMvc
class KitchenQueueApiTest {

    private static final HikariDataSource DATA_SOURCE = TestDatabase.create(true);
    private static final LocalDate TODAY = LocalDate.of(2031, 3, 5);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SEED_PASSWORD = "Demo@2026";

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(ZONE, TODAY.atTime(9, 0));
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
    private Clock clock;

    private MutableClock mutableClock;
    private long customer1;
    private long customer2;

    @BeforeEach
    void setUp() {
        mutableClock = (MutableClock) clock;
        mutableClock.set(TODAY.atTime(9, 0));
        jdbc.execute("TRUNCATE order_lines, orders, time_slots RESTART IDENTITY CASCADE");
        // Cấu hình tương đương mô hình quota Sprint 1 (làm trước 15 phút, đặt trước 100%, quầy không giới hạn), xem
        // PB-22.
        jdbc.update("UPDATE store_settings SET hold_minutes = 15, preorder_share_percent = 100,"
                + " pickup_limit_orders = 1000");
        customer1 =
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'khach1@example.com'", Long.class);
        customer2 = jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = '0901234567'", Long.class);
    }

    private long itemId(String name) {
        return jdbc.queryForObject("SELECT id FROM menu_items WHERE name = ?", Long.class, name);
    }

    private long place(long customer, int hour, int minute, String item, int quantity, int price) {
        PlaceOrderCommand command = new PlaceOrderCommand(
                UUID.randomUUID(),
                TODAY.atTime(hour, minute),
                List.of(new RequestedLine(itemId(item), quantity, price)));
        return placeOrderService.placeOrder(customer, command).order().id();
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

    private String queue() throws Exception {
        return mockMvc.perform(get("/api/kitchen/queue").session(login("bep1@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static List<Integer> ids(String json, String group) {
        return JsonPath.read(json, "$." + group + "[*].id");
    }

    @Test
    void emptyQueueHasThreeEmptyGroups() throws Exception {
        mockMvc.perform(get("/api/kitchen/queue").session(login("bep1@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preparing.length()").value(0))
                .andExpect(jsonPath("$.dueSoon.length()").value(0))
                .andExpect(jsonPath("$.upcoming.length()").value(0))
                .andExpect(jsonPath("$.serverTime").value(endsWith("+07:00")));
    }

    @Test
    void ordersAreGroupedSortedAndMarkedLate() throws Exception {
        // Đơn bị hủy phải rời hàng chờ; hủy ngay để không chiếm giới hạn số đơn chưa kết thúc của khách.
        long cancelled = place(customer1, 12, 30, "Nước suối", 1, 10000);
        jdbc.update("UPDATE orders SET status = 'CANCELLED' WHERE id = ?", cancelled);
        long working = place(customer1, 11, 0, "Burger bò", 2, 45000);
        mutableClock.set(TODAY.atTime(9, 5));
        long due = place(customer1, 11, 30, "Khoai tây chiên", 1, 25000);
        mutableClock.set(TODAY.atTime(9, 10));
        long dueLaterConfirmed = place(customer2, 11, 30, "Trà đào", 3, 20000);
        mutableClock.set(TODAY.atTime(9, 15));
        long upcoming = place(customer2, 13, 0, "Gà rán 2 miếng", 1, 55000);
        long ready = place(customer2, 12, 0, "Nước suối", 1, 10000);
        long yesterday = place(customer1, 14, 0, "Nước suối", 1, 10000);

        jdbc.update(
                "UPDATE orders SET status = 'PREPARING', preparing_at = now(), preparing_by = ? WHERE id = ?",
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'bep1@example.com'", Long.class),
                working);
        jdbc.update(
                "UPDATE orders SET status = 'READY', preparing_at = now(), preparing_by = ?, ready_at = now(), "
                        + "ready_by = ? WHERE id = ?",
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'bep1@example.com'", Long.class),
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'bep1@example.com'", Long.class),
                ready);
        jdbc.update(
                "UPDATE time_slots SET slot_date = ? WHERE id = (SELECT time_slot_id FROM orders WHERE id = ?)",
                TODAY.minusDays(1),
                yesterday);

        // 11:20: đơn 11:00 đang làm và đã trễ; hai đơn 11:30 vào cửa sổ chuẩn bị (từ 11:15) chưa trễ; đơn 13:00 sắp
        // tới.
        mutableClock.set(TODAY.atTime(11, 20));
        String json = queue();

        assertThat(ids(json, "preparing")).containsExactly((int) working);
        assertThat(ids(json, "dueSoon"))
                .containsExactly((int) due, (int) dueLaterConfirmed); // cùng khung: xác nhận trước lên trước
        assertThat(ids(json, "upcoming")).containsExactly((int) upcoming);
        assertThat((Boolean) JsonPath.read(json, "$.preparing[0].late")).isTrue();
        assertThat((List<Boolean>) JsonPath.read(json, "$.dueSoon[*].late")).containsOnly(false);
        assertThat((List<Boolean>) JsonPath.read(json, "$.upcoming[*].late")).containsOnly(false);
    }

    @Test
    void confirmedOrderEntersTheDueGroupAtTheWindowBoundaryAndIsLateAtSlotStart() throws Exception {
        long id = place(customer1, 12, 0, "Burger bò", 1, 45000);

        mutableClock.set(TODAY.atTime(11, 44));
        assertThat(ids(queue(), "upcoming")).containsExactly((int) id);

        mutableClock.set(TODAY.atTime(11, 45)); // 12:00 − 15 phút (BR-21)
        String atWindow = queue();
        assertThat(ids(atWindow, "dueSoon")).containsExactly((int) id);
        assertThat((Boolean) JsonPath.read(atWindow, "$.dueSoon[0].late")).isFalse();

        mutableClock.set(TODAY.atTime(12, 0)); // đến giờ bắt đầu khung (BR-26)
        assertThat((Boolean) JsonPath.read(queue(), "$.dueSoon[0].late")).isTrue();
    }

    @Test
    void orderShowsOnlyWhatTheKitchenNeeds() throws Exception {
        place(customer1, 12, 0, "Burger bò", 2, 45000);
        jdbc.update(
                "INSERT INTO order_lines (order_id, menu_item_id, item_name, unit_price, quantity)"
                        + " SELECT id, ?, 'Trà đào', 20000, 1 FROM orders",
                itemId("Trà đào"));
        mutableClock.set(TODAY.atTime(11, 50));

        String json = queue();

        mockMvc.perform(get("/api/kitchen/queue").session(login("bep1@example.com")))
                .andExpect(jsonPath("$.dueSoon[0].pickupCode")
                        .value(org.hamcrest.Matchers.matchesPattern("[A-HJKMNP-Z2-9]{6}")))
                .andExpect(jsonPath("$.dueSoon[0].slotStart").value("2031-03-05T12:00"))
                .andExpect(jsonPath("$.dueSoon[0].slotEnd").value("2031-03-05T12:15"))
                .andExpect(jsonPath("$.dueSoon[0].source").value("PRE_ORDER"))
                .andExpect(jsonPath("$.dueSoon[0].confirmedAt").value(endsWith("+07:00")))
                .andExpect(jsonPath("$.dueSoon[0].lines[0].name").value("Burger bò"))
                .andExpect(jsonPath("$.dueSoon[0].lines[0].quantity").value(2))
                .andExpect(jsonPath("$.dueSoon[0].lines[1].name").value("Trà đào"));
        // UC-12 Ghi chú: bếp không thấy giá, thanh toán hay điểm công việc.
        assertThat(json).doesNotContain("price", "Price", "amount", "payment", "workload", "customer");
    }

    @Test
    void onlyKitchenStaffCanSeeTheQueue() throws Exception {
        mockMvc.perform(get("/api/kitchen/queue")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/kitchen/queue").session(login("khach1@example.com")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/kitchen/queue").session(login("quay1@example.com")))
                .andExpect(status().isForbidden());
    }
}
