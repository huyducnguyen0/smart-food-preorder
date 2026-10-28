package vn.nhom15.preorder.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.config.OrderingProperties;
import vn.nhom15.preorder.order.LateRule;
import vn.nhom15.preorder.order.OrderSource;
import vn.nhom15.preorder.order.OrderStatus;
import vn.nhom15.preorder.slot.AdminSlotService;

/**
 * UC-16 Theo dõi vận hành (PB-17, FR-20): theo từng khung trong ngày (quota, điểm đã giữ, số đơn), đơn đang xử lý
 * và đơn trễ (BR-26), thống kê ngày. Số liệu là mức tải theo quota cửa hàng tự đặt, **không** phải năng lực bếp
 * thực tế (NT9).
 */
@Service
public class DashboardService {

    /** Một đơn đang xử lý, kèm cờ trễ theo {@link LateRule}. */
    public record DashboardOrder(
            long id,
            String pickupCode,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime slotStart,
            String status,
            String source,
            boolean late) {}

    /** Thống kê ngày theo FR-20. */
    public record DayStats(
            int preOrders,
            int walkIns,
            int pickedUp,
            int cancelled,
            int noShow,
            int lateNow,
            int heldPoints,
            int quotaPoints,
            int quotaUsagePercent) {}

    public record Dashboard(
            LocalDate date,
            List<AdminSlotService.AdminSlot> slots,
            List<DashboardOrder> activeOrders,
            DayStats stats) {}

    private final DashboardRepository repository;
    private final AdminSlotService adminSlotService;
    private final OrderingProperties properties;
    private final Clock clock;

    public DashboardService(
            DashboardRepository repository,
            AdminSlotService adminSlotService,
            OrderingProperties properties,
            Clock clock) {
        this.repository = repository;
        this.adminSlotService = adminSlotService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Dashboard today() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        AdminSlotService.AdminDay day = adminSlotService.today();

        List<DashboardOrder> active = repository.findActiveOrders(today).stream()
                .map(order -> new DashboardOrder(
                        order.id(),
                        order.pickupCode(),
                        order.slotStart(),
                        order.status(),
                        order.source(),
                        LateRule.isLate(
                                OrderStatus.valueOf(order.status()),
                                OrderSource.valueOf(order.source()),
                                order.slotStart(),
                                order.slotStart().plusMinutes(properties.slotMinutes()),
                                now)))
                .toList();

        Map<String, Integer> counts = repository.countBySourceAndStatus(today);
        int preOrders = sum(counts, "PRE_ORDER:");
        int walkIns = sum(counts, "WALK_IN:");
        int held = day.slots().stream()
                .mapToInt(AdminSlotService.AdminSlot::heldPoints)
                .sum();
        int quota =
                day.slots().stream().mapToInt(AdminSlotService.AdminSlot::quota).sum();
        DayStats stats = new DayStats(
                preOrders,
                walkIns,
                sum(counts, ":PICKED_UP"),
                sum(counts, ":CANCELLED"),
                sum(counts, ":NO_SHOW"),
                (int) active.stream().filter(DashboardOrder::late).count(),
                held,
                quota,
                quota == 0 ? 0 : Math.round(held * 100f / quota));
        return new Dashboard(today, day.slots(), active, stats);
    }

    private static int sum(Map<String, Integer> counts, String part) {
        return counts.entrySet().stream()
                .filter(e -> part.endsWith(":")
                        ? e.getKey().startsWith(part)
                        : e.getKey().endsWith(part))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }
}
