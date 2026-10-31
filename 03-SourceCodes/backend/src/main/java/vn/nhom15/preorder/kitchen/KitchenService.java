package vn.nhom15.preorder.kitchen;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.config.OrderingProperties;
import vn.nhom15.preorder.order.LateRule;
import vn.nhom15.preorder.order.OrderStatus;
import vn.nhom15.preorder.slot.KitchenPlanService;

/** UC-12.1 Xem hàng chờ bếp: chia ba nhóm, giữ thứ tự theo giờ khung rồi giờ xác nhận, đánh dấu đơn trễ. */
@Service
public class KitchenService {

    /** Nhóm của một đơn trong hàng chờ (UC-12 bước 2). */
    enum Group {
        PREPARING,
        DUE_SOON,
        UPCOMING
    }

    private final KitchenQueueRepository repository;
    private final OrderingProperties properties;
    private final Clock clock;
    private final KitchenPlanService kitchenPlanService;

    public KitchenService(
            KitchenQueueRepository repository,
            OrderingProperties properties,
            Clock clock,
            KitchenPlanService kitchenPlanService) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
        this.kitchenPlanService = kitchenPlanService;
    }

    @Transactional(readOnly = true)
    public KitchenQueue getQueue() {
        OffsetDateTime serverTime = OffsetDateTime.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        List<KitchenOrder> preparing = new ArrayList<>();
        List<KitchenOrder> dueSoon = new ArrayList<>();
        List<KitchenOrder> upcoming = new ArrayList<>();
        // PB-22: lịch bếp cho biết nên làm đơn nào lúc nào (làm sớm nhất có thể, trong giới hạn món còn ngon).
        Map<Long, LocalDateTime> plannedStart =
                kitchenPlanService.snapshot().current().firstIntervalByKey();
        for (KitchenQueueRepository.ActiveOrder active : repository.findActiveOrders(
                now.toLocalDate(), properties.slotMinutes(), properties.prepWindowMinutes())) {
            KitchenOrder found = active.order();
            LocalDateTime planned = plannedStart.get(found.id());
            LocalDateTime prepareFrom =
                    planned != null && planned.isBefore(found.prepareFrom()) ? planned : found.prepareFrom();
            KitchenOrder order = finish(
                    found,
                    LateRule.isLate(active.status(), found.source(), found.slotStart(), found.slotEnd(), now),
                    prepareFrom);
            switch (groupOf(active.status(), prepareFrom, now)) {
                case PREPARING -> preparing.add(order);
                case DUE_SOON -> dueSoon.add(order);
                case UPCOMING -> upcoming.add(order);
            }
        }
        return new KitchenQueue(serverTime, preparing, dueSoon, upcoming);
    }

    /**
     * Nhóm của một đơn: đang làm; hoặc đã xác nhận và đã tới giờ nên làm thì cần chuẩn bị; còn lại là sắp tới (bếp vẫn
     * làm trước được, BR-21). Giờ nên làm là giờ lịch bếp xếp, nhưng không muộn hơn {@code prepWindowMinutes} phút
     * trước giờ bắt đầu khung.
     */
    static Group groupOf(OrderStatus status, LocalDateTime prepareFrom, LocalDateTime now) {
        if (status == OrderStatus.PREPARING) {
            return Group.PREPARING;
        }
        return now.isBefore(prepareFrom) ? Group.UPCOMING : Group.DUE_SOON;
    }

    /** Đặt cờ trễ và viết thời điểm xác nhận theo múi giờ cửa hàng (database trả theo múi giờ của kết nối). */
    private KitchenOrder finish(KitchenOrder order, boolean late, LocalDateTime prepareFrom) {
        return new KitchenOrder(
                order.id(),
                order.pickupCode(),
                order.slotStart(),
                order.slotEnd(),
                order.source(),
                order.confirmedAt().atZoneSameInstant(clock.getZone()).toOffsetDateTime(),
                late,
                prepareFrom,
                order.lines());
    }
}
