package vn.nhom15.preorder.order;

import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.OrderingProperties;

/** UC-05 Xem đơn của tôi: chỉ đơn của khách đang đăng nhập (BR-33, NFR-03). */
@Service
public class MyOrderService {

    private final OrderRepository orderRepository;
    private final OrderingProperties properties;
    private final Clock clock;

    public MyOrderService(OrderRepository orderRepository, OrderingProperties properties, Clock clock) {
        this.orderRepository = orderRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> listOrders(long customerId) {
        return orderRepository.findSummaries(customerId, properties.slotMinutes());
    }

    /**
     * Chi tiết một đơn của khách.
     *
     * @throws ApiException {@code ORDER_NOT_FOUND} nếu đơn không có hoặc không phải của khách; hai trường hợp trả cùng
     *     một kết quả để không lộ đơn đó có thật hay không (UC-05 E1)
     */
    @Transactional(readOnly = true)
    public OrderDetail getOrder(long customerId, long orderId) {
        return orderRepository
                .findDetail(orderId, customerId, properties.slotMinutes())
                .map(order -> order.inZone(clock.getZone(), properties.prepWindowMinutes()))
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn"));
    }
}
