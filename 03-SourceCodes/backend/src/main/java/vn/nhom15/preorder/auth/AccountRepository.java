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

    /** Tìm tài khoản theo định danh đã chuẩn hóa. */
    public Optional<Account> findByLoginId(String loginId) {
        List<Account> found = jdbcTemplate.query(FIND_BY_LOGIN_ID_SQL, MAPPER, loginId);
        return found.stream().findFirst();
    }
}
