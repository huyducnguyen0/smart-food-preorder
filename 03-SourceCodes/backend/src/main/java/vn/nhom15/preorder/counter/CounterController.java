package vn.nhom15.preorder.counter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nhom15.preorder.auth.CurrentUser;
import vn.nhom15.preorder.order.OrderPaymentService;
import vn.nhom15.preorder.order.PaymentMethod;

/** Thao tác của quầy: xem đơn đang xử lý và ghi nhận thanh toán. Quầy và quản lý (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/counter")
public class CounterController {

    /** Nội dung ghi nhận thanh toán. */
    public record PaymentRequest(@NotNull PaymentMethod method) {}

    /** Kết quả ghi nhận thanh toán. */
    public record PaymentResult(long id, String paymentStatus, PaymentMethod paymentMethod) {}

    private final CounterService counterService;
    private final OrderPaymentService paymentService;

    public CounterController(CounterService counterService, OrderPaymentService paymentService) {
        this.counterService = counterService;
        this.paymentService = paymentService;
    }

    /** Đơn hôm nay đang xử lý, để quầy thu tiền và giao món. */
    @GetMapping("/orders")
    public Map<String, List<CounterOrder>> orders() {
        return Map.of("orders", counterService.listOpenOrders());
    }

    /** UC-08: ghi nhận đơn đã thanh toán kèm phương thức. */
    @PostMapping("/orders/{id}/payment")
    public PaymentResult pay(
            @PathVariable long id,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CurrentUser user) {
        PaymentMethod method = paymentService.recordPayment(id, request.method(), user.id());
        return new PaymentResult(id, "PAID", method);
    }
}
