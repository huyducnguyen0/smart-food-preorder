package vn.nhom15.preorder;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.SpringApplication;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Chạy toàn bộ backend trên một PostgreSQL nhúng, không cần Docker: {@code ./mvnw spring-boot:test-run}. Mỗi lần chạy
 * tạo database mới với dữ liệu mẫu; tắt ứng dụng là mất dữ liệu. Chỉ dùng để phát triển và thử tay trên máy cá nhân.
 */
public final class DevApplication {

    private DevApplication() {}

    public static void main(String[] args) {
        HikariDataSource dataSource = TestDatabase.create(true);
        System.setProperty("spring.datasource.url", dataSource.getJdbcUrl());
        System.setProperty("spring.datasource.username", dataSource.getUsername());
        System.setProperty("spring.datasource.password", dataSource.getPassword());
        SpringApplication.run(PreorderBackendApplication.class, args);
    }
}
