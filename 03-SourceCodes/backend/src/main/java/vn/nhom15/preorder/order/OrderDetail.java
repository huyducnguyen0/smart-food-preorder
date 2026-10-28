package vn.nhom15.preorder.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Chi tiết một đơn trả cho khách. Không có điểm công việc hay quota (BR-36).
 *
 * @param id mã đơn
 * @param pickupCode mã nhận
 * @param slotStart giờ bắt đầu khung nhận
 * @param slotEnd giờ kết thúc khung nhận
 * @param status trạng thái
 * @param paymentStatus trạng thái thanh toán
 * @param paymentMethod phương thức thanh toán, null nếu chưa thanh toán
 * @param totalAmount tổng tiền
 * @param confirmedAt thời điểm xác nhận
 * @param lines các dòng đơn
 * @param cancelReason lý do cửa hàng hủy đơn (BR-24); null nếu không bị cửa hàng hủy
 */
public record OrderDetail(
        long id,
        String pickupCode,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotStart,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotEnd,
        OrderStatus status,
        String paymentStatus,
        String paymentMethod,
        int totalAmount,
        OffsetDateTime confirmedAt,
        List<OrderLineView> lines,
        String cancelReason) {

    /** Bản sao với thời điểm xác nhận biểu diễn theo múi giờ của cửa hàng (cùng thời điểm, khác cách viết). */
    public OrderDetail inZone(ZoneId zone) {
        return new OrderDetail(
                id,
                pickupCode,
                slotStart,
                slotEnd,
                status,
                paymentStatus,
                paymentMethod,
                totalAmount,
                confirmedAt.atZoneSameInstant(zone).toOffsetDateTime(),
                lines,
                cancelReason);
    }
}
