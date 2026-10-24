package vn.nhom15.preorder.support;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * Tạo database PostgreSQL 18 thật cho test, chạy ngay trong tiến trình test (không cần Docker). Mỗi lần gọi {@link
 * #create()} trả về một database mới đã chạy đủ migration và dữ liệu mẫu. Server PostgreSQL chỉ khởi động một lần cho
 * cả lượt chạy test.
 */
public final class TestDatabase {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static EmbeddedPostgres server;

    private TestDatabase() {}

    /** Database mới với schema theo {@code db/migration}; dữ liệu mẫu {@code db/seed} chỉ nạp khi withSeed = true. */
    public static HikariDataSource create(boolean withSeed) {
        try {
            String name = "test_" + COUNTER.incrementAndGet();
            try (Connection connection = server().getPostgresDatabase().getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE " + name);
            }
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(server().getJdbcUrl("postgres", name));
            config.setUsername("postgres");
            config.setPassword("postgres");
            config.setMaximumPoolSize(64);
            // Chỉ giữ kết nối rảnh tối thiểu: nhiều database test cùng sống trong một lượt chạy.
            config.setMinimumIdle(1);
            HikariDataSource dataSource = new HikariDataSource(config);
            migrate(dataSource, withSeed);
            return dataSource;
        } catch (SQLException e) {
            throw new IllegalStateException("Không tạo được database test", e);
        }
    }

    private static void migrate(DataSource dataSource, boolean withSeed) {
        String[] locations = withSeed
                ? new String[] {"classpath:db/migration", "classpath:db/seed"}
                : new String[] {"classpath:db/migration"};
        Flyway.configure().dataSource(dataSource).locations(locations).load().migrate();
    }

    private static synchronized EmbeddedPostgres server() {
        if (server == null) {
            try {
                // Nhiều context Spring được giữ lại cùng lúc, mỗi context một nhóm kết nối: nâng giới hạn mặc định
                // (100).
                server = EmbeddedPostgres.builder()
                        .setServerConfig("max_connections", "400")
                        .start();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        server.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }));
            } catch (IOException e) {
                throw new UncheckedIOException("Không khởi động được PostgreSQL nhúng", e);
            }
        }
        return server;
    }
}
