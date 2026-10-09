package vn.nhom15.preorder.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cung cấp đồng hồ theo múi giờ cửa hàng. Code nghiệp vụ lấy thời gian hiện tại
 * qua Clock thay vì gọi trực tiếp now(), để test cố định được thời điểm.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
