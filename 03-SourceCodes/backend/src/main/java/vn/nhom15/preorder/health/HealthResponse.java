package vn.nhom15.preorder.health;

import java.time.OffsetDateTime;

/**
 * Kết quả kiểm tra sức khỏe hệ thống trả về cho /api/health.
 *
 * @param status        UP khi backend và database đều hoạt động, ngược lại DOWN
 * @param database      UP nếu truy vấn được database, DOWN nếu không
 * @param schemaVersion phiên bản migration mới nhất đã áp dụng, null nếu không đọc được
 * @param serverTime    thời gian hiện tại theo múi giờ cửa hàng
 */
public record HealthResponse(
        String status,
        String database,
        String schemaVersion,
        OffsetDateTime serverTime) {

    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
}
