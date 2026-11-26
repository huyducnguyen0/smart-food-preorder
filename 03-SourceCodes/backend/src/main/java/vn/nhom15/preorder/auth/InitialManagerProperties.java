package vn.nhom15.preorder.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tài khoản quản lý đầu tiên khi cài đặt (BR-32), đọc từ {@code app.initial-manager.*} (biến môi trường
 * {@code INITIAL_MANAGER_LOGIN_ID}, {@code INITIAL_MANAGER_PASSWORD}, {@code INITIAL_MANAGER_NAME}). Mật khẩu không
 * nằm trong mã nguồn hay migration.
 *
 * @param loginId email hoặc số điện thoại; để trống thì không tạo
 * @param password mật khẩu ban đầu, tối thiểu 8 ký tự
 * @param displayName tên hiển thị; để trống thì dùng "Quản lý"
 */
@ConfigurationProperties("app.initial-manager")
public record InitialManagerProperties(String loginId, String password, String displayName) {

    boolean configured() {
        return loginId != null && !loginId.isBlank();
    }
}
