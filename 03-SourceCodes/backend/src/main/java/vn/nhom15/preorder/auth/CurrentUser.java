package vn.nhom15.preorder.auth;

import java.io.Serializable;

/**
 * Người dùng đang đăng nhập: bản rút gọn của tài khoản, không có mật khẩu. Được lưu trong phiên và trả về cho frontend.
 *
 * @param id mã tài khoản
 * @param displayName tên hiển thị
 * @param role vai trò
 */
public record CurrentUser(long id, String displayName, Role role) implements Serializable {}
