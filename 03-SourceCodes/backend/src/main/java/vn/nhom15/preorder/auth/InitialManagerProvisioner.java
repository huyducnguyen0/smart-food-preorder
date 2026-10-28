package vn.nhom15.preorder.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-32: "tài khoản quản lý đầu tiên có sẵn khi cài đặt". Khi ứng dụng khởi động mà database chưa có quản lý nào, tạo
 * quản lý từ {@link InitialManagerProperties}. Đã có quản lý thì không làm gì (đổi biến môi trường sau đó không tạo
 * thêm hay đổi mật khẩu). Cấu hình sai thì dừng khởi động để người cài đặt thấy ngay, thay vì chạy mà không ai đăng
 * nhập được.
 */
@Component
@EnableConfigurationProperties(InitialManagerProperties.class)
public class InitialManagerProvisioner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialManagerProvisioner.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialManagerProperties properties;

    public InitialManagerProvisioner(
            AccountRepository accountRepository, PasswordEncoder passwordEncoder, InitialManagerProperties properties) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository.existsByRole(Role.MANAGER)) {
            return;
        }
        if (!properties.configured()) {
            log.warn("Chưa có tài khoản quản lý nào. Đặt INITIAL_MANAGER_LOGIN_ID và INITIAL_MANAGER_PASSWORD rồi"
                    + " khởi động lại để tạo quản lý đầu tiên.");
            return;
        }
        String loginId = RegistrationService.validLoginId(properties.loginId());
        String password = properties.password();
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("INITIAL_MANAGER_PASSWORD phải có tối thiểu 8 ký tự");
        }
        RegistrationService.checkPasswordLength(password);
        String name =
                properties.displayName() == null || properties.displayName().isBlank()
                        ? "Quản lý"
                        : properties.displayName().strip();
        accountRepository
                .insertIfAbsent(loginId, passwordEncoder.encode(password), name, Role.MANAGER)
                .orElseThrow(() -> new IllegalStateException(
                        "INITIAL_MANAGER_LOGIN_ID " + loginId + " đã thuộc một tài khoản khác"));
        log.info("Đã tạo tài khoản quản lý đầu tiên {}", loginId);
    }
}
