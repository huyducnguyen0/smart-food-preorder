package vn.nhom15.preorder.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Yêu cầu đặt đơn (UC-04 bước 7).
 *
 * @param requestId mã yêu cầu do frontend sinh khi khách vào bước xác nhận, giữ nguyên khi gửi lại (A1, BR-17)
 * @param slotStart giờ bắt đầu khung đã chọn, theo giờ cửa hàng
 * @param lines các dòng của giỏ
 */
public record PlaceOrderCommand(
        @NotNull UUID requestId,
        @NotNull LocalDateTime slotStart,
        @NotEmpty List<@Valid @NotNull RequestedLine> lines) {}
