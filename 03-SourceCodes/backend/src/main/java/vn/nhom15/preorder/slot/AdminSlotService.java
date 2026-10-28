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

    /** Một khung của hôm nay như quản lý thấy: có con số quota và điểm đã giữ (khác màn của khách, BR-36). */
    public record AdminSlot(
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime start,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime end,
            int quota,
            Integer quotaOverride,
            int heldPoints,
            int openOrders) {}

    /** Cấu hình cửa hàng và các khung hôm nay. */
    public record AdminDay(
            LocalDate date,
            @JsonFormat(pattern = "HH:mm") LocalTime openTime,
            @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
            int defaultQuotaPoints,
            int slotMinutes,
            List<AdminSlot> slots) {}

    private final StoreSettingsRepository settingsRepository;
    private final SlotRepository slotRepository;
    private final OrderingProperties properties;
    private final Clock clock;

    public AdminSlotService(
            StoreSettingsRepository settingsRepository,
            SlotRepository slotRepository,
            OrderingProperties properties,
            Clock clock) {
        this.settingsRepository = settingsRepository;
        this.slotRepository = slotRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /** UC-14 bước 2: mọi khung hôm nay trong giờ mở cửa, cùng các khung ngoài giờ còn đơn (nếu có). */
    @Transactional(readOnly = true)
    public AdminDay today() {
        LocalDate today = LocalDate.now(clock);
        StoreSettings settings = settingsRepository.get();
        Map<LocalTime, TimeSlot> rows = slotRepository.findByDate(today).stream()
                .collect(Collectors.toMap(TimeSlot::startTime, Function.identity()));
        Map<LocalTime, Integer> orders = slotRepository.countOpenOrdersByStart(today);

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
                    orders.getOrDefault(start, 0)));
        }
        return new AdminDay(
                today,
                settings.openTime(),
                settings.closeTime(),
                settings.defaultQuotaPoints(),
                properties.slotMinutes(),
                slots);
    }

    /**
     * UC-14.1, UC-14.2: đặt giờ mở cửa và quota mặc định.
     *
     * @throws ApiException {@code VALIDATION_ERROR} nếu giờ không đúng mốc hoặc mở không trước đóng;
     *     {@code QUOTA_BELOW_HELD} nếu khung dùng quota mặc định đang giữ nhiều điểm hơn quota mới;
     *     {@code HOURS_CONFLICT_WITH_ORDERS} nếu khung có đơn nằm ngoài giờ mới
     */
    @Transactional
    public AdminDay updateSettings(LocalTime openTime, LocalTime closeTime, int defaultQuota) {
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
        Map<LocalTime, Integer> orders = slotRepository.countOpenOrdersByStart(today);

        List<String> overQuota = new ArrayList<>();
        List<String> outside = new ArrayList<>();
        for (TimeSlot slot : slotRepository.findByDate(today)) {
            boolean hasOrders = slot.heldPoints() > 0 || orders.getOrDefault(slot.startTime(), 0) > 0;
            if (slot.quotaOverride() == null && slot.heldPoints() > defaultQuota) {
                overQuota.add(slot.startTime() + " (đang giữ " + slot.heldPoints() + ")");
            }
            boolean inside = !slot.startTime().isBefore(openTime)
                    && !slot.startTime().plusMinutes(properties.slotMinutes()).isAfter(closeTime);
            if (hasOrders && !inside) {
                outside.add(slot.startTime().toString());
            }
        }
        if (!overQuota.isEmpty()) {
            throw new ApiException(
                            ErrorCode.QUOTA_BELOW_HELD,
                            "Quota mặc định mới thấp hơn điểm đang giữ của khung: " + String.join(", ", overQuota))
                    .with("slots", overQuota);
        }
        if (!outside.isEmpty()) {
            throw new ApiException(
                            ErrorCode.HOURS_CONFLICT_WITH_ORDERS,
                            "Giờ mới làm mất khung đang có đơn: " + String.join(", ", outside))
                    .with("slots", outside);
        }
        settingsRepository.update(new StoreSettings(openTime, closeTime, defaultQuota));
        return today();
    }

    /**
     * UC-14.3: sửa quota riêng của một khung hôm nay; {@code null} trả về quota mặc định.
     *
     * @throws ApiException {@code VALIDATION_ERROR} nếu khung không thuộc giờ mở cửa;
     *     {@code QUOTA_BELOW_HELD} nếu quota
     *     mới thấp hơn điểm đang giữ
     */
    @Transactional
    public AdminDay setSlotQuota(LocalTime start, Integer quota) {
        settingsRepository.lockShared();
        StoreSettings settings = settingsRepository.get();
        if (!startsWithin(settings.openTime(), settings.closeTime()).contains(start)) {
            throw invalid("start", "Khung không thuộc giờ mở cửa hôm nay");
        }
        LocalDate today = LocalDate.now(clock);
        slotRepository.ensureExists(today, start);
        if (!slotRepository.setQuotaOverride(today, start, quota, settings.defaultQuotaPoints())) {
            int held =
                    slotRepository.find(today, start).map(TimeSlot::heldPoints).orElse(0);
            throw new ApiException(
                            ErrorCode.QUOTA_BELOW_HELD,
                            "Khung " + start + " đang giữ " + held + " điểm, không đặt quota thấp hơn được")
                    .with("heldPoints", held);
        }
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
