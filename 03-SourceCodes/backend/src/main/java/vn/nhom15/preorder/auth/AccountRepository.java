package vn.nhom15.preorder.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Truy vấn bảng {@code user_accounts}. */
@Repository
public class AccountRepository {

    private static final String FIND_BY_LOGIN_ID_SQL = """
            SELECT id, login_id, password_hash, display_name, role, locked
            FROM user_accounts
            WHERE login_id = ?
            """;

    private static final RowMapper<Account> MAPPER = (rs, rowNum) -> new Account(
            rs.getLong("id"),
            rs.getString("login_id"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            Role.valueOf(rs.getString("role")),
            rs.getBoolean("locked"));

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Tạo tài khoản nếu định danh chưa có. Ràng buộc duy nhất của database quyết định ai thắng khi hai người đăng ký
     * cùng định danh một lúc.
     *
     * @return mã tài khoản mới; rỗng nếu định danh đã có tài khoản
     */
    public Optional<Long> insertIfAbsent(String loginId, String passwordHash, String displayName, Role role) {
        return jdbcTemplate.queryForList("""
                                INSERT INTO user_accounts (login_id, password_hash, display_name, role)
                                VALUES (?, ?, ?, ?)
                                ON CONFLICT (login_id) DO NOTHING
                                RETURNING id
                                """, Long.class, loginId, passwordHash, displayName, role.name()).stream()
                .findFirst();
    }

    /** Một tài khoản nhân viên như quản lý thấy (không có mật khẩu). */
    public record StaffAccount(long id, String loginId, String displayName, Role role, boolean locked) {}

    /** Tài khoản nhân viên quầy và bếp (UC-15). */
    public List<StaffAccount> findStaff() {
        return jdbcTemplate.query(
                """
                        SELECT id, login_id, display_name, role, locked FROM user_accounts
                        WHERE role IN ('COUNTER', 'KITCHEN') ORDER BY role, id
                        """,
                (rs, rowNum) -> new StaffAccount(
                        rs.getLong("id"),
                        rs.getString("login_id"),
                        rs.getString("display_name"),
                        Role.valueOf(rs.getString("role")),
                        rs.getBoolean("locked")));
    }

    /**
     * Khóa hoặc mở khóa một tài khoản nhân viên quầy/bếp (BR-32). Không đụng tới khách và quản lý.
     *
     * @return true nếu có tài khoản nhân viên đó
     */
    public boolean setStaffLocked(long id, boolean locked) {
        return jdbcTemplate.update(
                        "UPDATE user_accounts SET locked = ? WHERE id = ? AND role IN ('COUNTER', 'KITCHEN')",
                        locked,
                        id)
                > 0;
    }

    /** Tìm tài khoản theo định danh đã chuẩn hóa. */
    public Optional<Account> findByLoginId(String loginId) {
        List<Account> found = jdbcTemplate.query(FIND_BY_LOGIN_ID_SQL, MAPPER, loginId);
        return found.stream().findFirst();
    }
}
