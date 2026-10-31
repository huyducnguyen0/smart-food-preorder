package vn.nhom15.preorder.slot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * Các nhánh quyết định của {@link QuotaService#reserve} (PB-22): quầy đủ đơn, đơn quá lớn, bếp không còn kịp, đơn 0
 * điểm, giữ chỗ thành công. Lịch bếp được giả lập bằng một ảnh chụp dựng tay: lúc 11:00, khung nhận 12:00, công suất
 * 20 điểm mỗi khoảng, làm trước tối đa 45 phút, đặt trước 70%, quầy tối đa 8 đơn.
 */
class QuotaServiceTest {

    private static final LocalDate DATE = LocalDate.of(2031, 3, 5);
    private static final LocalTime START = LocalTime.of(12, 0);
    private static final StoreSettings SETTINGS =
            new StoreSettings(LocalTime.of(10, 0), LocalTime.of(21, 0), 20, 45, 70, 8);

    private SlotRepository slots;
    private QuotaService service;
    private final List<KitchenPlanner.Job> jobs = new ArrayList<>();
    private final Map<LocalTime, Integer> pickup = new HashMap<>();

    @BeforeEach
    void setUp() {
        slots = mock(SlotRepository.class);
        StoreSettingsRepository settings = mock(StoreSettingsRepository.class);
        KitchenPlanService plan = mock(KitchenPlanService.class);
        when(slots.ensureExists(DATE, START)).thenReturn(5L);
        List<KitchenPlanner.Interval> intervals = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            intervals.add(new KitchenPlanner.Interval(DATE.atTime(11, 0).plusMinutes(15L * i), 20));
        }
        when(plan.snapshot())
                .thenAnswer(call -> new KitchenPlanService.Snapshot(
                        SETTINGS, intervals, List.copyOf(jobs), Map.copyOf(pickup), DATE.atTime(11, 0), 15));
        service = new QuotaService(slots, settings, plan);
    }

    private QuotaReservation reserve(int points, boolean walkIn) {
        return service.reserve(DATE, START, List.of(new QuotaLine(points, 1)), walkIn);
    }

    private static ErrorCode codeOf(Throwable e) {
        return ((ApiException) e).code();
    }

    @Test
    void orderPointsIsSumOfPointsTimesQuantity() {
        assertThat(QuotaService.orderPoints(List.of(new QuotaLine(5, 2), new QuotaLine(2, 3), new QuotaLine(0, 4))))
                .isEqualTo(16);
    }

    @Test
    void invalidLinesAreRejectedEarly() {
        assertThatThrownBy(() -> new QuotaLine(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuotaLine(2, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void successReturnsTheSlotAndRecordsThePoints() {
        QuotaReservation reservation = reserve(30, false);

        assertThat(reservation).isEqualTo(new QuotaReservation(5L, 30));
        verify(slots).addHeld(5L, 30);
    }

    @Test
    void pickupLimitIsCheckedFirst() {
        pickup.put(START, 8);

        assertThatThrownBy(() -> reserve(0, false))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SLOT_FULL))
                .satisfies(e -> assertThat(((ApiException) e).properties()).containsEntry("reason", "PICKUP_LIMIT"));
        verify(slots, never()).addHeld(anyLong(), anyInt());
    }

    @Test
    void preOrderLargerThanItsShareOfTheWindowIsTooLarge() {
        // Cửa sổ 11:15–12:00 có 3 khoảng × 14 điểm dành cho đặt trước = 42.
        assertThat(reserve(42, false).points()).isEqualTo(42);
        assertThatThrownBy(() -> reserve(43, false))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.ORDER_TOO_LARGE));
    }

    @Test
    void walkInMayUseTheWholeCapacityFromNow() {
        // Walk-in lúc 11:00 nhận lúc 12:00 làm được trong 11:00–12:15: 5 khoảng × 20 = 100 điểm.
        assertThat(reserve(100, true).points()).isEqualTo(100);
        assertThatThrownBy(() -> reserve(101, true))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.ORDER_TOO_LARGE));
    }

    @Test
    void kitchenWithoutRoomForTheNewOrderMeansSlotFull() {
        jobs.add(new KitchenPlanner.Job(
                1L, KitchenPlanner.Kind.PRE_ORDER, DATE.atTime(11, 15), DATE.atTime(12, 0), 40, false));

        assertThatThrownBy(() -> reserve(5, false))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SLOT_FULL))
                .satisfies(e -> assertThat(((ApiException) e).properties()).containsEntry("reason", "KITCHEN"));
        verify(slots, never()).addHeld(anyLong(), anyInt());
    }

    @Test
    void zeroPointOrderNeedsNoKitchenButCountsAtThePickupCounter() {
        jobs.add(new KitchenPlanner.Job(
                1L, KitchenPlanner.Kind.PRE_ORDER, DATE.atTime(11, 15), DATE.atTime(12, 0), 42, false));

        assertThat(reserve(0, false)).isEqualTo(new QuotaReservation(5L, 0));
        verify(slots, never()).addHeld(anyLong(), anyInt());
    }

    @Test
    void releaseRefusesToGoBelowZero() {
        when(slots.release(5L, 7)).thenReturn(false);

        assertThatThrownBy(() -> service.release(5L, 7)).isInstanceOf(IllegalStateException.class);
    }
}
