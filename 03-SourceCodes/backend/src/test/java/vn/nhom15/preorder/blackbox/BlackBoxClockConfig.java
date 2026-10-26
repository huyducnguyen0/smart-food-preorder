package vn.nhom15.preorder.blackbox;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import vn.nhom15.preorder.support.MutableClock;

/** Đồng hồ chỉnh được cho bộ test black-box: mọi test dùng chung một ngày cố định, chỉ đổi giờ. */
@TestConfiguration
class BlackBoxClockConfig {

    static final LocalDate TODAY = LocalDate.of(2031, 3, 5);
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    @Primary
    MutableClock blackBoxClock() {
        return new MutableClock(ZONE, TODAY.atTime(11, 0));
    }
}
