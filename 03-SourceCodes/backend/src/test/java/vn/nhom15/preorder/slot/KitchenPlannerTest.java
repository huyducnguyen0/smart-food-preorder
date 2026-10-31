package vn.nhom15.preorder.slot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.nhom15.preorder.slot.KitchenPlanner.Interval;
import vn.nhom15.preorder.slot.KitchenPlanner.Job;
import vn.nhom15.preorder.slot.KitchenPlanner.Kind;

/**
 * Bộ xếp lịch bếp (PB-22), test bằng số cụ thể: khoảng làm 15 phút, công suất 20 điểm. Đặt trước nhận lúc S làm trong
 * [S − 45 phút, S]; walk-in làm từ lúc đến, xong trong khung nhận.
 */
class KitchenPlannerTest {

    private static final LocalDate DAY = LocalDate.of(2031, 3, 5);

    private static LocalDateTime at(int hour, int minute) {
        return DAY.atTime(hour, minute);
    }

    /** Các khoảng làm từ {@code fromHour}:00, {@code count} khoảng, cùng công suất. */
    private static List<Interval> intervals(int fromHour, int count, int capacity) {
        List<Interval> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new Interval(at(fromHour, 0).plusMinutes(15L * i), capacity));
        }
        return result;
    }

    private static Job pre(long key, int hour, int minute, int work) {
        return new Job(key, Kind.PRE_ORDER, at(hour, minute).minusMinutes(45), at(hour, minute), work, false);
    }

    private static Job walkIn(long key, LocalDateTime arrive, int hour, int minute, int work) {
        return new Job(key, Kind.WALK_IN, arrive, at(hour, minute).plusMinutes(15), work, false);
    }

    @Test
    @DisplayName("Làm trước: 30 điểm cho khung nhận 12:00 xếp vào 11:15 (20) và 11:30 (10), khoảng 11:45 còn trống")
    void preparesEarliestFirst() {
        KitchenPlanner.Result result = KitchenPlanner.plan(intervals(11, 4, 20), 15, List.of(pre(1, 12, 0, 30)), 100);

        assertThat(result.feasible()).isTrue();
        assertThat(result.plannedByInterval())
                .containsEntry(at(11, 15), 20)
                .containsEntry(at(11, 30), 10)
                .containsEntry(at(11, 45), 0);
        assertThat(result.firstIntervalByKey()).containsEntry(1L, at(11, 15));
    }

    @Test
    @DisplayName("Mô hình cũ chỉ nhận 20 điểm cho khung 12:00; lịch bếp nhận được tới 60 điểm (3 khoảng làm)")
    void moreThanOneIntervalWorth() {
        List<Interval> bins = intervals(11, 4, 20);

        assertThat(KitchenPlanner.plan(bins, 15, List.of(pre(1, 12, 0, 60)), 100)
                        .feasible())
                .isTrue();
        assertThat(KitchenPlanner.plan(bins, 15, List.of(pre(1, 12, 0, 61)), 100)
                        .feasible())
                .isFalse();
    }

    @Test
    @DisplayName("Không làm trước quá H: khoảng 11:00 nằm ngoài cửa sổ [11:15, 12:00] của đơn khung 12:00")
    void neverBeforeHoldWindow() {
        KitchenPlanner.Result result = KitchenPlanner.plan(intervals(11, 4, 20), 15, List.of(pre(1, 12, 0, 10)), 100);

        assertThat(result.plannedByInterval()).containsEntry(at(11, 0), 0).containsEntry(at(11, 15), 10);
    }

    @Test
    @DisplayName("Dành chỗ cho walk-in: đặt trước chỉ dùng 70% (14/20) mỗi khoảng; walk-in dùng được phần còn lại")
    void preOrderShare() {
        List<Interval> bins = intervals(11, 4, 20);

        assertThat(KitchenPlanner.plan(bins, 15, List.of(pre(1, 12, 0, 42)), 70).feasible())
                .isTrue();
        assertThat(KitchenPlanner.plan(bins, 15, List.of(pre(1, 12, 0, 43)), 70).feasible())
                .isFalse();
        // Đặt trước đã dùng hết 14 điểm mỗi khoảng 11:15–11:45; walk-in đến 11:15 lấy lúc 11:45 vẫn có 3 × 6 điểm.
        List<Job> jobs = List.of(pre(1, 12, 0, 42), walkIn(2, at(11, 15), 11, 45, 18));
        assertThat(KitchenPlanner.plan(bins, 15, jobs, 70).feasible()).isTrue();
    }

    @Test
    @DisplayName("Hạn sớm làm trước: đơn khung 11:30 được ưu tiên, đơn khung 12:00 lùi sang khoảng sau")
    void earliestDeadlineFirst() {
        List<Job> jobs = List.of(pre(1, 12, 0, 20), pre(2, 11, 30, 20));

        KitchenPlanner.Result result = KitchenPlanner.plan(intervals(10, 8, 20), 15, jobs, 100);

        assertThat(result.feasible()).isTrue();
        assertThat(result.firstIntervalByKey()).containsEntry(2L, at(10, 45)).containsEntry(1L, at(11, 15));
    }

    @Test
    @DisplayName("Không bao giờ vượt công suất: tổng điểm mỗi khoảng ≤ công suất, kể cả khi không xếp được")
    void neverOverCapacity() {
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            jobs.add(pre(i, 12, 0, 9));
        }

        KitchenPlanner.Result result = KitchenPlanner.plan(intervals(11, 4, 20), 15, jobs, 100);

        assertThat(result.feasible()).isFalse();
        assertThat(result.plannedByInterval().values()).allMatch(points -> points <= 20);
    }

    @Test
    @DisplayName("Đơn quá hạn được xếp gấp nhất nhưng không làm cả lịch 'không xếp được'")
    void lateJobsDoNotBlock() {
        Job late = new Job(9L, Kind.PRE_ORDER, at(10, 0), at(10, 30), 10, true);

        KitchenPlanner.Result result =
                KitchenPlanner.plan(intervals(11, 4, 20), 15, List.of(late, pre(1, 12, 0, 50)), 100);

        assertThat(result.feasible()).isTrue();
        assertThat(result.firstIntervalByKey()).containsEntry(9L, at(11, 0));
    }

    @Test
    @DisplayName("Khoảng có công suất 0 (bếp nghỉ) bị bỏ qua")
    void zeroCapacityIntervalSkipped() {
        List<Interval> bins =
                List.of(new Interval(at(11, 15), 0), new Interval(at(11, 30), 20), new Interval(at(11, 45), 20));

        KitchenPlanner.Result result = KitchenPlanner.plan(bins, 15, List.of(pre(1, 12, 0, 25)), 100);

        assertThat(result.feasible()).isTrue();
        assertThat(result.firstIntervalByKey()).containsEntry(1L, at(11, 30));
    }
}
