package vn.nhom15.preorder.auth;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/** UC-01 Đăng ký tài khoản khách bằng email hoặc số điện thoại (BR-31); mật khẩu chỉ lưu dạng băm (NFR-06). */
@Service
public class RegistrationService {

    /** Email đơn giản: có một @, có phần tên miền chứa dấu chấm. Không kiểm tra hộp thư có thật. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** Số điện thoại Việt Nam: bắt đầu bằng 0, tổng 10 hoặc 11 chữ số. */
    private static final Pattern PHONE = Pattern.compile("^0\\d{9,10}$");

    /** BCrypt chỉ dùng 72 byte đầu; dài hơn thì từ chối để người dùng không tưởng phần sau có tác dụng. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Tạo tài khoản khách.
     *
     * @throws ApiException {@code VALIDATION_ERROR} nếu định danh sai dạng hoặc mật khẩu quá dài;
     *     {@code LOGIN_ID_TAKEN}
     *     nếu định danh đã có tài khoản (UC-01 E1)
     */
    @Transactional
    public CurrentUser register(RegisterRequest request) {
        String loginId = AuthService.normalizeLoginId(request.loginId());
        if (!EMAIL.matcher(loginId).matches() && !PHONE.matcher(loginId).matches()) {
            throw invalid("loginId", "Email hoặc số điện thoại không đúng dạng");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw invalid("password", "Mật khẩu quá dài (tối đa 72 byte)");
        }
        String displayName = request.displayName().strip();
        long id = accountRepository
                .insertIfAbsent(loginId, passwordEncoder.encode(request.password()), displayName, Role.CUSTOMER)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.LOGIN_ID_TAKEN, "Email hoặc số điện thoại này đã có tài khoản"));
        return new CurrentUser(id, displayName, Role.CUSTOMER);
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message)
                .with("errors", List.of(Map.of("field", field, "message", message)));
    }
}
