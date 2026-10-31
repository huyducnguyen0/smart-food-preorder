package vn.nhom15.preorder.blackbox;

import com.jayway.jsonpath.JsonPath;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.Cookie;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import vn.nhom15.preorder.order.OrderRepository;
import vn.nhom15.preorder.support.MutableClock;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Nền chung của bộ test black-box (T-25). Test chỉ đi qua API HTTP của ứng dụng thật trên PostgreSQL thật, như một
 * client bên ngoài: đăng nhập thật, header chống CSRF thật. Database chỉ được đọc để kiểm kết quả hoặc dựng điều kiện
 * trước mà API chưa có (ví dụ khung đã giữ 15 điểm, đơn đã hủy).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BlackBoxClockConfig.class)
abstract class BlackBoxBase {

    static final HikariDataSource DATA_SOURCE = TestDatabase.create(true);
    static final String PASSWORD = "Demo@2026";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATA_SOURCE::getJdbcUrl);
        registry.add("spring.datasource.username", DATA_SOURCE::getUsername);
        registry.add("spring.datasource.password", DATA_SOURCE::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MutableClock clock;

    @Autowired
    vn.nhom15.preorder.slot.KitchenPlanService kitchenPlanService;

    /** Dùng để tiêm lỗi database cho TC-ORD-29 và TC-KIT-23; các test khác không đụng tới. */
    @MockitoSpyBean
    OrderRepository orderRepository;

    /** Kết quả một lời gọi API. */
    record Res(int status, String body) {
        Object json(String path) {
            return JsonPath.read(body, path);
        }

        <T> T at(String path) {
            return JsonPath.read(body, path);
        }

        boolean has(String path) {
            try {
                Object value = JsonPath.read(body, path);
                return value != null && !(value instanceof List<?> list && list.isEmpty());
            } catch (Exception e) {
                return false;
            }
        }
    }

    /** Món của menu kiểm thử (BLACK_BOX_TEST_CASES mục 2). */
    record Item(long id, String name, int price, int points) {}

    @BeforeEach
    void resetWorld() {
        Mockito.reset(orderRepository);
        jdbc.execute("TRUNCATE order_lines, orders, time_slots RESTART IDENTITY CASCADE");
        jdbc.update("DELETE FROM user_accounts WHERE login_id LIKE 'bb-%'");
        jdbc.update("UPDATE user_accounts SET locked = (login_id = 'khoa@example.com')");
        // Cấu hình tương đương mô hình quota cũ (làm trước tối đa 15 phút, đặt trước 100%, quầy không giới hạn): đơn
        // nhận lúc S chỉ làm trong 15 phút ngay trước S, nên các ca của Sprint 1–3 về "khung còn bao nhiêu" giữ nguyên
        // ý nghĩa. Ca của lịch bếp (PB-22) tự đặt cấu hình thật bằng useSprint4Settings().
        jdbc.update("UPDATE store_settings SET open_time = '10:00', close_time = '21:00', default_quota_points = 20,"
                + " hold_minutes = 15, preorder_share_percent = 100, pickup_limit_orders = 1000");
        // Menu kiểm thử: giá, điểm, tình trạng theo bảng ở mục 2 của tài liệu test.
        // Món mẫu có mã 1–8 theo thứ tự chèn của R__seed_menu.sql; test quản lý món có thể đổi tên hoặc thêm món.
        String[][] menu = {
            {"Burger", "Burger bò", "45000", "5", "false", "false"},
            {"Burger", "Burger phô mai đôi", "59000", "7", "false", "false"},
            {"Burger", "Burger gà giòn", "39000", "5", "true", "false"},
            {"Gà rán", "Gà rán 2 miếng", "55000", "6", "false", "false"},
            {"Gà rán", "Khoai tây chiên", "25000", "2", "false", "false"},
            {"Đồ uống", "Trà đào", "20000", "0", "false", "false"},
            {"Đồ uống", "Nước suối", "10000", "0", "false", "false"},
            {"Burger", "Combo cũ", "99000", "10", "false", "true"},
        };
        jdbc.update("DELETE FROM menu_items WHERE id > ?", menu.length);
        for (int i = 0; i < menu.length; i++) {
            String[] row = menu[i];
            jdbc.update(
                    "UPDATE menu_items SET category_id = (SELECT id FROM menu_categories WHERE name = ?), name = ?,"
                            + " price = ?, workload_points = ?, available = ?, discontinued = ? WHERE id = ?",
                    row[0],
                    row[1],
                    Integer.parseInt(row[2]),
                    Integer.parseInt(row[3]),
                    !Boolean.parseBoolean(row[4]),
                    Boolean.parseBoolean(row[5]),
                    i + 1);
        }
        at("11:00");
    }

    // ---------- thời gian ----------

    /** Đặt "bây giờ" là giờ {@code hhmm} (ví dụ "11:00") của ngày kiểm thử. */
    void at(String hhmm) {
        clock.set(BlackBoxClockConfig.TODAY.atTime(LocalTime.parse(hhmm)));
    }

    /** Giờ bắt đầu khung dạng ISO của ngày kiểm thử, ví dụ {@code slot("12:00")} → {@code 2031-03-05T12:00}. */
    static String slot(String hhmm) {
        return BlackBoxClockConfig.TODAY + "T" + hhmm;
    }

    // ---------- HTTP ----------

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        String token = mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/me"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN")
                .getValue();
        return request.cookie(new Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", token);
    }

    private static Res res(MvcResult result) throws Exception {
        return new Res(result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    Res get(String path, MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(path);
        return res(mockMvc.perform(session == null ? request : request.session(session))
                .andReturn());
    }

    /** POST kèm token chống CSRF; {@code json} có thể null. */
    Res post(String path, MockHttpSession session, String json) throws Exception {
        MockHttpServletRequestBuilder request = withCsrf(MockMvcRequestBuilders.post(path));
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return res(mockMvc.perform(session == null ? request : request.session(session))
                .andReturn());
    }

    /** Gọi API với phương thức bất kỳ (ví dụ PUT) kèm token chống CSRF. */
    Res send(String method, String path, MockHttpSession session, String json) throws Exception {
        MockHttpServletRequestBuilder request =
                withCsrf(MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), path));
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return res(mockMvc.perform(session == null ? request : request.session(session))
                .andReturn());
    }

    /** POST không có token chống CSRF (TC-AUTH-18). */
    Res postWithoutCsrf(String path, MockHttpSession session, String json) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(path);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return res(mockMvc.perform(session == null ? request : request.session(session))
                .andReturn());
    }

    /** Đăng nhập thật; trả về phiên. Ném lỗi nếu không đăng nhập được. */
    MockHttpSession login(String loginId) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(MockMvcRequestBuilders.post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new AssertionError("Đăng nhập " + loginId + " thất bại: "
                    + result.getResponse().getContentAsString());
        }
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    Res loginRaw(String loginId, String password) throws Exception {
        return post(
                "/api/auth/login",
                null,
                "{\"loginId\":\"" + loginId + "\",\"password\":\"" + password.replace("\"", "\\\"") + "\"}");
    }

    // ---------- dữ liệu ----------

    Item item(String name) {
        return jdbc.queryForObject(
                "SELECT id, name, price, workload_points FROM menu_items WHERE name = ?",
                (rs, n) -> new Item(
                        rs.getLong("id"), rs.getString("name"), rs.getInt("price"), rs.getInt("workload_points")),
                name);
    }

    /** Dòng đơn khớp giá hiện tại của món: "khách đã thấy đúng giá". */
    String line(String name, int quantity) {
        Item item = item(name);
        return line(item.id(), quantity, item.price());
    }

    static String line(long id, int quantity, int unitPrice) {
        return "{\"menuItemId\":" + id + ",\"quantity\":" + quantity + ",\"unitPrice\":" + unitPrice + "}";
    }

    static String orderJson(String requestId, String slotStart, String... lines) {
        return "{\"requestId\":\"" + requestId + "\",\"slotStart\":\"" + slotStart + "\",\"lines\":["
                + String.join(",", lines) + "]}";
    }

    static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    /** Đặt đơn qua API với mã yêu cầu mới. */
    Res order(MockHttpSession session, String hhmm, String... lines) throws Exception {
        return post("/api/orders", session, orderJson(newRequestId(), slot(hhmm), lines));
    }

    long orderCount() {
        // Đơn của khách giả (dựng điều kiện "khung đã giữ n điểm") không tính.
        return jdbc.queryForObject(
                "SELECT count(*) FROM orders o LEFT JOIN user_accounts u ON u.id = o.customer_id"
                        + " WHERE u.login_id IS DISTINCT FROM ?",
                Long.class,
                PHANTOM);
    }

    /** Điểm đang giữ của khung {@code hhmm}; 0 nếu khung chưa có dòng. */
    int held(String hhmm) {
        List<Integer> rows = jdbc.queryForList(
                "SELECT held_points FROM time_slots WHERE slot_date = ? AND start_time = ?",
                Integer.class,
                BlackBoxClockConfig.TODAY,
                LocalTime.parse(hhmm));
        return rows.isEmpty() ? 0 : rows.getFirst();
    }

    /** Cấu hình mặc định của lịch bếp (PB-22): làm trước tối đa 45 phút, đặt trước 70%, quầy 8 đơn mỗi khung. */
    void useSprint4Settings() {
        jdbc.update(
                "UPDATE store_settings SET hold_minutes = 45, preorder_share_percent = 70, pickup_limit_orders = 8");
    }

    static final String PHANTOM = "bb-phantom@example.com";

    /**
     * Dựng điều kiện trước "khung nhận {@code hhmm} đã giữ n điểm": thêm một đơn đặt trước thật (của khách giả
     * {@link #PHANTOM}, không tính trong {@link #orderCount()}) cho phần điểm còn thiếu, để lịch bếp thấy đúng lượng
     * việc.
     */
    void setHeld(String hhmm, int points) {
        LocalTime start = LocalTime.parse(hhmm);
        jdbc.update(
                "INSERT INTO time_slots (slot_date, start_time) VALUES (?, ?)"
                        + " ON CONFLICT (slot_date, start_time) DO NOTHING",
                BlackBoxClockConfig.TODAY,
                start);
        long slotId = jdbc.queryForObject(
                "SELECT id FROM time_slots WHERE slot_date = ? AND start_time = ?",
                Long.class,
                BlackBoxClockConfig.TODAY,
                start);
        int real = jdbc.queryForObject(
                "SELECT COALESCE(sum(workload_points), 0) FROM orders WHERE time_slot_id = ?"
                        + " AND NOT (status = 'CANCELLED' AND preparing_at IS NULL)",
                Integer.class,
                slotId);
        int missing = points - real;
        if (missing > 0) {
            jdbc.update(
                    "INSERT INTO orders (customer_id, source, time_slot_id, status, payment_status, workload_points,"
                            + " total_amount, pickup_code, request_id, confirmed_at)"
                            + " VALUES (?, 'PRE_ORDER', ?, 'CONFIRMED', 'UNPAID', ?, 0, ?, ?, now())",
                    phantomCustomer(),
                    slotId,
                    missing,
                    phantomCode(),
                    UUID.randomUUID());
        }
        jdbc.update("UPDATE time_slots SET held_points = ? WHERE id = ?", Math.max(points, real), slotId);
    }

    private long phantomCustomer() {
        List<Long> ids = jdbc.queryForList("SELECT id FROM user_accounts WHERE login_id = ?", Long.class, PHANTOM);
        if (!ids.isEmpty()) {
            return ids.getFirst();
        }
        return jdbc.queryForObject(
                "INSERT INTO user_accounts (login_id, password_hash, display_name, role) VALUES (?, 'x', 'Khách giả',"
                        + " 'CUSTOMER') RETURNING id",
                Long.class,
                PHANTOM);
    }

    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private final java.util.Random codeRandom = new java.util.Random();

    private String phantomCode() {
        while (true) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                code.append(CODE_CHARS.charAt(codeRandom.nextInt(CODE_CHARS.length())));
            }
            Boolean taken = jdbc.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM orders WHERE pickup_code = ?)", Boolean.class, code.toString());
            if (!Boolean.TRUE.equals(taken)) {
                return code.toString();
            }
        }
    }

    /** Dựng điều kiện trước "khung nhận {@code hhmm} có quota riêng q": khoảng làm ngay trước khung có công suất q. */
    void setQuotaOverride(String hhmm, int quota) {
        upsertSlot(LocalTime.parse(hhmm).minusMinutes(15), "quota_override", quota);
    }

    /** Công suất bếp riêng của khoảng làm bắt đầu lúc {@code hhmm} (PB-22). */
    void setCapacity(String hhmm, int capacity) {
        upsertSlot(LocalTime.parse(hhmm), "quota_override", capacity);
    }

    private void upsertSlot(LocalTime start, String column, int value) {
        jdbc.update(
                "INSERT INTO time_slots (slot_date, start_time, " + column + ") VALUES (?, ?, ?)"
                        + " ON CONFLICT (slot_date, start_time) DO UPDATE SET " + column + " = EXCLUDED." + column,
                BlackBoxClockConfig.TODAY,
                start,
                value);
    }

    void setStatus(long orderId, String status) {
        switch (status) {
            case "CONFIRMED" -> {}
            case "PREPARING" ->
                jdbc.update(
                        "UPDATE orders SET status = 'PREPARING', preparing_at = now(), preparing_by = ? WHERE id = ?",
                        kitchenId(),
                        orderId);
            case "READY" ->
                jdbc.update(
                        "UPDATE orders SET status = 'READY', preparing_at = now(), preparing_by = ?, ready_at = now(),"
                                + " ready_by = ? WHERE id = ?",
                        kitchenId(),
                        kitchenId(),
                        orderId);
            default -> jdbc.update("UPDATE orders SET status = ? WHERE id = ?", status, orderId);
        }
    }

    long kitchenId() {
        return jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'bep1@example.com'", Long.class);
    }

    String statusOf(long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    /** Tạo {@code count} tài khoản khách phụ (cùng mật khẩu mẫu), trả về các định danh đăng nhập. */
    List<String> createCustomers(int count) {
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM user_accounts WHERE login_id = 'khach1@example.com'", String.class);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String loginId = "bb-" + UUID.randomUUID() + "@example.com";
            jdbc.update(
                    "INSERT INTO user_accounts (login_id, password_hash, display_name, role, locked)"
                            + " VALUES (?, ?, 'Khách thử', 'CUSTOMER', false)",
                    loginId,
                    hash);
            ids.add(loginId);
        }
        return ids;
    }

    List<MockHttpSession> sessionsFor(List<String> loginIds) throws Exception {
        List<MockHttpSession> sessions = new ArrayList<>();
        for (String loginId : loginIds) {
            sessions.add(login(loginId));
        }
        return sessions;
    }

    /**
     * Bất biến của Database Design mục 3 (TC-CON-07): với mọi khung, điểm đang giữ bằng tổng điểm các đơn đang giữ
     * quota của khung. Đơn hủy từ {@code CONFIRMED} (chưa từng bắt đầu làm) đã được hoàn điểm nên không tính (BR-23,
     * BR-24); mọi đơn khác vẫn giữ điểm.
     */
    void assertHeldMatchesOrders() {
        List<String> mismatches = new ArrayList<>();
        jdbc.query("""
                        SELECT s.start_time, s.held_points,
                               COALESCE((SELECT sum(o.workload_points) FROM orders o WHERE o.time_slot_id = s.id
                                         AND NOT (o.status = 'CANCELLED' AND o.preparing_at IS NULL)), 0) AS orders
                        FROM time_slots s
                        """, rs -> {
            LocalTime start = rs.getObject("start_time", LocalTime.class);
            int expected = rs.getInt("orders");
            if (rs.getInt("held_points") != expected) {
                mismatches.add(start + " held=" + rs.getInt("held_points") + " orders=" + expected);
            }
        });
        if (!mismatches.isEmpty()) {
            throw new AssertionError("held_points khác tổng điểm đơn: " + mismatches);
        }
        assertKitchenPlanWithinCapacity();
    }

    /**
     * Bất biến của lịch bếp (PB-22, NFR-01): với các đơn còn phải làm, bếp xếp được lịch mà không khoảng làm nào vượt
     * công suất. Tính lại độc lập từ database bằng chính bộ xếp lịch.
     */
    void assertKitchenPlanWithinCapacity() {
        vn.nhom15.preorder.slot.KitchenPlanService.Snapshot plan = kitchenPlanService.snapshot();
        if (!plan.acceptedOrdersStillFit()) {
            throw new AssertionError("Lịch bếp không còn xếp kịp các đơn đã nhận");
        }
    }

    /** Chạy các việc đồng loạt (xuất phát cùng lúc) và gom kết quả. */
    <T> List<T> concurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(tasks.size(), 1));
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return task.call();
                }));
            }
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
