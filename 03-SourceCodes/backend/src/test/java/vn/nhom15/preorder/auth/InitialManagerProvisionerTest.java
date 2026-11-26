package vn.nhom15.preorder.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Cài đặt không có dữ liệu mẫu (chỉ {@code db/migration}): quản lý đầu tiên được tạo từ cấu hình (BR-32), cấu hình
 * cửa hàng mặc định có sẵn từ migration V10. Chạy trên PostgreSQL thật.
 */
class InitialManagerProvisionerTest {

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static AccountRepository repository;
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @BeforeAll
    static void setUp() {
        dataSource = TestDatabase.create(false);
        jdbc = new JdbcTemplate(dataSource);
        repository = new AccountRepository(jdbc);
    }

    @AfterAll
    static void tearDown() {
        dataSource.close();
    }

    @BeforeEach
    void emptyAccounts() {
        jdbc.update("DELETE FROM user_accounts");
    }

    private static void run(String loginId, String password, String name) {
        new InitialManagerProvisioner(repository, ENCODER, new InitialManagerProperties(loginId, password, name))
                .run(null);
    }

    private List<Map<String, Object>> managers() {
        return jdbc.queryForList(
                "SELECT login_id, display_name, password_hash FROM user_accounts WHERE role = 'MANAGER'");
    }

    @Test
    @DisplayName("Không có dữ liệu mẫu: V10 vẫn tạo cấu hình cửa hàng mặc định 10:00–21:00, quota 20")
    void defaultStoreSettingsWithoutSeed() {
        assertThat(jdbc.queryForMap("SELECT open_time::text o, close_time::text c, default_quota_points q"
                        + " FROM store_settings WHERE id = 1"))
                .containsEntry("o", "10:00:00")
                .containsEntry("c", "21:00:00")
                .containsEntry("q", 20);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_accounts", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("Chưa có quản lý và có cấu hình: tạo quản lý, định danh chuẩn hóa, mật khẩu lưu dạng băm")
    void createsFirstManager() {
        run("  Chu.Cua.Hang@Example.com ", "MatKhau-Manh-2026", " Chủ cửa hàng ");

        List<Map<String, Object>> managers = managers();
        assertThat(managers).hasSize(1);
        assertThat(managers.getFirst())
                .containsEntry("login_id", "chu.cua.hang@example.com")
                .containsEntry("display_name", "Chủ cửa hàng");
        String hash = (String) managers.getFirst().get("password_hash");
        assertThat(hash).doesNotContain("MatKhau");
        assertThat(ENCODER.matches("MatKhau-Manh-2026", hash)).isTrue();
    }

    @Test
    @DisplayName("Đã có quản lý: không tạo thêm, không đổi mật khẩu, kể cả khi cấu hình đổi")
    void doesNothingWhenManagerExists() {
        run("a@example.com", "matkhau-cu-123", null);
        String before = (String) managers().getFirst().get("password_hash");

        run("b@example.com", "matkhau-moi-456", null);
        run("a@example.com", "matkhau-moi-456", null);

        assertThat(managers()).hasSize(1);
        assertThat(managers().getFirst()).containsEntry("login_id", "a@example.com");
        assertThat(managers().getFirst().get("password_hash")).isEqualTo(before);
    }

    @Test
    @DisplayName("Không cấu hình: không tạo gì, ứng dụng vẫn khởi động (chỉ cảnh báo); tên trống dùng 'Quản lý'")
    void noConfigurationNoManager() {
        run("", "", "");
        run(null, null, null);
        assertThat(managers()).isEmpty();

        run("0912345678", "matkhau-123", "  ");
        assertThat(managers().getFirst()).containsEntry("display_name", "Quản lý");
    }

    @Test
    @DisplayName("Cấu hình sai (định danh sai dạng, mật khẩu ngắn, định danh đã thuộc khách): dừng khởi động")
    void invalidConfigurationFailsFast() {
        assertThatThrownBy(() -> run("khong-hop-le", "matkhau-123", null)).hasMessageContaining("không đúng dạng");
        assertThatThrownBy(() -> run("ql@example.com", "1234567", null)).hasMessageContaining("8 ký tự");
        assertThatThrownBy(() -> run("ql@example.com", null, null)).hasMessageContaining("8 ký tự");

        repository.insertIfAbsent("khach@example.com", "x", "Khách", Role.CUSTOMER);
        assertThatThrownBy(() -> run("khach@example.com", "matkhau-123", null))
                .hasMessageContaining("đã thuộc một tài khoản khác");
        assertThat(managers()).isEmpty();
    }
}
