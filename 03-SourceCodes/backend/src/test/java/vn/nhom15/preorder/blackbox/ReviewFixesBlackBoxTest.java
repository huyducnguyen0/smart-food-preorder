package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box các lỗi tìm thấy ở buổi review ngày 2026-10-31 và cách sửa người dùng đã duyệt: đơn tồn từ ngày trước,
 * khách tự hủy đơn đã trả tiền, tài khoản bị khóa còn phiên đang mở, đổi giờ cùng lúc đặt đơn, bếp bắt đầu quá sớm.
 * Giờ hiện tại mặc định 11:00 ngày kiểm thử.
 */
class ReviewFixesBlackBoxTest extends BlackBoxBase {

    private long place(MockHttpSession session, String hhmm, String... lines) throws Exception {
        Res placed = order(session, hhmm, lines);
        assertThat(placed.status()).as(placed.body()).isEqualTo(201);
        return ((Number) placed.json("$.id")).longValue();
    }

    private void nextDayAt(String hhmm) {
        clock.set(BlackBoxClockConfig.TODAY.plusDays(1).atTime(java.time.LocalTime.parse(hhmm)));
    }

    private static String nextDaySlot(String hhmm) {
        return BlackBoxClockConfig.TODAY.plusDays(1) + "T" + hhmm;
    }

    // ---------- ① Đơn tồn từ ngày trước ----------

    @Test
    @DisplayName("TC-FIX-01 3 đơn chưa kết thúc của hôm qua không chặn khách đặt đơn hôm nay (BR-37 sửa đổi)")
    void fix01() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        for (int i = 0; i < 3; i++) {
            place(customer, "12:00", line("Khoai tây chiên", 1));
        }
        assertThat(order(customer, "12:15", line("Khoai tây chiên", 1)).status())
                .isEqualTo(409);

        nextDayAt("11:00");
        Res today = post(
                "/api/orders",
                login("khach1@example.com"),
                orderJson(newRequestId(), nextDaySlot("12:00"), line("Khoai tây chiên", 1)));

        assertThat(today.status()).isEqualTo(201);
    }

    @Test
    @DisplayName(
            "TC-FIX-02 Màn quầy hiện đơn tồn (cờ stale) để dọn: quản lý hủy đơn xác nhận, quầy đánh dấu đơn sẵn sàng")
    void fix02() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        long confirmed = place(customer, "12:00", line("Burger bò", 1));
        long ready = place(customer, "12:15", line("Burger bò", 1));
        setStatus(ready, "READY");
        nextDayAt("11:00");
        MockHttpSession counter = login("quay1@example.com");

        Res list = get("/api/counter/orders", counter);
        assertThat(list.<List<Boolean>>at("$.orders[?(@.id==" + confirmed + ")].stale"))
                .containsExactly(true);
        assertThat(list.<List<Boolean>>at("$.orders[?(@.id==" + ready + ")].overdue"))
                .containsExactly(true);
        assertThat((Integer)
                        get("/api/admin/dashboard", login("quanly@example.com")).at("$.stats.staleOpen"))
                .isEqualTo(2);

        assertThat(post("/api/counter/orders/" + ready + "/no-show", counter, null)
                        .status())
                .isEqualTo(200);
        assertThat(post(
                                "/api/counter/orders/" + confirmed + "/cancel",
                                login("quanly@example.com"),
                                "{\"reason\":\"Đơn tồn từ hôm qua\"}")
                        .status())
                .isEqualTo(200);
        assertThat(get("/api/counter/orders", counter).<List<Integer>>at("$.orders[*].id"))
                .isEmpty();
        assertThat((Integer)
                        get("/api/admin/dashboard", login("quanly@example.com")).at("$.stats.staleOpen"))
                .isZero();
    }

    @Test
    @DisplayName("TC-FIX-03 Đơn hôm nay không bị gắn cờ tồn")
    void fix03() throws Exception {
        long id = place(login("khach1@example.com"), "12:00", line("Burger bò", 1));

        assertThat(get("/api/counter/orders", login("quay1@example.com"))
                        .<List<Boolean>>at("$.orders[?(@.id==" + id + ")].stale"))
                .containsExactly(false);
    }

    // ---------- ② Khách tự hủy đơn đã trả tiền ----------

    @Test
    @DisplayName("TC-FIX-04 Đơn đã thanh toán: khách không tự hủy được (reason PAID); cửa hàng hủy kèm lý do được")
    void fix04() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        long id = place(customer, "12:00", line("Burger bò", 1));
        assertThat(post("/api/counter/orders/" + id + "/payment", login("quay1@example.com"), "{\"method\":\"CASH\"}")
                        .status())
                .isEqualTo(200);

        Res denied = post("/api/orders/" + id + "/cancel", customer, null);

        assertThat(denied.status()).isEqualTo(409);
        assertThat((String) denied.at("$.code")).isEqualTo("CANCEL_NOT_ALLOWED");
        assertThat((String) denied.at("$.reason")).isEqualTo("PAID");
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
        assertThat(held("12:00")).isEqualTo(5);

        Res byStore = post(
                "/api/counter/orders/" + id + "/cancel",
                login("quanly@example.com"),
                "{\"reason\":\"Khách gọi hủy, đã hoàn tiền mặt\"}");
        assertThat(byStore.status()).isEqualTo(200);
        assertThat(held("12:00")).isZero();
    }

    // ---------- ③ Tài khoản bị khóa còn phiên đang mở ----------

    @Test
    @DisplayName("TC-FIX-05 Khóa tài khoản bếp đang mở màn hình: yêu cầu tiếp theo bị từ chối 401, phiên bị hủy")
    void fix05() throws Exception {
        MockHttpSession kitchen = login("bep1@example.com");
        assertThat(get("/api/kitchen/queue", kitchen).status()).isEqualTo(200);
        long id = kitchenId();

        assertThat(post("/api/admin/staff/" + id + "/lock", login("quanly@example.com"), null)
                        .status())
                .isEqualTo(200);

        Res blocked = get("/api/kitchen/queue", kitchen);
        assertThat(blocked.status()).isEqualTo(401);
        assertThat((String) blocked.at("$.code")).isEqualTo("UNAUTHENTICATED");
        assertThat(kitchen.isInvalid()).isTrue();
        // Người khác không bị ảnh hưởng.
        assertThat(get("/api/counter/orders", login("quay1@example.com")).status())
                .isEqualTo(200);

        assertThat(post("/api/admin/staff/" + id + "/unlock", login("quanly@example.com"), null)
                        .status())
                .isEqualTo(200);
        assertThat(get("/api/kitchen/queue", login("bep1@example.com")).status())
                .isEqualTo(200);
    }

    // ---------- ④ Đổi giờ đóng cửa cùng lúc đặt đơn ----------

    @Test
    @DisplayName(
            "TC-FIX-06 Rút giờ đóng cửa cùng lúc khách đặt khung cuối: không bao giờ có đơn nằm ngoài giờ mới (BR-11)")
    void fix06() throws Exception {
        for (int round = 0; round < 15; round++) {
            jdbc.execute("TRUNCATE order_lines, orders, time_slots RESTART IDENTITY CASCADE");
            jdbc.update("UPDATE store_settings SET close_time = '21:00'");
            MockHttpSession customer = login("khach1@example.com");
            MockHttpSession manager = login("quanly@example.com");
            List<Callable<Res>> tasks = new ArrayList<>();
            tasks.add(() -> order(customer, "20:45", line("Burger bò", 1)));
            tasks.add(() -> send(
                    "PUT",
                    "/api/admin/settings",
                    manager,
                    "{\"openTime\":\"10:00\",\"closeTime\":\"20:45\",\"defaultQuotaPoints\":20}"));

            List<Res> results = concurrently(tasks);

            boolean placed = results.get(0).status() == 201;
            boolean narrowed = results.get(1).status() == 200;
            assertThat(placed && narrowed)
                    .as(
                            "vòng %d: đặt đơn %d, đổi giờ %d",
                            round, results.get(0).status(), results.get(1).status())
                    .isFalse();
            assertThat(placed || narrowed)
                    .as("vòng %d: ít nhất một thao tác thành công", round)
                    .isTrue();
        }
    }

    // ---------- ⑤ Bếp bắt đầu quá sớm ----------

    @Test
    @DisplayName(
            "TC-FIX-07 Bếp chỉ bắt đầu từ thời gian chuẩn bị (khung 12:00 → từ 11:45); trước đó khách vẫn tự hủy được")
    void fix07() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        long first = place(customer, "12:00", line("Burger bò", 1));
        long second = place(customer, "12:00", line("Burger bò", 1));
        MockHttpSession kitchen = login("bep1@example.com");

        Res early = post("/api/kitchen/orders/" + first + "/start", kitchen, null);
        assertThat(early.status()).isEqualTo(409);
        assertThat((String) early.at("$.code")).isEqualTo("PREPARATION_TOO_EARLY");
        assertThat((String) early.at("$.allowedFrom")).isEqualTo(slot("11:45"));
        assertThat(statusOf(first)).isEqualTo("CONFIRMED");
        assertThat(post("/api/orders/" + second + "/cancel", customer, null).status())
                .isEqualTo(200);

        at("11:44");
        assertThat(post("/api/kitchen/orders/" + first + "/start", kitchen, null)
                        .status())
                .isEqualTo(409);
        at("11:45");
        assertThat(post("/api/kitchen/orders/" + first + "/start", kitchen, null)
                        .status())
                .isEqualTo(200);
        assertThat(statusOf(first)).isEqualTo("PREPARING");
    }
}
