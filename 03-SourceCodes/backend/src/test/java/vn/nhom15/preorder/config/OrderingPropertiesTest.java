package vn.nhom15.preorder.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Cấu hình số nghiệp vụ: đọc đúng từ app.ordering.* và từ chối giá trị vô lý. */
class OrderingPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(OrderingProperties.class)
    static class Config {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void bindsTheValuesFromSettings() {
        runner.withPropertyValues(
                        "app.ordering.slot-minutes=15",
                        "app.ordering.min-lead-minutes=30",
                        "app.ordering.prep-window-minutes=15",
                        "app.ordering.max-open-orders-per-customer=3",
                        "app.ordering.no-show-grace-minutes=30")
                .run(context -> assertThat(context.getBean(OrderingProperties.class))
                        .isEqualTo(new OrderingProperties(15, 30, 15, 3, 30)));
    }

    @Test
    void rejectsNonsenseValues() {
        runner.withPropertyValues(
                        "app.ordering.slot-minutes=0",
                        "app.ordering.min-lead-minutes=30",
                        "app.ordering.prep-window-minutes=15",
                        "app.ordering.max-open-orders-per-customer=3")
                .run(context -> assertThat(context).hasFailed());
    }
}
