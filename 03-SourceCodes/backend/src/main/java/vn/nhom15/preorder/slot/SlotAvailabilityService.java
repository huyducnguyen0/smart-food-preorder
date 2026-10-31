package vn.nhom15.preorder.slot;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.OrderingProperties;
import vn.nhom15.preorder.menu.MenuItem;
import vn.nhom15.preorder.menu.MenuService;

/**
 * Khung giờ khách chọn được (UC-04 bước 6) và kiểm tra lại một khung khi gửi đơn (UC-04 bước 8b). Chỉ đọc: việc giữ
 * quota thật nằm ở {@link QuotaService}, nên tình trạng ở đây chỉ đúng tại thời điểm hiển thị (BR-36).
 */
@Service
public class SlotAvailabilityService {

    private final StoreSettingsRepository storeSettingsRepository;
    private final SlotRepository slotRepository;
    private final MenuService menuService;
    private final OrderingProperties properties;
    private final Clock clock;
    private final KitchenPlanService kitchenPlanService;

    public SlotAvailabilityService(
            StoreSettingsRepository storeSettingsRepository,
            SlotRepository slotRepository,
            MenuService menuService,
            OrderingProperties properties,
            Clock clock,
            KitchenPlanService kitchenPlanService) {
        this.storeSettingsRepository = storeSettingsRepository;
        this.slotRepository = slotRepository;
        this.menuService = menuService;
        this.properties = properties;
        this.clock = clock;
        this.kitchenPlanService = kitchenPlanService;
    }

    /**
     * Các khung hôm nay kèm tình trạng đặt được theo điểm của giỏ.
     *
     * @throws ApiException {@code VALIDATION_ERROR} nếu giỏ rỗng, số lượng nhỏ hơn 1 hoặc món không tồn tại
     */
    public SlotList listSlots(List<CartLine> lines) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        StoreSettings settings = storeSettingsRepository.get();

        List<LocalTime> candidates = candidateStartTimes(settings, now);
        if (candidates.isEmpty()) {
            return new SlotList(today, List.of(), SlotList.UnavailableReason.NO_SLOT_LEFT_TODAY);
        }

        int points = cartPoints(lines);
        // PB-22: khung đặt được khi quầy còn chỗ và bếp còn xếp kịp làm giỏ này trước giờ nhận (làm trước được tối đa
        // H phút). Chỉ đọc: kết quả đúng tại thời điểm hiển thị, việc giữ chỗ thật kiểm lại trong giao dịch (BR-36).
        KitchenPlanService.Snapshot plan = kitchenPlanService.snapshot();

        List<SlotView> views = new ArrayList<>();
        boolean anyBookable = false;
        boolean fitsSomeSlot = false;
        for (LocalTime start : candidates) {
            LocalDateTime startAt = today.atTime(start);
            KitchenPlanner.Job job = plan.preOrderJob(startAt, points);
            boolean fitsEmpty = !plan.tooLargeEvenWhenEmpty(job);
            boolean bookable = fitsEmpty && plan.pickupHasRoom(start) && plan.fits(job);
            anyBookable |= bookable;
            fitsSomeSlot |= fitsEmpty;
            views.add(new SlotView(startAt, startAt.plusMinutes(properties.slotMinutes()), bookable));
        }

        SlotList.UnavailableReason reason = null;
        if (!anyBookable) {
            reason = fitsSomeSlot
                    ? SlotList.UnavailableReason.ALL_SLOTS_FULL
                    : SlotList.UnavailableReason.ORDER_TOO_LARGE;
        }
        return new SlotList(today, List.copyOf(views), reason);
    }

    /**
     * Khung còn hợp lệ để đặt tại thời điểm này (BR-06, BR-15): hôm nay, đúng mốc, trong giờ mở cửa, cách hiện tại đủ
     * thời gian đặt trước.
     *
     * @throws ApiException {@code SLOT_NOT_BOOKABLE} nếu không hợp lệ
     */
    public void checkBookable(LocalDateTime slotStart) {
        // Khóa chia sẻ cấu hình TRƯỚC khi đọc giờ mở cửa, giữ tới hết giao dịch đặt đơn: quản lý thu hẹp giờ cùng lúc
        // phải chờ đơn này xong rồi mới kiểm tra "khung có đơn", nên không lọt đơn ngoài giờ mới (BR-11).
        storeSettingsRepository.lockShared();
        LocalDateTime now = LocalDateTime.now(clock);
        boolean valid = slotStart.toLocalDate().equals(now.toLocalDate())
                && slotStart.getSecond() == 0
                && slotStart.getNano() == 0
                && candidateStartTimes(storeSettingsRepository.get(), now).contains(slotStart.toLocalTime());
        if (!valid) {
            throw new ApiException(ErrorCode.SLOT_NOT_BOOKABLE, "Khung giờ này không còn đặt được, vui lòng chọn lại");
        }
    }

    /**
     * UC-18 / BR-16: tối đa 3 khung hợp lệ (BR-15) còn đủ quota cho giỏ, khác khung đã chọn, gần khung đã chọn nhất cả
     * trước lẫn sau; cùng khoảng cách thì khung sau đứng trước. Rỗng nếu không còn khung nào.
     */
    public List<SlotView> alternatives(LocalDateTime chosen, List<CartLine> lines) {
        return listSlots(lines).slots().stream()
                .filter(SlotView::bookable)
                .filter(slot -> !slot.start().equals(chosen))
                .sorted(java.util.Comparator.comparingLong((SlotView slot) -> Math.abs(
                                java.time.Duration.between(chosen, slot.start()).toMinutes()))
                        .thenComparing(SlotView::start, java.util.Comparator.reverseOrder()))
                .limit(3)
                .toList();
    }

    /**
     * BR-18: khung dự kiến sớm nhất cho đơn walk-in, tính từ khung hiện tại (khung chứa thời điểm bây giờ), còn đủ
     * quota.
     * Không áp dụng mốc đặt trước 30 phút của BR-15. Chỉ để báo khách, chưa giữ quota (BR-19).
     *
     * @throws ApiException {@code ORDER_TOO_LARGE} nếu đơn lớn hơn quota của mọi khung còn lại;
     *     {@code NO_SLOT_AVAILABLE}
     *     nếu không còn khung nào đủ chỗ hôm nay
     */
    public SlotView earliestWalkInSlot(int points) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        StoreSettings settings = storeSettingsRepository.get();
        KitchenPlanService.Snapshot plan = kitchenPlanService.snapshot();
        List<LocalTime> starts = walkInStartTimes(settings, now);
        boolean fitsSomeSlot = false;
        for (LocalTime start : starts) {
            LocalDateTime startAt = today.atTime(start);
            KitchenPlanner.Job job = plan.walkInJob(startAt, points);
            boolean fitsEmpty = !plan.tooLargeEvenWhenEmpty(job);
            fitsSomeSlot |= fitsEmpty;
            if (fitsEmpty && plan.pickupHasRoom(start) && plan.fits(job)) {
                return new SlotView(startAt, startAt.plusMinutes(properties.slotMinutes()), true);
            }
        }
        if (!fitsSomeSlot && !starts.isEmpty()) {
            throw new ApiException(
                    ErrorCode.ORDER_TOO_LARGE, "Đơn quá lớn cho một khung giờ, hãy tách đơn hoặc bớt món");
        }
        throw new ApiException(ErrorCode.NO_SLOT_AVAILABLE, "Hôm nay không còn khung nào đủ chỗ cho đơn này");
    }

    /**
     * Khung walk-in nhân viên xác nhận còn hợp lệ: hôm nay, đúng mốc, trong giờ mở cửa, chưa kết thúc.
     *
     * @throws ApiException {@code SLOT_NOT_BOOKABLE} nếu không hợp lệ
     */
    public void checkWalkInSlot(LocalDateTime slotStart) {
        storeSettingsRepository.lockShared(); // như checkBookable (BR-11)
        LocalDateTime now = LocalDateTime.now(clock);
        boolean valid = slotStart.toLocalDate().equals(now.toLocalDate())
                && slotStart.getSecond() == 0
                && slotStart.getNano() == 0
                && walkInStartTimes(storeSettingsRepository.get(), now).contains(slotStart.toLocalTime());
        if (!valid) {
            throw new ApiException(ErrorCode.SLOT_NOT_BOOKABLE, "Khung giờ này không còn nhận đơn, vui lòng xem lại");
        }
    }

    /** Khung hôm nay chưa kết thúc: từ khung hiện tại tới khung cuối trong giờ mở cửa. */
    private List<LocalTime> walkInStartTimes(StoreSettings settings, LocalDateTime now) {
        List<LocalTime> result = new ArrayList<>();
        LocalTime start = settings.openTime();
        while (!start.plusMinutes(properties.slotMinutes()).isAfter(settings.closeTime())) {
            if (now.toLocalDate()
                    .atTime(start)
                    .plusMinutes(properties.slotMinutes())
                    .isAfter(now)) {
                result.add(start);
            }
            LocalTime next = start.plusMinutes(properties.slotMinutes());
            if (next.isBefore(start)) {
                break;
            }
            start = next;
        }
        return result;
    }

    /** Điểm của giỏ; món tạm hết vẫn tính điểm vì lỗi món chỉ báo khi gửi đơn (UC-04 E7). */
    private int cartPoints(List<CartLine> lines) {
        if (lines.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Giỏ hàng đang trống");
        }
        Set<Long> ids = new HashSet<>();
        for (CartLine line : lines) {
            if (line.quantity() < 1) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Số lượng phải từ 1");
            }
            ids.add(line.menuItemId());
        }
        Map<Long, MenuItem> items = menuService.findItems(ids);
        if (!items.keySet().containsAll(ids)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Giỏ có món không tồn tại");
        }
        List<QuotaLine> quotaLines = lines.stream()
                .map(line -> new QuotaLine(items.get(line.menuItemId()).workloadPoints(), line.quantity()))
                .toList();
        return QuotaService.orderPoints(quotaLines);
    }

    /** Giờ bắt đầu các khung hợp lệ hôm nay: từ giờ mở cửa, mỗi khung đủ trước giờ đóng, cách bây giờ đủ thời gian. */
    private List<LocalTime> candidateStartTimes(StoreSettings settings, LocalDateTime now) {
        LocalDateTime earliest = now.plusMinutes(properties.minLeadMinutes());
        List<LocalTime> result = new ArrayList<>();
        LocalTime start = settings.openTime();
        while (!start.plusMinutes(properties.slotMinutes()).isAfter(settings.closeTime())) {
            if (!now.toLocalDate().atTime(start).isBefore(earliest)) {
                result.add(start);
            }
            LocalTime next = start.plusMinutes(properties.slotMinutes());
            if (next.isBefore(start)) {
                break; // vượt qua nửa đêm
            }
            start = next;
        }
        return result;
    }
}
