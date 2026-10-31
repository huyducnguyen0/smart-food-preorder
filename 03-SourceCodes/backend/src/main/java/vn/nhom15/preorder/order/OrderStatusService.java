package vn.nhom15.preorder.order;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.OrderingProperties;

/**
 * Các chuyển trạng thái theo tiến độ (SRS B.5): bếp {@code CONFIRMED → PREPARING → READY}, quầy bàn giao
 * {@code READY → PICKED_UP}. Các chuyển ngoại lệ (hủy, không đến nhận) ở {@link OrderCancellationService}; cả hai đều
 * thuộc module order (Architecture mục 2). Cập nhật theo cả đơn, không theo từng món (BR-22). Quota vẫn giữ sau khi
 * chuyển (BR-14).
 */
@Service
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderingProperties properties;
    private final Clock clock;

    public OrderStatusService(OrderRepository orderRepository, OrderingProperties properties, Clock clock) {
        this.orderRepository = orderRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Bếp bắt đầu làm đơn (UC-12 bước 4–5). Chỉ khi đơn đã vào thời gian chuẩn bị (BR-21 sửa đổi 2026-10-31): bắt đầu
     * sớm hơn sẽ lấy mất quyền tự hủy của khách (BR-23). Khóa dòng đơn để không chen ngang lần hủy cùng lúc.
     *
     * @throws ApiException {@code ORDER_NOT_FOUND} nếu không có đơn; {@code INVALID_STATUS_TRANSITION} nếu đơn không
     *     còn ở {@code CONFIRMED} (UC-12 E1); {@code PREPARATION_TOO_EARLY} nếu chưa tới thời gian chuẩn bị
     */
    @Transactional
    public OrderStatus startPreparing(long orderId, long staffId) {
        OrderRepository.LockedOrder order = orderRepository
                .lockForChange(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn"));
        if (order.status() != OrderStatus.CONFIRMED) {
            throw new ApiException(
                            ErrorCode.INVALID_STATUS_TRANSITION,
                            "Đơn đang ở trạng thái " + order.status() + ", cần CONFIRMED mới chuyển được")
                    .with("currentStatus", order.status());
        }
        LocalDateTime allowedFrom = preparationStartsAt(order.slotStart());
        if (LocalDateTime.now(clock).isBefore(allowedFrom)) {
            throw new ApiException(
                            ErrorCode.PREPARATION_TOO_EARLY,
                            "Chưa tới thời gian chuẩn bị, bắt đầu được từ " + allowedFrom.toLocalTime())
                    .with("allowedFrom", allowedFrom.toString());
        }
        orderRepository.markPreparing(orderId, staffId, OffsetDateTime.now(clock));
        return OrderStatus.PREPARING;
    }

    /** Thời điểm đơn vào thời gian chuẩn bị: giờ bắt đầu khung trừ cửa sổ chuẩn bị (BR-21). */
    public LocalDateTime preparationStartsAt(LocalDateTime slotStart) {
        return slotStart.minusMinutes(properties.prepWindowMinutes());
    }

    /**
     * Bếp làm xong cả đơn (UC-12 bước 6–7).
     *
     * @throws ApiException {@code ORDER_NOT_FOUND} nếu không có đơn;
     *     {@code INVALID_STATUS_TRANSITION} nếu đơn không còn
     *     ở {@code PREPARING} (UC-12 E1)
     */
    @Transactional
    public OrderStatus markReady(long orderId, long staffId) {
        if (orderRepository.markReady(orderId, staffId, OffsetDateTime.now(clock))) {
            return OrderStatus.READY;
        }
        throw rejection(orderId, OrderStatus.PREPARING);
    }

    /**
     * Quầy bàn giao đơn cho khách (UC-09): đơn phải sẵn sàng, đúng mã nhận và đã thanh toán (BR-29). Mã so không phân
     * biệt chữ hoa thường và bỏ khoảng trắng hai đầu vì quầy gõ lại từ lời khách đọc.
     *
     * @throws ApiException {@code ORDER_NOT_FOUND}; {@code INVALID_STATUS_TRANSITION} nếu đơn không ở {@code READY}
     *     (kể cả đơn đã bàn giao); {@code PICKUP_CODE_MISMATCH}; {@code ORDER_NOT_PAID}
     */
    @Transactional
    public OrderStatus handOver(long orderId, String pickupCode, long staffId) {
        String code = pickupCode == null ? "" : pickupCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (orderRepository.markPickedUp(orderId, code, staffId, OffsetDateTime.now(clock))) {
            return OrderStatus.PICKED_UP;
        }
        OrderRepository.HandoverState state = orderRepository
                .findHandoverState(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn"));
        if (state.status() != OrderStatus.READY) {
            throw new ApiException(
                            ErrorCode.INVALID_STATUS_TRANSITION,
                            "Đơn đang ở trạng thái " + state.status() + ", chỉ đơn READY mới bàn giao được")
                    .with("currentStatus", state.status());
        }
        if (!state.pickupCode().trim().equals(code)) {
            throw new ApiException(ErrorCode.PICKUP_CODE_MISMATCH, "Mã nhận không khớp với đơn");
        }
        throw new ApiException(ErrorCode.ORDER_NOT_PAID, "Đơn chưa thanh toán, ghi nhận thanh toán trước khi bàn giao");
    }

    /** Không đổi được dòng nào: hoặc không có đơn, hoặc trạng thái hiện tại không phải trạng thái nguồn. */
    private ApiException rejection(long orderId, OrderStatus required) {
        OrderStatus current = orderRepository
                .findStatus(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn"));
        return new ApiException(
                        ErrorCode.INVALID_STATUS_TRANSITION,
                        "Đơn đang ở trạng thái " + current + ", cần " + required + " mới chuyển được")
                .with("currentStatus", current);
    }
}
