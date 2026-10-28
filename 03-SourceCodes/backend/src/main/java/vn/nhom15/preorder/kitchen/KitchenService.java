package vn.nhom15.preorder.kitchen;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.config.OrderingProperties;
import vn.nhom15.preorder.order.LateRule;
import vn.nhom15.preorder.order.OrderStatus;

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

    public KitchenService(KitchenQueueRepository repository, OrderingProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public KitchenQueue getQueue() {
        OffsetDateTime serverTime = OffsetDateTime.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        List<KitchenOrder> preparing = new ArrayList<>();
        List<KitchenOrder> dueSoon = new ArrayList<>();
        List<KitchenOrder> upcoming = new ArrayList<>();
        for (KitchenQueueRepository.ActiveOrder active :
                repository.findActiveOrders(now.toLocalDate(), properties.slotMinutes())) {
            KitchenOrder found = active.order();
            KitchenOrder order = finish(
                    found, LateRule.isLate(active.status(), found.source(), found.slotStart(), found.slotEnd(), now));
            switch (groupOf(active.status(), order.slotStart(), now, properties.prepWindowMinutes())) {
                case PREPARING -> preparing.add(order);
                case DUE_SOON -> dueSoon.add(order);
                case UPCOMING -> upcoming.add(order);
            }
        }
        return new KitchenQueue(serverTime, preparing, dueSoon, upcoming);
    }

    /**
     * Nhóm của một đơn: đang làm; hoặc đã xác nhận và đã tới {@code prepWindowMinutes} phút trước giờ bắt đầu khung
     * (BR-21) thì cần chuẩn bị; còn lại là sắp tới.
     */
    static Group groupOf(OrderStatus status, LocalDateTime slotStart, LocalDateTime now, int prepWindowMinutes) {
        if (status == OrderStatus.PREPARING) {
            return Group.PREPARING;
        }
        return now.isBefore(slotStart.minusMinutes(prepWindowMinutes)) ? Group.UPCOMING : Group.DUE_SOON;
    }

    /** Đặt cờ trễ và viết thời điểm xác nhận theo múi giờ cửa hàng (database trả theo múi giờ của kết nối). */
    private KitchenOrder finish(KitchenOrder order, boolean late) {
        return new KitchenOrder(
                order.id(),
                order.pickupCode(),
                order.slotStart(),
                order.slotEnd(),
                order.source(),
                order.confirmedAt().atZoneSameInstant(clock.getZone()).toOffsetDateTime(),
                late,
                order.lines());
    }
}
