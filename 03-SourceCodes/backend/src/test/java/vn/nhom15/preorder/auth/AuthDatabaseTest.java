package vn.nhom15.preorder.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.support.TestDatabase;

/** Kiểm tra schema {@code user_accounts} và dữ liệu mẫu trên PostgreSQL thật. */
class AuthDatabaseTest {

    /** Mật khẩu chung của tài khoản mẫu, ghi trong 03-SourceCodes/README.md (chỉ dùng cho local). */
    private static final String SEED_PASSWORD = "Demo@2026";

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static AuthService authService;

    @BeforeAll
    static void setUp() {
        dataSource = TestDatabase.create(true);
        jdbc = new JdbcTemplate(dataSource);
        authService = new AuthService(new AccountRepository(jdbc), new BCryptPasswordEncoder(4));
    }

    @AfterAll
    static void tearDown() {
        dataSource.close();
    }

    @Test
    void seedAccountsCoverAllRolesAndOneLocked() {
        assertThat(jdbc.queryForList("SELECT DISTINCT role FROM user_accounts", String.class))
                .containsExactlyInAnyOrder("CUSTOMER", "COUNTER", "KITCHEN", "MANAGER");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_accounts WHERE locked", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void passwordsAreStoredAsHashesNotPlainText() {
        assertThat(jdbc.queryForList("SELECT password_hash FROM user_accounts", String.class))
                .allSatisfy(hash -> assertThat(hash).startsWith("$2").doesNotContain(SEED_PASSWORD));
    }

    @Test
    void everySeedAccountCanLoginWithTheDocumentedPassword() {
        // Đây cũng là kiểm tra: bản băm trong file seed thật sự khớp mật khẩu ghi trong README.
        assertThat(authService.authenticate("khach1@example.com", SEED_PASSWORD).role())
                .isEqualTo(Role.CUSTOMER);
        assertThat(authService.authenticate("0901234567", SEED_PASSWORD).role()).isEqualTo(Role.CUSTOMER);
        assertThat(authService.authenticate("bep1@example.com", SEED_PASSWORD).role())
                .isEqualTo(Role.KITCHEN);
        assertThat(authService.authenticate("quay1@example.com", SEED_PASSWORD).role())
                .isEqualTo(Role.COUNTER);
        assertThat(authService.authenticate("quanly@example.com", SEED_PASSWORD).role())
                .isEqualTo(Role.MANAGER);
    }

    @Test
    void loginIdIsNormalizedBeforeLookup() {
        assertThat(authService
                        .authenticate("  KHACH1@Example.COM ", SEED_PASSWORD)
                        .displayName())
                .isEqualTo("Khách Một");
        assertThat(authService.authenticate(" 0901 234 567 ", SEED_PASSWORD).displayName())
                .isEqualTo("Khách Hai");
    }

    @Test
    void lockedAccountWithCorrectPasswordIsRejectedAsLocked() {
        assertThatThrownBy(() -> authService.authenticate("khoa@example.com", SEED_PASSWORD))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ACCOUNT_LOCKED));
    }

    @Test
    void lockedAccountWithWrongPasswordLooksLikeAnyWrongLogin() {
        assertThatThrownBy(() -> authService.authenticate("khoa@example.com", "sai-mat-khau"))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void databaseRejectsDuplicateLoginIdAndUnknownRole() {
        String insert =
                "INSERT INTO user_accounts (login_id, password_hash, display_name, role) VALUES (?, 'x', 'Thử', ?)";

        assertThatThrownBy(() -> jdbc.update(insert, "khach1@example.com", "CUSTOMER"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_user_accounts_login_id");
        assertThatThrownBy(() -> jdbc.update(insert, "moi@example.com", "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_user_accounts_role");
    }
}
