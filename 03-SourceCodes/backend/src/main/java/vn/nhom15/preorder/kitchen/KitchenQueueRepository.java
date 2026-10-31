package vn.nhom15.preorder.kitchen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.nhom15.preorder.order.OrderSource;
import vn.nhom15.preorder.order.OrderStatus;

/** Chỉ đọc: module kitchen không ghi {@code orders} và {@code order_lines} (Architecture mục 2). */
@Repository
public class KitchenQueueRepository {

    /** Đơn kèm trạng thái, để {@link KitchenService} chia nhóm. */
    record ActiveOrder(OrderStatus status, KitchenOrder order) {}

    private final JdbcTemplate jdbcTemplate;

    public KitchenQueueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Đơn {@code CONFIRMED} và {@code PREPARING} có khung thuộc ngày {@code day}, sắp theo giờ bắt đầu khung rồi thời
     * điểm xác nhận (BR-22). Cờ {@code late} chưa được đặt: nó cần giờ hiện tại, nên do service tính.
     */
    List<ActiveOrder> findActiveOrders(LocalDate day, int slotMinutes, int prepWindowMinutes) {
        Map<Long, List<KitchenOrder.Line>> linesByOrder = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                        SELECT l.order_id, l.item_name, l.quantity
                        FROM order_lines l
                        JOIN orders o ON o.id = l.order_id
                        JOIN time_slots s ON s.id = o.time_slot_id
                        WHERE s.slot_date = ? AND o.status IN ('CONFIRMED', 'PREPARING')
                        ORDER BY l.id
                        """,
                rs -> {
                    linesByOrder
                            .computeIfAbsent(rs.getLong("order_id"), id -> new ArrayList<>())
                            .add(new KitchenOrder.Line(rs.getString("item_name"), rs.getInt("quantity")));
                },
                day);

        return jdbcTemplate.query(
                """
                        SELECT o.id, o.pickup_code, s.start_time, o.source, o.status, o.confirmed_at
                        FROM orders o JOIN time_slots s ON s.id = o.time_slot_id
                        WHERE s.slot_date = ? AND o.status IN ('CONFIRMED', 'PREPARING')
                        ORDER BY s.start_time, o.confirmed_at, o.id
                        """,
                (rs, rowNum) -> {
                    long id = rs.getLong("id");
                    LocalDateTime start = LocalDateTime.of(day, rs.getObject("start_time", LocalTime.class));
                    return new ActiveOrder(
                            OrderStatus.valueOf(rs.getString("status")),
                            new KitchenOrder(
                                    id,
                                    rs.getString("pickup_code"),
                                    start,
                                    start.plusMinutes(slotMinutes),
                                    OrderSource.valueOf(rs.getString("source")),
                                    rs.getObject("confirmed_at", OffsetDateTime.class),
                                    false,
                                    start.minusMinutes(prepWindowMinutes),
                                    List.copyOf(linesByOrder.getOrDefault(id, List.of()))));
                },
                day);
    }
}
