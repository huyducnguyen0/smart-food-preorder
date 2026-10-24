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
