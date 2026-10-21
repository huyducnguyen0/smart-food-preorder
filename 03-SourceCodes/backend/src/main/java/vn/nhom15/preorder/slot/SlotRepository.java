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
     * Bảo đảm khung có dòng và trả về mã dòng. An toàn khi nhiều giao dịch gọi cùng lúc: giao dịch đến sau chờ giao dịch
     * đến trước kết thúc rồi bỏ qua việc chèn. Câu SELECT chạy riêng để thấy dòng vừa được giao dịch khác ghi.
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
     * <p>Vì sao không bị chen ngang: UPDATE khóa dòng khung. Giao dịch đến sau phải chờ, rồi PostgreSQL kiểm tra lại điều
     * kiện WHERE trên giá trị mới nhất của dòng, nên hai đơn không thể cùng "thấy còn chỗ" dựa trên số liệu cũ.
     *
     * @return mã khung nếu giữ được; rỗng nếu không đủ chỗ (hoặc khung chưa có dòng)
     */
    public Optional<Long> tryHold(LocalDate date, LocalTime start, int points, int defaultQuota) {
        List<Long> held = jdbcTemplate.queryForList(HOLD_SQL, Long.class, points, date, start, points, defaultQuota);
        return held.stream().findFirst();
    }
}
