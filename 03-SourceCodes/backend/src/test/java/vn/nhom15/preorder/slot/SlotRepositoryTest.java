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

    /** Mỗi dòng: quota riêng (null = không có), điểm đang giữ, điểm đơn, quota mặc định, giữ được hay không. */
    static Stream<Arguments> holdBoundaries() {
        return Stream.of(
                Arguments.of(null, 0, 20, 20, true), // đúng bằng quota mặc định (≤)
                Arguments.of(null, 0, 21, 20, false), // hơn quota mặc định một điểm
                Arguments.of(null, 10, 10, 20, true), // còn lại đúng bằng điểm đơn
                Arguments.of(null, 10, 11, 20, false), // còn lại ít hơn điểm đơn một điểm
                Arguments.of(null, 19, 1, 20, true), // điểm cuối cùng của khung
                Arguments.of(5, 0, 5, 20, true), // quota riêng nhỏ hơn mặc định: đúng bằng quota riêng
                Arguments.of(5, 0, 6, 20, false), // vượt quota riêng dù chưa vượt quota mặc định
                Arguments.of(30, 25, 5, 20, true), // quota riêng lớn hơn mặc định: dùng quota riêng
                Arguments.of(30, 25, 6, 20, false), // vượt quota riêng
                Arguments.of(0, 0, 1, 20, false)); // quota riêng bằng 0: khung đóng
    }

    @ParameterizedTest(name = "quota riêng {0}, đang giữ {1}, đơn {2}, mặc định {3} → giữ được = {4}")
    @MethodSource("holdBoundaries")
    void holdFollowsTheQuotaInequality(Integer override, int held, int points, int defaultQuota, boolean expected) {
        LocalDate date = newDay();
        jdbc.update(
                "INSERT INTO time_slots (slot_date, start_time, quota_override, held_points) VALUES (?, ?, ?, ?)",
                date,
                START,
                override,
                held);

        boolean granted = repository.tryHold(date, START, points, defaultQuota).isPresent();

        assertThat(granted).isEqualTo(expected);
        int heldAfter = jdbc.queryForObject(
                "SELECT held_points FROM time_slots WHERE slot_date = ? AND start_time = ?",
                Integer.class,
                date,
                START);
        assertThat(heldAfter).as("chỉ tăng đúng khi giữ được").isEqualTo(expected ? held + points : held);
    }

    @Test
    void holdOnASlotWithoutARowGrantsNothing() {
        // ensureExists phải được gọi trước; không có dòng thì UPDATE không khớp dòng nào.
        assertThat(repository.tryHold(newDay(), START, 1, 20)).isEmpty();
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
        // Database trống (không có dữ liệu mẫu): thiếu cấu hình cửa hàng là lỗi cài đặt, không phải lỗi người dùng.
        StoreSettingsRepository settings = new StoreSettingsRepository(jdbc);

        assertThatThrownBy(settings::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("store_settings");
    }
}
