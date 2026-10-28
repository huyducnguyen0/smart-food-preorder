package vn.nhom15.preorder.slot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * UC-17 Kiểm tra và giữ quota. Dùng chung cho pre-order và walk-in (cùng một quota, NT1). Là nơi duy nhất làm tăng điểm
 * đang giữ của khung (Architecture mục 2).
 */
@Service
public class QuotaService {

    private final SlotRepository slotRepository;
    private final StoreSettingsRepository storeSettingsRepository;

    public QuotaService(SlotRepository slotRepository, StoreSettingsRepository storeSettingsRepository) {
        this.slotRepository = slotRepository;
        this.storeSettingsRepository = storeSettingsRepository;
    }

    /** Điểm đơn = Σ (điểm món × số lượng) (BR-08). */
    public static int orderPoints(List<QuotaLine> lines) {
        int total = 0;
        for (QuotaLine line : lines) {
            total = Math.addExact(total, Math.multiplyExact(line.workloadPoints(), line.quantity()));
        }
        return total;
    }

    /**
     * Kiểm tra và giữ quota cho một khung (UC-17 bước 1–6).
     *
     * <p>Phải gọi trong giao dịch của use case gọi (MANDATORY): việc giữ quota và việc tạo đơn thành công hoặc thất bại
     * cùng nhau (BR-10, UC-17 E3). Lỗi ở bước sau làm giao dịch hủy, quota vừa giữ tự trở về như cũ.
     *
     * @throws ApiException {@code ORDER_TOO_LARGE} nếu điểm đơn lớn hơn quota của cả khung (UC-17 E1, BR-12);
     *     {@code SLOT_FULL} nếu quota còn lại không đủ (UC-17 E2, BR-09)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public QuotaReservation reserve(LocalDate date, LocalTime start, List<QuotaLine> lines) {
        int points = orderPoints(lines);
        // Xếp hàng với thao tác đổi cấu hình của quản lý (BR-11); nhiều lần đặt vẫn chạy song song với nhau.
        storeSettingsRepository.lockShared();
        long slotId = slotRepository.ensureExists(date, start);

        // BR-13: đơn 0 điểm không chiếm quota; vẫn cần mã khung để gắn vào đơn.
        if (points == 0) {
            return new QuotaReservation(slotId, 0);
        }

        int defaultQuota = storeSettingsRepository.get().defaultQuotaPoints();
        Optional<Long> held = slotRepository.tryHold(date, start, points, defaultQuota);
        if (held.isPresent()) {
            return new QuotaReservation(held.get(), points);
        }

        // Không giữ được: phân biệt "đơn quá lớn cho cả khung" (đổi khung cũng vô ích) với "khung vừa hết chỗ".
        TimeSlot slot = slotRepository
                .find(date, start)
                .orElseThrow(() ->
                        new IllegalStateException("Khung vừa được tạo nhưng không tìm thấy: " + date + " " + start));
        if (points > slot.quota(defaultQuota)) {
            throw new ApiException(
                    ErrorCode.ORDER_TOO_LARGE, "Đơn quá lớn cho một khung giờ, vui lòng liên hệ cửa hàng");
        }
        throw new ApiException(ErrorCode.SLOT_FULL, "Khung giờ này vừa hết chỗ, vui lòng chọn khung khác");
    }
}
