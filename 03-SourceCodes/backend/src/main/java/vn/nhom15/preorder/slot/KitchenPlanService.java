package vn.nhom15.preorder.slot;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import vn.nhom15.preorder.config.OrderingProperties;

/**
 * Dựng lịch bếp của hôm nay từ database (PB-22): các khoảng làm 15 phút với công suất, các đơn còn phải làm, số đơn mỗi
 * khung nhận. Dùng chung cho nhận đơn (đặt trước, walk-in), danh sách khung của khách, hàng chờ bếp và màn quản lý.
 */
@Service
public class KitchenPlanService {

    /** Một đơn còn phải làm, như lịch bếp cần biết. */
    public record OpenOrder(
            long id,
            boolean walkIn,
            boolean preparing,
            int points,
            LocalDateTime slotStart,
            LocalDateTime confirmedAt) {}

    /**
     * Ảnh chụp lịch bếp tại một thời điểm, để thử thêm một đơn mà không phải đọc lại database.
     *
     * @param settings cấu hình cửa hàng
     * @param intervals các khoảng làm còn dùng được (khoảng hiện tại đã trừ phần thời gian đã qua)
     * @param jobs các đơn đã nhận còn phải làm
     * @param pickupOrders số đơn (không tính đơn hủy) theo giờ bắt đầu khung nhận
     * @param now thời điểm chụp
     * @param slotMinutes độ dài khung
     */
    public record Snapshot(
            StoreSettings settings,
            List<KitchenPlanner.Interval> intervals,
            List<KitchenPlanner.Job> jobs,
            Map<LocalTime, Integer> pickupOrders,
            LocalDateTime now,
            int slotMinutes) {

        /** Lịch hiện tại của các đơn đã nhận. */
        public KitchenPlanner.Result current() {
            return KitchenPlanner.plan(intervals, slotMinutes, jobs, 100);
        }

        /** Việc của một đơn đặt trước mới nhận lúc {@code slotStart}: làm trong [giờ nhận − H, giờ nhận]. */
        public KitchenPlanner.Job preOrderJob(LocalDateTime slotStart, int points) {
            return new KitchenPlanner.Job(
                    null,
                    KitchenPlanner.Kind.PRE_ORDER,
                    slotStart.minusMinutes(settings.holdMinutes()),
                    slotStart,
                    points,
                    false);
        }

        /** Việc của một đơn walk-in mới: làm từ bây giờ, xong trong khung nhận. */
        public KitchenPlanner.Job walkInJob(LocalDateTime slotStart, int points) {
            return new KitchenPlanner.Job(
                    null,
                    KitchenPlanner.Kind.WALK_IN,
                    floor(now, slotMinutes),
                    slotStart.plusMinutes(slotMinutes),
                    points,
                    false);
        }

        /**
         * Nhận thêm việc này được không. Hai điều kiện độc lập:
         *
         * <ol>
         *   <li>Công suất bếp: mọi đơn đã nhận cùng đơn mới xếp kịp với toàn bộ công suất. Đây là bảo đảm bếp không
         *       bao giờ bị giao quá sức (NFR-01).
         *   <li>Phần dành cho walk-in, chỉ với đơn đặt trước mới: riêng các đơn đặt trước (kể cả đơn mới) xếp kịp trong
         *       phần công suất dành cho đặt trước. Quản lý giảm tỉ lệ này thì chỉ chặn đơn đặt trước mới, walk-in vẫn
         *       nhận theo công suất còn lại.
         * </ol>
         */
        public boolean fits(KitchenPlanner.Job extra) {
            if (extra.work() == 0) {
                return true;
            }
            List<KitchenPlanner.Job> all = new ArrayList<>(jobs);
            all.add(extra);
            if (!KitchenPlanner.plan(intervals, slotMinutes, all, 100).feasible()) {
                return false;
            }
            if (extra.kind() != KitchenPlanner.Kind.PRE_ORDER) {
                return true;
            }
            List<KitchenPlanner.Job> preOrders = all.stream()
                    .filter(job -> job.kind() == KitchenPlanner.Kind.PRE_ORDER)
                    .toList();
            return KitchenPlanner.plan(intervals, slotMinutes, preOrders, settings.preOrderSharePercent())
                    .feasible();
        }

        /** Các đơn đã nhận còn xếp kịp với toàn bộ công suất (dùng khi quản lý giảm công suất hay đổi cấu hình). */
        public boolean acceptedOrdersStillFit() {
            return KitchenPlanner.plan(intervals, slotMinutes, jobs, 100).feasible();
        }

        /** Lịch trống (không có đơn nào) vẫn không xếp được việc này: đơn quá lớn so với bếp, đổi khung cũng vô ích. */
        public boolean tooLargeEvenWhenEmpty(KitchenPlanner.Job job) {
            int share = job.kind() == KitchenPlanner.Kind.PRE_ORDER ? settings.preOrderSharePercent() : 100;
            return job.work() > 0
                    && !KitchenPlanner.plan(intervals, slotMinutes, List.of(job), share)
                            .feasible();
        }

        /** Khung nhận còn chỗ ở quầy. */
        public boolean pickupHasRoom(LocalTime slotStart) {
            return pickupOrders.getOrDefault(slotStart, 0) < settings.pickupLimitOrders();
        }
    }

    private final StoreSettingsRepository settingsRepository;
    private final SlotRepository slotRepository;
    private final OrderingProperties properties;
    private final Clock clock;

    public KitchenPlanService(
            StoreSettingsRepository settingsRepository,
            SlotRepository slotRepository,
            OrderingProperties properties,
            Clock clock) {
        this.settingsRepository = settingsRepository;
        this.slotRepository = slotRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /** Lịch bếp của hôm nay tại thời điểm hiện tại, theo cấu hình đang lưu. */
    public Snapshot snapshot() {
        return snapshot(settingsRepository.get(), Map.of());
    }

    /**
     * Lịch bếp với cấu hình và công suất riêng giả định (để kiểm tra trước khi quản lý đổi).
     *
     * @param settings cấu hình dùng để dựng lịch
     * @param capacityOverrides công suất riêng thay thế theo giờ bắt đầu khoảng; giá trị null nghĩa là bỏ công suất
     *     riêng
     */
    public Snapshot snapshot(StoreSettings settings, Map<LocalTime, Integer> capacityOverrides) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        int slot = properties.slotMinutes();
        Map<LocalTime, TimeSlot> rows = slotRepository.findByDate(today).stream()
                .collect(Collectors.toMap(TimeSlot::startTime, Function.identity()));

        // Khoảng làm: từ (giờ mở cửa − H) để đơn của các khung đầu có thời gian làm trước, tới giờ đóng cửa.
        List<KitchenPlanner.Interval> intervals = new ArrayList<>();
        LocalDateTime first = floor(today.atTime(settings.openTime()).minusMinutes(settings.holdMinutes()), slot);
        LocalDateTime close = today.atTime(settings.closeTime());
        for (LocalDateTime start = first; start.isBefore(close); start = start.plusMinutes(slot)) {
            LocalDateTime end = start.plusMinutes(slot);
            if (!end.isAfter(now)) {
                continue;
            }
            int capacity;
            LocalTime key = start.toLocalTime();
            if (start.toLocalDate().equals(today) && capacityOverrides.containsKey(key)) {
                Integer override = capacityOverrides.get(key);
                capacity = override != null ? override : settings.defaultQuotaPoints();
            } else {
                TimeSlot row = start.toLocalDate().equals(today) ? rows.get(key) : null;
                capacity = row != null ? row.quota(settings.defaultQuotaPoints()) : settings.defaultQuotaPoints();
            }
            if (start.isBefore(now)) {
                // Khoảng đang diễn ra: chỉ còn phần thời gian chưa qua.
                long left = Duration.between(now, end).toSeconds();
                capacity = (int) (capacity * left / (slot * 60L));
            }
            intervals.add(new KitchenPlanner.Interval(start, capacity));
        }

        List<KitchenPlanner.Job> jobs = new ArrayList<>();
        for (OpenOrder order : slotRepository.findOrdersToCook(today, clock.getZone())) {
            jobs.add(jobOf(order, settings, now, slot));
        }
        return new Snapshot(settings, intervals, jobs, slotRepository.countPickupOrdersByStart(today), now, slot);
    }

    private static KitchenPlanner.Job jobOf(OpenOrder order, StoreSettings settings, LocalDateTime now, int slot) {
        LocalDateTime release;
        LocalDateTime deadline;
        KitchenPlanner.Kind kind;
        if (order.walkIn()) {
            kind = KitchenPlanner.Kind.WALK_IN;
            release = floor(order.confirmedAt(), slot);
            deadline = order.slotStart().plusMinutes(slot);
        } else {
            kind = KitchenPlanner.Kind.PRE_ORDER;
            release = order.slotStart().minusMinutes(settings.holdMinutes());
            deadline = order.slotStart();
        }
        if (order.preparing()) {
            // Bếp đang làm (có thể đã làm trước cả cửa sổ): coi như làm được ngay từ bây giờ.
            release = floor(now, slot);
        }
        boolean late = !deadline.isAfter(now);
        return new KitchenPlanner.Job(order.id(), kind, release, deadline, order.points(), late);
    }

    static LocalDateTime floor(LocalDateTime time, int slotMinutes) {
        int minute = time.getMinute() - time.getMinute() % slotMinutes;
        return time.withMinute(minute).withSecond(0).withNano(0);
    }
}
