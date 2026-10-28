package vn.nhom15.preorder.slot;

import java.time.LocalTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc bảng {@code store_settings} (luôn đúng một dòng). */
@Repository
public class StoreSettingsRepository {

    private static final String SELECT_SQL = """
            SELECT open_time, close_time, default_quota_points FROM store_settings WHERE id = 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public StoreSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Khóa chia sẻ dòng cấu hình đến hết giao dịch. Mọi lần giữ quota gọi trước khi đọc quota mặc định, nên quản lý đổi
     * giờ mở cửa hoặc quota mặc định (khóa ghi) phải chờ các lần đặt đang chạy xong, và ngược lại (BR-11).
     */
    public void lockShared() {
        jdbcTemplate.queryForList("SELECT id FROM store_settings WHERE id = 1 FOR SHARE", Integer.class);
    }

    /** Khóa ghi dòng cấu hình đến hết giao dịch, trước khi kiểm tra và đổi cấu hình. */
    public void lockForUpdate() {
        jdbcTemplate.queryForList("SELECT id FROM store_settings WHERE id = 1 FOR UPDATE", Integer.class);
    }

    /** Ghi cấu hình mới (UC-14). Chỉ gọi sau khi đã kiểm tra BR-11 trong cùng giao dịch, có khóa ghi. */
    public void update(StoreSettings settings) {
        jdbcTemplate.update("""
                        UPDATE store_settings
                        SET open_time = ?, close_time = ?, default_quota_points = ?, updated_at = now()
                        WHERE id = 1
                        """, settings.openTime(), settings.closeTime(), settings.defaultQuotaPoints());
    }

    /** Cấu hình hiện tại. Thiếu dòng cấu hình là lỗi cài đặt, không phải lỗi người dùng. */
    public StoreSettings get() {
        List<StoreSettings> found = jdbcTemplate.query(
                SELECT_SQL,
                (rs, rowNum) -> new StoreSettings(
                        rs.getObject("open_time", LocalTime.class),
                        rs.getObject("close_time", LocalTime.class),
                        rs.getInt("default_quota_points")));
        if (found.isEmpty()) {
            throw new IllegalStateException("Chưa có cấu hình cửa hàng (bảng store_settings trống)");
        }
        return found.getFirst();
    }
}
