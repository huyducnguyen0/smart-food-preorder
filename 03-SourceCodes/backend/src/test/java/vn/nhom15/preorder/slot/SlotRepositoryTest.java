package vn.nhom15.preorder.slot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Điều kiện giữ quota ở mức câu SQL: {@code held_points + points <= COALESCE(quota_override, defaultQuota)}.
 * Bảng giá trị biên dưới đây là test white-box cho điều kiện này (xem WHITE_BOX_QUOTA.md mục 4): mỗi dòng chọn
 * một tổ hợp của "có hay không có quota riêng" và vị trí của tổng điểm so với quota (nhỏ hơn, đúng bằng, lớn hơn
 * một điểm).
 */
class SlotRepositoryTest {

    private static final LocalTime START = LocalTime.of(12, 0);
    private static final AtomicInteger NEXT_DAY = new AtomicInteger();

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static SlotRepository repository;

    @BeforeAll
    static void setUp() {
        dataSource = TestDatabase.create(false);
        jdbc = new JdbcTemplate(dataSource);
        repository = new SlotRepository(jdbc);
    }

    @AfterAll
    static void tearDown() {
        dataSource.close();
    }

    private static LocalDate newDay() {
        return LocalDate.of(2031, 1, 1).plusDays(NEXT_DAY.incrementAndGet());
    }

    /** Mỗi dòng: điểm đang giữ, điểm cần hoàn, hoàn được hay không (không bao giờ xuống dưới 0). */
    static Stream<Arguments> releaseBoundaries() {
        return Stream.of(
                Arguments.of(10, 10, true), // hoàn đúng bằng điểm đang giữ
                Arguments.of(10, 4, true),
                Arguments.of(3, 4, false)); // hoàn nhiều hơn điểm đang giữ: từ chối, giữ nguyên
    }

    @ParameterizedTest(name = "đang giữ {0}, hoàn {1} → hoàn được = {2}")
    @MethodSource("releaseBoundaries")
    void releaseNeverGoesBelowZero(int held, int points, boolean expected) {
        LocalDate date = newDay();
        long id = repository.ensureExists(date, START);
        repository.addHeld(id, held);

        assertThat(repository.release(id, points)).isEqualTo(expected);
        int after = jdbc.queryForObject("SELECT held_points FROM time_slots WHERE id = ?", Integer.class, id);
        assertThat(after).isEqualTo(expected ? held - points : held);
    }

    @Test
    void capacityOverrideCanBeSetAndCleared() {
        LocalDate date = newDay();
        long id = repository.ensureExists(date, START);

        repository.setCapacityOverride(id, 30);
        assertThat(repository.find(date, START).orElseThrow().quota(20)).isEqualTo(30);
        repository.setCapacityOverride(id, null);
        assertThat(repository.find(date, START).orElseThrow().quota(20)).isEqualTo(20);
    }

    @Test
    void ensureExistsCreatesOnceAndReturnsTheSameRowAfterwards() {
        LocalDate date = newDay();

        long first = repository.ensureExists(date, START);
        long second = repository.ensureExists(date, START);

        assertThat(second).isEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM time_slots WHERE slot_date = ?", Integer.class, date))
                .isEqualTo(1);
        assertThat(repository.find(date, START)).get().satisfies(slot -> {
            assertThat(slot.heldPoints()).isZero();
            assertThat(slot.quotaOverride()).isNull();
        });
    }

    @Test
    void findByDateReturnsOnlyThatDayInTimeOrder() {
        LocalDate date = newDay();
        repository.ensureExists(date, LocalTime.of(13, 0));
        repository.ensureExists(date, LocalTime.of(11, 30));
        repository.ensureExists(newDay(), LocalTime.of(9, 0));

        List<TimeSlot> slots = repository.findByDate(date);

        assertThat(slots).extracting(TimeSlot::startTime).containsExactly(LocalTime.of(11, 30), LocalTime.of(13, 0));
    }

    @Test
    void findOfAMissingSlotIsEmpty() {
        assertThat(repository.find(newDay(), START)).isEmpty();
    }

    @Test
    void storeSettingsWithoutARowIsAnInstallationError() {
        // Từ V10 migration luôn tạo dòng cấu hình mặc định; xóa đi để kiểm tra database bị sửa tay sai: thiếu cấu
        // hình cửa hàng là lỗi cài đặt, không phải lỗi người dùng.
        StoreSettingsRepository settings = new StoreSettingsRepository(jdbc);
        jdbc.update("DELETE FROM store_settings");
        try {
            assertThatThrownBy(settings::get)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("store_settings");
        } finally {
            jdbc.update("INSERT INTO store_settings (id, open_time, close_time, default_quota_points)"
                    + " VALUES (1, '10:00', '21:00', 20)");
        }
    }
}
