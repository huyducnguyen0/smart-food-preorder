package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box UC-08 Ghi nhận thanh toán và danh sách đơn của quầy (PB-16, T-29), qua API HTTP thật trên PostgreSQL thật.
 * ID ca TC-PAY-xx theo tiêu chí chấp nhận của PB-16 (xem UC-08).
 */
class CounterPaymentBlackBoxTest extends BlackBoxBase {

    private static String method(String name) {
        return "{\"method\":\"" + name + "\"}";
    }

    private long place(MockHttpSession customer, String hhmm) throws Exception {
        Res placed = order(customer, hhmm, line("Burger bò", 1));
        assertThat(placed.status()).isEqualTo(201);
        return ((Number) placed.json("$.id")).longValue();
    }

    private Res pay(long id, String body, MockHttpSession counter) throws Exception {
        return post("/api/counter/orders/" + id + "/payment", counter, body);
    }

    @Test
    @DisplayName("TC-PAY-01 Đơn mới của khách mặc định chưa thanh toán (BR-28)")
    void pay01() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        long id = place(customer, "12:00");

        assertThat((String) get("/api/orders/" + id, customer).at("$.paymentStatus"))
                .isEqualTo("UNPAID");
        Res list = get("/api/counter/orders", login("quay1@example.com"));
        assertThat(list.<List<String>>at("$.orders[*].paymentStatus")).containsExactly("UNPAID");
    }

    @Test
    @DisplayName("TC-PAY-02 Quầy ghi nhận tiền mặt: PAID, ghi người và thời điểm, khách thấy đã thanh toán")
    void pay02() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        long id = place(customer, "12:00");

        Res result = pay(id, method("CASH"), login("quay1@example.com"));

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.paymentStatus")).isEqualTo("PAID");
        assertThat((String) result.at("$.paymentMethod")).isEqualTo("CASH");
        assertThat(jdbc.queryForObject("SELECT payment_method FROM orders WHERE id = ?", String.class, id))
                .isEqualTo("CASH");
        assertThat(jdbc.queryForObject("SELECT paid_by FROM orders WHERE id = ?", Long.class, id))
                .isEqualTo(jdbc.queryForObject(
                        "SELECT id FROM user_accounts WHERE login_id = 'quay1@example.com'", Long.class));
        assertThat(jdbc.queryForObject("SELECT paid_at IS NOT NULL FROM orders WHERE id = ?", Boolean.class, id))
                .isTrue();
        Res detail = get("/api/orders/" + id, customer);
        assertThat((String) detail.at("$.paymentStatus")).isEqualTo("PAID");
        assertThat((String) detail.at("$.paymentMethod")).isEqualTo("CASH");
    }

    @Test
    @DisplayName("TC-PAY-03 Quầy ghi nhận chuyển khoản tại quầy")
    void pay03() throws Exception {
        long id = place(login("khach1@example.com"), "12:00");

        Res result = pay(id, method("BANK_TRANSFER"), login("quay1@example.com"));

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.paymentMethod")).isEqualTo("BANK_TRANSFER");
    }

    @Test
    @DisplayName(
            "TC-PAY-04 Ghi nhận được ở mọi bước đang xử lý: xác nhận, đang chuẩn bị, sẵn sàng (trả khi nhận, BR-27)")
    void pay04() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        MockHttpSession counter = login("quay1@example.com");
        for (String status : List.of("CONFIRMED", "PREPARING", "READY")) {
            long id = place(customer, "13:00");
            setStatus(id, status);

            assertThat(pay(id, method("CASH"), counter).status()).as(status).isEqualTo(200);

            setStatus(id, "PICKED_UP");
        }
    }

    @Test
    @DisplayName("TC-PAY-05 Ghi nhận lần hai: bị từ chối, giữ nguyên phương thức đã ghi")
    void pay05() throws Exception {
        long id = place(login("khach1@example.com"), "12:00");
        MockHttpSession counter = login("quay1@example.com");
        pay(id, method("CASH"), counter);

        Res again = pay(id, method("BANK_TRANSFER"), counter);

        assertThat(again.status()).isEqualTo(409);
        assertThat((String) again.at("$.code")).isEqualTo("ALREADY_PAID");
        assertThat(jdbc.queryForObject("SELECT payment_method FROM orders WHERE id = ?", String.class, id))
                .isEqualTo("CASH");
    }

    @Test
    @DisplayName("TC-PAY-06 Đơn đã bàn giao, hủy hoặc vắng mặt: không ghi nhận thanh toán được")
    void pay06() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        MockHttpSession counter = login("quay1@example.com");
        for (String status : List.of("CANCELLED", "NO_SHOW")) {
            long id = place(customer, "13:00");
            setStatus(id, status);

            Res result = pay(id, method("CASH"), counter);

            assertThat(result.status()).as(status).isEqualTo(409);
            assertThat((String) result.at("$.code")).isEqualTo("ORDER_NOT_PAYABLE");
            assertThat((String) result.at("$.currentStatus")).isEqualTo(status);
            assertThat(jdbc.queryForObject("SELECT payment_status FROM orders WHERE id = ?", String.class, id))
                    .isEqualTo("UNPAID");
        }
    }

    @Test
    @DisplayName("TC-PAY-07 Đơn không tồn tại: không tìm thấy; phương thức thiếu hoặc lạ: dữ liệu không hợp lệ")
    void pay07() throws Exception {
        long id = place(login("khach1@example.com"), "12:00");
        MockHttpSession counter = login("quay1@example.com");

        assertThat(pay(987654, method("CASH"), counter).status()).isEqualTo(404);
        for (String body : List.of("{}", method("BITCOIN"), method("PAID"), "")) {
            assertThat(pay(id, body.isEmpty() ? null : body, counter).status())
                    .as(body)
                    .isEqualTo(400);
        }
        assertThat(jdbc.queryForObject("SELECT payment_status FROM orders WHERE id = ?", String.class, id))
                .isEqualTo("UNPAID");
    }

    @Test
    @DisplayName("TC-PAY-08 Chỉ quầy và quản lý ghi nhận được; khách, bếp, chưa đăng nhập bị từ chối")
    void pay08() throws Exception {
        long id = place(login("khach1@example.com"), "12:00");

        assertThat(pay(id, method("CASH"), null).status()).isEqualTo(401);
        assertThat(pay(id, method("CASH"), login("khach1@example.com")).status())
                .isEqualTo(403);
        assertThat(pay(id, method("CASH"), login("bep1@example.com")).status()).isEqualTo(403);
        assertThat(get("/api/counter/orders", login("khach1@example.com")).status())
                .isEqualTo(403);
        assertThat(get("/api/counter/orders", null).status()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT payment_status FROM orders WHERE id = ?", String.class, id))
                .isEqualTo("UNPAID");

        assertThat(pay(id, method("CASH"), login("quanly@example.com")).status())
                .as("quản lý làm được thao tác của quầy")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("TC-PAY-09 Hai nhân viên cùng bấm: đúng một lần thành công")
    void pay09() throws Exception {
        long id = place(login("khach1@example.com"), "12:00");
        MockHttpSession counterA = login("quay1@example.com");
        MockHttpSession counterB = login("quay1@example.com");

        List<Res> results = concurrently(
                List.of(() -> pay(id, method("CASH"), counterA), () -> pay(id, method("BANK_TRANSFER"), counterB)));

        assertThat(results.stream().filter(r -> r.status() == 200)).hasSize(1);
        Res rejected =
                results.stream().filter(r -> r.status() != 200).findFirst().orElseThrow();
        assertThat(rejected.status()).isEqualTo(409);
        assertThat((String) rejected.at("$.code")).isEqualTo("ALREADY_PAID");
    }

    @Test
    @DisplayName("TC-PAY-10 Lỗi database khi ghi nhận: báo lỗi, đơn vẫn chưa thanh toán, thử lại được")
    void pay10() throws Exception {
        long id = place(login("khach1@example.com"), "12:00");
        MockHttpSession counter = login("quay1@example.com");
        doThrow(new IllegalStateException("Lỗi database giả lập"))
                .when(orderRepository)
                .markPaid(anyLong(), any(), anyLong(), any());

        Res failed = pay(id, method("CASH"), counter);

        assertThat(failed.status()).isEqualTo(500);
        assertThat(jdbc.queryForObject("SELECT payment_status FROM orders WHERE id = ?", String.class, id))
                .isEqualTo("UNPAID");
        Mockito.reset(orderRepository);
        assertThat(pay(id, method("CASH"), counter).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("TC-PAY-11 Danh sách của quầy: đơn hôm nay đang xử lý, đủ thông tin thu tiền, sắp theo khung")
    void pay11() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        MockHttpSession kh2 = login("0901234567");
        long late = place(kh1, "14:00");
        long early = place(kh2, "12:00");
        long done = place(kh1, "12:30");
        long cancelled = place(kh2, "12:45");
        long yesterday = place(kh2, "13:30");
        setStatus(done, "PICKED_UP");
        setStatus(cancelled, "CANCELLED");
        jdbc.update(
                "UPDATE time_slots SET slot_date = ? WHERE id = (SELECT time_slot_id FROM orders WHERE id = ?)",
                BlackBoxClockConfig.TODAY.minusDays(1),
                yesterday);

        Res list = get("/api/counter/orders", login("quay1@example.com"));

        assertThat(list.<List<Integer>>at("$.orders[*].id")).containsExactly((int) early, (int) late);
        assertThat((String) list.at("$.orders[0].customerName")).isEqualTo("Khách Hai");
        assertThat((String) list.at("$.orders[0].slotStart")).isEqualTo(slot("12:00"));
        assertThat((Integer) list.at("$.orders[0].totalAmount")).isEqualTo(45000);
        assertThat((String) list.at("$.orders[0].pickupCode")).hasSize(6);
        assertThat(list.<List<String>>at("$.orders[0].lines[*].name")).containsExactly("Burger bò");
    }
}
