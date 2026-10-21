package vn.nhom15.preorder.slot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/** Logic quyết định của UC-17 (không cần database): tính điểm, đơn 0 điểm, phân biệt E1 và E2. */
class QuotaServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 21);
    private static final LocalTime START = LocalTime.of(12, 0);
    private static final int DEFAULT_QUOTA = 20;

    private SlotRepository slots;
    private QuotaService service;

    @BeforeEach
    void setUp() {
        slots = mock(SlotRepository.class);
        StoreSettingsRepository settings = mock(StoreSettingsRepository.class);
        when(settings.get()).thenReturn(new StoreSettings(LocalTime.of(10, 0), LocalTime.of(21, 0), DEFAULT_QUOTA));
        when(slots.ensureExists(DATE, START)).thenReturn(5L);
        service = new QuotaService(slots, settings);
    }

    private static List<QuotaLine> lines(int... pointsAndQuantity) {
        // Mỗi cặp (điểm, số lượng) là một dòng đơn.
        return java.util.stream.IntStream.range(0, pointsAndQuantity.length / 2)
                .mapToObj(i -> new QuotaLine(pointsAndQuantity[2 * i], pointsAndQuantity[2 * i + 1]))
                .toList();
    }

    @Test
    void orderPointsIsSumOfPointsTimesQuantity() {
        // BR-08: 3×2 + 1×4 + 0×5 = 10
        assertThat(QuotaService.orderPoints(lines(3, 2, 1, 4, 0, 5))).isEqualTo(10);
        assertThat(QuotaService.orderPoints(List.of())).isZero();
    }

    @Test
    void invalidLinesAreRejectedEarly() {
        assertThatThrownBy(() -> new QuotaLine(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuotaLine(2, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void successReturnsTheHeldSlotAndPoints() {
        when(slots.tryHold(DATE, START, 7, DEFAULT_QUOTA)).thenReturn(Optional.of(5L));

        QuotaReservation reservation = service.reserve(DATE, START, lines(3, 2, 1, 1));

        assertThat(reservation).isEqualTo(new QuotaReservation(5L, 7));
    }

    @Test
    void zeroPointOrderDoesNotTouchQuota() {
        // BR-13: đơn toàn đồ uống 0 điểm không chiếm quota, nhưng vẫn trả về khung để gắn vào đơn.
        QuotaReservation reservation = service.reserve(DATE, START, lines(0, 3));

        assertThat(reservation).isEqualTo(new QuotaReservation(5L, 0));
        verify(slots, never())
                .tryHold(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyInt(), anyInt());
    }

    @Test
    void notEnoughRemainingQuotaReportsSlotFull() {
        // UC-17 E2: đơn 8 điểm, quota khung 20, nhưng chỉ còn lại ít hơn 8.
        when(slots.tryHold(DATE, START, 8, DEFAULT_QUOTA)).thenReturn(Optional.empty());
        when(slots.find(DATE, START)).thenReturn(Optional.of(new TimeSlot(5L, DATE, START, null, 15)));

        assertThatThrownBy(() -> service.reserve(DATE, START, lines(8, 1)))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.SLOT_FULL));
    }

    @Test
    void orderLargerThanTheWholeSlotReportsTooLarge() {
        // UC-17 E1: đơn 21 điểm lớn hơn cả quota 20 của khung, kể cả khi khung còn trống.
        when(slots.tryHold(DATE, START, 21, DEFAULT_QUOTA)).thenReturn(Optional.empty());
        when(slots.find(DATE, START)).thenReturn(Optional.of(new TimeSlot(5L, DATE, START, null, 0)));

        assertThatThrownBy(() -> service.reserve(DATE, START, lines(7, 3)))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDER_TOO_LARGE));
    }

    @Test
    void slotsOwnQuotaTakesPrecedenceWhenDecidingTooLarge() {
        // Quản lý đã sửa quota riêng của khung xuống 6: đơn 8 điểm là "quá lớn" dù quota mặc định là 20.
        when(slots.tryHold(DATE, START, 8, DEFAULT_QUOTA)).thenReturn(Optional.empty());
        when(slots.find(DATE, START)).thenReturn(Optional.of(new TimeSlot(5L, DATE, START, 6, 0)));

        assertThatThrownBy(() -> service.reserve(DATE, START, lines(8, 1)))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDER_TOO_LARGE));
    }

    @Test
    void timeSlotQuotaUsesOverrideOtherwiseDefault() {
        assertThat(new TimeSlot(1, DATE, START, null, 0).quota(20)).isEqualTo(20);
        assertThat(new TimeSlot(1, DATE, START, 12, 0).quota(20)).isEqualTo(12);
    }
}
