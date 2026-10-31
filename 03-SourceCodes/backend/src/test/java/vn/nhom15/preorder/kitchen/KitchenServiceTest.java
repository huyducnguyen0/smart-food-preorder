package vn.nhom15.preorder.kitchen;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import vn.nhom15.preorder.kitchen.KitchenService.Group;
import vn.nhom15.preorder.order.LateRule;
import vn.nhom15.preorder.order.OrderSource;
import vn.nhom15.preorder.order.OrderStatus;

/** Quy tắc chia nhóm và đánh dấu trễ của hàng chờ bếp (UC-12 bước 2; BR-21, BR-26) tại các giá trị biên. */
class KitchenServiceTest {

    private static final LocalDateTime SLOT = LocalDateTime.of(2031, 3, 5, 11, 30);
    private static final int WINDOW = 15;

    /** Giờ nên làm mặc định: 15 phút trước khung (lịch bếp không xếp sớm hơn). */
    private static Group group(OrderStatus status, LocalDateTime now) {
        return KitchenService.groupOf(status, SLOT.minusMinutes(WINDOW), now);
    }

    @Test
    void confirmedOrderBecomesDueExactlyFifteenMinutesBeforeTheSlot() {
        assertThat(group(OrderStatus.CONFIRMED, SLOT.minusMinutes(16))).isEqualTo(Group.UPCOMING);
        assertThat(group(OrderStatus.CONFIRMED, SLOT.minusMinutes(15))).isEqualTo(Group.DUE_SOON); // biên: ≥
        assertThat(group(OrderStatus.CONFIRMED, SLOT.minusMinutes(1))).isEqualTo(Group.DUE_SOON);
    }

    @Test
    void confirmedOrderStaysDueAfterTheSlotHasStarted() {
        assertThat(group(OrderStatus.CONFIRMED, SLOT)).isEqualTo(Group.DUE_SOON);
        assertThat(group(OrderStatus.CONFIRMED, SLOT.plusMinutes(40))).isEqualTo(Group.DUE_SOON);
    }

    @Test
    void preparingOrderIsAlwaysInTheWorkingGroup() {
        assertThat(group(OrderStatus.PREPARING, SLOT.minusHours(3))).isEqualTo(Group.PREPARING);
        assertThat(group(OrderStatus.PREPARING, SLOT.plusHours(1))).isEqualTo(Group.PREPARING);
    }

    @Test
    void kitchenPlanCanMakeAnOrderDueEarlier() {
        // PB-22: lịch bếp xếp đơn khung 11:30 làm từ 10:45 (bếp đang vắng) thì 10:45 đã vào nhóm cần chuẩn bị.
        LocalDateTime planned = SLOT.minusMinutes(45);
        assertThat(KitchenService.groupOf(OrderStatus.CONFIRMED, planned, planned.minusMinutes(1)))
                .isEqualTo(Group.UPCOMING);
        assertThat(KitchenService.groupOf(OrderStatus.CONFIRMED, planned, planned))
                .isEqualTo(Group.DUE_SOON);
    }

    @Test
    void preOrderIsLateFromTheMomentTheSlotStarts() {
        LocalDateTime end = SLOT.plusMinutes(15);
        assertThat(LateRule.isLate(OrderStatus.CONFIRMED, OrderSource.PRE_ORDER, SLOT, end, SLOT.minusSeconds(1)))
                .isFalse();
        // BR-26: đã tới giờ bắt đầu khung là trễ
        assertThat(LateRule.isLate(OrderStatus.CONFIRMED, OrderSource.PRE_ORDER, SLOT, end, SLOT))
                .isTrue();
        assertThat(LateRule.isLate(OrderStatus.PREPARING, OrderSource.PRE_ORDER, SLOT, end, SLOT.plusMinutes(5)))
                .isTrue();
    }

    @Test
    void walkInIsLateOnlyFromTheEndOfItsSlot() {
        // Walk-in được xếp vào khung đang diễn ra: chỉ trễ khi đã qua giờ kết thúc khung (quyết định 2026-10-28).
        LocalDateTime end = SLOT.plusMinutes(15);
        assertThat(LateRule.isLate(OrderStatus.CONFIRMED, OrderSource.WALK_IN, SLOT, end, SLOT.plusMinutes(5)))
                .isFalse();
        assertThat(LateRule.isLate(OrderStatus.PREPARING, OrderSource.WALK_IN, SLOT, end, end.minusSeconds(1)))
                .isFalse();
        assertThat(LateRule.isLate(OrderStatus.CONFIRMED, OrderSource.WALK_IN, SLOT, end, end))
                .isTrue();
    }

    @Test
    void readyOrFinishedOrdersAreNeverLate() {
        LocalDateTime later = SLOT.plusHours(2);
        for (OrderStatus status :
                new OrderStatus[] {OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.CANCELLED, OrderStatus.NO_SHOW
                }) {
            assertThat(LateRule.isLate(status, OrderSource.PRE_ORDER, SLOT, SLOT.plusMinutes(15), later))
                    .isFalse();
        }
    }
}
