package vn.nhom15.preorder.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Một dòng khách gửi lên khi đặt đơn.
 *
 * @param menuItemId mã món
 * @param quantity số lượng; giới hạn trên là chặn kỹ thuật tránh tràn số khi nhân với giá và điểm, không phải quy tắc
 *     nghiệp vụ
 * @param unitPrice giá khách đã thấy trong giỏ, để backend so với giá hiện tại (BR-35)
 */
public record RequestedLine(
        long menuItemId,
        @Min(1) @Max(RequestedLine.MAX_QUANTITY) int quantity,
        @Min(0) int unitPrice) {

    public static final int MAX_QUANTITY = 999;
}
