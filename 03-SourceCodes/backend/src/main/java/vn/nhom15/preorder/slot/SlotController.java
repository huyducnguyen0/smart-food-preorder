package vn.nhom15.preorder.slot;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/** Danh sách khung giờ cho khách chọn khi đặt (UC-04 bước 6). Chỉ vai trò khách (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotAvailabilityService slotAvailabilityService;

    public SlotController(SlotAvailabilityService slotAvailabilityService) {
        this.slotAvailabilityService = slotAvailabilityService;
    }

    /** {@code GET /api/slots?lines=12:2,15:1}: mỗi phần tử là {@code mãMón:sốLượng}. */
    @GetMapping
    public SlotList listSlots(@RequestParam("lines") String lines) {
        return slotAvailabilityService.listSlots(parseLines(lines));
    }

    static List<CartLine> parseLines(String text) {
        List<CartLine> lines = new ArrayList<>();
        for (String part : text.split(",", -1)) {
            String[] pair = part.trim().split(":");
            try {
                if (pair.length != 2) {
                    throw new NumberFormatException(part);
                }
                lines.add(new CartLine(Long.parseLong(pair[0].trim()), Integer.parseInt(pair[1].trim())));
            } catch (NumberFormatException e) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Giỏ hàng gửi lên không đúng định dạng");
            }
        }
        return lines;
    }
}
