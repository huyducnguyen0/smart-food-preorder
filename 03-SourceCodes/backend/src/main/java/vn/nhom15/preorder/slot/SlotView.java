package vn.nhom15.preorder.slot;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Một khung giờ trong danh sách cho khách chọn. Không có quota hay điểm: khách chỉ biết đặt được hay không (BR-36).
 *
 * @param start giờ bắt đầu khung
 * @param end giờ kết thúc khung
 * @param bookable đặt được với giỏ hiện tại tại thời điểm hiển thị
 */
public record SlotView(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime start,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime end,
        boolean bookable) {}
