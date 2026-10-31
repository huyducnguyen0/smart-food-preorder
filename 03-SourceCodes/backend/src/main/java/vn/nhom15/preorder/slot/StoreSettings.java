package vn.nhom15.preorder.slot;

import java.time.LocalTime;

/**
 * Cấu hình cửa hàng (BR-06, BR-07, PB-22).
 *
 * @param openTime giờ mở cửa
 * @param closeTime giờ đóng cửa; khung nhận cuối kết thúc không muộn hơn giờ này
 * @param defaultQuotaPoints công suất bếp mặc định (điểm công việc) của mỗi khoảng làm 15 phút
 * @param holdMinutes làm trước tối đa bao lâu so với giờ nhận (món còn ngon); cửa sổ làm của đơn đặt trước
 * @param preOrderSharePercent phần trăm công suất mỗi khoảng làm mà đơn đặt trước được dùng (phần còn lại cho walk-in)
 * @param pickupLimitOrders số đơn tối đa trong một khung nhận (quầy)
 */
public record StoreSettings(
        LocalTime openTime,
        LocalTime closeTime,
        int defaultQuotaPoints,
        int holdMinutes,
        int preOrderSharePercent,
        int pickupLimitOrders) {}
