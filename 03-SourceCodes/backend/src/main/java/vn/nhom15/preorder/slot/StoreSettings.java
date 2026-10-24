package vn.nhom15.preorder.slot;

import java.time.LocalTime;

/**
 * Cấu hình cửa hàng: giờ mở cửa và quota mặc định (BR-06, BR-07).
 *
 * @param openTime giờ mở cửa
 * @param closeTime giờ đóng cửa; khung cuối kết thúc không muộn hơn giờ này
 * @param defaultQuotaPoints quota mặc định của mỗi khung, tính bằng điểm công việc
 */
public record StoreSettings(LocalTime openTime, LocalTime closeTime, int defaultQuotaPoints) {}
