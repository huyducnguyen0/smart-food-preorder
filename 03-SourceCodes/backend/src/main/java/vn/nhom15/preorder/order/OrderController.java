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

/** Đặt đơn (UC-04 bước 7–11) và xem đơn của khách (UC-05). Chỉ vai trò khách (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderService placeOrderService;
    private final MyOrderService myOrderService;

    public OrderController(PlaceOrderService placeOrderService, MyOrderService myOrderService) {
        this.placeOrderService = placeOrderService;
        this.myOrderService = myOrderService;
    }

    /** 201 với đơn mới, hoặc 200 với đơn cũ khi mã yêu cầu đã có đơn (A1). */
    @PostMapping
    public ResponseEntity<OrderDetail> placeOrder(
            @Valid @RequestBody PlaceOrderCommand command, @AuthenticationPrincipal CurrentUser user) {
        PlaceOrderResult result = placeOrderService.placeOrder(user.id(), command);
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
}
