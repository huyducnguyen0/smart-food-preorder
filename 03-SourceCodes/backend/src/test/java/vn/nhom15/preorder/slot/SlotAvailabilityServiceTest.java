package vn.nhom15.preorder.slot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.OrderingProperties;
import vn.nhom15.preorder.menu.MenuItem;
import vn.nhom15.preorder.menu.MenuService;

/**
 * Danh sách khung giờ và kiểm tra khung (UC-04 bước 6, 8b) với đồng hồ cố định. Không cần database: các số liệu
 * khung/quota là dữ liệu giả để kiểm từng biên của quy tắc (BR-06, BR-15, BR-36).
 */
class SlotAvailabilityServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2031, 3, 5);
    private static final int DEFAULT_QUOTA = 20;

    private SlotRepository slots;
    private MenuService menu;
    private StoreSettingsRepository settings;
    private final List<KitchenPlanService.OpenOrder> orders = new java.util.ArrayList<>();

    /**
     * Cấu hình tương đương mô hình quota cũ: làm trước tối đa 15 phút, đặt trước dùng 100% công suất, quầy không giới
     * hạn. Khi đó đơn nhận lúc S chỉ được làm trong 15 phút ngay trước S, nên "khung S còn bao nhiêu" giống quota cũ.
     */
    private static StoreSettings legacy() {
        return new StoreSettings(LocalTime.of(10, 0), LocalTime.of(21, 0), DEFAULT_QUOTA, 15, 100, 1000);
    }

    /** Cấu hình mặc định của Sprint 4 (PB-22). */
    private static StoreSettings sprint4() {
        return new StoreSettings(LocalTime.of(10, 0), LocalTime.of(21, 0), DEFAULT_QUOTA, 45, 70, 8);
    }

    @BeforeEach
    void setUp() {
        slots = mock(SlotRepository.class);
        menu = mock(MenuService.class);
        settings = mock(StoreSettingsRepository.class);
        orders.clear();
        when(settings.get()).thenReturn(legacy());
        when(slots.findByDate(TODAY)).thenReturn(List.of());
        when(slots.findOrdersToCook(org.mockito.ArgumentMatchers.eq(TODAY), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> List.copyOf(orders));
        when(slots.countPickupOrdersByStart(TODAY)).thenAnswer(call -> {
            Map<LocalTime, Integer> counts = new java.util.HashMap<>();
            orders.forEach(order -> counts.merge(order.slotStart().toLocalTime(), 1, Integer::sum));
            return counts;
        });
        // Món 1: 5 điểm; món 2: 0 điểm (đồ uống); món 3: 25 điểm (lớn hơn quota mặc định).
        when(menu.findItems(anyCollection())).thenAnswer(call -> {
            Map<Long, MenuItem> all = Map.of(1L, item(1, 5), 2L, item(2, 0), 3L, item(3, 25));
            java.util.Collection<Long> ids = call.getArgument(0);
            return all.entrySet().stream()
                    .filter(e -> ids.contains(e.getKey()))
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        });
    }

    private static MenuItem item(long id, int points) {
        return new MenuItem(id, 1, "Món " + id, null, null, 10000, points, true, false);
    }

    private SlotAvailabilityService serviceAt(String localDateTime) {
        Clock clock =
                Clock.fixed(LocalDateTime.parse(localDateTime).atZone(ZONE).toInstant(), ZONE);
        OrderingProperties properties = new OrderingProperties(15, 30, 15, 3, 30);
        return new SlotAvailabilityService(
                settings, slots, menu, properties, clock, new KitchenPlanService(settings, slots, properties, clock));
    }

    private static List<CartLine> cart(long itemId, int quantity) {
        return List.of(new CartLine(itemId, quantity));
    }

    private static TimeSlot slot(int hour, int minute, Integer quotaOverride, int held) {
        return new TimeSlot(100 + hour * 4 + minute / 15, TODAY, LocalTime.of(hour, minute), quotaOverride, held);
    }

    /** Một đơn đặt trước đã nhận, {@code points} điểm, nhận lúc giờ:phút. */
    private void preOrder(int hour, int minute, int points) {
        orders.add(new KitchenPlanService.OpenOrder(
                orders.size() + 1, false, false, points, TODAY.atTime(hour, minute), TODAY.atTime(8, 0)));
    }

    @Test
    void listsEveryQuarterFromOpeningWhenTheStoreIsNotOpenYet() {
        SlotList list = serviceAt("2031-03-05T08:00").listSlots(cart(1, 1));

        assertThat(list.date()).isEqualTo(TODAY);
        assertThat(list.slots()).hasSize(44); // 10:00 → 20:45 là 44 khung 15 phút
        assertThat(list.slots().getFirst().start()).isEqualTo(TODAY.atTime(10, 0));
        assertThat(list.slots().getFirst().end()).isEqualTo(TODAY.atTime(10, 15));
        assertThat(list.slots().getLast().start()).isEqualTo(TODAY.atTime(20, 45));
        assertThat(list.slots().getLast().end()).isEqualTo(TODAY.atTime(21, 0));
        assertThat(list.unavailableReason()).isNull();
    }

    @Test
    void firstSlotIsExactlyThirtyMinutesAhead() {
        // BR-15: 10:00 + 30 phút = 10:30 → khung 10:30 còn nhận (biên), 10:15 thì không.
        assertThat(serviceAt("2031-03-05T10:00")
                        .listSlots(cart(1, 1))
                        .slots()
                        .getFirst()
                        .start())
                .isEqualTo(TODAY.atTime(10, 30));
        // Trễ một phút: 10:01 + 30 = 10:31 → khung đầu là 10:45.
        assertThat(serviceAt("2031-03-05T10:01")
                        .listSlots(cart(1, 1))
                        .slots()
                        .getFirst()
                        .start())
                .isEqualTo(TODAY.atTime(10, 45));
    }

    @Test
    void noSlotLeftTodayWhenTheLastSlotIsTooSoon() {
        // Khung cuối 20:45 cần đặt trước 20:15. 20:16 → không còn khung nào (E2).
        SlotList late = serviceAt("2031-03-05T20:16").listSlots(cart(1, 1));
        assertThat(late.slots()).isEmpty();
        assertThat(late.unavailableReason()).isEqualTo(SlotList.UnavailableReason.NO_SLOT_LEFT_TODAY);

        SlotList justInTime = serviceAt("2031-03-05T20:15").listSlots(cart(1, 1));
        assertThat(justInTime.slots()).extracting(SlotView::start).containsExactly(TODAY.atTime(20, 45));
    }

    @Test
    void bookableFollowsRemainingQuotaWithoutRevealingNumbers() {
        // Khung 12:00 đã có đơn 15 điểm → còn 5. Giỏ 5 điểm đặt được (biên), giỏ 10 thì không (BR-36: chỉ có cờ).
        preOrder(12, 0, 15);

        SlotList five = serviceAt("2031-03-05T09:00").listSlots(cart(1, 1));
        assertThat(bookable(five, 12, 0)).isTrue();

        SlotList ten = serviceAt("2031-03-05T09:00").listSlots(cart(1, 2));
        assertThat(bookable(ten, 12, 0)).isFalse();
        assertThat(bookable(ten, 12, 15)).isTrue();
        assertThat(ten.unavailableReason()).isNull();
    }

    @Test
    void quotaOverrideOfASlotIsUsedInsteadOfTheDefault() {
        // Công suất riêng của khoảng làm 11:45 (cho khung nhận 12:00) là 30, của khoảng 12:00 (cho 12:15) là 4.
        when(slots.findByDate(TODAY)).thenReturn(List.of(slot(11, 45, 30, 0), slot(12, 0, 4, 0)));

        SlotList list = serviceAt("2031-03-05T09:00").listSlots(cart(3, 1)); // 25 điểm

        assertThat(bookable(list, 12, 0)).as("quota riêng 30 chứa được 25").isTrue();
        assertThat(bookable(list, 12, 15)).as("quota riêng 4 nhỏ hơn mặc định").isFalse();
        assertThat(bookable(list, 12, 30)).as("quota mặc định 20 không chứa 25").isFalse();
        assertThat(list.unavailableReason()).isNull();
    }

    @Test
    void allSlotsFullWhenCartFitsAnEmptySlotButNoSlotHasRoom() {
        // Mọi khung hôm nay đã có đơn 16 điểm → còn 4 < 5 điểm của giỏ, nhưng 5 ≤ công suất 20 (E3).
        for (int i = 0; i < 44; i++) {
            preOrder(10 + (i * 15) / 60, (i * 15) % 60, 16);
        }

        SlotList list = serviceAt("2031-03-05T09:00").listSlots(cart(1, 1));

        assertThat(list.slots()).hasSize(44).allMatch(s -> !s.bookable());
        assertThat(list.unavailableReason()).isEqualTo(SlotList.UnavailableReason.ALL_SLOTS_FULL);
    }

    @Test
    void orderTooLargeWhenCartExceedsTheQuotaOfEverySlot() {
        SlotList list = serviceAt("2031-03-05T09:00").listSlots(cart(3, 1)); // 25 > 20

        assertThat(list.slots()).allMatch(s -> !s.bookable());
        assertThat(list.unavailableReason()).isEqualTo(SlotList.UnavailableReason.ORDER_TOO_LARGE);
    }

    @Test
    void zeroPointCartFitsEvenAFullSlot() {
        preOrder(12, 0, 20);

        SlotList list = serviceAt("2031-03-05T09:00").listSlots(cart(2, 3));

        assertThat(bookable(list, 12, 0)).isTrue(); // BR-13: đơn 0 điểm không chiếm quota
        assertThat(list.unavailableReason()).isNull();
    }

    @Test
    void invalidCartIsRejected() {
        SlotAvailabilityService service = serviceAt("2031-03-05T09:00");

        assertCode(() -> service.listSlots(List.of()), ErrorCode.VALIDATION_ERROR);
        assertCode(() -> service.listSlots(cart(1, 0)), ErrorCode.VALIDATION_ERROR);
        assertCode(() -> service.listSlots(cart(99, 1)), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void checkBookableAcceptsOnlyValidSlotsOfToday() {
        SlotAvailabilityService service = serviceAt("2031-03-05T10:00");

        service.checkBookable(TODAY.atTime(10, 30)); // đúng biên đặt trước
        service.checkBookable(TODAY.atTime(20, 45)); // khung cuối
        assertSlotRejected(service, TODAY.atTime(10, 15)); // quá gần
        assertSlotRejected(service, TODAY.atTime(21, 0)); // sau giờ đóng cửa
        assertSlotRejected(service, TODAY.atTime(12, 7)); // không đúng mốc 15 phút
        assertSlotRejected(service, TODAY.atTime(12, 0, 30)); // có giây
        assertSlotRejected(service, TODAY.plusDays(1).atTime(12, 0)); // không phải hôm nay
        assertSlotRejected(service, TODAY.minusDays(1).atTime(12, 0));
    }

    // ---------- PB-22: lịch bếp với cấu hình mặc định (làm trước 45 phút, đặt trước 70%, quầy 8 đơn) ----------

    @Test
    void preparingAheadAcceptsMoreThanOneIntervalForASlot() {
        when(settings.get()).thenReturn(sprint4());
        // Khung nhận 12:00 làm được trong 11:15, 11:30, 11:45: 3 × 14 = 42 điểm cho đặt trước (mô hình cũ chỉ 20).
        preOrder(12, 0, 35);

        SlotList list = serviceAt("2031-03-05T09:00").listSlots(cart(1, 1)); // thêm 5 → 40 ≤ 42

        assertThat(bookable(list, 12, 0)).isTrue();
        SlotList more = serviceAt("2031-03-05T09:00").listSlots(cart(1, 2)); // thêm 10 → 45 > 42
        assertThat(bookable(more, 12, 0)).isFalse();
    }

    @Test
    void pickupLimitClosesASlotEvenIfTheKitchenHasRoom() {
        when(settings.get()).thenReturn(sprint4());
        for (int i = 0; i < 8; i++) {
            preOrder(12, 0, 0); // 8 đơn chỉ có đồ uống: bếp không tốn công nhưng quầy đủ khách
        }

        SlotList list = serviceAt("2031-03-05T09:00").listSlots(cart(2, 1));

        assertThat(bookable(list, 12, 0)).isFalse();
        assertThat(bookable(list, 12, 15)).isTrue();
    }

    @Test
    void walkInUsesTheShareReservedForWalkIns() {
        when(settings.get()).thenReturn(sprint4());
        // Đặt trước đã lấy hết phần 70% của 11:00–11:45; walk-in lúc 11:00 vẫn còn 6 điểm mỗi khoảng.
        preOrder(11, 15, 14);
        preOrder(11, 30, 14);
        preOrder(11, 45, 14);
        preOrder(12, 0, 14);
        SlotAvailabilityService service = serviceAt("2031-03-05T11:00");

        assertThat(service.earliestWalkInSlot(5).start()).isEqualTo(TODAY.atTime(11, 0));
    }

    private static boolean bookable(SlotList list, int hour, int minute) {
        return list.slots().stream()
                .filter(s -> s.start().equals(TODAY.atTime(hour, minute)))
                .findFirst()
                .orElseThrow()
                .bookable();
    }

    private static void assertSlotRejected(SlotAvailabilityService service, LocalDateTime start) {
        assertCode(() -> service.checkBookable(start), ErrorCode.SLOT_NOT_BOOKABLE);
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo(code);
    }
}
