package vn.nhom15.preorder.order;

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
import vn.nhom15.preorder.support.TestDatabase;

/**
 * "Đơn của tôi" qua HTTP (UC-05): danh sách theo thứ tự, chi tiết, và không xem được đơn của khách khác (BR-33,
 * NFR-03).
 * Đây là phần "đơn của khách khác" của T-14 đã hoãn tới khi có API đơn.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MyOrderApiTest {

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

    private long customer1;
    private long customer2;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE order_lines, orders, time_slots RESTART IDENTITY CASCADE");
        customer1 =
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'khach1@example.com'", Long.class);
        customer2 = jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = '0901234567'", Long.class);
    }

    private long place(long customer, int hour, int minute) {
        long itemId = jdbc.queryForObject("SELECT id FROM menu_items WHERE name = 'Khoai tây chiên'", Long.class);
        PlaceOrderCommand command = new PlaceOrderCommand(
                UUID.randomUUID(), TODAY.atTime(hour, minute), List.of(new RequestedLine(itemId, 1, 25000)));
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

    @Test
    void customerWithoutOrdersGetsAnEmptyList() throws Exception {
        mockMvc.perform(get("/api/orders").session(login("khach1@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(0));
    }

    @Test
    void openOrdersComeFirstByPickupTimeThenFinishedOnesNewestFirst() throws Exception {
        long late = place(customer1, 13, 0);
        long early = place(customer1, 12, 0);
        long middle = place(customer1, 12, 30);
        // Hai đơn đã kết thúc: đơn tạo sau (mã lớn hơn) là mới hơn.
        jdbc.update("UPDATE orders SET status = 'PICKED_UP' WHERE id = ?", late);
        long cancelledLater = place(customer1, 14, 0);
        jdbc.update("UPDATE orders SET status = 'CANCELLED' WHERE id = ?", cancelledLater);

        String body = mockMvc.perform(get("/api/orders").session(login("khach1@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Integer> ids = com.jayway.jsonpath.JsonPath.read(body, "$.orders[*].id");
        assertThat(ids).containsExactly((int) early, (int) middle, (int) cancelledLater, (int) late);
    }

    @Test
    void listShowsOnlyTheCustomersOwnOrdersWithTheDocumentedFields() throws Exception {
        long mine = place(customer1, 12, 0);
        place(customer2, 12, 0);

        mockMvc.perform(get("/api/orders").session(login("khach1@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(1))
                .andExpect(jsonPath("$.orders[0].id").value(mine))
                .andExpect(jsonPath("$.orders[0].pickupCode")
                        .value(org.hamcrest.Matchers.matchesPattern("[A-HJKMNP-Z2-9]{6}")))
                .andExpect(jsonPath("$.orders[0].slotStart").value("2031-03-05T12:00"))
                .andExpect(jsonPath("$.orders[0].slotEnd").value("2031-03-05T12:15"))
                .andExpect(jsonPath("$.orders[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orders[0].paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.orders[0].totalAmount").value(25000))
                .andExpect(jsonPath("$.orders[0].lines").doesNotExist());
    }

    @Test
    void detailShowsSavedLinesPickupCodeAndPaymentReminderData() throws Exception {
        long id = place(customer1, 12, 0);
        // Giá món đổi sau khi đặt không làm đổi đơn cũ (BR-35).
        jdbc.update("UPDATE menu_items SET price = 99000 WHERE name = 'Khoai tây chiên'");
        try {
            mockMvc.perform(get("/api/orders/" + id).session(login("khach1@example.com")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                    .andExpect(jsonPath("$.paymentMethod").doesNotExist())
                    .andExpect(jsonPath("$.totalAmount").value(25000))
                    .andExpect(jsonPath("$.confirmedAt").value(org.hamcrest.Matchers.endsWith("+07:00")))
                    .andExpect(jsonPath("$.lines[0].name").value("Khoai tây chiên"))
                    .andExpect(jsonPath("$.lines[0].unitPrice").value(25000));
        } finally {
            jdbc.update("UPDATE menu_items SET price = 25000 WHERE name = 'Khoai tây chiên'");
        }
    }

    @Test
    void anotherCustomersOrderLooksExactlyLikeANonexistentOne() throws Exception {
        long othersOrder = place(customer2, 12, 0);
        MockHttpSession me = login("khach1@example.com");

        String forOthers = mockMvc.perform(get("/api/orders/" + othersOrder).session(me))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String forMissing = mockMvc.perform(get("/api/orders/987654").session(me))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // UC-05 E1: không lộ đơn đó có thật hay không. Chỉ trường "instance" khác nhau vì nó lặp lại đường dẫn khách
        // gửi.
        assertThat(forOthers.replace("/api/orders/" + othersOrder, "/api/orders/X"))
                .isEqualTo(forMissing.replace("/api/orders/987654", "/api/orders/X"));
        assertThat(forOthers).doesNotContain("pickupCode");
    }

    @Test
    void malformedIdIsAValidationErrorAndRolesAreEnforced() throws Exception {
        MockHttpSession me = login("khach1@example.com");

        mockMvc.perform(get("/api/orders/abc").session(me))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders").session(login("bep1@example.com"))).andExpect(status().isForbidden());
    }
}
