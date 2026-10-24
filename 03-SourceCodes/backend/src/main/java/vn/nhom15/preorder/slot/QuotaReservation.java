package vn.nhom15.preorder.slot;

/**
 * Kết quả giữ quota thành công, để use case gọi lưu vào đơn.
 *
 * @param timeSlotId khung đã giữ (dòng trong {@code time_slots})
 * @param points điểm đơn đã giữ; 0 nếu đơn không chiếm quota (BR-13)
 */
public record QuotaReservation(long timeSlotId, int points) {}
