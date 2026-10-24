package vn.nhom15.preorder.kitchen;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import vn.nhom15.preorder.kitchen.KitchenService.Group;
import vn.nhom15.preorder.order.OrderStatus;

/** Quy tắc chia nhóm và đánh dấu trễ của hàng chờ bếp (UC-12 bước 2; BR-21, BR-26) tại các giá trị biên. */
class KitchenServiceTest {

    private static final LocalDateTime SLOT = LocalDateTime.of(2031, 3, 5, 11, 30);
    private static final int WINDOW = 15;

    private static Group group(OrderStatus status, LocalDateTime now) {
        return KitchenService.groupOf(status, SLOT, now, WINDOW);
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
    void windowFollowsTheConfiguredNumberOfMinutes() {
        assertThat(KitchenService.groupOf(OrderStatus.CONFIRMED, SLOT, SLOT.minusMinutes(20), 30))
                .isEqualTo(Group.DUE_SOON);
        assertThat(KitchenService.groupOf(OrderStatus.CONFIRMED, SLOT, SLOT.minusMinutes(20), 0))
                .isEqualTo(Group.UPCOMING);
    }

    @Test
    void orderIsLateFromTheMomentTheSlotStarts() {
        assertThat(KitchenService.isLate(SLOT, SLOT.minusSeconds(1))).isFalse();
        assertThat(KitchenService.isLate(SLOT, SLOT)).isTrue(); // BR-26: đã tới giờ bắt đầu khung là trễ
        assertThat(KitchenService.isLate(SLOT, SLOT.plusMinutes(5))).isTrue();
    }
}
