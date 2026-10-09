package vn.nhom15.preorder.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

/** Kiểm tra logic HealthService với database giả lập và đồng hồ cố định. */
class HealthServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T02:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final HealthService healthService = new HealthService(jdbcTemplate, clock);

    @Test
    @SuppressWarnings("unchecked")
    void reportsUpWithSchemaVersionWhenDatabaseResponds() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class))).thenReturn("1");

        HealthResponse result = healthService.check();

        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.database()).isEqualTo("UP");
        assertThat(result.schemaVersion()).isEqualTo("1");
        assertThat(result.serverTime()).isEqualTo(OffsetDateTime.parse("2026-10-12T09:00:00+07:00"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsDownWhenDatabaseIsUnreachable() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class)))
                .thenThrow(new CannotGetJdbcConnectionException("Connection refused"));

        HealthResponse result = healthService.check();

        assertThat(result.status()).isEqualTo("DOWN");
        assertThat(result.database()).isEqualTo("DOWN");
        assertThat(result.schemaVersion()).isNull();
    }
}
