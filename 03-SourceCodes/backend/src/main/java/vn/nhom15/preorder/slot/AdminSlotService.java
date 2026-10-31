package vn.nhom15.preorder.slot;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.OrderingProperties;

/**
 * UC-14 Cấu hình giờ mở cửa và quota: xem quota và điểm đã giữ từng khung hôm nay; đặt giờ mở cửa, quota mặc định; sửa
 * quota riêng một khung hôm nay (BR-06, BR-07). Không bao giờ đặt quota thấp hơn điểm đã giữ hay thu hẹp giờ mở cửa làm
 * mất khung đang có đơn (BR-11).
 */
@Service
public class AdminSlotService {

    /**
     * Một khung 15 phút của hôm nay như quản lý thấy (khác màn của khách, BR-36). Từ PB-22 mỗi khung có hai vai: khoảng
     * làm của bếp (công suất, điểm bếp dự kiến làm) và khung nhận của khách (điểm, số đơn nhận trong khung).
     *
     * @param quota công suất bếp của khoảng làm này (riêng nếu có, ngược lại mặc định)
     * @param quotaOverride công suất riêng; null nếu dùng mặc định
     * @param heldPoints tổng điểm các đơn nhận trong khung này
     * @param openOrders số đơn đang xử lý nhận trong khung này
     * @param plannedPoints điểm bếp dự kiến làm trong khoảng này theo lịch bếp hiện tại
     * @param pickupOrders số đơn (không tính đơn hủy) nhận trong khung này, so với giới hạn quầy
     */
    public record AdminSlot(
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime start,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime end,
            int quota,
            Integer quotaOverride,
            int heldPoints,
            int openOrders,
            int plannedPoints,
            int pickupOrders) {}

    /** Cấu hình cửa hàng và các khung hôm nay. */
    public record AdminDay(
            LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime openTime,
            @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
            int defaultQuotaPoints,
            int slotMinutes,
            int holdMinutes,
            int preOrderSharePercent,
            int pickupLimitOrders,
            List<AdminSlot> slots) {}

    private final StoreSettingsRepository settingsRepository;
    private final SlotRepository slotRepository;
    private final OrderingProperties properties;
    private final Clock clock;
    private final KitchenPlanService kitchenPlanService;

    public AdminSlotService(
            StoreSettingsRepository settingsRepository,
            SlotRepository slotRepository,
            OrderingProperties properties,
            Clock clock,
            KitchenPlanService kitchenPlanService) {
        this.settingsRepository = settingsRepository;
        this.slotRepository = slotRepository;
        this.properties = properties;
        this.clock = clock;
        this.kitchenPlanService = kitchenPlanService;
    }

    /** UC-14 bước 2: mọi khung hôm nay trong giờ mở cửa, cùng các khung ngoài giờ còn đơn (nếu có). */
    @Transactional(readOnly = true)
    public AdminDay today() {
        LocalDate today = LocalDate.now(clock);
        StoreSettings settings = settingsRepository.get();
        Map<LocalTime, TimeSlot> rows = slotRepository.findByDate(today).stream()
                .collect(Collectors.toMap(TimeSlot::startTime, Function.identity()));
        Map<LocalTime, Integer> orders = slotRepository.countOpenOrdersByStart(today);
        KitchenPlanService.Snapshot plan = kitchenPlanService.snapshot();
        Map<LocalDateTime, Integer> planned = plan.current().plannedByInterval();

        List<AdminSlot> slots = new ArrayList<>();
        for (LocalTime start : startsWithin(settings.openTime(), settings.closeTime())) {
            TimeSlot row = rows.get(start);
            LocalDateTime startAt = today.atTime(start);
            slots.add(new AdminSlot(
                    startAt,
                    startAt.plusMinutes(properties.slotMinutes()),
                    row == null ? settings.defaultQuotaPoints() : row.quota(settings.defaultQuotaPoints()),
                    row == null ? null : row.quotaOverride(),
                    row == null ? 0 : row.heldPoints(),
                    orders.getOrDefault(start, 0),
                    planned.getOrDefault(startAt, 0),
                    plan.pickupOrders().getOrDefault(start, 0)));
        }
        return new AdminDay(
                today,
                settings.openTime(),
                settings.closeTime(),
                settings.defaultQuotaPoints(),
                properties.slotMinutes(),
                settings.holdMinutes(),
                settings.preOrderSharePercent(),
                settings.pickupLimitOrders(),
                slots);
    }

    /**
     * UC-14.1, UC-14.2: đặt giờ mở cửa, công suất bếp mặc định và các thông số lịch bếp (PB-22). Thông số để null thì
     * giữ nguyên giá trị đang dùng.
     *
     * @throws ApiException {@code VALIDATION_ERROR} nếu giờ không đúng mốc hoặc mở không trước đóng;
     *     {@code QUOTA_BELOW_HELD} nếu cấu hình mới làm bếp không còn kịp cho các đơn đã nhận (BR-11);
     *     {@code HOURS_CONFLICT_WITH_ORDERS} nếu khung có đơn nằm ngoài giờ mới
     */
    @Transactional
    public AdminDay updateSettings(
            LocalTime openTime,
            LocalTime closeTime,
            int defaultQuota,
            Integer holdMinutes,
            Integer preOrderSharePercent,
            Integer pickupLimitOrders) {
        requireOnMark(openTime, "openTime");
        requireOnMark(closeTime, "closeTime");
        if (!openTime.isBefore(closeTime)) {
            throw invalid("closeTime", "Giờ đóng cửa phải sau giờ mở cửa");
        }
        if (openTime.plusMinutes(properties.slotMinutes()).isAfter(closeTime)) {
            throw invalid("closeTime", "Giờ mở cửa phải đủ ít nhất một khung");
        }
        settingsRepository.lockForUpdate();
        LocalDate today = LocalDate.now(clock);
        settingsRepository.lockKitchenPlan(today);
        StoreSettings current = settingsRepository.get();
        StoreSettings next = new StoreSettings(
                openTime,
                closeTime,
                defaultQuota,
                holdMinutes != null ? holdMinutes : current.holdMinutes(),
                preOrderSharePercent != null ? preOrderSharePercent : current.preOrderSharePercent(),
                pickupLimitOrders != null ? pickupLimitOrders : current.pickupLimitOrders());
        if (next.holdMinutes() < properties.slotMinutes()) {
            throw invalid("holdMinutes", "Thời gian làm trước tối thiểu " + properties.slotMinutes() + " phút");
        }

        Map<LocalTime, Integer> orders = slotRepository.countOpenOrdersByStart(today);
        List<String> outside = new ArrayList<>();
        for (TimeSlot slot : slotRepository.findByDate(today)) {
            boolean hasOrders = slot.heldPoints() > 0 || orders.getOrDefault(slot.startTime(), 0) > 0;
            boolean inside = !slot.startTime().isBefore(openTime)
                    && !slot.startTime().plusMinutes(properties.slotMinutes()).isAfter(closeTime);
            if (hasOrders && !inside) {
                outside.add(slot.startTime().toString());
            }
        }
        if (!outside.isEmpty()) {
            throw new ApiException(
                            ErrorCode.HOURS_CONFLICT_WITH_ORDERS,
                            "Giờ mới làm mất khung đang có đơn: " + String.join(", ", outside))
                    .with("slots", outside);
        }
        if (!kitchenPlanService.snapshot(next, Map.of()).acceptedOrdersStillFit()) {
            throw new ApiException(
                    ErrorCode.QUOTA_BELOW_HELD, "Với cấu hình mới, bếp không còn kịp làm các đơn đã nhận");
        }
        settingsRepository.update(next);
        return today();
    }

    /**
     * UC-14.3: sửa công suất bếp riêng của một khoảng làm hôm nay; {@code null} trả về công suất mặc định. Không
     * cho giảm tới mức bếp không còn kịp làm các đơn đã nhận (BR-11).
     *
     * @throws ApiException {@code VALIDATION_ERROR} nếu khung không thuộc giờ mở cửa;
     *     {@code QUOTA_BELOW_HELD} nếu bếp không còn kịp làm các đơn đã nhận
     */
    @Transactional
    public AdminDay setSlotQuota(LocalTime start, Integer quota) {
        settingsRepository.lockShared();
        LocalDate today = LocalDate.now(clock);
        settingsRepository.lockKitchenPlan(today);
        StoreSettings settings = settingsRepository.get();
        if (!startsWithin(settings.openTime(), settings.closeTime()).contains(start)) {
            throw invalid("start", "Khung không thuộc giờ mở cửa hôm nay");
        }
        Map<LocalTime, Integer> change = new java.util.HashMap<>();
        change.put(start, quota);
        if (!kitchenPlanService.snapshot(settings, change).acceptedOrdersStillFit()) {
            throw new ApiException(
                            ErrorCode.QUOTA_BELOW_HELD,
                            "Giảm công suất khung " + start + " thì bếp không còn kịp làm các đơn đã nhận")
                    .with("start", start.toString());
        }
        long slotId = slotRepository.ensureExists(today, start);
        slotRepository.setCapacityOverride(slotId, quota);
        return today();
    }

    private List<LocalTime> startsWithin(LocalTime open, LocalTime close) {
        List<LocalTime> starts = new ArrayList<>();
        LocalTime start = open;
        while (!start.plusMinutes(properties.slotMinutes()).isAfter(close)) {
            starts.add(start);
            LocalTime next = start.plusMinutes(properties.slotMinutes());
            if (next.isBefore(start)) {
                break;
            }
            start = next;
        }
        return starts;
    }

    private void requireOnMark(LocalTime time, String field) {
        if (time.getSecond() != 0 || time.getNano() != 0 || time.getMinute() % properties.slotMinutes() != 0) {
            throw invalid(field, "Giờ phải đúng mốc " + properties.slotMinutes() + " phút (BR-06)");
        }
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message)
                .with("errors", List.of(Map.of("field", field, "message", message)));
    }
}
