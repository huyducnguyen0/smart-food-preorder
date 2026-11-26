package vn.nhom15.preorder.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Theo dõi vận hành trong ngày (PB-17). Chỉ quản lý (đường dẫn {@code /api/admin/**}). */
@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardService.Dashboard today() {
        return service.today();
    }
}
