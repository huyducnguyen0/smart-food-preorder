package vn.nhom15.preorder.order;

import java.time.LocalDateTime;

/**
 * BR-26 Đơn trễ: đơn {@code CONFIRMED} hoặc {@code PREPARING} đã qua mốc mà vẫn chưa sẵn sàng. Pre-order trễ từ giờ
 * bắt đầu khung (khách có thể tới từ lúc đó). Walk-in trễ từ giờ kết thúc khung, vì walk-in được xếp vào khung đang
 * diễn ra nên tính từ giờ bắt đầu thì đơn vừa tạo đã "trễ" (quyết định 2026-10-28). Chỉ để hiển thị, không chặn thao
 * tác. Hàng chờ bếp và màn theo dõi dùng chung quy tắc này.
 */
public final class LateRule {

    private LateRule() {}

    /**
     * @param status trạng thái hiện tại; chỉ {@code CONFIRMED} và {@code PREPARING} có thể trễ
     * @param source nguồn đơn
     * @param slotStart giờ bắt đầu khung nhận
     * @param slotEnd giờ kết thúc khung nhận
     * @param now giờ hiện tại theo giờ cửa hàng; đúng bằng mốc là đã trễ
     */
    public static boolean isLate(
            OrderStatus status, OrderSource source, LocalDateTime slotStart, LocalDateTime slotEnd, LocalDateTime now) {
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.PREPARING) {
            return false;
        }
        LocalDateTime from = source == OrderSource.WALK_IN ? slotEnd : slotStart;
        return !now.isBefore(from);
    }
}
