package vn.nhom15.preorder.kitchen;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Hàng chờ bếp (UC-12). Chỉ vai trò bếp (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    /** UC-12.1: các đơn hôm nay, ba nhóm. */
    @GetMapping("/queue")
    public KitchenQueue queue() {
        return kitchenService.getQueue();
    }
}
