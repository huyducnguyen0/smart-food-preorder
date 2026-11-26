package vn.nhom15.preorder.menu;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý món (UC-13). Chỉ quản lý (cấu hình ở SecurityConfig). Khác API công khai: có điểm công việc và món ngừng
 * bán.
 */
@RestController
@RequestMapping("/api/admin/menu")
public class AdminMenuController {

    /**
     * Thông tin món quản lý nhập (BR-01). Giới hạn trên là chặn kỹ thuật tránh tràn số, không phải quy tắc nghiệp vụ.
     */
    public record ItemRequest(
            @NotNull(message = "Chọn nhóm món") Long categoryId,

            @NotBlank(message = "Nhập tên món") @Size(max = 100, message = "Tên tối đa 100 ký tự")
            String name,

            @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
            String description,

            @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
            @Pattern(regexp = "^(https?://.*)?$", message = "Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://")
            String imageUrl,

            @NotNull(message = "Nhập giá") @Min(value = 0, message = "Giá không âm") @Max(100_000_000)
            Integer price,

            @NotNull(message = "Nhập điểm công việc") @Min(value = 0, message = "Điểm không âm") @Max(1_000)
            Integer workloadPoints) {

        AdminMenuRepository.ItemFields toFields() {
            return new AdminMenuRepository.ItemFields(categoryId, name, description, imageUrl, price, workloadPoints);
        }
    }

    public record AvailabilityRequest(@NotNull Boolean available) {}

    private final AdminMenuService service;

    public AdminMenuController(AdminMenuService service) {
        this.service = service;
    }

    @GetMapping
    public AdminMenuService.AdminMenu menu() {
        return service.getMenu();
    }

    @PostMapping("/items")
    public ResponseEntity<MenuItem> create(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.toFields()));
    }

    @PutMapping("/items/{id}")
    public MenuItem update(@PathVariable long id, @Valid @RequestBody ItemRequest request) {
        return service.update(id, request.toFields());
    }

    @PostMapping("/items/{id}/availability")
    public MenuItem availability(@PathVariable long id, @Valid @RequestBody AvailabilityRequest request) {
        return service.setAvailable(id, request.available());
    }

    @PostMapping("/items/{id}/discontinue")
    public MenuItem discontinue(@PathVariable long id) {
        return service.setDiscontinued(id, true);
    }

    @PostMapping("/items/{id}/restore")
    public MenuItem restore(@PathVariable long id) {
        return service.setDiscontinued(id, false);
    }
}
