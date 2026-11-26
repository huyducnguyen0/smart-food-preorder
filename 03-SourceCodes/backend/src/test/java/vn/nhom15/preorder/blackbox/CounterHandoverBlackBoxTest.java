package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box UC-09 Bàn giao đơn (PB-15, T-30): chỉ bàn giao đơn READY, đúng mã nhận và đã thanh toán (BR-29), qua API
 * HTTP thật trên PostgreSQL thật. ID ca TC-HAND-xx theo tiêu chí chấp nhận của PB-15.
 */
class CounterHandoverBlackBoxTest extends BlackBoxBase {

    private record Placed(long id, String code) {}

    private Placed place(String hhmm) throws Exception {
        Res placed = order(login("khach1@example.com"), hhmm, line("Burger bò", 1));
        assertThat(placed.status()).isEqualTo(201);
        return new Placed(((Number) placed.json("$.id")).longValue(), placed.at("$.pickupCode"));
    }

    private Placed placeReadyAndPaid() throws Exception {
        Placed order = place("12:00");
        setStatus(order.id(), "READY");
        jdbc.update(
                "UPDATE orders SET payment_status = 'PAID', payment_method = 'CASH', paid_at = now(), paid_by = ?"
                        + " WHERE id = ?",
                counterId(),
                order.id());
        return order;
    }

    private long counterId() {
        return jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'quay1@example.com'", Long.class);
    }

    private Res handOver(long id, String code, MockHttpSession session) throws Exception {
        String body = code == null ? "{}" : "{\"pickupCode\":\"" + code + "\"}";
        return post("/api/counter/orders/" + id + "/handover", session, body);
    }

    @Test
    @DisplayName("TC-HAND-01 Đơn READY, đã thanh toán, đúng mã: bàn giao được, ghi người và thời điểm")
    void hand01() throws Exception {
        Placed order = placeReadyAndPaid();
        MockHttpSession counter = login("quay1@example.com");

        Res result = handOver(order.id(), order.code(), counter);

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.status")).isEqualTo("PICKED_UP");
        assertThat(statusOf(order.id())).isEqualTo("PICKED_UP");
        assertThat(jdbc.queryForObject("SELECT picked_up_by FROM orders WHERE id = ?", Long.class, order.id()))
                .isEqualTo(counterId());
        assertThat(jdbc.queryForObject(
                        "SELECT picked_up_at IS NOT NULL FROM orders WHERE id = ?", Boolean.class, order.id()))
                .isTrue();
        // Đơn rời danh sách của quầy; khách thấy "đã nhận"; quota vẫn giữ (BR-14).
        assertThat(get("/api/counter/orders", counter).<List<Integer>>at("$.orders[*].id"))
                .isEmpty();
        assertThat((String) get("/api/orders/" + order.id(), login("khach1@example.com"))
                        .at("$.status"))
                .isEqualTo("PICKED_UP");
        assertThat(held("12:00")).isEqualTo(5);
    }

    @Test
    @DisplayName("TC-HAND-02 Mã nhập chữ thường hoặc có khoảng trắng hai đầu vẫn khớp")
    void hand02() throws Exception {
        Placed order = placeReadyAndPaid();

        Res result = handOver(order.id(), "  " + order.code().toLowerCase() + " ", login("quay1@example.com"));

        assertThat(result.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("TC-HAND-03 Sai mã nhận: bị từ chối kèm lý do, đơn vẫn READY")
    void hand03() throws Exception {
        Placed order = placeReadyAndPaid();
        String wrong = order.code().equals("AAAAAA") ? "BBBBBB" : "AAAAAA";

        Res result = handOver(order.id(), wrong, login("quay1@example.com"));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("PICKUP_CODE_MISMATCH");
        assertThat(result.body()).as("không lộ mã đúng").doesNotContain(order.code());
        assertThat(statusOf(order.id())).isEqualTo("READY");
    }

    @Test
    @DisplayName("TC-HAND-04 Đơn chưa thanh toán: bị từ chối kèm lý do, đơn vẫn READY")
    void hand04() throws Exception {
        Placed order = place("12:00");
        setStatus(order.id(), "READY");

        Res result = handOver(order.id(), order.code(), login("quay1@example.com"));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("ORDER_NOT_PAID");
        assertThat(statusOf(order.id())).isEqualTo("READY");
    }

    @Test
    @DisplayName("TC-HAND-05 Đơn chưa READY (xác nhận, đang chuẩn bị): bị từ chối, báo trạng thái hiện tại")
    void hand05() throws Exception {
        MockHttpSession counter = login("quay1@example.com");
        for (String status : List.of("CONFIRMED", "PREPARING")) {
            Placed order = place("12:00");
            setStatus(order.id(), status);
            jdbc.update(
                    "UPDATE orders SET payment_status = 'PAID', payment_method = 'CASH', paid_at = now(),"
                            + " paid_by = ? WHERE id = ?",
                    counterId(),
                    order.id());

            Res result = handOver(order.id(), order.code(), counter);

            assertThat(result.status()).as(status).isEqualTo(409);
            assertThat((String) result.at("$.code")).isEqualTo("INVALID_STATUS_TRANSITION");
            assertThat((String) result.at("$.currentStatus")).isEqualTo(status);
            assertThat(statusOf(order.id())).isEqualTo(status);
            setStatus(order.id(), "NO_SHOW");
        }
    }

    @Test
    @DisplayName("TC-HAND-06 Đơn đã bàn giao không bàn giao lại được; đơn hủy hoặc vắng mặt cũng không")
    void hand06() throws Exception {
        Placed order = placeReadyAndPaid();
        MockHttpSession counter = login("quay1@example.com");
        assertThat(handOver(order.id(), order.code(), counter).status()).isEqualTo(200);

        Res again = handOver(order.id(), order.code(), counter);

        assertThat(again.status()).isEqualTo(409);
        assertThat((String) again.at("$.code")).isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat((String) again.at("$.currentStatus")).isEqualTo("PICKED_UP");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders WHERE picked_up_by IS NOT NULL", Long.class))
                .isEqualTo(1);
        for (String status : List.of("CANCELLED", "NO_SHOW")) {
            Placed other = place("12:15");
            setStatus(other.id(), status);
            assertThat(handOver(other.id(), other.code(), counter).status())
                    .as(status)
                    .isEqualTo(409);
            setStatus(other.id(), "PICKED_UP");
        }
    }

    @Test
    @DisplayName("TC-HAND-07 Đơn không tồn tại: không tìm thấy; thiếu hoặc trống mã: dữ liệu không hợp lệ")
    void hand07() throws Exception {
        Placed order = placeReadyAndPaid();
        MockHttpSession counter = login("quay1@example.com");

        assertThat(handOver(987654, "ABCDEF", counter).status()).isEqualTo(404);
        assertThat(handOver(order.id(), null, counter).status()).isEqualTo(400);
        assertThat(handOver(order.id(), "   ", counter).status()).isEqualTo(400);
        assertThat(statusOf(order.id())).isEqualTo("READY");
    }

    @Test
    @DisplayName("TC-HAND-08 Chỉ quầy và quản lý bàn giao được; khách, bếp, chưa đăng nhập bị từ chối")
    void hand08() throws Exception {
        Placed order = placeReadyAndPaid();

        assertThat(handOver(order.id(), order.code(), null).status()).isEqualTo(401);
        assertThat(handOver(order.id(), order.code(), login("khach1@example.com"))
                        .status())
                .isEqualTo(403);
        assertThat(handOver(order.id(), order.code(), login("bep1@example.com")).status())
                .isEqualTo(403);
        assertThat(statusOf(order.id())).isEqualTo("READY");

        assertThat(handOver(order.id(), order.code(), login("quanly@example.com"))
                        .status())
                .as("quản lý làm được thao tác của quầy")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("TC-HAND-09 Hai nhân viên cùng bàn giao một đơn: đúng một lần thành công")
    void hand09() throws Exception {
        Placed order = placeReadyAndPaid();
        MockHttpSession counterA = login("quay1@example.com");
        MockHttpSession counterB = login("quay1@example.com");

        List<Res> results = concurrently(List.of(
                () -> handOver(order.id(), order.code(), counterA),
                () -> handOver(order.id(), order.code(), counterB)));

        assertThat(results.stream().filter(r -> r.status() == 200)).hasSize(1);
        assertThat(results.stream().filter(r -> r.status() == 409)).hasSize(1);
    }

    @Test
    @DisplayName("TC-HAND-10 Lỗi database khi bàn giao: báo lỗi, đơn vẫn READY, thử lại được")
    void hand10() throws Exception {
        Placed order = placeReadyAndPaid();
        MockHttpSession counter = login("quay1@example.com");
        doThrow(new IllegalStateException("Lỗi database giả lập"))
                .when(orderRepository)
                .markPickedUp(anyLong(), anyString(), anyLong(), any());

        Res failed = handOver(order.id(), order.code(), counter);

        assertThat(failed.status()).isEqualTo(500);
        assertThat(statusOf(order.id())).isEqualTo("READY");
        Mockito.reset(orderRepository);
        assertThat(handOver(order.id(), order.code(), counter).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("TC-HAND-11 Vòng đời trọn vẹn: đặt, bếp làm, quầy thu tiền, quầy bàn giao")
    void hand11() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        Res placed = order(customer, "12:00", line("Burger bò", 2), line("Trà đào", 1));
        long id = ((Number) placed.json("$.id")).longValue();
        String code = placed.at("$.pickupCode");
        MockHttpSession kitchen = login("bep1@example.com");
        MockHttpSession counter = login("quay1@example.com");
        assertThat((String) get("/api/orders/" + id, customer).at("$.status")).isEqualTo("CONFIRMED");

        at("11:45"); // thời gian chuẩn bị của khung 12:00 (BR-21)
        assertThat(post("/api/kitchen/orders/" + id + "/start", kitchen, null).status())
                .isEqualTo(200);
        assertThat(post("/api/kitchen/orders/" + id + "/complete", kitchen, null)
                        .status())
                .isEqualTo(200);
        assertThat(handOver(id, code, counter).status()).as("chưa thanh toán").isEqualTo(409);
        assertThat(post("/api/counter/orders/" + id + "/payment", counter, "{\"method\":\"BANK_TRANSFER\"}")
                        .status())
                .isEqualTo(200);
        assertThat(handOver(id, code, counter).status()).isEqualTo(200);

        Res detail = get("/api/orders/" + id, customer);
        assertThat((String) detail.at("$.status")).isEqualTo("PICKED_UP");
        assertThat((String) detail.at("$.paymentStatus")).isEqualTo("PAID");
        assertThat((String) detail.at("$.paymentMethod")).isEqualTo("BANK_TRANSFER");
        assertThat((Integer) detail.at("$.totalAmount")).isEqualTo(110000);
        assertThat(held("12:00")).isEqualTo(10);
    }
}
