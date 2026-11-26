package vn.nhom15.preorder.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Nhân viên quầy xác nhận đơn walk-in sau khi khách đồng ý (UC-07).
 *
 * @param requestId mã yêu cầu sinh khi mở màn tạo đơn, giữ nguyên khi gửi lại (NFR-02)
 * @param slotStart khung đã báo khách và khách đồng ý
 * @param lines món, số lượng và giá đang hiển thị
 * @param paymentMethod phương thức khách đã trả khi đặt (BR-20, BR-27)
 */
public record WalkInCommand(
        @NotNull UUID requestId,
        @NotNull LocalDateTime slotStart,
        @NotEmpty List<@Valid @NotNull RequestedLine> lines,
        @NotNull PaymentMethod paymentMethod) {}
