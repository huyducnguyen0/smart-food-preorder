package vn.nhom15.preorder.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Giao dịch đặt đơn (UC-04 bước 8–11, UC-17) chạy trên PostgreSQL thật, không mock repository (Class Design mục 3).
 * Đồng hồ cố định ở 09:00 ngày {@link #TODAY} nên mọi khung 10:00–20:45 đều hợp lệ.
 */
@SpringBootTest
class PlaceOrderIntegrationTest {

    private static final HikariDataSource DATA_SOURCE = TestDatabase.create(true);
    private static final LocalDate TODAY = LocalDate.of(2031, 3, 5);
    private static final LocalDateTime NOON = TODAY.atTime(12, 0);
    private static final LocalDateTime NOON_15 = TODAY.atTime(12, 15);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
    private PlaceOrderService service;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private OrderRepository orderRepository;

    private long customer1;
    private long customer2;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE order_lines, orders, time_slots RESTART IDENTITY CASCADE");
        // Cấu hình tương đương mô hình quota Sprint 1 (làm trước 15 phút, đặt trước 100%, quầy không giới hạn), xem
        // PB-22.
        jdbc.update("UPDATE store_settings SET hold_minutes = 15, preorder_share_percent = 100,"
                + " pickup_limit_orders = 1000");
        customer1 = accountId("khach1@example.com");
        customer2 = accountId("0901234567");
    }

    @AfterEach
    void restoreMenu() {
        // Mỗi test có thể đổi giá hoặc tình trạng món; trả về như dữ liệu mẫu.
        jdbc.update("UPDATE menu_items SET price = 45000, available = true WHERE name = 'Burger bò'");
        Mockito.reset(orderRepository);
    }

    private long accountId(String loginId) {
        return jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = ?", Long.class, loginId);
    }

    private long itemId(String name) {
        return jdbc.queryForObject("SELECT id FROM menu_items WHERE name = ?", Long.class, name);
    }

    private int itemPrice(String name) {
        return jdbc.queryForObject("SELECT price FROM menu_items WHERE name = ?", Integer.class, name);
    }

    /** Dòng đơn khớp giá hiện tại của món (khách "đã thấy" đúng giá). */
    private RequestedLine line(String name, int quantity) {
        return new RequestedLine(itemId(name), quantity, itemPrice(name));
    }

    private PlaceOrderCommand command(LocalDateTime slot, RequestedLine... lines) {
        return new PlaceOrderCommand(UUID.randomUUID(), slot, List.of(lines));
    }

    private int heldPoints(LocalDateTime slot) {
        List<Integer> held = jdbc.queryForList(
                "SELECT held_points FROM time_slots WHERE slot_date = ? AND start_time = ?",
                Integer.class,
                slot.toLocalDate(),
                slot.toLocalTime());
        return held.isEmpty() ? 0 : held.getFirst();
    }

    private int orderCount() {
        return jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class);
    }

    /** Bất bất biến của Database Design mục 3: điểm đang giữ của khung bằng tổng điểm các đơn của khung. */
    private void assertQuotaMatchesOrders() {
        Integer mismatches = jdbc.queryForObject("""
                        SELECT count(*) FROM time_slots s
                        WHERE s.held_points <> COALESCE((SELECT sum(o.workload_points) FROM orders o
                                                         WHERE o.time_slot_id = s.id), 0)
                        """, Integer.class);
        assertThat(mismatches).as("khung có held_points khác tổng điểm đơn").isZero();
    }

    private static ErrorCode codeOf(Throwable e) {
        return ((ApiException) e).code();
    }

    // ----- Thành công -----

    @Test
    void successfulOrderStoresPricesPointsAndHoldsQuota() {
        // 2 × Burger bò (5 điểm) + 1 × Trà đào (0 điểm) = 10 điểm; 2×45000 + 20000 = 110000.
        PlaceOrderResult result =
                service.placeOrder(customer1, command(NOON, line("Burger bò", 2), line("Trà đào", 1)));

        OrderDetail order = result.order();
        assertThat(result.created()).isTrue();
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.paymentStatus()).isEqualTo("UNPAID");
        assertThat(order.paymentMethod()).isNull();
        assertThat(order.totalAmount()).isEqualTo(110000);
        assertThat(order.pickupCode()).matches("[A-HJKMNP-Z2-9]{6}");
        assertThat(order.slotStart()).isEqualTo(NOON);
        assertThat(order.slotEnd()).isEqualTo(NOON_15);
        assertThat(order.confirmedAt().getOffset().getTotalSeconds()).isEqualTo(7 * 3600);
        assertThat(order.lines())
                .extracting(
                        OrderLineView::name,
                        OrderLineView::quantity,
                        OrderLineView::unitPrice,
                        OrderLineView::lineTotal)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Burger bò", 2, 45000, 90000),
                        org.assertj.core.groups.Tuple.tuple("Trà đào", 1, 20000, 20000));
        assertThat(heldPoints(NOON)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT workload_points FROM orders WHERE id = ?", Integer.class, order.id()))
                .isEqualTo(10);
        assertQuotaMatchesOrders();
    }

    @Test
    void zeroPointOrderIsAcceptedEvenWhenTheSlotIsFull() {
        jdbc.update(
                "INSERT INTO time_slots (slot_date, start_time, quota_override) VALUES (?, ?, 0)",
                TODAY,
                NOON.toLocalTime());

        PlaceOrderResult result = service.placeOrder(customer1, command(NOON, line("Nước suối", 2)));

        assertThat(result.created()).isTrue();
        assertThat(heldPoints(NOON)).isZero();
        assertQuotaMatchesOrders();
    }

    // ----- A1: gửi lại cùng mã yêu cầu -----

    @Test
    void resendingTheSameRequestReturnsTheSameOrderAndHoldsNothingMore() {
        PlaceOrderCommand command = command(NOON, line("Burger bò", 1));

        PlaceOrderResult first = service.placeOrder(customer1, command);
        PlaceOrderResult second = service.placeOrder(customer1, command);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.order().id()).isEqualTo(first.order().id());
        assertThat(second.order().pickupCode()).isEqualTo(first.order().pickupCode());
        assertThat(orderCount()).isEqualTo(1);
        assertThat(heldPoints(NOON)).isEqualTo(5);
    }

    @Test
    void sameRequestIdOfAnotherCustomerIsAnIndependentRequest() {
        UUID shared = UUID.randomUUID();
        RequestedLine item = line("Burger bò", 1);

        PlaceOrderResult a = service.placeOrder(customer1, new PlaceOrderCommand(shared, NOON, List.of(item)));
        PlaceOrderResult b = service.placeOrder(customer2, new PlaceOrderCommand(shared, NOON, List.of(item)));

        assertThat(a.order().id()).isNotEqualTo(b.order().id());
        assertThat(orderCount()).isEqualTo(2);
    }

    @Test
    void concurrentResendsOfOneRequestCreateExactlyOneOrder() throws Exception {
        PlaceOrderCommand command = command(NOON, line("Burger bò", 1));

        List<PlaceOrderResult> results = runConcurrently(8, i -> service.placeOrder(customer1, command));

        assertThat(results).hasSize(8);
        assertThat(results.stream().filter(PlaceOrderResult::created)).hasSize(1);
        assertThat(results.stream().map(r -> r.order().id()).distinct()).hasSize(1);
        assertThat(orderCount()).isEqualTo(1);
        assertThat(heldPoints(NOON)).isEqualTo(5);
        assertQuotaMatchesOrders();
    }

    // ----- E5: giới hạn đơn chưa kết thúc -----

    @Test
    void customerCannotExceedTheOpenOrderLimit() {
        for (int i = 0; i < 3; i++) {
            service.placeOrder(customer1, command(NOON, line("Khoai tây chiên", 1)));
        }

        assertThatThrownBy(() -> service.placeOrder(customer1, command(NOON, line("Khoai tây chiên", 1))))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    assertThat(codeOf(e)).isEqualTo(ErrorCode.OPEN_ORDER_LIMIT_REACHED);
                    assertThat(((ApiException) e).properties()).containsEntry("openOrders", 3);
                });

        assertThat(orderCount()).isEqualTo(3);
        assertThat(heldPoints(NOON)).isEqualTo(6);
        // Khách khác không bị ảnh hưởng.
        assertThat(service.placeOrder(customer2, command(NOON, line("Khoai tây chiên", 1)))
                        .created())
                .isTrue();
    }

    @Test
    void finishedOrdersDoNotCountTowardsTheLimit() {
        for (int i = 0; i < 3; i++) {
            service.placeOrder(customer1, command(NOON, line("Khoai tây chiên", 1)));
        }
        jdbc.update("UPDATE orders SET status = 'PICKED_UP' WHERE id = (SELECT min(id) FROM orders)");

        assertThat(service.placeOrder(customer1, command(NOON, line("Khoai tây chiên", 1)))
                        .created())
                .isTrue();
    }

    @Test
    void concurrentOrdersOfOneCustomerRespectTheLimit() throws Exception {
        List<Object> outcomes = runConcurrentlyCollectingErrors(
                6, i -> service.placeOrder(customer1, command(NOON, line("Khoai tây chiên", 1))));

        assertThat(outcomes.stream().filter(PlaceOrderResult.class::isInstance)).hasSize(3);
        assertThat(outcomes.stream().filter(ApiException.class::isInstance).map(o -> ((ApiException) o).code()))
                .containsOnly(ErrorCode.OPEN_ORDER_LIMIT_REACHED)
                .hasSize(3);
        assertThat(orderCount()).isEqualTo(3);
        assertQuotaMatchesOrders();
    }

    // ----- E6: khung không hợp lệ -----

    @Test
    void invalidSlotIsRejectedWithoutSideEffects() {
        for (LocalDateTime slot : List.of(
                TODAY.atTime(9, 30), // trước giờ mở cửa
                TODAY.atTime(21, 0), // sau giờ đóng cửa
                TODAY.atTime(12, 5), // không đúng mốc
                TODAY.plusDays(1).atTime(12, 0))) { // không phải hôm nay
            assertThatThrownBy(() -> service.placeOrder(customer1, command(slot, line("Burger bò", 1))))
                    .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SLOT_NOT_BOOKABLE));
        }
        assertThat(orderCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM time_slots", Integer.class))
                .isZero();
    }

    // ----- E7: món thay đổi -----

    @Test
    void changedPriceIsReportedAndNothingIsCreated() {
        RequestedLine seen = line("Burger bò", 1);
        jdbc.update("UPDATE menu_items SET price = 50000 WHERE name = 'Burger bò'");

        assertThatThrownBy(() ->
                        service.placeOrder(customer1, new PlaceOrderCommand(UUID.randomUUID(), NOON, List.of(seen))))
                .satisfies(e -> {
                    assertThat(codeOf(e)).isEqualTo(ErrorCode.ITEMS_CHANGED);
                    assertThat(((ApiException) e).properties().get("items").toString())
                            .contains("PRICE_CHANGED")
                            .contains("currentPrice=50000");
                });

        assertThat(orderCount()).isZero();
        assertThat(heldPoints(NOON)).isZero();
    }

    @Test
    void soldOutDiscontinuedAndUnknownItemsAreAllReportedTogether() {
        RequestedLine soldOut = line("Burger gà giòn", 1);
        RequestedLine discontinued = line("Combo cũ", 1);
        RequestedLine unknown = new RequestedLine(999_999, 1, 1000);

        assertThatThrownBy(() -> service.placeOrder(
                        customer1,
                        new PlaceOrderCommand(UUID.randomUUID(), NOON, List.of(soldOut, discontinued, unknown))))
                .satisfies(e -> {
                    assertThat(codeOf(e)).isEqualTo(ErrorCode.ITEMS_CHANGED);
                    String items = ((ApiException) e).properties().get("items").toString();
                    assertThat(items).contains("UNAVAILABLE").doesNotContain("PRICE_CHANGED");
                    assertThat(items.split("UNAVAILABLE", -1)).hasSize(4); // ba món
                });
        assertThat(orderCount()).isZero();
    }

    @Test
    void sameItemOnTwoLinesIsRejected() {
        RequestedLine one = line("Burger bò", 1);

        assertThatThrownBy(() -> service.placeOrder(
                        customer1, new PlaceOrderCommand(UUID.randomUUID(), NOON, List.of(one, one))))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    // ----- E8, E9: quota -----

    @Test
    void orderLargerThanTheWholeSlotIsTooLargeNotFull() {
        // 5 × 5 = 25 điểm > quota 20 (E9): đổi khung cũng vô ích.
        assertThatThrownBy(() -> service.placeOrder(customer1, command(NOON, line("Burger bò", 5))))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.ORDER_TOO_LARGE));
        assertThat(orderCount()).isZero();
        assertThat(heldPoints(NOON)).isZero();
    }

    @Test
    void secondOrderThatDoesNotFitGetsSlotFullAndFirstOrderIsUntouched() {
        service.placeOrder(customer1, command(NOON, line("Burger bò", 3))); // 15 điểm, còn 5

        assertThatThrownBy(() -> service.placeOrder(customer2, command(NOON, line("Burger bò", 2)))) // cần 10
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SLOT_FULL));

        assertThat(orderCount()).isEqualTo(1);
        assertThat(heldPoints(NOON)).isEqualTo(15);
        // Khung khác vẫn đặt được.
        assertThat(service.placeOrder(customer2, command(NOON_15, line("Burger bò", 2)))
                        .created())
                .isTrue();
        assertQuotaMatchesOrders();
    }

    @Test
    void manyCustomersRacingForOneSlotNeverOverbook() throws Exception {
        // 30 khách, mỗi khách 1 đơn 2 điểm vào cùng một khung quota 20 → đúng 10 đơn được nhận (NFR-01).
        List<Long> customers = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            customers.add(jdbc.queryForObject(
                    "INSERT INTO user_accounts (login_id, password_hash, display_name, role, locked)"
                            + " VALUES (?, 'x', 'Khách đua', 'CUSTOMER', false) RETURNING id",
                    Long.class,
                    "dua" + i + "@example.com"));
        }

        try {
            List<Object> outcomes = runConcurrentlyCollectingErrors(
                    30, i -> service.placeOrder(customers.get(i), command(NOON, line("Khoai tây chiên", 1))));

            assertThat(outcomes.stream().filter(PlaceOrderResult.class::isInstance))
                    .hasSize(10);
            assertThat(outcomes.stream().filter(ApiException.class::isInstance).map(o -> ((ApiException) o).code()))
                    .containsOnly(ErrorCode.SLOT_FULL)
                    .hasSize(20);
            assertThat(heldPoints(NOON)).isEqualTo(20);
            assertThat(orderCount()).isEqualTo(10);
            assertQuotaMatchesOrders();
        } finally {
            jdbc.execute("TRUNCATE order_lines, orders RESTART IDENTITY CASCADE");
            jdbc.update("DELETE FROM user_accounts WHERE login_id LIKE 'dua%@example.com'");
        }
    }

    // ----- BR-10: tạo đơn và giữ quota cùng thành công hoặc cùng thất bại -----

    @Test
    void failureAfterHoldingQuotaRollsTheQuotaBack() {
        doThrow(new IllegalStateException("Lỗi giả lập khi chèn đơn"))
                .when(orderRepository)
                .insert(any());

        assertThatThrownBy(() -> service.placeOrder(customer1, command(NOON, line("Burger bò", 2))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orderCount()).isZero();
        assertThat(heldPoints(NOON)).as("quota vừa giữ phải trở về như cũ").isZero();
        // Gửi lại an toàn sau lỗi (E10): đơn được tạo bình thường.
        Mockito.reset(orderRepository);
        assertThat(service.placeOrder(customer1, command(NOON, line("Burger bò", 2)))
                        .created())
                .isTrue();
        assertThat(heldPoints(NOON)).isEqualTo(10);
    }

    // ----- Tiện ích chạy đồng thời -----

    private interface Task<T> {
        T run(int index) throws Exception;
    }

    private <T> List<T> runConcurrently(int threads, Task<T> task) throws Exception {
        List<Object> outcomes = runConcurrentlyCollectingErrors(threads, task);
        List<T> results = new ArrayList<>();
        for (Object outcome : outcomes) {
            if (outcome instanceof Throwable error) {
                throw new AssertionError("Một luồng gặp lỗi không mong đợi", error);
            }
            @SuppressWarnings("unchecked")
            T value = (T) outcome;
            results.add(value);
        }
        return results;
    }

    /** Chạy đồng loạt (xuất phát cùng lúc) và gom kết quả hoặc lỗi của từng luồng. */
    private <T> List<Object> runConcurrentlyCollectingErrors(int threads, Task<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                Callable<Object> call = () -> {
                    start.await();
                    try {
                        return task.run(index);
                    } catch (Exception e) {
                        return e;
                    }
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get());
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}
