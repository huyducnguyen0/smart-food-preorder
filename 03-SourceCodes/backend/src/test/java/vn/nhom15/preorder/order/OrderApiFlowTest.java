package vn.nhom15.preorder.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Đặt đơn qua HTTP trên toàn bộ ứng dụng thật: đăng nhập, phân quyền, kiểm tra dữ liệu gửi lên, mã HTTP và dạng JSON
 * của hợp đồng API (Architecture mục 5). Logic nghiệp vụ chi tiết nằm ở {@link PlaceOrderIntegrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiFlowTest {

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

    private long burgerId;
    private long teaId;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE order_lines, orders, time_slots RESTART IDENTITY CASCADE");
        // Cấu hình tương đương mô hình quota Sprint 1 (làm trước 15 phút, đặt trước 100%, quầy không giới hạn), xem
        // PB-22.
        jdbc.update("UPDATE store_settings SET hold_minutes = 15, preorder_share_percent = 100,"
                + " pickup_limit_orders = 1000");
        burgerId = jdbc.queryForObject("SELECT id FROM menu_items WHERE name = 'Burger bò'", Long.class);
        teaId = jdbc.queryForObject("SELECT id FROM menu_items WHERE name = 'Trà đào'", Long.class);
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

    private String body(String requestId, String slotStart, String lines) {
        return "{\"requestId\":\"" + requestId + "\",\"slotStart\":\"" + slotStart + "\",\"lines\":[" + lines + "]}";
    }

    private String lineJson(long id, int quantity, int unitPrice) {
        return "{\"menuItemId\":" + id + ",\"quantity\":" + quantity + ",\"unitPrice\":" + unitPrice + "}";
    }

    @Test
    void customerListsSlotsThenPlacesAnOrderAndResendGetsTheSameOne() throws Exception {
        MockHttpSession customer = login("khach1@example.com");

        mockMvc.perform(get("/api/slots").param("lines", burgerId + ":2").session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2031-03-05"))
                .andExpect(jsonPath("$.slots.length()").value(44))
                .andExpect(jsonPath("$.slots[0].start").value("2031-03-05T10:00"))
                .andExpect(jsonPath("$.slots[0].bookable").value(true))
                .andExpect(jsonPath("$.unavailableReason").doesNotExist());

        String requestId = "8a0f3c2e-5b1d-4c47-9a0e-2f6d1c3b7e91";
        String json =
                body(requestId, "2031-03-05T11:30", lineJson(burgerId, 2, 45000) + "," + lineJson(teaId, 1, 20000));

        MvcResult created = mockMvc.perform(withCsrf(post("/api/orders"))
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/orders/")))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.paymentMethod").doesNotExist())
                .andExpect(jsonPath("$.totalAmount").value(110000))
                .andExpect(jsonPath("$.slotStart").value("2031-03-05T11:30"))
                .andExpect(jsonPath("$.slotEnd").value("2031-03-05T11:45"))
                .andExpect(jsonPath("$.pickupCode").value(org.hamcrest.Matchers.matchesPattern("[A-HJKMNP-Z2-9]{6}")))
                .andExpect(jsonPath("$.confirmedAt").value(org.hamcrest.Matchers.endsWith("+07:00")))
                .andExpect(jsonPath("$.lines[0].name").value("Burger bò"))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(90000))
                .andReturn();
        String createdBody = created.getResponse().getContentAsString();
        // BR-36: khách không bao giờ thấy điểm công việc hay quota.
        assertThat(createdBody)
                .doesNotContain("workload")
                .doesNotContain("quota")
                .doesNotContain("held");

        // A1: gửi lại cùng mã yêu cầu → 200 và đúng đơn đó.
        MvcResult resent = mockMvc.perform(withCsrf(post("/api/orders"))
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(resent.getResponse().getContentAsString()).isEqualTo(createdBody);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void slotListShowsFullSlotAsNotBookableAfterAnotherCustomerFilledIt() throws Exception {
        MockHttpSession first = login("khach1@example.com");
        MockHttpSession second = login("0901234567");
        mockMvc.perform(withCsrf(post("/api/orders"))
                        .session(first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(
                                "11111111-1111-4111-8111-111111111111",
                                "2031-03-05T12:00",
                                lineJson(burgerId, 4, 45000))))
                .andExpect(status().isCreated()); // 20 điểm = đầy khung

        mockMvc.perform(get("/api/slots").param("lines", burgerId + ":1").session(second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[?(@.start == '2031-03-05T12:00')].bookable")
                        .value(false))
                .andExpect(jsonPath("$.slots[?(@.start == '2031-03-05T12:15')].bookable")
                        .value(true));

        mockMvc.perform(withCsrf(post("/api/orders"))
                        .session(second)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(
                                "22222222-2222-4222-8222-222222222222",
                                "2031-03-05T12:00",
                                lineJson(burgerId, 1, 45000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_FULL"));
    }

    @Test
    void businessErrorsUseDocumentedCodesAndStatuses() throws Exception {
        MockHttpSession customer = login("khach1@example.com");

        // E7: giá khác giỏ → 409 ITEMS_CHANGED kèm danh sách món.
        mockMvc.perform(withCsrf(post("/api/orders"))
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(
                                "33333333-3333-4333-8333-333333333333", "2031-03-05T12:00", lineJson(burgerId, 1, 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ITEMS_CHANGED"))
                .andExpect(jsonPath("$.items[0].menuItemId").value(burgerId))
                .andExpect(jsonPath("$.items[0].reason").value("PRICE_CHANGED"))
                .andExpect(jsonPath("$.items[0].currentPrice").value(45000));

        // E6: khung đã qua / không hợp lệ → 409 SLOT_NOT_BOOKABLE.
        mockMvc.perform(withCsrf(post("/api/orders"))
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(
                                "44444444-4444-4444-8444-444444444444",
                                "2031-03-05T09:00",
                                lineJson(burgerId, 1, 45000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_BOOKABLE"));

        // E9: 5 × 5 = 25 điểm > quota 20 → 409 ORDER_TOO_LARGE.
        mockMvc.perform(withCsrf(post("/api/orders"))
                        .session(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(
                                "55555555-5555-4555-8555-555555555555",
                                "2031-03-05T12:00",
                                lineJson(burgerId, 5, 45000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_TOO_LARGE"));
    }

    @Test
    void malformedRequestsAreRejectedWith400() throws Exception {
        MockHttpSession customer = login("khach1@example.com");

        String[] bad = {
            "{}",
            body("not-a-uuid", "2031-03-05T12:00", lineJson(burgerId, 1, 45000)),
            body("66666666-6666-4666-8666-666666666666", "ngày-sai", lineJson(burgerId, 1, 45000)),
            body("66666666-6666-4666-8666-666666666666", "2031-03-05T12:00", ""),
            body("66666666-6666-4666-8666-666666666666", "2031-03-05T12:00", lineJson(burgerId, 0, 45000)),
            body("66666666-6666-4666-8666-666666666666", "2031-03-05T12:00", lineJson(burgerId, 1000, 45000)),
            body("66666666-6666-4666-8666-666666666666", "2031-03-05T12:00", lineJson(burgerId, 1, -1)),
            body(
                    "66666666-6666-4666-8666-666666666666",
                    "2031-03-05T12:00",
                    lineJson(burgerId, 1, 45000) + "," + lineJson(burgerId, 2, 45000)),
        };
        for (String json : bad) {
            mockMvc.perform(withCsrf(post("/api/orders"))
                            .session(customer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class))
                .isZero();

        for (String lines : new String[] {"", "abc", "1", "1:x", "1:2,", burgerId + ":0", "999999:1"}) {
            mockMvc.perform(get("/api/slots").param("lines", lines).session(customer))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(get("/api/slots").session(customer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void onlyLoggedInCustomersCanUseSlotsAndOrders() throws Exception {
        String json = body("77777777-7777-4777-8777-777777777777", "2031-03-05T12:00", lineJson(burgerId, 1, 45000));
        MockHttpSession kitchen = login("bep1@example.com");

        mockMvc.perform(get("/api/slots").param("lines", burgerId + ":1")).andExpect(status().isUnauthorized());
        mockMvc.perform(withCsrf(post("/api/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/slots").param("lines", burgerId + ":1").session(kitchen))
                .andExpect(status().isForbidden());
        mockMvc.perform(withCsrf(post("/api/orders"))
                        .session(kitchen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
        // Thiếu token CSRF: bị chặn trước cả khi vào controller.
        mockMvc.perform(post("/api/orders")
                        .session(login("khach1@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class))
                .isZero();
    }
}
