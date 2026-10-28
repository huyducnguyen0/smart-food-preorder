package vn.nhom15.preorder.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Đơn sắp chèn vào database, đã qua mọi bước kiểm tra. Trạng thái ban đầu luôn là {@code CONFIRMED} và thanh toán
 * {@code UNPAID} (BR-28), nên không nằm ở đây.
 *
 * @param customerId khách đặt, null với đơn walk-in
 * @param source nguồn đơn
 * @param timeSlotId khung nhận
 * @param workloadPoints điểm đơn lưu lúc xác nhận (BR-08)
 * @param totalAmount tổng tiền
 * @param pickupCode mã nhận
 * @param requestId mã yêu cầu (BR-17)
 * @param confirmedAt thời điểm xác nhận
 * @param lines các dòng đơn
 * @param createdBy nhân viên tạo đơn walk-in; null với pre-order
 * @param paidWith phương thức đã thu khi tạo đơn (walk-in trả khi đặt, BR-20); null nếu chưa thanh toán
 */
public record NewOrder(
        Long customerId,
        OrderSource source,
        long timeSlotId,
        int workloadPoints,
        int totalAmount,
        String pickupCode,
        UUID requestId,
        OffsetDateTime confirmedAt,
        List<NewOrderLine> lines,
        Long createdBy,
        PaymentMethod paidWith) {

    /** Một dòng đơn sắp chèn. */
    public record NewOrderLine(long menuItemId, String itemName, int unitPrice, int quantity) {}
}
