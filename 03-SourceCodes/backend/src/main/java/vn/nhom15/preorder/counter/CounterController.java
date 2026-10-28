package vn.nhom15.preorder.counter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nhom15.preorder.auth.CurrentUser;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.order.OrderDetail;
import vn.nhom15.preorder.order.OrderPaymentService;
import vn.nhom15.preorder.order.OrderStatus;
import vn.nhom15.preorder.order.OrderStatusService;
import vn.nhom15.preorder.order.PaymentMethod;
import vn.nhom15.preorder.order.PlaceOrderResult;
import vn.nhom15.preorder.order.RequestedLine;
import vn.nhom15.preorder.order.WalkInCommand;
import vn.nhom15.preorder.order.WalkInService;

/** Thao tác của quầy: xem đơn đang xử lý và ghi nhận thanh toán. Quầy và quản lý (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/counter")
public class CounterController {

    /** Nội dung ghi nhận thanh toán. */
    public record PaymentRequest(@NotNull PaymentMethod method) {}

    /** Mã nhận khách đọc ra khi đến lấy. */
    public record HandoverRequest(@NotBlank String pickupCode) {}

    /** Kết quả bàn giao. */
    public record HandoverResult(long id, OrderStatus status) {}

    /** Món nhân viên đang nhập cho khách walk-in. */
    public record EstimateRequest(@NotEmpty List<@Valid @NotNull RequestedLine> lines) {}

    /** Kết quả ghi nhận thanh toán. */
    public record PaymentResult(long id, String paymentStatus, PaymentMethod paymentMethod) {}

    private final CounterService counterService;
    private final OrderPaymentService paymentService;
    private final OrderStatusService orderStatusService;
    private final WalkInService walkInService;

    public CounterController(
            CounterService counterService,
            OrderPaymentService paymentService,
            OrderStatusService orderStatusService,
            WalkInService walkInService) {
        this.counterService = counterService;
        this.paymentService = paymentService;
        this.orderStatusService = orderStatusService;
        this.walkInService = walkInService;
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

    /** UC-09: bàn giao đơn sẵn sàng, đúng mã nhận và đã thanh toán (BR-29). */
    @PostMapping("/orders/{id}/handover")
    public HandoverResult handOver(
            @PathVariable long id,
            @Valid @RequestBody HandoverRequest request,
            @AuthenticationPrincipal CurrentUser user) {
        return new HandoverResult(id, orderStatusService.handOver(id, request.pickupCode(), user.id()));
    }

    /** UC-07 bước 2–3: khung dự kiến sớm nhất để báo khách (BR-18); chưa giữ quota. */
    @PostMapping("/walk-in/estimate")
    public WalkInService.WalkInEstimate estimate(@Valid @RequestBody EstimateRequest request) {
        return walkInService.estimate(request.lines());
    }

    /**
     * UC-07 bước 5–7: xác nhận đơn walk-in khách đã đồng ý. 201 với đơn mới, 200 khi gửi lại cùng mã yêu cầu. Khung vừa
     * hết chỗ thì trả {@code SLOT_FULL} kèm {@code nextSlot} để báo lại khách (BR-19).
     */
    @PostMapping("/walk-in")
    public ResponseEntity<OrderDetail> confirmWalkIn(
            @Valid @RequestBody WalkInCommand command, @AuthenticationPrincipal CurrentUser user) {
        PlaceOrderResult result;
        try {
            result = walkInService.confirm(user.id(), command);
        } catch (ApiException e) {
            // Giao dịch đặt đã hủy; tính khung mới sau khi hủy để thấy đúng quota hiện tại.
            if (e.code() == ErrorCode.SLOT_FULL) {
                walkInService.nextSlotAfterFull(command.lines()).ifPresent(slot -> e.with("nextSlot", slot));
            }
            throw e;
        }
        if (result.created()) {
            return ResponseEntity.created(
                            URI.create("/api/counter/orders/" + result.order().id()))
                    .body(result.order());
        }
        return ResponseEntity.ok(result.order());
    }
}
