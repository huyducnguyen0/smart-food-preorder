package vn.nhom15.preorder.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Nội dung yêu cầu đăng nhập (UC-02 bước 2).
 *
 * @param loginId email hoặc số điện thoại
 * @param password mật khẩu
 */
public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại")
        String loginId,

        @NotBlank(message = "Vui lòng nhập mật khẩu") String password) {}
