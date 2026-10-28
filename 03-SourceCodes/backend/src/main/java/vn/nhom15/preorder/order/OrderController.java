package vn.nhom15.preorder.order;

import jakarta.validation.Valid;
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
import vn.nhom15.preorder.slot.CartLine;
import vn.nhom15.preorder.slot.SlotAvailabilityService;

/** Đặt đơn (UC-04 bước 7–11) và xem đơn của khách (UC-05). Chỉ vai trò khách (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderService placeOrderService;
    private final MyOrderService myOrderService;
    private final SlotAvailabilityService slotAvailabilityService;
    private final OrderCancellationService cancellationService;

    public OrderController(
            PlaceOrderService placeOrderService,
            MyOrderService myOrderService,
            SlotAvailabilityService slotAvailabilityService,
            OrderCancellationService cancellationService) {
        this.placeOrderService = placeOrderService;
        this.myOrderService = myOrderService;
        this.slotAvailabilityService = slotAvailabilityService;
        this.cancellationService = cancellationService;
    }

    /**
     * 201 với đơn mới, hoặc 200 với đơn cũ khi mã yêu cầu đã có đơn (A1). Khung vừa hết chỗ thì lỗi {@code SLOT_FULL}
     * kèm {@code alternatives}: tối đa 3 khung thay thế (UC-18, BR-16).
     */
    @PostMapping
    public ResponseEntity<OrderDetail> placeOrder(
            @Valid @RequestBody PlaceOrderCommand command, @AuthenticationPrincipal CurrentUser user) {
        PlaceOrderResult result;
        try {
            result = placeOrderService.placeOrder(user.id(), command);
        } catch (ApiException e) {
            // Tính đề xuất sau khi giao dịch đặt đã hủy, trên quota hiện tại.
            if (e.code() == ErrorCode.SLOT_FULL) {
                List<CartLine> cart = command.lines().stream()
                        .map(line -> new CartLine(line.menuItemId(), line.quantity()))
                        .toList();
                e.with("alternatives", slotAvailabilityService.alternatives(command.slotStart(), cart));
            }
            throw e;
        }
        if (result.created()) {
            return ResponseEntity.created(
                            URI.create("/api/orders/" + result.order().id()))
                    .body(result.order());
        }
        return ResponseEntity.ok(result.order());
    }

    /** UC-05 bước 1–2: đơn của khách đang đăng nhập. */
    @GetMapping
    public Map<String, List<OrderSummary>> listMyOrders(@AuthenticationPrincipal CurrentUser user) {
        return Map.of("orders", myOrderService.listOrders(user.id()));
    }

    /** UC-05 bước 3–4: chi tiết một đơn của mình; đơn của người khác trả 404 như đơn không tồn tại (E1). */
    @GetMapping("/{id}")
    public OrderDetail getMyOrder(@PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
        return myOrderService.getOrder(user.id(), id);
    }

    /** Kết quả đổi trạng thái. */
    public record StatusResult(long id, OrderStatus status) {}

    /** UC-06: khách hủy đơn của mình khi còn được hủy (BR-23). */
    @PostMapping("/{id}/cancel")
    public StatusResult cancel(@PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
        return new StatusResult(id, cancellationService.cancelByCustomer(id, user.id()));
    }
}
