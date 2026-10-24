package vn.nhom15.preorder.kitchen;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import vn.nhom15.preorder.order.OrderSource;

/**
 * Một đơn trong hàng chờ bếp. Không có giá và trạng thái thanh toán vì bếp không cần cho việc chế biến (UC-12 Ghi
 * chú).
 *
 * @param id mã đơn
 * @param pickupCode mã nhận
 * @param slotStart giờ bắt đầu khung nhận
 * @param slotEnd giờ kết thúc khung nhận
 * @param source nguồn đơn
 * @param confirmedAt thời điểm xác nhận
 * @param late đã qua giờ bắt đầu khung mà chưa xong (BR-26); chỉ để hiển thị
 * @param lines các món cần làm
 */
public record KitchenOrder(
        long id,
        String pickupCode,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotStart,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotEnd,
        OrderSource source,
        OffsetDateTime confirmedAt,
        boolean late,
        List<Line> lines) {

    /** Một món và số lượng cần làm. */
    public record Line(String name, int quantity) {}
}
