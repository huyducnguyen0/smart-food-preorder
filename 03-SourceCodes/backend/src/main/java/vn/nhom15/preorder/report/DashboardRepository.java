package vn.nhom15.preorder.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Chỉ đọc: module report không ghi bảng nào (Architecture mục 2). */
@Repository
public class DashboardRepository {

    /** Một đơn đang xử lý (xác nhận, đang chuẩn bị, sẵn sàng) của ngày. */
    public record ActiveOrder(long id, String pickupCode, LocalDateTime slotStart, String status, String source) {}

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<ActiveOrder> findActiveOrders(LocalDate day) {
        return jdbcTemplate.query(
                """
                        SELECT o.id, o.pickup_code, s.start_time, o.status, o.source
                        FROM orders o JOIN time_slots s ON s.id = o.time_slot_id
                        WHERE s.slot_date = ? AND o.status IN ('CONFIRMED', 'PREPARING', 'READY')
                        ORDER BY s.start_time, o.confirmed_at, o.id
                        """,
                (rs, rowNum) -> new ActiveOrder(
                        rs.getLong("id"),
                        rs.getString("pickup_code"),
                        LocalDateTime.of(day, rs.getObject("start_time", LocalTime.class)),
                        rs.getString("status"),
                        rs.getString("source")),
                day);
    }

    /** Số đơn chưa kết thúc của các ngày trước {@code day}: cửa hàng cần dọn ở màn quầy. */
    int countStaleOpenOrders(LocalDate day) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT count(*) FROM orders o JOIN time_slots s ON s.id = o.time_slot_id
                        WHERE s.slot_date < ? AND o.status IN ('CONFIRMED', 'PREPARING', 'READY')
                        """, Integer.class, day);
        return count == null ? 0 : count;
    }

    /** Số đơn của ngày theo cặp (nguồn, trạng thái). */
    Map<String, Integer> countBySourceAndStatus(LocalDate day) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                        SELECT o.source, o.status, count(*) AS n
                        FROM orders o JOIN time_slots s ON s.id = o.time_slot_id
                        WHERE s.slot_date = ?
                        GROUP BY o.source, o.status
                        """,
                rs -> {
                    counts.put(rs.getString("source") + ":" + rs.getString("status"), rs.getInt("n"));
                },
                day);
        return counts;
    }
}
