package vn.nhom15.preorder.counter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.nhom15.preorder.order.OrderSource;
import vn.nhom15.preorder.order.OrderStatus;
import vn.nhom15.preorder.order.PaymentMethod;

/** Chỉ đọc: module counter không ghi {@code orders} và {@code order_lines} (Architecture mục 2). */
@Repository
public class CounterQueueRepository {

    private final JdbcTemplate jdbcTemplate;

    public CounterQueueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Đơn đang xử lý (xác nhận, đang chuẩn bị, sẵn sàng) có khung thuộc ngày {@code day}, sắp theo giờ khung rồi giờ
     * xác nhận. Đơn đã bàn giao, hủy hay vắng mặt không còn việc cho quầy nên không có trong danh sách.
     */
    List<CounterOrder> findOpenOrders(LocalDate day, int slotMinutes) {
        Map<Long, List<CounterOrder.Line>> linesByOrder = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                        SELECT l.order_id, l.item_name, l.quantity
                        FROM order_lines l
                        JOIN orders o ON o.id = l.order_id
                        JOIN time_slots s ON s.id = o.time_slot_id
                        WHERE s.slot_date = ? AND o.status IN ('CONFIRMED', 'PREPARING', 'READY')
                        ORDER BY l.id
                        """,
                rs -> {
                    linesByOrder
                            .computeIfAbsent(rs.getLong("order_id"), id -> new ArrayList<>())
                            .add(new CounterOrder.Line(rs.getString("item_name"), rs.getInt("quantity")));
                },
                day);

        return jdbcTemplate.query(
                """
                        SELECT o.id, o.pickup_code, u.display_name, s.start_time, o.source, o.status,
                               o.payment_status, o.payment_method, o.total_amount
                        FROM orders o
                        JOIN time_slots s ON s.id = o.time_slot_id
                        LEFT JOIN user_accounts u ON u.id = o.customer_id
                        WHERE s.slot_date = ? AND o.status IN ('CONFIRMED', 'PREPARING', 'READY')
                        ORDER BY s.start_time, o.confirmed_at, o.id
                        """,
                (rs, rowNum) -> {
                    long id = rs.getLong("id");
                    LocalDateTime start = LocalDateTime.of(day, rs.getObject("start_time", LocalTime.class));
                    String method = rs.getString("payment_method");
                    return new CounterOrder(
                            id,
                            rs.getString("pickup_code"),
                            rs.getString("display_name"),
                            start,
                            start.plusMinutes(slotMinutes),
                            OrderSource.valueOf(rs.getString("source")),
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getString("payment_status"),
                            method == null ? null : PaymentMethod.valueOf(method),
                            rs.getInt("total_amount"),
                            List.copyOf(linesByOrder.getOrDefault(id, List.of())));
                },
                day);
    }
}
