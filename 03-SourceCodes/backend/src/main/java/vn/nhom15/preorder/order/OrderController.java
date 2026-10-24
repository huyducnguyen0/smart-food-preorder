package vn.nhom15.preorder.order;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nhom15.preorder.auth.CurrentUser;

/** Đặt đơn của khách (UC-04 bước 7–11). Chỉ vai trò khách (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderService placeOrderService;

    public OrderController(PlaceOrderService placeOrderService) {
        this.placeOrderService = placeOrderService;
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
}
