package vn.nhom15.preorder.counter;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import vn.nhom15.preorder.order.OrderSource;
import vn.nhom15.preorder.order.OrderStatus;
import vn.nhom15.preorder.order.PaymentMethod;

/**
 * Một đơn trong danh sách của quầy: đủ để nhận diện khách, thu tiền và giao đúng đơn.
 *
 * @param id mã đơn
 * @param pickupCode mã nhận khách đọc ra khi đến lấy
 * @param customerName tên khách, null với đơn walk-in
 * @param slotStart giờ bắt đầu khung nhận
 * @param slotEnd giờ kết thúc khung nhận
 * @param source nguồn đơn
 * @param status trạng thái đơn
 * @param paymentStatus {@code UNPAID} hoặc {@code PAID}
 * @param paymentMethod phương thức nếu đã thanh toán, ngược lại null
 * @param totalAmount số tiền cần thu
 * @param lines các món và số lượng
 * @param overdue đơn sẵn sàng đã quá giờ kết thúc khung cộng thời gian chờ, được đánh dấu không đến nhận (BR-25)
 */
public record CounterOrder(
        long id,
        String pickupCode,
        String customerName,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotStart,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotEnd,
        OrderSource source,
        OrderStatus status,
        String paymentStatus,
        PaymentMethod paymentMethod,
        int totalAmount,
        List<Line> lines,
        boolean overdue) {

    /** Một món và số lượng. */
    public record Line(String name, int quantity) {}
}
