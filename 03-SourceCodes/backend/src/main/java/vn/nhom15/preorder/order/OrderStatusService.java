package vn.nhom15.preorder.order;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * Nơi duy nhất đổi trạng thái đơn của bếp (Architecture mục 2). Chỉ cài hai bước SRS B.5 mà bếp được làm:
 * {@code CONFIRMED → PREPARING} và {@code PREPARING → READY}; mọi chuyển khác bị từ chối vì không có câu lệnh nào cho
 * phép. Cập nhật theo cả đơn, không theo từng món (BR-22). Quota vẫn giữ sau khi chuyển (BR-14).
 */
@Service
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderStatusService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    /**
     * Bếp bắt đầu làm đơn (UC-12 bước 4–5), kể cả đơn sắp tới (BR-21).
     *
     * @throws ApiException {@code ORDER_NOT_FOUND} nếu không có đơn;
     *     {@code INVALID_STATUS_TRANSITION} nếu đơn không còn
     *     ở {@code CONFIRMED} (UC-12 E1)
     */
    @Transactional
    public OrderStatus startPreparing(long orderId, long staffId) {
        if (orderRepository.markPreparing(orderId, staffId, OffsetDateTime.now(clock))) {
            return OrderStatus.PREPARING;
        }
        throw rejection(orderId, OrderStatus.CONFIRMED);
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
