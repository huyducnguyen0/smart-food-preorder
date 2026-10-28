package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box đồng thời (UC-17, NFR-01, NFR-02), UC-05 Đơn của tôi và UC-12 Xử lý đơn tại bếp (T-25). Các test đồng thời
 * gửi yêu cầu HTTP cùng lúc từ nhiều luồng vào ứng dụng thật và PostgreSQL thật.
 */
class ConcurrencyMyKitchenBlackBoxTest extends BlackBoxBase {

    private long id(Res placed) {
        return ((Number) placed.json("$.id")).longValue();
    }

    private void setWorkload(String name, int points) {
        jdbc.update("UPDATE menu_items SET workload_points = ? WHERE name = ?", points, name);
    }

    private long place(MockHttpSession session, String hhmm, String... lines) throws Exception {
        Res result = order(session, hhmm, lines);
        assertThat(result.status()).as(result.body()).isEqualTo(201);
        return id(result);
    }

    // ---------- Đồng thời ----------

    @Test
    @DisplayName("TC-CON-01 50 khách đặt đơn 3 điểm vào cùng khung quota 20: đúng 6 đơn, giữ 18, không vượt 20")
    void con01() throws Exception {
        setWorkload("Khoai tây chiên", 3);
        List<MockHttpSession> sessions = sessionsFor(createCustomers(50));
        List<Callable<Res>> tasks = new ArrayList<>();
        for (MockHttpSession session : sessions) {
            tasks.add(() -> order(session, "12:00", line("Khoai tây chiên", 1)));
        }

        List<Res> results = concurrently(tasks);

        assertThat(results.stream().filter(r -> r.status() == 201)).hasSize(6);
        assertThat(results.stream().filter(r -> r.status() == 409))
                .hasSize(44)
                .allSatisfy(r -> assertThat((String) r.at("$.code")).isEqualTo("SLOT_FULL"));
        assertThat(held("12:00")).isEqualTo(18);
        assertThat(orderCount()).isEqualTo(6);
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-CON-02 Khung còn đúng 10 điểm, hai khách cùng đặt 8 điểm: đúng 1 đơn được nhận")
    void con02() throws Exception {
        setWorkload("Khoai tây chiên", 8);
        setHeld("12:00", 10);
        MockHttpSession kh1 = login("khach1@example.com");
        MockHttpSession kh2 = login("0901234567");

        List<Res> results = concurrently(List.of(
                () -> order(kh1, "12:00", line("Khoai tây chiên", 1)),
                () -> order(kh2, "12:00", line("Khoai tây chiên", 1))));

        assertThat(results.stream().filter(r -> r.status() == 201)).hasSize(1);
        assertThat(results.stream().filter(r -> r.status() == 409)).hasSize(1);
        assertThat(held("12:00")).isEqualTo(18);
    }

    @Test
    @DisplayName(
            "TC-CON-03 60 yêu cầu 1–6 điểm, quota 15: giữ = tổng đơn nhận ≤ 15, đơn bị từ chối đều quá phần còn lại")
    void con03() throws Exception {
        setWorkload("Khoai tây chiên", 1);
        setQuotaOverride("12:00", 15);
        List<MockHttpSession> sessions = sessionsFor(createCustomers(60));
        Random random = new Random(20261025);
        List<Integer> quantities = new ArrayList<>();
        List<Callable<Res>> tasks = new ArrayList<>();
        for (MockHttpSession session : sessions) {
            int quantity = 1 + random.nextInt(6);
            quantities.add(quantity);
            tasks.add(() -> order(session, "12:00", line("Khoai tây chiên", quantity)));
        }

        List<Res> results = concurrently(tasks);

        int accepted = 0;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).status() == 201) {
                accepted += quantities.get(i);
            }
        }
        assertThat(accepted).isLessThanOrEqualTo(15);
        assertThat(held("12:00")).isEqualTo(accepted);
        int remaining = 15 - accepted;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).status() != 201) {
                assertThat(results.get(i).status()).isEqualTo(409);
                assertThat(quantities.get(i))
                        .as("đơn bị từ chối phải lớn hơn phần còn lại")
                        .isGreaterThan(remaining);
            }
        }
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName(
            "TC-CON-04 KH1 đang có 2 đơn, gửi cùng lúc hai yêu cầu mã khác: chỉ một đơn được nhận, không bao giờ 4 đơn")
    void con04() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        place(kh1, "12:00", line("Burger bò", 1));
        place(kh1, "12:15", line("Burger bò", 1));

        List<Res> results = concurrently(List.of(
                () -> order(kh1, "12:30", line("Burger bò", 1)), () -> order(kh1, "12:45", line("Burger bò", 1))));

        assertThat(results.stream().filter(r -> r.status() == 201)).hasSize(1);
        Res rejected =
                results.stream().filter(r -> r.status() != 201).findFirst().orElseThrow();
        assertThat(rejected.status()).isEqualTo(409);
        assertThat((String) rejected.at("$.code")).isEqualTo("OPEN_ORDER_LIMIT_REACHED");
        assertThat(orderCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-CON-05 Hai nhóm khách đặt đồng thời vào hai khung: mỗi khung giữ đúng theo quota riêng")
    void con05() throws Exception {
        setWorkload("Khoai tây chiên", 3);
        List<MockHttpSession> groupA = sessionsFor(createCustomers(10));
        List<MockHttpSession> groupB = sessionsFor(createCustomers(10));
        List<Callable<Res>> tasks = new ArrayList<>();
        for (MockHttpSession session : groupA) {
            tasks.add(() -> order(session, "12:00", line("Khoai tây chiên", 1)));
        }
        for (MockHttpSession session : groupB) {
            tasks.add(() -> order(session, "12:15", line("Khoai tây chiên", 1)));
        }

        concurrently(tasks);

        assertThat(held("12:00")).isEqualTo(18);
        assertThat(held("12:15")).isEqualTo(18);
        assertThat(orderCount()).isEqualTo(12);
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-CON-06 Gửi lại cùng mã 5 lần, trộn tuần tự và đồng thời: đúng 1 đơn, quota giữ 1 lần")
    void con06() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        String body = orderJson(newRequestId(), slot("12:00"), line("Burger bò", 2));
        post("/api/orders", kh1, body);
        List<Callable<Res>> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(() -> post("/api/orders", kh1, body));
        }
        concurrently(tasks);
        post("/api/orders", kh1, body);

        assertThat(orderCount()).isEqualTo(1);
        assertThat(held("12:00")).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-CON-07 Sau các luồng đồng thời: điểm đang giữ của mọi khung = tổng điểm đơn, ≤ quota")
    void con07() throws Exception {
        setWorkload("Khoai tây chiên", 3);
        List<MockHttpSession> sessions = sessionsFor(createCustomers(20));
        List<Callable<Res>> tasks = new ArrayList<>();
        for (int i = 0; i < sessions.size(); i++) {
            MockHttpSession session = sessions.get(i);
            String hhmm = i % 2 == 0 ? "12:00" : "12:15";
            tasks.add(() -> order(session, hhmm, line("Khoai tây chiên", 1)));
        }
        concurrently(tasks);

        assertHeldMatchesOrders();
        assertThat(held("12:00")).isLessThanOrEqualTo(20);
        assertThat(held("12:15")).isLessThanOrEqualTo(20);
    }

    // ---------- UC-05 Đơn của tôi ----------

    @Test
    @DisplayName("TC-MY-01 KH1 có 2 đơn, KH2 có 1 đơn: KH1 chỉ thấy đơn của mình, đủ trường")
    void my01() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        place(kh1, "12:00", line("Burger bò", 1));
        place(kh1, "12:15", line("Burger bò", 1));
        place(login("0901234567"), "12:30", line("Burger bò", 1));

        Res list = get("/api/orders", kh1);

        assertThat(list.<List<Object>>at("$.orders")).hasSize(2);
        assertThat(list.<List<Integer>>at("$.orders[*].id")).hasSize(2);
        for (String field : List.of("pickupCode", "slotStart", "slotEnd", "status", "paymentStatus", "totalAmount")) {
            assertThat(list.<List<Object>>at("$.orders[*]." + field))
                    .as(field)
                    .hasSize(2)
                    .doesNotContainNull();
        }
    }

    @Test
    @DisplayName("TC-MY-02 Đơn mở theo giờ nhận tăng dần, đơn kết thúc sau, mới nhất trước")
    void my02() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        long open14 = place(kh1, "14:00", line("Burger bò", 1));
        long open12 = place(kh1, "12:00", line("Burger bò", 1));
        long yesterdayPicked = place(kh1, "12:30", line("Burger bò", 1));
        setStatus(yesterdayPicked, "PICKED_UP");
        long dayBeforeCancelled = place(kh1, "13:00", line("Burger bò", 1));
        setStatus(dayBeforeCancelled, "CANCELLED");
        // Đơn đã kết thúc: đơn tạo sau là mới hơn.

        List<Integer> ids = get("/api/orders", kh1).at("$.orders[*].id");

        assertThat(ids.subList(0, 2)).containsExactly((int) open12, (int) open14);
        assertThat(ids.subList(2, 4)).containsExactly((int) dayBeforeCancelled, (int) yesterdayPicked);
    }

    @Test
    @DisplayName(
            "TC-MY-03 Chi tiết đơn: món, giá lưu, tổng tiền, khung, trạng thái, mã nhận, thanh toán, thời điểm đặt")
    void my03() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        long id = place(kh1, "12:00", line("Burger bò", 2), line("Trà đào", 1));

        Res detail = get("/api/orders/" + id, kh1);

        assertThat(detail.status()).isEqualTo(200);
        assertThat(detail.<List<String>>at("$.lines[*].name")).containsExactly("Burger bò", "Trà đào");
        assertThat(detail.<List<Integer>>at("$.lines[*].quantity")).containsExactly(2, 1);
        assertThat(detail.<List<Integer>>at("$.lines[*].unitPrice")).containsExactly(45000, 20000);
        assertThat((Integer) detail.at("$.totalAmount")).isEqualTo(110000);
        assertThat((String) detail.at("$.slotStart")).isEqualTo(slot("12:00"));
        assertThat((String) detail.at("$.status")).isEqualTo("CONFIRMED");
        assertThat((String) detail.at("$.pickupCode")).hasSize(6);
        assertThat((String) detail.at("$.paymentStatus")).isEqualTo("UNPAID");
        assertThat((String) detail.at("$.confirmedAt")).endsWith("+07:00");
    }

    @Test
    @DisplayName("TC-MY-08 Đơn của khách khác trả giống hệt đơn không tồn tại")
    void my08() throws Exception {
        long others = place(login("0901234567"), "12:00", line("Burger bò", 1));
        MockHttpSession kh1 = login("khach1@example.com");

        Res forOthers = get("/api/orders/" + others, kh1);
        Res forMissing = get("/api/orders/987654", kh1);

        assertThat(forOthers.status()).isEqualTo(404);
        assertThat(forOthers.body().replace("/api/orders/" + others, "X"))
                .isEqualTo(forMissing.body().replace("/api/orders/987654", "X"));
        assertThat(forOthers.body()).doesNotContain("pickupCode");
    }

    @Test
    @DisplayName("TC-MY-09 Chưa đăng nhập và BẾP gọi API danh sách và chi tiết đơn: bị từ chối")
    void my09() throws Exception {
        long id = place(login("khach1@example.com"), "12:00", line("Burger bò", 1));
        MockHttpSession kitchen = login("bep1@example.com");

        assertThat(get("/api/orders", null).status()).isEqualTo(401);
        assertThat(get("/api/orders/" + id, null).status()).isEqualTo(401);
        assertThat(get("/api/orders", kitchen).status()).isEqualTo(403);
        assertThat(get("/api/orders/" + id, kitchen).status()).isEqualTo(403);
    }

    @Test
    @DisplayName("TC-MY-11 Đơn walk-in không xuất hiện trong danh sách của khách")
    void my11() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        long mine = place(kh1, "12:00", line("Burger bò", 1));
        jdbc.update(
                "INSERT INTO orders (customer_id, source, time_slot_id, status, payment_status, workload_points,"
                        + " total_amount, pickup_code, confirmed_at, created_by)"
                        + " SELECT NULL, 'WALK_IN', time_slot_id, 'CONFIRMED', 'UNPAID', 0, 0, 'WXYZ23', now(), ?"
                        + " FROM orders WHERE id = ?",
                kitchenId(),
                mine);

        List<Integer> ids = get("/api/orders", kh1).at("$.orders[*].id");

        assertThat(ids).containsExactly((int) mine);
    }

    // ---------- UC-12 Xử lý đơn tại bếp ----------

    private Res queue() throws Exception {
        return get("/api/kitchen/queue", login("bep1@example.com"));
    }

    private List<Integer> ids(Res queue, String group) {
        return queue.at("$." + group + "[*].id");
    }

    /** Đặt đơn lúc 09:00 rồi chuyển đồng hồ về giờ cần xem. */
    private long placeEarly(MockHttpSession session, String slotHhmm, String confirmedAt, String... lines)
            throws Exception {
        at(confirmedAt);
        long id = place(session, slotHhmm, lines);
        return id;
    }

    @Test
    @DisplayName("TC-KIT-01 T=11:20: Đang làm = A; Cần chuẩn bị = B; Sắp tới = C và D")
    void kit01() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        MockHttpSession kh2 = login("0901234567");
        long a = placeEarly(kh1, "11:30", "09:00", line("Burger bò", 1));
        long b = placeEarly(kh1, "11:30", "09:05", line("Burger bò", 1));
        long c = placeEarly(kh2, "11:45", "09:10", line("Burger bò", 1));
        long d = placeEarly(kh2, "13:00", "09:15", line("Burger bò", 1));
        setStatus(a, "PREPARING");
        at("11:20");

        Res result = queue();

        assertThat(ids(result, "preparing")).containsExactly((int) a);
        assertThat(ids(result, "dueSoon")).containsExactly((int) b);
        assertThat(ids(result, "upcoming")).containsExactly((int) c, (int) d);
    }

    @Test
    @DisplayName("TC-KIT-02 Khung 12:00, T=11:45: thuộc Cần chuẩn bị (đúng 15 phút trước)")
    void kit02() throws Exception {
        long id = placeEarly(login("khach1@example.com"), "12:00", "09:00", line("Burger bò", 1));
        at("11:45");

        assertThat(ids(queue(), "dueSoon")).containsExactly((int) id);
    }

    @Test
    @DisplayName("TC-KIT-03 Khung 12:00, T=11:44: thuộc Sắp tới (còn 16 phút)")
    void kit03() throws Exception {
        long id = placeEarly(login("khach1@example.com"), "12:00", "09:00", line("Burger bò", 1));
        at("11:44");

        assertThat(ids(queue(), "upcoming")).containsExactly((int) id);
    }

    @Test
    @DisplayName("TC-KIT-04 Thứ tự theo giờ khung rồi giờ xác nhận: C, B, A, không theo thời điểm tạo")
    void kit04() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        MockHttpSession kh2 = login("0901234567");
        // Tạo theo thứ tự A, B, C nhưng giờ xác nhận ghi trong database khác thứ tự tạo.
        long a = place(kh1, "12:15", line("Burger bò", 1));
        long b = place(kh1, "12:00", line("Burger bò", 1));
        long c = place(kh2, "12:00", line("Burger bò", 1));
        jdbc.update(
                "UPDATE orders SET confirmed_at = ? WHERE id = ?",
                java.time.OffsetDateTime.parse("2031-03-05T10:00:00+07:00"),
                a);
        jdbc.update(
                "UPDATE orders SET confirmed_at = ? WHERE id = ?",
                java.time.OffsetDateTime.parse("2031-03-05T11:00:00+07:00"),
                b);
        jdbc.update(
                "UPDATE orders SET confirmed_at = ? WHERE id = ?",
                java.time.OffsetDateTime.parse("2031-03-05T10:30:00+07:00"),
                c);
        at("12:00");

        Res result = queue();

        assertThat(ids(result, "dueSoon")).containsExactly((int) c, (int) b, (int) a);
    }

    @Test
    @DisplayName("TC-KIT-05 Đơn đã xác nhận khung 11:15, T=11:20: đánh dấu trễ")
    void kit05() throws Exception {
        placeEarly(login("khach1@example.com"), "11:15", "09:00", line("Burger bò", 1));
        at("11:20");

        assertThat(queue().<List<Boolean>>at("$.dueSoon[*].late")).containsExactly(true);
    }

    @Test
    @DisplayName("TC-KIT-06 Khung 11:30: đúng 11:30 thì trễ, 11:29 thì chưa")
    void kit06() throws Exception {
        placeEarly(login("khach1@example.com"), "11:30", "09:00", line("Burger bò", 1));

        at("11:29");
        assertThat(queue().<List<Boolean>>at("$.dueSoon[*].late")).containsExactly(false);
        at("11:30");
        assertThat(queue().<List<Boolean>>at("$.dueSoon[*].late")).containsExactly(true);
    }

    @Test
    @DisplayName("TC-KIT-08 Đơn READY, đã nhận, đã hủy, của hôm qua: không có trong hàng chờ")
    void kit08() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        MockHttpSession kh2 = login("0901234567");
        long ready = placeEarly(kh1, "12:00", "09:00", line("Burger bò", 1));
        long picked = placeEarly(kh1, "12:15", "09:00", line("Burger bò", 1));
        long cancelled = placeEarly(kh1, "12:30", "09:00", line("Burger bò", 1));
        long yesterday = placeEarly(kh2, "12:45", "09:00", line("Burger bò", 1));
        long visible = placeEarly(kh2, "13:00", "09:00", line("Burger bò", 1));
        setStatus(ready, "READY");
        setStatus(picked, "PICKED_UP");
        setStatus(cancelled, "CANCELLED");
        jdbc.update(
                "UPDATE time_slots SET slot_date = ? WHERE id = (SELECT time_slot_id FROM orders WHERE id = ?)",
                BlackBoxClockConfig.TODAY.minusDays(1),
                yesterday);
        at("11:00");

        Res result = queue();

        List<Integer> all = new ArrayList<>(ids(result, "preparing"));
        all.addAll(ids(result, "dueSoon"));
        all.addAll(ids(result, "upcoming"));
        assertThat(all).containsExactly((int) visible);
    }

    @Test
    @DisplayName("TC-KIT-09 (phần API) Thẻ đơn có mã, khung, nguồn, món, giờ xác nhận; không có giá hay thanh toán")
    void kit09() throws Exception {
        placeEarly(login("khach1@example.com"), "12:00", "09:00", line("Burger bò", 2), line("Trà đào", 1));
        at("11:50");

        Res result = queue();

        assertThat((String) result.at("$.dueSoon[0].pickupCode")).hasSize(6);
        assertThat((String) result.at("$.dueSoon[0].slotStart")).isEqualTo(slot("12:00"));
        assertThat((String) result.at("$.dueSoon[0].source")).isEqualTo("PRE_ORDER");
        assertThat(result.<List<String>>at("$.dueSoon[0].lines[*].name")).containsExactly("Burger bò", "Trà đào");
        assertThat((String) result.at("$.dueSoon[0].confirmedAt")).isNotBlank();
        assertThat(result.body())
                .doesNotContainIgnoringCase("price")
                .doesNotContainIgnoringCase("amount")
                .doesNotContainIgnoringCase("payment")
                .doesNotContainIgnoringCase("workload");
    }

    @Test
    @DisplayName("TC-KIT-13 BẾP bấm Bắt đầu: PREPARING, ghi người và thời điểm, sang nhóm Đang làm")
    void kit13() throws Exception {
        long id = placeEarly(login("khach1@example.com"), "13:00", "09:00", line("Burger bò", 1));
        at("11:00");
        MockHttpSession kitchen = login("bep1@example.com");

        Res result = post("/api/kitchen/orders/" + id + "/start", kitchen, null);

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.status")).isEqualTo("PREPARING");
        assertThat(jdbc.queryForObject("SELECT preparing_by FROM orders WHERE id = ?", Long.class, id))
                .isEqualTo(kitchenId());
        assertThat(jdbc.queryForObject("SELECT preparing_at IS NOT NULL FROM orders WHERE id = ?", Boolean.class, id))
                .isTrue();
        assertThat(ids(get("/api/kitchen/queue", kitchen), "preparing")).containsExactly((int) id);
    }

    @Test
    @DisplayName("TC-KIT-14 BẾP bấm Hoàn tất: READY, ghi người và thời điểm, rời hàng chờ, quota vẫn giữ")
    void kit14() throws Exception {
        long id = place(login("khach1@example.com"), "13:00", line("Burger bò", 1));
        MockHttpSession kitchen = login("bep1@example.com");
        post("/api/kitchen/orders/" + id + "/start", kitchen, null);

        Res result = post("/api/kitchen/orders/" + id + "/complete", kitchen, null);

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.status")).isEqualTo("READY");
        assertThat(jdbc.queryForObject("SELECT ready_by FROM orders WHERE id = ?", Long.class, id))
                .isEqualTo(kitchenId());
        Res queue = get("/api/kitchen/queue", kitchen);
        assertThat(ids(queue, "preparing")).isEmpty();
        assertThat(ids(queue, "dueSoon")).isEmpty();
        assertThat(ids(queue, "upcoming")).isEmpty();
        assertThat(held("13:00")).isEqualTo(5);
    }

    @Test
    @DisplayName("TC-KIT-15 (phần API) Khách thấy READY sau khi bếp hoàn tất")
    void kit15() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        long id = place(kh1, "13:00", line("Burger bò", 1));
        MockHttpSession kitchen = login("bep1@example.com");
        post("/api/kitchen/orders/" + id + "/start", kitchen, null);
        post("/api/kitchen/orders/" + id + "/complete", kitchen, null);

        assertThat((String) get("/api/orders/" + id, kh1).at("$.status")).isEqualTo("READY");
    }

    @Test
    @DisplayName("TC-KIT-16 Bếp thứ hai bấm Bắt đầu trên màn cũ: không đổi đơn, báo trạng thái hiện tại")
    void kit16() throws Exception {
        long id = place(login("khach1@example.com"), "13:00", line("Burger bò", 1));
        post("/api/kitchen/orders/" + id + "/start", login("bep1@example.com"), null);

        Res second = post("/api/kitchen/orders/" + id + "/start", login("bep1@example.com"), null);

        assertThat(second.status()).isEqualTo(409);
        assertThat((String) second.at("$.code")).isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat((String) second.at("$.currentStatus")).isEqualTo("PREPARING");
        assertThat(statusOf(id)).isEqualTo("PREPARING");
    }

    @Test
    @DisplayName("TC-KIT-17 Hai bếp bấm Bắt đầu cùng lúc: đúng một lần thành công")
    void kit17() throws Exception {
        long id = place(login("khach1@example.com"), "13:00", line("Burger bò", 1));
        MockHttpSession kitchenA = login("bep1@example.com");
        MockHttpSession kitchenB = login("bep1@example.com");

        List<Res> results = concurrently(List.of(
                () -> post("/api/kitchen/orders/" + id + "/start", kitchenA, null),
                () -> post("/api/kitchen/orders/" + id + "/start", kitchenB, null)));

        assertThat(results.stream().filter(r -> r.status() == 200)).hasSize(1);
        Res rejected =
                results.stream().filter(r -> r.status() != 200).findFirst().orElseThrow();
        assertThat(rejected.status()).isEqualTo(409);
        assertThat((String) rejected.at("$.code")).isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders WHERE preparing_by IS NOT NULL", Long.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("TC-KIT-18 Hoàn tất khi đơn còn CONFIRMED: bị từ chối, đơn không đổi")
    void kit18() throws Exception {
        long id = place(login("khach1@example.com"), "13:00", line("Burger bò", 1));

        Res result = post("/api/kitchen/orders/" + id + "/complete", login("bep1@example.com"), null);

        assertThat(result.status()).isEqualTo(409);
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("TC-KIT-19 Đơn READY: Bắt đầu và Hoàn tất đều bị từ chối, đơn giữ nguyên")
    void kit19() throws Exception {
        long id = place(login("khach1@example.com"), "13:00", line("Burger bò", 1));
        MockHttpSession kitchen = login("bep1@example.com");
        post("/api/kitchen/orders/" + id + "/start", kitchen, null);
        post("/api/kitchen/orders/" + id + "/complete", kitchen, null);

        assertThat(post("/api/kitchen/orders/" + id + "/start", kitchen, null).status())
                .isEqualTo(409);
        assertThat(post("/api/kitchen/orders/" + id + "/complete", kitchen, null)
                        .status())
                .isEqualTo(409);
        assertThat(statusOf(id)).isEqualTo("READY");
    }

    @Test
    @DisplayName("TC-KIT-20 (phần API) Không có API nào đưa đơn đang làm về Đã xác nhận")
    void kit20() throws Exception {
        long id = place(login("khach1@example.com"), "13:00", line("Burger bò", 1));
        MockHttpSession kitchen = login("bep1@example.com");
        post("/api/kitchen/orders/" + id + "/start", kitchen, null);

        for (String action : List.of("revert", "undo", "cancel", "reset", "confirm")) {
            Res result = post("/api/kitchen/orders/" + id + "/" + action, kitchen, null);
            assertThat(result.status()).as(action).isGreaterThanOrEqualTo(400);
        }
        assertThat(statusOf(id)).isEqualTo("PREPARING");
    }

    @Test
    @DisplayName("TC-KIT-21 KH1, QUẦY, QL gọi API bắt đầu và hoàn tất: bị từ chối, đơn không đổi")
    void kit21() throws Exception {
        long id = place(login("khach1@example.com"), "13:00", line("Burger bò", 1));

        for (String loginId : List.of("khach1@example.com", "quay1@example.com", "quanly@example.com")) {
            MockHttpSession session = login(loginId);
            assertThat(post("/api/kitchen/orders/" + id + "/start", session, null)
                            .status())
                    .as(loginId)
                    .isEqualTo(403);
            assertThat(post("/api/kitchen/orders/" + id + "/complete", session, null)
                            .status())
                    .as(loginId)
                    .isEqualTo(403);
        }
        assertThat(post("/api/kitchen/orders/" + id + "/start", null, null).status())
                .isEqualTo(401);
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("TC-KIT-22 Bắt đầu đơn không tồn tại: không tìm thấy, không lỗi hệ thống")
    void kit22() throws Exception {
        Res result = post("/api/kitchen/orders/987654/start", login("bep1@example.com"), null);

        assertThat(result.status()).isEqualTo(404);
        assertThat((String) result.at("$.code")).isEqualTo("ORDER_NOT_FOUND");
    }

    @Test
    @DisplayName("TC-KIT-23 Lỗi database khi cập nhật: báo lỗi, trạng thái đơn không đổi, thử lại được")
    void kit23() throws Exception {
        long id = place(login("khach1@example.com"), "13:00", line("Burger bò", 1));
        MockHttpSession kitchen = login("bep1@example.com");
        doThrow(new IllegalStateException("Lỗi database giả lập"))
                .when(orderRepository)
                .markPreparing(anyLong(), anyLong(), any());

        Res failed = post("/api/kitchen/orders/" + id + "/start", kitchen, null);

        assertThat(failed.status()).isEqualTo(500);
        assertThat((String) failed.at("$.code")).isEqualTo("INTERNAL_ERROR");
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
        Mockito.reset(orderRepository);
        assertThat(post("/api/kitchen/orders/" + id + "/start", kitchen, null).status())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("TC-KIT-25 Đơn ở nhóm Sắp tới (còn hơn 15 phút): bếp được bắt đầu sớm")
    void kit25() throws Exception {
        long id = placeEarly(login("khach1@example.com"), "13:00", "09:00", line("Burger bò", 1));
        at("11:00");
        MockHttpSession kitchen = login("bep1@example.com");
        assertThat(ids(get("/api/kitchen/queue", kitchen), "upcoming")).containsExactly((int) id);

        assertThat(post("/api/kitchen/orders/" + id + "/start", kitchen, null).status())
                .isEqualTo(200);
        assertThat(statusOf(id)).isEqualTo("PREPARING");
    }
}
