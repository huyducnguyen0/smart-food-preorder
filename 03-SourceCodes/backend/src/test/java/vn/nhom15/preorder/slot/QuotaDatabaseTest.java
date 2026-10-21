package vn.nhom15.preorder.slot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.support.TestDatabase;

/**
 * Giữ quota trên PostgreSQL thật, gồm cả test đồng thời cho NFR-01 (bản rút gọn; bản 50 yêu cầu đầy đủ qua API ở Sprint
 * 2). Dùng một Spring context nhỏ để {@code @Transactional} của {@link QuotaService} thật sự có hiệu lực. Mỗi test dùng
 * một ngày riêng nên không ảnh hưởng nhau. Dữ liệu mẫu: quota mặc định 20 điểm.
 */
class QuotaDatabaseTest {

    private static final LocalTime START = LocalTime.of(12, 0);
    private static final AtomicInteger NEXT_DAY = new AtomicInteger();

    private static HikariDataSource dataSource;
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static TransactionTemplate tx;
    private static QuotaService service;

    private LocalDate date;

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        JdbcTemplate jdbcTemplate(DataSource ds) {
            return new JdbcTemplate(ds);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }

        @Bean
        SlotRepository slotRepository(JdbcTemplate jdbcTemplate) {
            return new SlotRepository(jdbcTemplate);
        }

        @Bean
        StoreSettingsRepository storeSettingsRepository(JdbcTemplate jdbcTemplate) {
            return new StoreSettingsRepository(jdbcTemplate);
        }

        @Bean
        QuotaService quotaService(SlotRepository slots, StoreSettingsRepository settings) {
            return new QuotaService(slots, settings);
        }
    }

    @BeforeAll
    static void setUp() {
        dataSource = TestDatabase.create(true);
        context = new AnnotationConfigApplicationContext();
        context.registerBean("dataSource", DataSource.class, () -> dataSource);
        context.register(TestConfig.class);
        context.refresh();
        jdbc = context.getBean(JdbcTemplate.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        service = context.getBean(QuotaService.class);
    }

    @AfterAll
    static void tearDown() {
        context.close();
        dataSource.close();
    }

    @BeforeEach
    void newDay() {
        date = LocalDate.of(2030, 1, 1).plusDays(NEXT_DAY.incrementAndGet());
    }

    // ---------- Hỗ trợ ----------

    private QuotaReservation reserve(int points) {
        return tx.execute(status -> service.reserve(date, START, List.of(new QuotaLine(points, 1))));
    }

    private int held() {
        return jdbc.queryForObject(
                "SELECT held_points FROM time_slots WHERE slot_date = ? AND start_time = ?",
                Integer.class,
                date,
                START);
    }

    private int slotRows() {
        return jdbc.queryForObject("SELECT count(*) FROM time_slots WHERE slot_date = ?", Integer.class, date);
    }

    private void setQuotaOverride(int quota) {
        jdbc.update(
                "INSERT INTO time_slots (slot_date, start_time, quota_override) VALUES (?, ?, ?)", date, START, quota);
    }

    private static ErrorCode codeOf(Throwable e) {
        return ((ApiException) e).code();
    }

    private record Outcome(boolean held, int points, ErrorCode error) {}

    /** Chạy n yêu cầu giữ quota cùng lúc (cùng chờ một tín hiệu xuất phát), mỗi yêu cầu trong giao dịch riêng. */
    private List<Outcome> reserveConcurrently(List<Integer> pointsPerRequest) throws Exception {
        int n = pointsPerRequest.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (int points : pointsPerRequest) {
            Callable<Outcome> task = () -> {
                ready.countDown();
                go.await();
                try {
                    reserve(points);
                    return new Outcome(true, points, null);
                } catch (ApiException e) {
                    return new Outcome(false, points, e.code());
                }
            };
            futures.add(pool.submit(task));
        }
        ready.await();
        go.countDown();
        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return outcomes;
    }

    // ---------- UC-17: luồng chính và các luồng lỗi ----------

    @Test
    void reserveCreatesTheSlotRowAndHoldsThePoints() {
        QuotaReservation reservation = reserve(8);

        assertThat(reservation.points()).isEqualTo(8);
        assertThat(held()).isEqualTo(8);
        assertThat(slotRows()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT id FROM time_slots WHERE slot_date = ? AND start_time = ?", Long.class, date, START))
                .isEqualTo(reservation.timeSlotId());
    }

    @Test
    void ordersAreAcceptedUpToExactlyTheQuotaThenRejected() {
        // Quota mặc định 20 (dữ liệu mẫu): 12 + 8 = 20 vừa đủ (BR-09 dùng "≤"), điểm thứ 21 bị từ chối.
        reserve(12);
        reserve(8);
        assertThat(held()).isEqualTo(20);

        assertThatThrownBy(() -> reserve(1))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SLOT_FULL));
        assertThat(held()).isEqualTo(20);
    }

    @Test
    void orderLargerThanTheSlotQuotaIsTooLargeNotFull() {
        // UC-17 E1: 21 > quota 20, kể cả khi khung còn hoàn toàn trống.
        assertThatThrownBy(() -> reserve(21))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.ORDER_TOO_LARGE));
        // Lỗi làm giao dịch hủy, nên dòng khung vừa tạo cũng không còn.
        assertThat(slotRows()).isZero();
    }

    @Test
    void slotQuotaOverrideTakesPrecedenceOverTheDefault() {
        // BR-07: quản lý sửa quota riêng của khung xuống 5.
        setQuotaOverride(5);

        assertThatThrownBy(() -> reserve(6))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.ORDER_TOO_LARGE));
        reserve(5);
        assertThatThrownBy(() -> reserve(1))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SLOT_FULL));
        assertThat(held()).isEqualTo(5);
    }

    @Test
    void zeroPointOrderIsAcceptedEvenWhenTheSlotIsFull() {
        // BR-13: đơn 0 điểm không chiếm quota nên vẫn nhận được khi khung đã đầy.
        reserve(20);

        QuotaReservation reservation = reserve(0);

        assertThat(reservation.points()).isZero();
        assertThat(held()).isEqualTo(20);
    }

    @Test
    void differentSlotsHaveIndependentQuota() {
        reserve(20);

        QuotaReservation other =
                tx.execute(status -> service.reserve(date, START.plusMinutes(15), List.of(new QuotaLine(20, 1))));

        assertThat(other.points()).isEqualTo(20);
        assertThat(slotRows()).isEqualTo(2);
    }

    // ---------- BR-10: tạo đơn và giữ quota cùng thành công hoặc cùng thất bại ----------

    @Test
    void holdIsUndoneWhenTheCallingTransactionFailsAfterReserving() {
        // UC-17 E3: use case gọi lỗi ở bước tạo đơn sau khi đã giữ quota → quota trở về như cũ.
        reserve(5);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                    service.reserve(date, START, List.of(new QuotaLine(10, 1)));
                    throw new IllegalStateException("lỗi giả lập ở bước tạo đơn");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(held()).isEqualTo(5);
    }

    @Test
    void reserveRefusesToRunOutsideATransaction() {
        assertThatThrownBy(() -> service.reserve(date, START, List.of(new QuotaLine(1, 1))))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void databaseConstraintBlocksOverbookingEvenIfCodeIsWrong() {
        setQuotaOverride(10);

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE time_slots SET held_points = 11 WHERE slot_date = ? AND start_time = ?", date, START))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_time_slots_held_within_override");
    }

    // ---------- NFR-01: nhất quán khi đồng thời ----------

    @Test
    void fiftyConcurrentOrdersNeverExceedTheQuota() throws Exception {
        // Quota 20, mỗi đơn 3 điểm: chỉ vừa 6 đơn (18 điểm); 44 đơn còn lại phải bị từ chối vì hết chỗ.
        List<Outcome> outcomes = reserveConcurrently(java.util.Collections.nCopies(50, 3));

        long accepted = outcomes.stream().filter(Outcome::held).count();
        assertThat(accepted).isEqualTo(6);
        assertThat(outcomes.stream().filter(o -> !o.held()).map(Outcome::error))
                .hasSize(44)
                .containsOnly(ErrorCode.SLOT_FULL);
        assertThat(held()).isEqualTo(18);
        assertThat(slotRows())
                .as("khung được tạo đúng một dòng dù nhiều yêu cầu cùng lúc")
                .isEqualTo(1);
    }

    @Test
    void concurrentOrdersOfDifferentSizesKeepHeldPointsConsistent() throws Exception {
        setQuotaOverride(15);
        Random random = new Random(20261021);
        List<Integer> sizes = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            sizes.add(1 + random.nextInt(6));
        }

        List<Outcome> outcomes = reserveConcurrently(sizes);

        int acceptedPoints = outcomes.stream()
                .filter(Outcome::held)
                .mapToInt(Outcome::points)
                .sum();
        // Bất biến: điểm đang giữ đúng bằng tổng điểm các đơn được nhận, và không vượt quota.
        assertThat(held()).isEqualTo(acceptedPoints);
        assertThat(acceptedPoints).isLessThanOrEqualTo(15);
        // Điểm đang giữ chỉ tăng, nên đơn bị từ chối lúc nào đó cũng lớn hơn phần còn lại lúc kết thúc;
        // nếu không thì một đơn vừa chỗ đã bị từ chối oan.
        int remaining = 15 - acceptedPoints;
        assertThat(outcomes.stream().filter(o -> !o.held()).mapToInt(Outcome::points))
                .allSatisfy(points -> assertThat(points).isGreaterThan(remaining));
        assertThat(slotRows()).isEqualTo(1);
    }

    @Test
    void concurrentFirstUseOfANewSlotCreatesOneRowAndNeverOversells() throws Exception {
        // 30 yêu cầu 1 điểm cùng lúc vào khung chưa có dòng, quota 20: đúng 20 được nhận.
        List<Outcome> outcomes = reserveConcurrently(java.util.Collections.nCopies(30, 1));

        assertThat(outcomes.stream().filter(Outcome::held).count()).isEqualTo(20);
        assertThat(held()).isEqualTo(20);
        assertThat(slotRows()).isEqualTo(1);
    }
}
