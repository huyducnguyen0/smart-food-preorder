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
import java.util.stream.Collectors;
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

    public SlotAvailabilityService(
            StoreSettingsRepository storeSettingsRepository,
            SlotRepository slotRepository,
            MenuService menuService,
            OrderingProperties properties,
            Clock clock) {
        this.storeSettingsRepository = storeSettingsRepository;
        this.slotRepository = slotRepository;
        this.menuService = menuService;
        this.properties = properties;
        this.clock = clock;
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
        Map<LocalTime, TimeSlot> existing =
                slotRepository.findByDate(today).stream().collect(Collectors.toMap(TimeSlot::startTime, s -> s));

        List<SlotView> views = new ArrayList<>();
        boolean anyBookable = false;
        boolean fitsSomeSlot = false;
        for (LocalTime start : candidates) {
            TimeSlot slot = existing.get(start);
            int quota = slot != null ? slot.quota(settings.defaultQuotaPoints()) : settings.defaultQuotaPoints();
            int held = slot != null ? slot.heldPoints() : 0;
            boolean bookable = points <= quota - held;
            anyBookable |= bookable;
            fitsSomeSlot |= points <= quota;
            LocalDateTime startAt = today.atTime(start);
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
        LocalDateTime now = LocalDateTime.now(clock);
        boolean valid = slotStart.toLocalDate().equals(now.toLocalDate())
                && slotStart.getSecond() == 0
                && slotStart.getNano() == 0
                && candidateStartTimes(storeSettingsRepository.get(), now).contains(slotStart.toLocalTime());
        if (!valid) {
            throw new ApiException(ErrorCode.SLOT_NOT_BOOKABLE, "Khung giờ này không còn đặt được, vui lòng chọn lại");
        }
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
