package vn.nhom15.preorder.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Truy vấn các bảng {@code orders} và {@code order_lines}. Chỉ module order được ghi hai bảng này. */
@Repository
public class OrderRepository {

    private static final String INSERT_ORDER_SQL = """
            INSERT INTO orders (customer_id, source, time_slot_id, status, payment_status, workload_points,
                                total_amount, pickup_code, request_id, confirmed_at)
            VALUES (?, ?, ?, 'CONFIRMED', 'UNPAID', ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String INSERT_LINE_SQL = """
            INSERT INTO order_lines (order_id, menu_item_id, item_name, unit_price, quantity) VALUES (?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Khóa dòng tài khoản khách đến hết giao dịch. Các lần đặt đồng thời của cùng một khách phải xếp hàng, nên lần sau
     * thấy được đơn lần trước vừa tạo (BR-17, BR-37). Khách khác không bị ảnh hưởng.
     */
    public void lockCustomer(long customerId) {
        jdbcTemplate.queryForList("SELECT id FROM user_accounts WHERE id = ? FOR UPDATE", Long.class, customerId);
    }

    /**
     * Chuyển {@code CONFIRMED → PREPARING} trong một câu lệnh có điều kiện trạng thái. Hai người cùng bấm: câu thứ hai
     * chờ câu thứ nhất, rồi PostgreSQL kiểm tra lại điều kiện trên giá trị mới nên không đổi dòng nào.
     *
     * @return true nếu đơn vừa được chuyển
     */
    public boolean markPreparing(long orderId, long staffId, OffsetDateTime at) {
        return jdbcTemplate.update("""
                                UPDATE orders SET status = 'PREPARING', preparing_at = ?, preparing_by = ?
                                WHERE id = ? AND status = 'CONFIRMED'
                                """, at, staffId, orderId) > 0;
    }

    /** Chuyển {@code PREPARING → READY}, cùng cách bảo vệ như {@link #markPreparing}. */
    public boolean markReady(long orderId, long staffId, OffsetDateTime at) {
        return jdbcTemplate.update("""
                                UPDATE orders SET status = 'READY', ready_at = ?, ready_by = ?
                                WHERE id = ? AND status = 'PREPARING'
                                """, at, staffId, orderId) > 0;
    }

    /**
     * Ghi nhận đã thanh toán: chỉ đơn chưa thanh toán và còn đang xử lý (chưa bàn giao, hủy hay vắng mặt). Một câu lệnh
     * có điều kiện nên hai nhân viên cùng bấm chỉ một người thành công.
     *
     * @return true nếu vừa ghi nhận
     */
    public boolean markPaid(long orderId, PaymentMethod method, long staffId, OffsetDateTime at) {
        return jdbcTemplate.update("""
                                UPDATE orders
                                SET payment_status = 'PAID', payment_method = ?, paid_at = ?, paid_by = ?
                                WHERE id = ? AND payment_status = 'UNPAID'
                                  AND status IN ('CONFIRMED', 'PREPARING', 'READY')
                                """, method.name(), at, staffId, orderId) > 0;
    }

    /** Trạng thái và tình trạng thanh toán hiện tại của đơn, rỗng nếu không có đơn. */
    public Optional<PaymentState> findPaymentState(long orderId) {
        return jdbcTemplate
                .query(
                        "SELECT status, payment_status FROM orders WHERE id = ?",
                        (rs, rowNum) -> new PaymentState(
                                OrderStatus.valueOf(rs.getString("status")), rs.getString("payment_status")),
                        orderId)
                .stream()
                .findFirst();
    }

    /** Ảnh chụp trạng thái đơn và tình trạng thanh toán, để giải thích vì sao một thao tác bị từ chối. */
    public record PaymentState(OrderStatus status, String paymentStatus) {}

    /** Trạng thái hiện tại của đơn, rỗng nếu không có đơn. */
    public Optional<OrderStatus> findStatus(long orderId) {
        return jdbcTemplate.queryForList("SELECT status FROM orders WHERE id = ?", String.class, orderId).stream()
                .findFirst()
                .map(OrderStatus::valueOf);
    }

    /** Đơn đã tạo cho mã yêu cầu này của khách, nếu có (A1). */
    public Optional<Long> findIdByRequest(long customerId, UUID requestId) {
        return jdbcTemplate
                .queryForList(
                        "SELECT id FROM orders WHERE customer_id = ? AND request_id = ?",
                        Long.class,
                        customerId,
                        requestId)
                .stream()
                .findFirst();
    }

    /** Số đơn chưa kết thúc của khách (BR-37): cùng tập trạng thái với {@link OrderStatus#isOpen()}. */
    public int countOpenOrders(long customerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders WHERE customer_id = ? AND status IN ('CONFIRMED', 'PREPARING', 'READY')",
                Integer.class,
                customerId);
        return Objects.requireNonNull(count);
    }

    public boolean pickupCodeExists(String code) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM orders WHERE pickup_code = ?)", Boolean.class, code));
    }

    /** Chèn đơn và các dòng, trả mã đơn. Gọi trong giao dịch đặt đơn. */
    public long insert(NewOrder order) {
        Long id = jdbcTemplate.queryForObject(
                INSERT_ORDER_SQL,
                Long.class,
                order.customerId(),
                order.source().name(),
                order.timeSlotId(),
                order.workloadPoints(),
                order.totalAmount(),
                order.pickupCode(),
                order.requestId(),
                order.confirmedAt());
        long orderId = Objects.requireNonNull(id);
        jdbcTemplate.batchUpdate(INSERT_LINE_SQL, order.lines(), order.lines().size(), (ps, line) -> {
            ps.setLong(1, orderId);
            ps.setLong(2, line.menuItemId());
            ps.setString(3, line.itemName());
            ps.setInt(4, line.unitPrice());
            ps.setInt(5, line.quantity());
        });
        return orderId;
    }

    /**
     * Đơn của một khách theo UC-05 bước 2: đơn chưa kết thúc trước, sắp theo giờ nhận tăng dần; rồi đơn đã kết thúc,
     * mới nhất trước.
     */
    public List<OrderSummary> findSummaries(long customerId, int slotMinutes) {
        return jdbcTemplate.query(
                """
                        SELECT o.id, o.pickup_code, s.slot_date, s.start_time, o.status, o.payment_status,
                               o.total_amount
                        FROM orders o JOIN time_slots s ON s.id = o.time_slot_id
                        WHERE o.customer_id = ?
                        ORDER BY CASE WHEN o.status IN ('CONFIRMED', 'PREPARING', 'READY') THEN 0 ELSE 1 END,
                                 CASE WHEN o.status IN ('CONFIRMED', 'PREPARING', 'READY')
                                      THEN s.slot_date + s.start_time END,
                                 o.confirmed_at DESC, o.id DESC
                        """,
                (rs, rowNum) -> {
                    LocalDateTime start = LocalDateTime.of(
                            rs.getObject("slot_date", LocalDate.class), rs.getObject("start_time", LocalTime.class));
                    return new OrderSummary(
                            rs.getLong("id"),
                            rs.getString("pickup_code"),
                            start,
                            start.plusMinutes(slotMinutes),
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getString("payment_status"),
                            rs.getInt("total_amount"));
                },
                customerId);
    }

    /** Chi tiết đơn của một khách; đơn không tồn tại hoặc của khách khác thì rỗng. */
    public Optional<OrderDetail> findDetail(long orderId, long customerId, int slotMinutes) {
        List<OrderLineView> lines = jdbcTemplate.query(
                """
                        SELECT menu_item_id, item_name, quantity, unit_price FROM order_lines
                        WHERE order_id = ? ORDER BY id
                        """,
                (rs, rowNum) -> new OrderLineView(
                        rs.getLong("menu_item_id"),
                        rs.getString("item_name"),
                        rs.getInt("quantity"),
                        rs.getInt("unit_price"),
                        rs.getInt("quantity") * rs.getInt("unit_price")),
                orderId);
        return jdbcTemplate
                .query(
                        """
                                SELECT o.id, o.pickup_code, s.slot_date, s.start_time, o.status, o.payment_status,
                                       o.payment_method, o.total_amount, o.confirmed_at
                                FROM orders o JOIN time_slots s ON s.id = o.time_slot_id
                                WHERE o.id = ? AND o.customer_id = ?
                                """,
                        (rs, rowNum) -> {
                            LocalDateTime start = LocalDateTime.of(
                                    rs.getObject("slot_date", LocalDate.class),
                                    rs.getObject("start_time", LocalTime.class));
                            return new OrderDetail(
                                    rs.getLong("id"),
                                    rs.getString("pickup_code"),
                                    start,
                                    start.plusMinutes(slotMinutes),
                                    OrderStatus.valueOf(rs.getString("status")),
                                    rs.getString("payment_status"),
                                    rs.getString("payment_method"),
                                    rs.getInt("total_amount"),
                                    rs.getObject("confirmed_at", OffsetDateTime.class),
                                    lines);
                        },
                        orderId,
                        customerId)
                .stream()
                .findFirst();
    }
}
