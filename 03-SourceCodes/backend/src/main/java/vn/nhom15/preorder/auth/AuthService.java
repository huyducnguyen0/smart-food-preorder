package vn.nhom15.preorder.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/** Xác thực đăng nhập theo UC-02 bước 3–5. */
@Service
public class AuthService {

    /** BCrypt chỉ dùng 72 byte đầu của mật khẩu; dài hơn thì coi là sai để tránh lỗi và tiêu tốn tài nguyên. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private static final String INVALID_CREDENTIALS_MESSAGE = "Thông tin đăng nhập không đúng";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        // Bản băm giả để vẫn tốn thời gian so mật khẩu khi định danh không tồn tại (không lộ tài khoản nào có thật).
        this.dummyHash = passwordEncoder.encode("khong-co-tai-khoan-nay");
    }

    /**
     * Xác thực định danh và mật khẩu.
     *
     * <p>Thứ tự theo UC-02: so mật khẩu trước, rồi mới kiểm tra khóa (E2); nhờ vậy người không biết mật khẩu không biết
     * được tài khoản có bị khóa hay không. Sai định danh hoặc sai mật khẩu đều trả cùng một lỗi (E1).
     *
     * @throws ApiException {@code INVALID_CREDENTIALS} hoặc {@code ACCOUNT_LOCKED}
     */
    public CurrentUser authenticate(String loginId, String password) {
        Optional<Account> found = accountRepository.findByLoginId(normalizeLoginId(loginId));
        boolean passwordTooLong = password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
        String hash = found.map(Account::passwordHash).orElse(dummyHash);
        boolean passwordMatches = !passwordTooLong && passwordEncoder.matches(password, hash);

        if (found.isEmpty() || !passwordMatches) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, INVALID_CREDENTIALS_MESSAGE);
        }
        Account account = found.get();
        // BR-32: tài khoản bị khóa không đăng nhập được.
        if (account.locked()) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản đã bị khóa, liên hệ quản lý");
        }
        return new CurrentUser(account.id(), account.displayName(), account.role());
    }

    /**
     * Chuẩn hóa định danh như UC-02 bước 3: bỏ khoảng trắng đầu cuối; email không phân biệt hoa thường; số điện thoại
     * bỏ mọi khoảng trắng.
     */
    public static String normalizeLoginId(String raw) {
        String trimmed = raw.strip();
        if (trimmed.contains("@")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return trimmed.replaceAll("\\s+", "");
    }
}
