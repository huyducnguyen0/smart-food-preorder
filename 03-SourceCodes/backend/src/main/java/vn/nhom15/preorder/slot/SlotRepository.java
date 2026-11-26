package vn.nhom15.preorder.slot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Truy vấn bảng {@code time_slots}. Chỉ module slot được ghi cột {@code held_points} (Architecture mục 2). */
@Repository
public class SlotRepository {

    private static final String SELECT_PREFIX = """
            SELECT id, slot_date, start_time, quota_override, held_points FROM time_slots
            """;

    private static final String INSERT_IF_ABSENT_SQL = """
            INSERT INTO time_slots (slot_date, start_time) VALUES (?, ?)
            ON CONFLICT (slot_date, start_time) DO NOTHING
            """;

    private static final RowMapper<TimeSlot> MAPPER = (rs, rowNum) -> new TimeSlot(
            rs.getLong("id"),
            rs.getObject("slot_date", LocalDate.class),
            rs.getObject("start_time", LocalTime.class),
            rs.getObject("quota_override", Integer.class),
            rs.getInt("held_points"));

    private final JdbcTemplate jdbcTemplate;

    public SlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Các khung của một ngày đã có dòng, sắp theo giờ. Khung chưa có dòng nghĩa là chưa có đơn và chưa sửa quota. */
    public List<TimeSlot> findByDate(LocalDate date) {
        return jdbcTemplate.query(SELECT_PREFIX + "WHERE slot_date = ? ORDER BY start_time", MAPPER, date);
    }

    public Optional<TimeSlot> find(LocalDate date, LocalTime start) {
        return jdbcTemplate
                .query(SELECT_PREFIX + "WHERE slot_date = ? AND start_time = ?", MAPPER, date, start)
                .stream()
                .findFirst();
    }

    /**
     * Đặt công suất bếp riêng cho một khoảng làm; {@code null} trả về công suất mặc định. Gọi sau khi đã kiểm tra lịch
     * bếp vẫn kịp (BR-11), trong cùng giao dịch, dưới khóa lịch bếp của ngày.
     */
    public void setCapacityOverride(long slotId, Integer capacity) {
        jdbcTemplate.update("UPDATE time_slots SET quota_override = ? WHERE id = ?", capacity, slotId);
    }

    /** Số đơn đang xử lý (xác nhận, đang chuẩn bị, sẵn sàng) theo giờ bắt đầu khung của một ngày. */
    public java.util.Map<LocalTime, Integer> countOpenOrdersByStart(LocalDate date) {
        java.util.Map<LocalTime, Integer> counts = new java.util.HashMap<>();
        jdbcTemplate.query(
                """
                        SELECT s.start_time, count(o.id) AS n
                        FROM time_slots s JOIN orders o ON o.time_slot_id = s.id
                        WHERE s.slot_date = ? AND o.status IN ('CONFIRMED', 'PREPARING', 'READY')
                        GROUP BY s.start_time
                        """,
                rs -> {
                    counts.put(rs.getObject("start_time", LocalTime.class), rs.getInt("n"));
                },
                date);
        return counts;
    }

    /**
     * Trả lại điểm của đơn bị hủy cho khung (BR-23, BR-24). Không bao giờ xuống dưới 0 nhờ điều kiện trong câu lệnh.
     *
     * @return true nếu đã trả
     */
    public boolean release(long slotId, int points) {
        return jdbcTemplate.update(
                        "UPDATE time_slots SET held_points = held_points - ? WHERE id = ? AND held_points >= ?",
                        points,
                        slotId,
                        points)
                > 0;
    }

    /**
     * Bảo đảm khung có dòng và trả về mã dòng. An toàn khi nhiều giao dịch gọi cùng lúc: giao dịch đến sau chờ
     * giao dịch đến trước kết thúc rồi bỏ qua việc chèn. Câu SELECT chạy riêng để thấy dòng vừa được giao dịch khác
     * ghi.
     */
    public long ensureExists(LocalDate date, LocalTime start) {
        jdbcTemplate.update(INSERT_IF_ABSENT_SQL, date, start);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM time_slots WHERE slot_date = ? AND start_time = ?", Long.class, date, start);
        return Objects.requireNonNull(id);
    }

    /**
     * Cộng điểm của đơn vừa nhận vào khung nhận (BR-14). Từ PB-22 đây là số liệu thống kê và đối chiếu, không còn là
     * giới hạn: việc nhận đơn đã được lịch bếp kiểm tra trong cùng giao dịch, dưới khóa lịch bếp của ngày.
     */
    public void addHeld(long slotId, int points) {
        jdbcTemplate.update("UPDATE time_slots SET held_points = held_points + ? WHERE id = ?", points, slotId);
    }

    /** Đơn còn phải làm của một ngày (đã xác nhận hoặc đang làm, có điểm công việc), cho lịch bếp. */
    public List<KitchenPlanService.OpenOrder> findOrdersToCook(LocalDate date, java.time.ZoneId zone) {
        return jdbcTemplate.query(
                """
                        SELECT o.id, o.source, o.status, o.workload_points, s.start_time, o.confirmed_at
                        FROM orders o JOIN time_slots s ON s.id = o.time_slot_id
                        WHERE s.slot_date = ? AND o.status IN ('CONFIRMED', 'PREPARING') AND o.workload_points > 0
                        ORDER BY o.id
                        """,
                (rs, rowNum) -> new KitchenPlanService.OpenOrder(
                        rs.getLong("id"),
                        "WALK_IN".equals(rs.getString("source")),
                        "PREPARING".equals(rs.getString("status")),
                        rs.getInt("workload_points"),
                        date.atTime(rs.getObject("start_time", LocalTime.class)),
                        rs.getObject("confirmed_at", java.time.OffsetDateTime.class)
                                .atZoneSameInstant(zone)
                                .toLocalDateTime()),
                date);
    }

    /** Số đơn (không tính đơn hủy) theo giờ bắt đầu khung nhận của một ngày: giới hạn quầy (PB-22). */
    public java.util.Map<LocalTime, Integer> countPickupOrdersByStart(LocalDate date) {
        java.util.Map<LocalTime, Integer> counts = new java.util.HashMap<>();
        jdbcTemplate.query(
                """
                        SELECT s.start_time, count(o.id) AS n
                        FROM time_slots s JOIN orders o ON o.time_slot_id = s.id
                        WHERE s.slot_date = ? AND o.status <> 'CANCELLED'
                        GROUP BY s.start_time
                        """,
                rs -> {
                    counts.put(rs.getObject("start_time", LocalTime.class), rs.getInt("n"));
                },
                date);
        return counts;
    }
}
