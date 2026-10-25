package vn.nhom15.preorder.kitchen;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.nhom15.preorder.auth.CurrentUser;
import vn.nhom15.preorder.order.OrderStatusService;

/** Hàng chờ bếp (UC-12). Chỉ vai trò bếp (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;
    private final OrderStatusService orderStatusService;

    public KitchenController(KitchenService kitchenService, OrderStatusService orderStatusService) {
        this.kitchenService = kitchenService;
        this.orderStatusService = orderStatusService;
    }

    /** UC-12.1: các đơn hôm nay, ba nhóm. */
    @GetMapping("/queue")
    public KitchenQueue queue() {
        return kitchenService.getQueue();
    }

    /** UC-12.2 bước 4–5: {@code CONFIRMED → PREPARING}. */
    @PostMapping("/orders/{id}/start")
    public StatusChange start(@PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
        return new StatusChange(id, orderStatusService.startPreparing(id, user.id()));
    }

    /** UC-12.2 bước 6–7: {@code PREPARING → READY}. */
    @PostMapping("/orders/{id}/complete")
    public StatusChange complete(@PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
        return new StatusChange(id, orderStatusService.markReady(id, user.id()));
    }
}
