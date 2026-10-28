package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box Sprint 2: UC-07 Đơn walk-in (PB-11), UC-18 Đề xuất khung thay thế (PB-09) và NFR-01 khi hai kênh cùng đặt,
 * qua API HTTP thật trên PostgreSQL thật. Giờ hiện tại mặc định 11:00.
 */
class Sprint2WalkInBlackBoxTest extends BlackBoxBase {

    private Res estimate(MockHttpSession session, String... lines) throws Exception {
        return post("/api/counter/walk-in/estimate", session, "{\"lines\":[" + String.join(",", lines) + "]}");
    }

    private static String walkInJson(String requestId, String slotStart, String method, String... lines) {
        return "{\"requestId\":\"" + requestId + "\",\"slotStart\":\"" + slotStart + "\",\"paymentMethod\":\"" + method
                + "\",\"lines\":[" + String.join(",", lines) + "]}";
    }

    private Res walkIn(MockHttpSession session, String hhmm, String... lines) throws Exception {
        return post("/api/counter/walk-in", session, walkInJson(newRequestId(), slot(hhmm), "CASH", lines));
    }

    private void fillAllSlotsFrom(String fromHhmm, int held) {
        for (LocalTime t = LocalTime.parse(fromHhmm); !t.isAfter(LocalTime.of(20, 45)); t = t.plusMinutes(15)) {
            setHeld(t.toString(), held);
        }
    }

    // ---------- UC-07 Walk-in ----------

    @Test
    @DisplayName("TC-WALK-01 Khung dự kiến là khung hiện tại (không áp mốc 30 phút), kèm số tiền cần thu (BR-18)")
    void walk01() throws Exception {
        at("11:05");

        Res result = estimate(login("quay1@example.com"), line("Burger bò", 2), line("Trà đào", 1));

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.slot.start")).isEqualTo(slot("11:00"));
        assertThat((String) result.at("$.slot.end")).isEqualTo(slot("11:15"));
        assertThat((Integer) result.at("$.totalAmount")).isEqualTo(110000);
        assertThat(result.body()).doesNotContainIgnoringCase("held").doesNotContainIgnoringCase("quota");
        assertThat(held("11:00")).as("ước tính chưa giữ quota (BR-19)").isZero();
    }

    @Test
    @DisplayName("TC-WALK-02 Khung hiện tại không đủ chỗ thì lấy khung sớm nhất còn đủ")
    void walk02() throws Exception {
        setHeld("11:00", 18);
        setHeld("11:15", 16);

        Res result = estimate(login("quay1@example.com"), line("Burger bò", 1));

        assertThat((String) result.at("$.slot.start")).isEqualTo(slot("11:30"));
    }

    @Test
    @DisplayName("TC-WALK-03 Không còn khung đủ chỗ, sau giờ đóng cửa, hoặc đơn quá lớn: báo lý do")
    void walk03() throws Exception {
        MockHttpSession counter = login("quay1@example.com");
        fillAllSlotsFrom("11:00", 20);
        Res none = estimate(counter, line("Burger bò", 1));
        assertThat(none.status()).isEqualTo(409);
        assertThat((String) none.at("$.code")).isEqualTo("NO_SLOT_AVAILABLE");

        Res tooLarge = estimate(counter, line("Burger phô mai đôi", 3));
        assertThat((String) tooLarge.at("$.code")).isEqualTo("ORDER_TOO_LARGE");

        at("21:00");
        assertThat((String) estimate(counter, line("Trà đào", 1)).at("$.code")).isEqualTo("NO_SLOT_AVAILABLE");
    }

    @Test
    @DisplayName("TC-WALK-04 Xác nhận: đơn walk-in đã thanh toán, không có khách, ghi nhân viên tạo, giữ quota")
    void walk04() throws Exception {
        MockHttpSession counter = login("quay1@example.com");

        Res result = post(
                "/api/counter/walk-in",
                counter,
                walkInJson(newRequestId(), slot("11:00"), "BANK_TRANSFER", line("Burger bò", 2)));

        assertThat(result.status()).isEqualTo(201);
        long id = ((Number) result.json("$.id")).longValue();
        assertThat((String) result.at("$.status")).isEqualTo("CONFIRMED");
        assertThat((String) result.at("$.paymentStatus")).isEqualTo("PAID");
        assertThat((String) result.at("$.paymentMethod")).isEqualTo("BANK_TRANSFER");
        assertThat((String) result.at("$.pickupCode")).hasSize(6);
        assertThat(jdbc.queryForObject("SELECT source FROM orders WHERE id = ?", String.class, id))
                .isEqualTo("WALK_IN");
        assertThat(jdbc.queryForObject("SELECT customer_id FROM orders WHERE id = ?", Long.class, id))
                .isNull();
        long counterId =
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'quay1@example.com'", Long.class);
        assertThat(jdbc.queryForObject("SELECT created_by FROM orders WHERE id = ?", Long.class, id))
                .isEqualTo(counterId);
        assertThat(jdbc.queryForObject("SELECT paid_by FROM orders WHERE id = ?", Long.class, id))
                .isEqualTo(counterId);
        assertThat(held("11:00")).isEqualTo(10);
        // Đơn walk-in đi chung luồng: bếp thấy, quầy thấy, không xuất hiện ở "Đơn của tôi" của khách nào.
        assertThat(get("/api/kitchen/queue", login("bep1@example.com")).<List<String>>at("$..source"))
                .contains("WALK_IN");
        assertThat(get("/api/counter/orders", counter).<List<Object>>at("$.orders[?(@.id == " + id + ")].customerName"))
                .containsExactly((Object) null);
        assertThat(get("/api/orders", login("khach1@example.com")).<List<Object>>at("$.orders"))
                .isEmpty();
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-WALK-05 Gửi lại cùng mã yêu cầu (liên tiếp và đồng thời): đúng một đơn, giữ quota một lần")
    void walk05() throws Exception {
        MockHttpSession counter = login("quay1@example.com");
        String body = walkInJson(newRequestId(), slot("11:00"), "CASH", line("Burger bò", 1));
        Res first = post("/api/counter/walk-in", counter, body);
        List<Callable<Res>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> post("/api/counter/walk-in", counter, body));
        }

        List<Res> again = concurrently(tasks);

        assertThat(first.status()).isEqualTo(201);
        assertThat(again).allSatisfy(r -> {
            assertThat(r.status()).isEqualTo(200);
            assertThat(r.<Integer>at("$.id")).isEqualTo(first.<Integer>at("$.id"));
        });
        assertThat(orderCount()).isEqualTo(1);
        assertThat(held("11:00")).isEqualTo(5);
    }

    @Test
    @DisplayName("TC-WALK-06 Khung đã báo khách vừa hết chỗ: không tạo đơn, trả khung mới để báo lại khách (BR-19)")
    void walk06() throws Exception {
        MockHttpSession counter = login("quay1@example.com");
        assertThat((String) estimate(counter, line("Burger bò", 2)).at("$.slot.start"))
                .isEqualTo(slot("11:00"));
        setHeld("11:00", 15);

        Res result = walkIn(counter, "11:00", line("Burger bò", 2));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("SLOT_FULL");
        assertThat((String) result.at("$.nextSlot.start")).isEqualTo(slot("11:15"));
        assertThat(orderCount()).isZero();
        assertThat(held("11:00")).isEqualTo(15);
    }

    @Test
    @DisplayName("TC-WALK-07 Khung đã qua, ngày khác, sai mốc, ngoài giờ: không xác nhận được")
    void walk07() throws Exception {
        at("11:20");
        MockHttpSession counter = login("quay1@example.com");

        for (String start : List.of(
                slot("11:00"), BlackBoxClockConfig.TODAY.plusDays(1) + "T12:00", slot("11:20"), slot("21:00"))) {
            Res result = post(
                    "/api/counter/walk-in", counter, walkInJson(newRequestId(), start, "CASH", line("Burger bò", 1)));
            assertThat(result.status()).as(start).isEqualTo(409);
            assertThat((String) result.at("$.code")).isEqualTo("SLOT_NOT_BOOKABLE");
        }
        assertThat(walkIn(counter, "11:15", line("Burger bò", 1)).status())
                .as("khung hiện tại 11:15 vẫn nhận dù đã bắt đầu")
                .isEqualTo(201);
    }

    @Test
    @DisplayName("TC-WALK-08 Món đổi giá hoặc hết hàng; thiếu phương thức thanh toán; không có món: bị từ chối")
    void walk08() throws Exception {
        MockHttpSession counter = login("quay1@example.com");
        Item burger = item("Burger bò");

        Res changed = walkIn(counter, "11:00", line(burger.id(), 1, 1000));
        assertThat((String) changed.at("$.code")).isEqualTo("ITEMS_CHANGED");
        assertThat(post(
                                "/api/counter/walk-in",
                                counter,
                                "{\"requestId\":\"" + newRequestId() + "\",\"slotStart\":\"" + slot("11:00")
                                        + "\",\"lines\":[" + line("Burger bò", 1) + "]}")
                        .status())
                .isEqualTo(400);
        assertThat(post("/api/counter/walk-in", counter, walkInJson(newRequestId(), slot("11:00"), "CASH"))
                        .status())
                .isEqualTo(400);
        assertThat(orderCount()).isZero();
    }

    @Test
    @DisplayName("TC-WALK-09 Chỉ quầy và quản lý tạo walk-in; khách và bếp bị từ chối")
    void walk09() throws Exception {
        String body = walkInJson(newRequestId(), slot("11:00"), "CASH", line("Burger bò", 1));

        assertThat(post("/api/counter/walk-in", login("khach1@example.com"), body)
                        .status())
                .isEqualTo(403);
        assertThat(post("/api/counter/walk-in", login("bep1@example.com"), body).status())
                .isEqualTo(403);
        assertThat(post("/api/counter/walk-in", null, body).status()).isEqualTo(401);
        assertThat(orderCount()).isZero();
        assertThat(post("/api/counter/walk-in", login("quanly@example.com"), body)
                        .status())
                .isEqualTo(201);
    }

    @Test
    @DisplayName("TC-WALK-10 Vòng đời walk-in: tạo (đã trả), bếp làm, quầy bàn giao bằng mã")
    void walk10() throws Exception {
        MockHttpSession counter = login("quay1@example.com");
        Res created = walkIn(counter, "11:00", line("Khoai tây chiên", 1));
        long id = ((Number) created.json("$.id")).longValue();
        MockHttpSession kitchen = login("bep1@example.com");

        post("/api/kitchen/orders/" + id + "/start", kitchen, null);
        post("/api/kitchen/orders/" + id + "/complete", kitchen, null);
        Res handed = post(
                "/api/counter/orders/" + id + "/handover",
                counter,
                "{\"pickupCode\":\"" + created.<String>at("$.pickupCode") + "\"}");

        assertThat(handed.status()).isEqualTo(200);
        assertThat(statusOf(id)).isEqualTo("PICKED_UP");
    }

    // ---------- NFR-01: hai kênh cùng đặt ----------

    @Test
    @DisplayName("TC-CON-08 25 pre-order và 25 walk-in 3 điểm cùng lúc vào một khung quota 20: đúng 6 đơn, giữ 18")
    void con08() throws Exception {
        jdbc.update("UPDATE menu_items SET workload_points = 3 WHERE name = 'Khoai tây chiên'");
        List<MockHttpSession> customers = sessionsFor(createCustomers(25));
        MockHttpSession counter = login("quay1@example.com");
        MockHttpSession manager = login("quanly@example.com");
        List<Callable<Res>> tasks = new ArrayList<>();
        for (MockHttpSession customer : customers) {
            tasks.add(() -> order(customer, "12:00", line("Khoai tây chiên", 1)));
        }
        for (int i = 0; i < 25; i++) {
            MockHttpSession staff = i % 2 == 0 ? counter : manager;
            tasks.add(() -> walkIn(staff, "12:00", line("Khoai tây chiên", 1)));
        }

        List<Res> results = concurrently(tasks);

        assertThat(results.stream().filter(r -> r.status() == 201)).hasSize(6);
        assertThat(results.stream().filter(r -> r.status() != 201)).hasSize(44).allSatisfy(r -> {
            assertThat(r.status()).isEqualTo(409);
            assertThat((String) r.at("$.code")).isEqualTo("SLOT_FULL");
        });
        assertThat(held("12:00")).isEqualTo(18);
        assertThat(orderCount()).isEqualTo(6);
        assertHeldMatchesOrders();
    }

    // ---------- UC-18 Đề xuất khung thay thế ----------

    @Test
    @DisplayName("TC-ALT-01 Khung chọn hết chỗ: đề xuất 3 khung gần nhất, cùng khoảng cách thì khung sau trước (BR-16)")
    void alt01() throws Exception {
        setHeld("12:00", 20);

        Res result = order(login("khach1@example.com"), "12:00", line("Burger bò", 1));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("SLOT_FULL");
        assertThat(result.<List<String>>at("$.alternatives[*].start"))
                .containsExactly(slot("12:15"), slot("11:45"), slot("12:30"));
    }

    @Test
    @DisplayName("TC-ALT-02 Khung lân cận cũng đầy: bỏ qua, lấy khung kế tiếp")
    void alt02() throws Exception {
        setHeld("12:00", 20);
        setHeld("12:15", 18);
        setHeld("11:45", 16);

        Res result = order(login("khach1@example.com"), "12:00", line("Burger bò", 1));

        assertThat(result.<List<String>>at("$.alternatives[*].start"))
                .containsExactly(slot("12:30"), slot("11:30"), slot("12:45"));
    }

    @Test
    @DisplayName("TC-ALT-03 Chỉ đề xuất khung còn đặt trước được (BR-15) và tối đa 3")
    void alt03() throws Exception {
        at("11:00");
        setHeld("11:30", 20);

        Res result = order(login("khach1@example.com"), "11:30", line("Burger bò", 1));

        assertThat(result.<List<String>>at("$.alternatives[*].start"))
                .containsExactly(slot("11:45"), slot("12:00"), slot("12:15"))
                .doesNotContain(slot("11:15"));
    }

    @Test
    @DisplayName("TC-ALT-04 Không còn khung nào đủ chỗ: danh sách đề xuất rỗng")
    void alt04() throws Exception {
        fillAllSlotsFrom("11:30", 20);

        Res result = order(login("khach1@example.com"), "12:00", line("Burger bò", 1));

        assertThat(result.status()).isEqualTo(409);
        assertThat(result.<List<Object>>at("$.alternatives")).isEmpty();
    }

    @Test
    @DisplayName("TC-ALT-05 Chọn khung được đề xuất: đặt được, quota kiểm tra lại qua PB-05")
    void alt05() throws Exception {
        setHeld("12:00", 20);
        MockHttpSession customer = login("khach1@example.com");
        String suggested = order(customer, "12:00", line("Burger bò", 1))
                .<List<String>>at("$.alternatives[*].start")
                .getFirst();

        Res result = post("/api/orders", customer, orderJson(newRequestId(), suggested, line("Burger bò", 1)));

        assertThat(result.status()).isEqualTo(201);
        assertThat(held(suggested.substring(11))).isEqualTo(5);
    }
}
