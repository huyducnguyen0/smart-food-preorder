package vn.nhom15.preorder.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Một dòng của danh sách "Đơn của tôi" (UC-05 bước 2).
 *
 * @param id mã đơn
 * @param pickupCode mã nhận
 * @param slotStart giờ bắt đầu khung nhận (có cả ngày)
 * @param slotEnd giờ kết thúc khung nhận
 * @param status trạng thái
 * @param paymentStatus trạng thái thanh toán
 * @param totalAmount tổng tiền
 */
public record OrderSummary(
        long id,
        String pickupCode,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotStart,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotEnd,
        OrderStatus status,
        String paymentStatus,
        int totalAmount) {}
