package vn.nhom15.preorder.slot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * UC-17 Kiểm tra và giữ chỗ cho một đơn. Dùng chung cho pre-order và walk-in (cùng một bếp, NT1). Từ PB-22 (Sprint 4)
 * việc giữ chỗ dựa trên lịch bếp: bếp còn xếp kịp làm đơn này mà không làm trễ đơn nào đã nhận, và khung nhận còn chỗ
 * ở quầy. Đây vẫn là nơi duy nhất ghi điểm đang giữ của khung (Architecture mục 2).
 */
@Service
public class QuotaService {

    private final SlotRepository slotRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final KitchenPlanService kitchenPlanService;

    public QuotaService(
            SlotRepository slotRepository,
            StoreSettingsRepository storeSettingsRepository,
            KitchenPlanService kitchenPlanService) {
        this.slotRepository = slotRepository;
        this.storeSettingsRepository = storeSettingsRepository;
        this.kitchenPlanService = kitchenPlanService;
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
     * Hoàn điểm của một đơn bị hủy (BR-23, BR-24). Chạy trong giao dịch của thao tác hủy: hủy và hoàn quota thành công
     * cùng nhau. Lịch bếp tự nhả phần việc của đơn đã hủy vì chỉ tính đơn còn phải làm.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(long timeSlotId, int points) {
        if (points == 0) {
            return;
        }
        if (!slotRepository.release(timeSlotId, points)) {
            throw new IllegalStateException(
                    "Điểm đang giữ của khung " + timeSlotId + " ít hơn điểm cần hoàn " + points);
        }
    }

    /**
     * Kiểm tra và giữ chỗ cho một đơn nhận lúc {@code start} của ngày {@code date} (UC-17 bước 1–6).
     *
     * <p>Phải gọi trong giao dịch của use case gọi (MANDATORY): giữ chỗ và tạo đơn thành công hoặc thất bại cùng nhau
     * (BR-10). Khóa lịch bếp của ngày đến hết giao dịch, nên hai lần nhận đơn cùng lúc phải lần lượt và không thể cùng
     * "thấy bếp còn kịp" dựa trên số liệu cũ (NFR-01).
     *
     * @param walkIn đơn tại quầy: làm từ bây giờ, dùng được cả phần công suất dành cho walk-in
     * @throws ApiException {@code ORDER_TOO_LARGE} nếu bếp trống hoàn toàn cũng không kịp làm đơn này (UC-17 E1);
     *     {@code SLOT_FULL} nếu bếp không còn kịp hoặc khung nhận đã đủ đơn (UC-17 E2)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public QuotaReservation reserve(LocalDate date, LocalTime start, List<QuotaLine> lines, boolean walkIn) {
        int points = orderPoints(lines);
        // Xếp hàng với thao tác đổi cấu hình của quản lý (BR-11), rồi với các lần nhận đơn khác của ngày.
        storeSettingsRepository.lockShared();
        storeSettingsRepository.lockKitchenPlan(date);
        long slotId = slotRepository.ensureExists(date, start);

        KitchenPlanService.Snapshot plan = kitchenPlanService.snapshot();
        if (!plan.pickupHasRoom(start)) {
            throw new ApiException(ErrorCode.SLOT_FULL, "Khung giờ này đã đủ khách nhận, vui lòng chọn khung khác")
                    .with("reason", "PICKUP_LIMIT");
        }
        KitchenPlanner.Job job =
                walkIn ? plan.walkInJob(date.atTime(start), points) : plan.preOrderJob(date.atTime(start), points);
        if (plan.tooLargeEvenWhenEmpty(job)) {
            throw new ApiException(
                    ErrorCode.ORDER_TOO_LARGE, "Đơn quá lớn cho một khung giờ, vui lòng liên hệ cửa hàng");
        }
        if (!plan.fits(job)) {
            throw new ApiException(ErrorCode.SLOT_FULL, "Khung giờ này vừa hết chỗ, vui lòng chọn khung khác")
                    .with("reason", "KITCHEN");
        }
        // BR-13: đơn 0 điểm không tốn công bếp; vẫn tính vào số đơn của khung nhận.
        if (points > 0) {
            slotRepository.addHeld(slotId, points);
        }
        return new QuotaReservation(slotId, points);
    }
}
