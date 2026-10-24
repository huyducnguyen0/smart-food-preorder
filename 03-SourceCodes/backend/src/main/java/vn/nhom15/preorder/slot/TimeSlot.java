package vn.nhom15.preorder.slot;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Một khung giờ đã có dòng trong bảng {@code time_slots}.
 *
 * @param id khóa chính
 * @param slotDate ngày theo giờ cửa hàng
 * @param startTime giờ bắt đầu khung
 * @param quotaOverride quota riêng của khung; null nghĩa là dùng quota mặc định (BR-07)
 * @param heldPoints tổng điểm các đơn đang giữ quota của khung (BR-09, BR-14)
 */
public record TimeSlot(long id, LocalDate slotDate, LocalTime startTime, Integer quotaOverride, int heldPoints) {

    /** Quota hiệu lực của khung: quota riêng nếu có, ngược lại quota mặc định. */
    public int quota(int defaultQuota) {
        return quotaOverride != null ? quotaOverride : defaultQuota;
    }
}
