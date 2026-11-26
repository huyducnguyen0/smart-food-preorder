package vn.nhom15.preorder.order;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.auth.Role;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.OrderingProperties;
import vn.nhom15.preorder.slot.QuotaService;

/**
 * Các chuyển trạng thái ngoại lệ (SRS B.5): khách hủy (UC-06, BR-23), cửa hàng hủy kèm lý do (UC-11, BR-24), đánh dấu
 * không đến nhận (UC-10, BR-25). Mỗi thao tác khóa dòng đơn, kiểm tra quy tắc, đổi trạng thái và hoàn quota (nếu có)
 * trong cùng một giao dịch.
 */
@Service
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final QuotaService quotaService;
    private final OrderingProperties properties;
    private final Clock clock;

    public OrderCancellationService(
            OrderRepository orderRepository, QuotaService quotaService, OrderingProperties properties, Clock clock) {
        this.orderRepository = orderRepository;
        this.quotaService = quotaService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * UC-06: khách hủy đơn của mình khi còn {@code CONFIRMED} và chưa vào cửa sổ chuẩn bị; hoàn đủ quota (BR-23).
     *
     * @throws ApiException {@code ORDER_NOT_FOUND} nếu không có đơn hoặc không phải của khách;
     *     {@code INVALID_STATUS_TRANSITION} nếu đơn không còn {@code CONFIRMED}; {@code CANCEL_NOT_ALLOWED} nếu đã vào
     *     cửa sổ chuẩn bị
     */
    @Transactional
    public OrderStatus cancelByCustomer(long orderId, long customerId) {
        OrderRepository.LockedOrder order = orderRepository
                .lockForChange(orderId)
                .filter(found -> found.customerId() != null && found.customerId() == customerId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn"));
        requireStatus(order, OrderStatus.CONFIRMED);
        if (order.paid()) {
            // Cửa hàng đã thu tiền: hủy phải qua cửa hàng (kèm lý do) để cửa hàng biết mà hoàn tiền (BR-23, BR-30).
            throw new ApiException(
                            ErrorCode.CANCEL_NOT_ALLOWED,
                            "Đơn đã thanh toán nên không tự hủy được, vui lòng liên hệ cửa hàng để hủy và hoàn tiền")
                    .with("reason", "PAID");
        }
        LocalDateTime windowStart = order.slotStart().minusMinutes(properties.prepWindowMinutes());
        if (!LocalDateTime.now(clock).isBefore(windowStart)) {
            throw new ApiException(
                            ErrorCode.CANCEL_NOT_ALLOWED,
                            "Đơn đã vào thời gian chuẩn bị nên không tự hủy được, vui lòng liên hệ cửa hàng")
                    .with("reason", "PREPARATION_WINDOW")
                    .with("cancelDeadline", windowStart.toString());
        }
        orderRepository.markCancelled(orderId, customerId, null, OffsetDateTime.now(clock));
        quotaService.release(order.timeSlotId(), order.workloadPoints());
        return OrderStatus.CANCELLED;
    }

    /**
     * UC-11: cửa hàng hủy kèm lý do (BR-24). Quản lý hủy được đơn {@code CONFIRMED} hoặc {@code PREPARING}; nhân viên
     * quầy chỉ hủy được đơn walk-in {@code CONFIRMED}. Hoàn quota khi hủy từ {@code CONFIRMED}, không hoàn khi từ
     * {@code PREPARING}.
     *
     * @throws ApiException {@code ORDER_NOT_FOUND}; {@code CANCEL_NOT_ALLOWED} nếu quầy hủy đơn pre-order;
     *     {@code INVALID_STATUS_TRANSITION} nếu trạng thái không cho hủy
     */
    @Transactional
    public OrderStatus cancelByStore(long orderId, long staffId, Role role, String reason) {
        OrderRepository.LockedOrder order = orderRepository
                .lockForChange(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn"));
        if (role == Role.COUNTER) {
            if (order.source() != OrderSource.WALK_IN) {
                throw new ApiException(
                        ErrorCode.CANCEL_NOT_ALLOWED,
                        "Nhân viên quầy chỉ hủy được đơn walk-in; đơn này cần quản lý hủy");
            }
            requireStatus(order, OrderStatus.CONFIRMED);
        } else if (order.status() != OrderStatus.CONFIRMED && order.status() != OrderStatus.PREPARING) {
            throw invalid(order.status(), "Chỉ hủy được đơn đã xác nhận hoặc đang chuẩn bị");
        }
        orderRepository.markCancelled(orderId, staffId, reason.strip(), OffsetDateTime.now(clock));
        if (order.status() == OrderStatus.CONFIRMED) {
            quotaService.release(order.timeSlotId(), order.workloadPoints());
        }
        return OrderStatus.CANCELLED;
    }

    /**
     * UC-10: đánh dấu đơn sẵn sàng không đến nhận khi đã quá giờ kết thúc khung cộng thời gian chờ; không hoàn quota
     * (BR-25, BR-14).
     *
     * @throws ApiException {@code ORDER_NOT_FOUND}; {@code INVALID_STATUS_TRANSITION} nếu không ở {@code READY};
     *     {@code NO_SHOW_TOO_EARLY} nếu chưa quá hạn
     */
    @Transactional
    public OrderStatus markNoShow(long orderId, long staffId) {
        OrderRepository.LockedOrder order = orderRepository
                .lockForChange(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn"));
        requireStatus(order, OrderStatus.READY);
        LocalDateTime allowedFrom = noShowAllowedFrom(order.slotStart());
        if (LocalDateTime.now(clock).isBefore(allowedFrom)) {
            throw new ApiException(
                            ErrorCode.NO_SHOW_TOO_EARLY,
                            "Chưa quá hạn nhận, chỉ đánh dấu không đến nhận được từ " + allowedFrom.toLocalTime())
                    .with("allowedFrom", allowedFrom.toString());
        }
        orderRepository.markNoShow(orderId, staffId, OffsetDateTime.now(clock));
        return OrderStatus.NO_SHOW;
    }

    /** Thời điểm được đánh dấu không đến nhận: giờ kết thúc khung cộng thời gian chờ (BR-25). */
    public LocalDateTime noShowAllowedFrom(LocalDateTime slotStart) {
        return slotStart.plusMinutes(properties.slotMinutes()).plusMinutes(properties.noShowGraceMinutes());
    }

    private static void requireStatus(OrderRepository.LockedOrder order, OrderStatus required) {
        if (order.status() != required) {
            throw invalid(order.status(), "Đơn đang ở trạng thái " + order.status() + ", cần " + required);
        }
    }

    private static ApiException invalid(OrderStatus current, String message) {
        return new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, message).with("currentStatus", current);
    }
}
