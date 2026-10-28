package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box Sprint 3: khách hủy đơn (UC-06, BR-23), cửa hàng hủy kèm lý do (UC-11, BR-24), đánh dấu không đến nhận
 * (UC-10, BR-25), qua API HTTP thật trên PostgreSQL thật. Giờ hiện tại mặc định 11:00; cửa sổ chuẩn bị 15 phút, thời
 * gian chờ trước khi được đánh dấu no-show 30 phút sau khi khung kết thúc.
 */
class Sprint3CancelNoShowBlackBoxTest extends BlackBoxBase {

    private long place(String hhmm, String... lines) throws Exception {
        Res placed = order(login("khach1@example.com"), hhmm, lines);
        assertThat(placed.status()).isEqualTo(201);
        return ((Number) placed.json("$.id")).longValue();
    }

    private long walkIn(String hhmm) throws Exception {
        Res created = post(
                "/api/counter/walk-in",
                login("quay1@example.com"),
                "{\"requestId\":\"" + newRequestId() + "\",\"slotStart\":\"" + slot(hhmm)
                        + "\",\"paymentMethod\":\"CASH\",\"lines\":[" + line("Burger bò", 1) + "]}");
        assertThat(created.status()).isEqualTo(201);
        return ((Number) created.json("$.id")).longValue();
    }

    private Res customerCancel(long id) throws Exception {
        return post("/api/orders/" + id + "/cancel", login("khach1@example.com"), null);
    }

    private Res storeCancel(long id, String loginId, String reason) throws Exception {
        String body = reason == null ? "{}" : "{\"reason\":\"" + reason + "\"}";
        return post("/api/counter/orders/" + id + "/cancel", login(loginId), body);
    }

    private Res noShow(long id, String loginId) throws Exception {
        return post("/api/counter/orders/" + id + "/no-show", login(loginId), null);
    }

    // ---------- UC-06 khách hủy ----------

    @Test
    @DisplayName(
            "TC-CAN-01 Khách hủy đơn CONFIRMED trước cửa sổ chuẩn bị: CANCELLED, hoàn đủ điểm, ghi người và lúc hủy")
    void can01() throws Exception {
        long id = place("12:00", line("Burger bò", 2));
        assertThat(held("12:00")).isEqualTo(10);

        Res result = customerCancel(id);

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.status")).isEqualTo("CANCELLED");
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        assertThat(held("12:00")).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT cancelled_by = customer_id AND cancelled_at IS NOT NULL FROM orders WHERE id = ?",
                        Boolean.class,
                        id))
                .isTrue();
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-CAN-02 Hủy ngay trước mốc (11:44 cho khung 12:00) được; đúng mốc 11:45 thì bị từ chối kèm hạn")
    void can02() throws Exception {
        long first = place("12:00", line("Burger bò", 1));
        long second = place("12:00", line("Burger bò", 1));

        at("11:44");
        assertThat(customerCancel(first).status()).isEqualTo(200);

        at("11:45");
        Res late = customerCancel(second);
        assertThat(late.status()).isEqualTo(409);
        assertThat((String) late.at("$.code")).isEqualTo("CANCEL_NOT_ALLOWED");
        assertThat((String) late.at("$.cancelDeadline")).isEqualTo(slot("11:45"));
        assertThat(statusOf(second)).isEqualTo("CONFIRMED");
        assertThat(held("12:00")).isEqualTo(5);
    }

    @Test
    @DisplayName("TC-CAN-03 Đơn đã PREPARING hoặc đã hủy: khách không hủy được, quota không đổi")
    void can03() throws Exception {
        long preparing = place("12:00", line("Burger bò", 1));
        setStatus(preparing, "PREPARING");

        Res result = customerCancel(preparing);

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat(held("12:00")).isEqualTo(5);

        long other = place("12:15", line("Burger bò", 1));
        assertThat(customerCancel(other).status()).isEqualTo(200);
        Res again = customerCancel(other);
        assertThat(again.status()).isEqualTo(409);
        assertThat(held("12:15")).isZero();
    }

    @Test
    @DisplayName("TC-CAN-04 Khách không hủy được đơn của người khác: 404, đơn giữ nguyên")
    void can04() throws Exception {
        long id = place("12:00", line("Burger bò", 1));
        MockHttpSession other = login(createCustomers(1).getFirst());

        Res result = post("/api/orders/" + id + "/cancel", other, null);

        assertThat(result.status()).isEqualTo(404);
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("TC-CAN-05 Sau khi hủy, khách đặt lại được khung vừa đầy và giới hạn đơn mở được giải phóng")
    void can05() throws Exception {
        setQuotaOverride("12:00", 5);
        long id = place("12:00", line("Burger bò", 1));
        assertThat(order(login("khach1@example.com"), "12:00", line("Burger bò", 1))
                        .status())
                .isEqualTo(409);

        assertThat(customerCancel(id).status()).isEqualTo(200);

        assertThat(order(login("khach1@example.com"), "12:00", line("Burger bò", 1))
                        .status())
                .isEqualTo(201);
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-CAN-06 Hai yêu cầu hủy cùng lúc: chỉ một thành công, điểm chỉ hoàn một lần")
    void can06() throws Exception {
        long id = place("12:00", line("Burger bò", 1));
        MockHttpSession customer = login("khach1@example.com");
        List<Callable<Res>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> post("/api/orders/" + id + "/cancel", customer, null));
        }

        List<Res> results = concurrently(tasks);

        assertThat(results.stream().filter(r -> r.status() == 200)).hasSize(1);
        assertThat(held("12:00")).isZero();
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-CAN-07 Bếp không bắt đầu làm được đơn đã hủy; đơn không còn trong hàng đợi bếp")
    void can07() throws Exception {
        long id = place("11:30", line("Burger bò", 1));
        assertThat(customerCancel(id).status()).isEqualTo(200);
        MockHttpSession kitchen = login("bep1@example.com");

        assertThat(get("/api/kitchen/queue", kitchen).body()).doesNotContain("\"id\":" + id + ",");
        assertThat(post("/api/kitchen/orders/" + id + "/start", kitchen, null).status())
                .isEqualTo(409);
    }

    // ---------- UC-11 cửa hàng hủy ----------

    @Test
    @DisplayName("TC-STC-01 Quản lý hủy đơn CONFIRMED có lý do: hoàn điểm; khách thấy lý do ở chi tiết đơn")
    void stc01() throws Exception {
        long id = place("12:00", line("Burger bò", 1));

        Res result = storeCancel(id, "quanly@example.com", "Hết nguyên liệu");

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.status")).isEqualTo("CANCELLED");
        assertThat(held("12:00")).isZero();
        Res detail = get("/api/orders/" + id, login("khach1@example.com"));
        assertThat((String) detail.at("$.status")).isEqualTo("CANCELLED");
        assertThat((String) detail.at("$.cancelReason")).isEqualTo("Hết nguyên liệu");
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-STC-02 Quản lý hủy đơn PREPARING: CANCELLED, không hoàn điểm (BR-24)")
    void stc02() throws Exception {
        long id = place("12:00", line("Burger bò", 1));
        setStatus(id, "PREPARING");

        Res result = storeCancel(id, "quanly@example.com", "Khách gọi báo không lấy");

        assertThat(result.status()).isEqualTo(200);
        assertThat(held("12:00")).isEqualTo(5);
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-STC-03 Thiếu lý do hoặc lý do toàn khoảng trắng: 400, đơn giữ nguyên")
    void stc03() throws Exception {
        long id = place("12:00", line("Burger bò", 1));

        assertThat(storeCancel(id, "quanly@example.com", null).status()).isEqualTo(400);
        assertThat(storeCancel(id, "quanly@example.com", "   ").status()).isEqualTo(400);
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("TC-STC-04 Quầy hủy được đơn walk-in CONFIRMED (hoàn điểm), không hủy được đơn pre-order")
    void stc04() throws Exception {
        long walkIn = walkIn("11:15");
        long preOrder = place("12:00", line("Burger bò", 1));

        assertThat(storeCancel(walkIn, "quay1@example.com", "Khách đổi ý").status())
                .isEqualTo(200);
        assertThat(held("11:15")).isZero();

        Res denied = storeCancel(preOrder, "quay1@example.com", "Thử");
        assertThat(denied.status()).isEqualTo(409);
        assertThat((String) denied.at("$.code")).isEqualTo("CANCEL_NOT_ALLOWED");
        assertThat(statusOf(preOrder)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("TC-STC-05 Quầy không hủy được walk-in đã PREPARING; không ai hủy được đơn READY")
    void stc05() throws Exception {
        long walkIn = walkIn("11:15");
        setStatus(walkIn, "PREPARING");
        long ready = place("12:00", line("Burger bò", 1));
        setStatus(ready, "READY");

        assertThat(storeCancel(walkIn, "quay1@example.com", "Thử").status()).isEqualTo(409);
        assertThat(storeCancel(ready, "quanly@example.com", "Thử").status()).isEqualTo(409);
        assertThat(statusOf(walkIn)).isEqualTo("PREPARING");
        assertThat(statusOf(ready)).isEqualTo("READY");
    }

    @Test
    @DisplayName("TC-STC-06 Bếp và khách không gọi được API hủy của cửa hàng (403)")
    void stc06() throws Exception {
        long id = place("12:00", line("Burger bò", 1));

        assertThat(storeCancel(id, "bep1@example.com", "x").status()).isEqualTo(403);
        assertThat(storeCancel(id, "khach1@example.com", "x").status()).isEqualTo(403);
    }

    // ---------- UC-10 no-show ----------

    @Test
    @DisplayName("TC-NOS-01 Đơn READY quá hạn (12:00–12:15 + 30 phút → từ 12:45): quầy đánh dấu NO_SHOW, không hoàn")
    void nos01() throws Exception {
        long id = place("12:00", line("Burger bò", 1));
        setStatus(id, "READY");
        at("12:45");

        Res result = noShow(id, "quay1@example.com");

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.status")).isEqualTo("NO_SHOW");
        assertThat(held("12:00")).isEqualTo(5);
        assertThat(jdbc.queryForObject(
                        "SELECT no_show_by IS NOT NULL AND no_show_at IS NOT NULL FROM orders WHERE id = ?",
                        Boolean.class,
                        id))
                .isTrue();
        assertThat((String)
                        get("/api/orders/" + id, login("khach1@example.com")).at("$.status"))
                .isEqualTo("NO_SHOW");
    }

    @Test
    @DisplayName("TC-NOS-02 Trước hạn (12:44): bị từ chối kèm mốc được phép; danh sách quầy chưa gắn cờ quá hạn")
    void nos02() throws Exception {
        long id = place("12:00", line("Burger bò", 1));
        setStatus(id, "READY");
        at("12:44");
        MockHttpSession counter = login("quay1@example.com");

        Res result = noShow(id, "quay1@example.com");

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("NO_SHOW_TOO_EARLY");
        assertThat((String) result.at("$.allowedFrom")).isEqualTo(slot("12:45"));
        assertThat(get("/api/counter/orders", counter).<List<Boolean>>at("$.orders[?(@.id==" + id + ")].overdue"))
                .containsExactly(false);

        at("12:45");
        assertThat(get("/api/counter/orders", counter).<List<Boolean>>at("$.orders[?(@.id==" + id + ")].overdue"))
                .containsExactly(true);
    }

    @Test
    @DisplayName("TC-NOS-03 Đơn chưa READY không đánh dấu no-show được; quản lý cũng đánh dấu được")
    void nos03() throws Exception {
        long confirmed = place("12:00", line("Burger bò", 1));
        long ready = place("12:15", line("Burger bò", 1));
        setStatus(ready, "READY");
        at("13:30");

        assertThat(noShow(confirmed, "quay1@example.com").status()).isEqualTo(409);
        assertThat(noShow(ready, "quanly@example.com").status()).isEqualTo(200);
        assertThat(statusOf(confirmed)).isEqualTo("CONFIRMED");
    }
}
