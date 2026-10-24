package vn.nhom15.preorder.slot;

import java.time.LocalDate;
import java.util.List;

/**
 * Kết quả liệt kê khung giờ (UC-04 bước 6).
 *
 * @param date ngày đang liệt kê, tức hôm nay theo giờ cửa hàng
 * @param slots các khung hợp lệ theo giờ mở cửa và thời gian đặt trước, kể cả khung không đủ chỗ
 * @param unavailableReason lý do khi không chọn được khung nào, null nếu có ít nhất một khung đặt được
 */
public record SlotList(LocalDate date, List<SlotView> slots, UnavailableReason unavailableReason) {

    /** Lý do không chọn được khung nào (UC-04 E2, E3, E4). */
    public enum UnavailableReason {
        NO_SLOT_LEFT_TODAY,
        ALL_SLOTS_FULL,
        ORDER_TOO_LARGE
    }
}
