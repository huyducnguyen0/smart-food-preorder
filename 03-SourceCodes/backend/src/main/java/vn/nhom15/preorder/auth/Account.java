package vn.nhom15.preorder.auth;

/**
 * Tài khoản đọc từ bảng {@code user_accounts}, gồm bản băm mật khẩu. Chỉ module auth được dùng; phần còn lại của hệ
 * thống dùng {@link CurrentUser}.
 *
 * @param id khóa chính
 * @param loginId email (viết thường) hoặc số điện thoại, đã chuẩn hóa
 * @param passwordHash bản băm BCrypt của mật khẩu (NFR-06)
 * @param displayName tên hiển thị
 * @param role vai trò
 * @param locked tài khoản bị quản lý khóa (BR-32)
 */
public record Account(long id, String loginId, String passwordHash, String displayName, Role role, boolean locked) {}
