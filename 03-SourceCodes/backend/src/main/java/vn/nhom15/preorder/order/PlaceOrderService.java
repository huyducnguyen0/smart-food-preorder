package vn.nhom15.preorder.order;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

/**
 * UC-04 bước 8–11: kiểm tra, giữ quota và tạo đơn trong <b>một giao dịch</b> (BR-10). Mọi lỗi là {@code ApiException}
 * (RuntimeException) nên giao dịch bị hủy: quota vừa giữ và đơn vừa chèn cùng biến mất. Thứ tự các bước theo Class
 * Design mục 3.
 */
@Service
public class PlaceOrderService {

    private final OrderRepository orderRepository;
    private final MenuService menuService;
    private final SlotAvailabilityService slotAvailabilityService;
    private final QuotaService quotaService;
    private final PickupCodeGenerator pickupCodeGenerator;
    private final OrderingProperties properties;
    private final Clock clock;

    public PlaceOrderService(
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
     * Đặt đơn cho khách.
     *
     * @throws ApiException {@code VALIDATION_ERROR} (một món xuất hiện hai lần), {@code OPEN_ORDER_LIMIT_REACHED},
     *     {@code SLOT_NOT_BOOKABLE}, {@code ITEMS_CHANGED}, {@code ORDER_TOO_LARGE}, {@code SLOT_FULL}
     */
    @Transactional
    public PlaceOrderResult placeOrder(long customerId, PlaceOrderCommand command) {
        rejectDuplicateItems(command.lines());

        // 1. Xếp hàng các lần đặt của cùng một khách; 2. gửi lại cùng mã yêu cầu thì trả đơn cũ (A1, BR-17).
        orderRepository.lockCustomer(customerId);
        Optional<Long> existing = orderRepository.findIdByRequest(customerId, command.requestId());
        if (existing.isPresent()) {
            return new PlaceOrderResult(loadDetail(existing.get(), customerId), false);
        }

        // 3. Giới hạn đơn chưa kết thúc (E5, BR-37).
        int openOrders = orderRepository.countOpenOrders(customerId);
        if (openOrders >= properties.maxOpenOrdersPerCustomer()) {
            throw new ApiException(
                            ErrorCode.OPEN_ORDER_LIMIT_REACHED,
                            "Bạn đã có " + openOrders + " đơn đang chờ, vui lòng nhận đơn trước khi đặt thêm")
                    .with("openOrders", openOrders)
                    .with("limit", properties.maxOpenOrdersPerCustomer());
        }

        // 4. Khung còn hợp lệ (E6, BR-15). 5. Món còn bán và đúng giá khách đã thấy (E7, BR-35).
        slotAvailabilityService.checkBookable(command.slotStart());
        Map<Long, MenuItem> items = menuService.findItems(
                command.lines().stream().map(RequestedLine::menuItemId).toList());
        checkItems(command.lines(), items);

        // 6. Giữ quota (UC-17), 7. cấp mã nhận, 8. chèn đơn và dòng đơn.
        List<QuotaLine> quotaLines = command.lines().stream()
                .map(line -> new QuotaLine(items.get(line.menuItemId()).workloadPoints(), line.quantity()))
                .toList();
        QuotaReservation reservation = quotaService.reserve(
                command.slotStart().toLocalDate(), command.slotStart().toLocalTime(), quotaLines);

        List<NewOrder.NewOrderLine> orderLines = command.lines().stream()
                .map(line -> {
                    MenuItem item = items.get(line.menuItemId());
                    return new NewOrder.NewOrderLine(item.id(), item.name(), item.price(), line.quantity());
                })
                .toList();
        int total = orderLines.stream()
                .mapToInt(line -> Math.multiplyExact(line.unitPrice(), line.quantity()))
                .reduce(0, Math::addExact);
        long orderId = orderRepository.insert(new NewOrder(
                customerId,
                OrderSource.PRE_ORDER,
                reservation.timeSlotId(),
                reservation.points(),
                total,
                newPickupCode(),
                command.requestId(),
                OffsetDateTime.now(clock),
                orderLines));

        // 9. Đọc lại để trả đúng những gì đã lưu.
        return new PlaceOrderResult(loadDetail(orderId, customerId), true);
    }

    private static void rejectDuplicateItems(List<RequestedLine> lines) {
        Set<Long> seen = new HashSet<>();
        for (RequestedLine line : lines) {
            if (!seen.add(line.menuItemId())) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Mỗi món chỉ được xuất hiện một dòng trong đơn");
            }
        }
    }

    /** Báo cùng lúc mọi món đã thay đổi để khách cập nhật giỏ một lần (E7). */
    private static void checkItems(List<RequestedLine> lines, Map<Long, MenuItem> items) {
        List<Map<String, Object>> changed = new ArrayList<>();
        for (RequestedLine line : lines) {
            MenuItem item = items.get(line.menuItemId());
            if (item == null || !item.isOrderable()) {
                changed.add(change(line.menuItemId(), "UNAVAILABLE", null));
            } else if (item.price() != line.unitPrice()) {
                changed.add(change(line.menuItemId(), "PRICE_CHANGED", item.price()));
            }
        }
        if (!changed.isEmpty()) {
            throw new ApiException(
                            ErrorCode.ITEMS_CHANGED, "Một số món đã thay đổi so với giỏ của bạn, vui lòng xem lại")
                    .with("items", changed);
        }
    }

    private static Map<String, Object> change(long menuItemId, String reason, Integer currentPrice) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("menuItemId", menuItemId);
        change.put("reason", reason);
        if (currentPrice != null) {
            change.put("currentPrice", currentPrice);
        }
        return change;
    }

    private String newPickupCode() {
        String code;
        do {
            code = pickupCodeGenerator.generate();
        } while (orderRepository.pickupCodeExists(code));
        return code;
    }

    private OrderDetail loadDetail(long orderId, long customerId) {
        return orderRepository
                .findDetail(orderId, customerId, properties.slotMinutes())
                .orElseThrow(() -> new IllegalStateException("Không đọc lại được đơn vừa tạo: " + orderId))
                .inZone(clock.getZone());
    }
}
