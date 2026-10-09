package vn.nhom15.preorder.health;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Kiểm tra backend có truy vấn được database và đọc phiên bản migration Flyway.
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private static final String LATEST_SCHEMA_VERSION_SQL = """
            SELECT version FROM flyway_schema_history
            WHERE success AND version IS NOT NULL
            ORDER BY installed_rank DESC
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public HealthService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public HealthResponse check() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        try {
            String schemaVersion =
                    jdbcTemplate.query(LATEST_SCHEMA_VERSION_SQL, rs -> rs.next() ? rs.getString("version") : null);
            return new HealthResponse(HealthResponse.UP, HealthResponse.UP, schemaVersion, now);
        } catch (DataAccessException e) {
            log.warn("Health check: không truy vấn được database: {}", e.getMessage());
            return new HealthResponse(HealthResponse.DOWN, HealthResponse.DOWN, null, now);
        }
    }
}
