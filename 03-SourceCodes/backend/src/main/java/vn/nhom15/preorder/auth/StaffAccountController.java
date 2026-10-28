package vn.nhom15.preorder.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tài khoản nhân viên (UC-15). Chỉ quản lý (cấu hình ở SecurityConfig, đường dẫn {@code /api/admin/**}). */
@RestController
@RequestMapping("/api/admin/staff")
public class StaffAccountController {

    public record StaffRequest(
            @NotBlank(message = "Nhập email hoặc số điện thoại") @Size(max = 100)
            String loginId,

            @NotBlank(message = "Nhập tên") @Size(max = 100) String displayName,

            @NotBlank(message = "Nhập mật khẩu") @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
            String password,

            @NotNull(message = "Chọn vai trò") Role role) {}

    private final StaffAccountService service;

    public StaffAccountController(StaffAccountService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, List<AccountRepository.StaffAccount>> list() {
        return Map.of("staff", service.list());
    }

    @PostMapping
    public ResponseEntity<AccountRepository.StaffAccount> create(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request.loginId(), request.displayName(), request.password(), request.role()));
    }

    @PostMapping("/{id}/lock")
    public Map<String, List<AccountRepository.StaffAccount>> lock(@PathVariable long id) {
        return Map.of("staff", service.setLocked(id, true));
    }

    @PostMapping("/{id}/unlock")
    public Map<String, List<AccountRepository.StaffAccount>> unlock(@PathVariable long id) {
        return Map.of("staff", service.setLocked(id, false));
    }
}
