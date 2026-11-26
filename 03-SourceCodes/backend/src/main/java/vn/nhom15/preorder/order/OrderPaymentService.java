package vn.nhom15.preorder.order;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * UC-08 Ghi nhận thanh toán: quầy chuyển {@code UNPAID → PAID} kèm phương thức (BR-27). Chỉ ghi nhận, không xử lý tiền
 * và không hoàn tiền (BR-30). Module order là nơi duy nhất ghi bảng đơn.
 */
@Service
public class OrderPaymentService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderPaymentService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    /**
     * Ghi nhận khách đã trả tiền.
     *
     * @throws ApiException {@code ORDER_NOT_FOUND} nếu không có đơn; {@code ALREADY_PAID} nếu đã ghi nhận;
     *     {@code ORDER_NOT_PAYABLE} nếu đơn đã bàn giao, hủy hoặc vắng mặt
     */
    @Transactional
    public PaymentMethod recordPayment(long orderId, PaymentMethod method, long staffId) {
        if (orderRepository.markPaid(orderId, method, staffId, OffsetDateTime.now(clock))) {
            return method;
        }
        OrderRepository.PaymentState state = orderRepository
                .findPaymentState(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn"));
        if ("PAID".equals(state.paymentStatus())) {
            throw new ApiException(ErrorCode.ALREADY_PAID, "Đơn này đã được ghi nhận thanh toán");
        }
        throw new ApiException(
                        ErrorCode.ORDER_NOT_PAYABLE,
                        "Đơn đang ở trạng thái " + state.status() + ", không ghi nhận thanh toán được")
                .with("currentStatus", state.status());
    }
}
