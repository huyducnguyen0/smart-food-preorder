package vn.nhom15.preorder.slot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cấu hình giờ mở cửa và quota (UC-14). Chỉ quản lý (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminSlotController {

    public record SettingsRequest(
            @NotNull(message = "Nhập giờ mở cửa") LocalTime openTime,
            @NotNull(message = "Nhập giờ đóng cửa") LocalTime closeTime,

            @NotNull(message = "Nhập quota mặc định") @Min(value = 0, message = "Quota không âm") @Max(10_000)
            Integer defaultQuotaPoints) {}

    /** Quota riêng của khung; {@code null} để dùng lại quota mặc định. */
    public record SlotQuotaRequest(
            @Min(value = 0, message = "Quota không âm") @Max(10_000)
            Integer quota) {}

    private final AdminSlotService service;

    public AdminSlotController(AdminSlotService service) {
        this.service = service;
    }

    @GetMapping("/slots/today")
    public AdminSlotService.AdminDay today() {
        return service.today();
    }

    @PutMapping("/settings")
    public AdminSlotService.AdminDay updateSettings(@Valid @RequestBody SettingsRequest request) {
        return service.updateSettings(request.openTime(), request.closeTime(), request.defaultQuotaPoints());
    }

    @PutMapping("/slots/today/{start}/quota")
    public AdminSlotService.AdminDay setSlotQuota(
            @PathVariable LocalTime start, @Valid @RequestBody SlotQuotaRequest request) {
        return service.setSlotQuota(start, request.quota());
    }
}
