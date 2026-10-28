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

    private static final String HOLD_SQL = """
            UPDATE time_slots
            SET held_points = held_points + ?
            WHERE slot_date = ? AND start_time = ?
              AND held_points + ? <= COALESCE(quota_override, ?)
            RETURNING id
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
     * Đặt quota riêng cho khung, chỉ khi không thấp hơn điểm đang giữ (BR-11). {@code quota = null} trả khung về quota
     * mặc định, cũng chỉ khi quota mặc định không thấp hơn điểm đang giữ. Một câu lệnh có điều kiện nên không bị chen
     * ngang bởi lần giữ quota cùng lúc.
     *
     * @return true nếu đã đổi
     */
    public boolean setQuotaOverride(LocalDate date, LocalTime start, Integer quota, int defaultQuota) {
        return jdbcTemplate.update("""
                                UPDATE time_slots SET quota_override = ?
                                WHERE slot_date = ? AND start_time = ? AND held_points <= COALESCE(?, ?)
                                """, quota, date, start, quota, defaultQuota) > 0;
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
     * Kiểm tra và giữ quota trong <b>một câu lệnh</b> (BR-09, NFR-01): chỉ cộng điểm khi điểm đang giữ cộng điểm đơn
     * không vượt quota hiệu lực của khung.
     *
     * <p>Vì sao không bị chen ngang: UPDATE khóa dòng khung. Giao dịch đến sau phải chờ, rồi PostgreSQL kiểm tra lại
     * điều kiện WHERE trên giá trị mới nhất của dòng, nên hai đơn không thể cùng "thấy còn chỗ" dựa trên số liệu cũ.
     *
     * @return mã khung nếu giữ được; rỗng nếu không đủ chỗ (hoặc khung chưa có dòng)
     */
    public Optional<Long> tryHold(LocalDate date, LocalTime start, int points, int defaultQuota) {
        List<Long> held = jdbcTemplate.queryForList(HOLD_SQL, Long.class, points, date, start, points, defaultQuota);
        return held.stream().findFirst();
    }
}
