package vn.nhom15.preorder.order;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.config.OrderingProperties;
import vn.nhom15.preorder.menu.MenuItem;
import vn.nhom15.preorder.menu.MenuService;
import vn.nhom15.preorder.slot.QuotaLine;
import vn.nhom15.preorder.slot.QuotaReservation;
import vn.nhom15.preorder.slot.QuotaService;
import vn.nhom15.preorder.slot.SlotAvailabilityService;
import vn.nhom15.preorder.slot.SlotView;

/**
 * UC-07 Tạo đơn walk-in: nhân viên quầy nhập món, báo khách khung dự kiến sớm nhất (BR-18), chỉ xác nhận khi khách
 * đồng ý. Lúc xác nhận kiểm tra lại quota bằng đúng {@link QuotaService} của pre-order, nên hai kênh dùng chung một
 * quota và không bao giờ nhận vượt (BR-09, NFR-01).
 */
@Service
public class WalkInService {

    /** Khung dự kiến và số tiền cần thu, để nhân viên báo khách trước khi xác nhận. */
    public record WalkInEstimate(SlotView slot, int totalAmount) {}

    private final OrderRepository orderRepository;
    private final MenuService menuService;
    private final SlotAvailabilityService slotAvailabilityService;
    private final QuotaService quotaService;
    private final PickupCodeGenerator pickupCodeGenerator;
    private final OrderingProperties properties;
    private final Clock clock;

    public WalkInService(
            OrderRepository orderRepository,
            MenuService menuService,
            SlotAvailabilityService slotAvailabilityService,
            QuotaService quotaService,
            PickupCodeGenerator pickupCodeGenerator,
            OrderingProperties properties,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.menuService = menuService;
        this.slotAvailabilityService = slotAvailabilityService;
        this.quotaService = quotaService;
        this.pickupCodeGenerator = pickupCodeGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * UC-07 bước 2–3: khung dự kiến sớm nhất cho các món (chưa giữ quota, BR-19).
     *
     * @throws ApiException {@code ITEMS_CHANGED} nếu món không còn bán hoặc giá khác; {@code ORDER_TOO_LARGE};
     *     {@code NO_SLOT_AVAILABLE}
     */
    @Transactional(readOnly = true)
    public WalkInEstimate estimate(List<RequestedLine> lines) {
        PlaceOrderService.rejectDuplicateItems(lines);
        Map<Long, MenuItem> items = loadItems(lines);
        PlaceOrderService.checkItems(lines, items);
        int points = QuotaService.orderPoints(quotaLines(lines, items));
        return new WalkInEstimate(slotAvailabilityService.earliestWalkInSlot(points), total(lines, items));
    }

    /**
     * UC-07 bước 5–7: xác nhận đơn khách đã đồng ý. Gửi lại cùng mã yêu cầu trả lại đúng đơn đã tạo.
     *
     * @throws ApiException {@code SLOT_NOT_BOOKABLE}, {@code ITEMS_CHANGED}, {@code ORDER_TOO_LARGE}, {@code SLOT_FULL}
     *     (khung vừa hết chỗ: báo lại khách khung mới, BR-19)
     */
    @Transactional
    public PlaceOrderResult confirm(long staffId, WalkInCommand command) {
        PlaceOrderService.rejectDuplicateItems(command.lines());
        // Xếp hàng các lần gửi của cùng một nhân viên để lần gửi lại thấy đơn lần trước vừa tạo (NFR-02).
        orderRepository.lockCustomer(staffId);
        Optional<Long> existing = orderRepository.findIdByWalkInRequest(command.requestId());
        if (existing.isPresent()) {
            return new PlaceOrderResult(load(existing.get()), false);
        }

        slotAvailabilityService.checkWalkInSlot(command.slotStart());
        Map<Long, MenuItem> items = loadItems(command.lines());
        PlaceOrderService.checkItems(command.lines(), items);
        QuotaReservation reservation = quotaService.reserve(
                command.slotStart().toLocalDate(),
                command.slotStart().toLocalTime(),
                quotaLines(command.lines(), items));

        List<NewOrder.NewOrderLine> orderLines = command.lines().stream()
                .map(line -> {
                    MenuItem item = items.get(line.menuItemId());
                    return new NewOrder.NewOrderLine(item.id(), item.name(), item.price(), line.quantity());
                })
                .toList();
        long orderId = orderRepository.insert(new NewOrder(
                null,
                OrderSource.WALK_IN,
                reservation.timeSlotId(),
                reservation.points(),
                total(command.lines(), items),
                newPickupCode(),
                command.requestId(),
                OffsetDateTime.now(clock),
                orderLines,
                staffId,
                command.paymentMethod()));
        return new PlaceOrderResult(load(orderId), true);
    }

    private Map<Long, MenuItem> loadItems(List<RequestedLine> lines) {
        return menuService.findItems(
                lines.stream().map(RequestedLine::menuItemId).toList());
    }

    private static List<QuotaLine> quotaLines(List<RequestedLine> lines, Map<Long, MenuItem> items) {
        return lines.stream()
                .map(line -> new QuotaLine(items.get(line.menuItemId()).workloadPoints(), line.quantity()))
                .toList();
    }

    private static int total(List<RequestedLine> lines, Map<Long, MenuItem> items) {
        return lines.stream()
                .mapToInt(
                        line -> Math.multiplyExact(items.get(line.menuItemId()).price(), line.quantity()))
                .reduce(0, Math::addExact);
    }

    private String newPickupCode() {
        String code;
        do {
            code = pickupCodeGenerator.generate();
        } while (orderRepository.pickupCodeExists(code));
        return code;
    }

    private OrderDetail load(long orderId) {
        return orderRepository
                .findDetailById(orderId, properties.slotMinutes())
                .orElseThrow(() -> new IllegalStateException("Không đọc lại được đơn walk-in: " + orderId))
                .inZone(clock.getZone(), properties.prepWindowMinutes());
    }

    /** Khung mới để báo lại khách khi khung đã báo vừa hết chỗ; rỗng nếu hôm nay không còn khung nào. */
    public Optional<SlotView> nextSlotAfterFull(List<RequestedLine> lines) {
        try {
            return Optional.of(estimate(lines).slot());
        } catch (ApiException e) {
            if (e.code() == ErrorCode.NO_SLOT_AVAILABLE || e.code() == ErrorCode.ORDER_TOO_LARGE) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
