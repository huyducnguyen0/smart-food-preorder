package vn.nhom15.preorder.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Nội dung đăng ký tài khoản khách (UC-01).
 *
 * @param loginId email hoặc số điện thoại, chưa có tài khoản nào dùng (BR-31)
 * @param displayName tên hiển thị
 * @param password mật khẩu, lưu dạng băm (NFR-06)
 */
public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại") @Size(max = 100)
        String loginId,

        @NotBlank(message = "Vui lòng nhập tên") @Size(max = 100, message = "Tên tối đa 100 ký tự")
        String displayName,

        @NotBlank(message = "Vui lòng nhập mật khẩu") @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
        String password) {}
