package vn.nhom15.preorder.order;

/**
 * Một dòng của đơn đã tạo, với tên và giá tại thời điểm đặt (BR-35).
 *
 * @param menuItemId mã món
 * @param name tên món lúc đặt
 * @param quantity số lượng
 * @param unitPrice giá lúc đặt
 * @param lineTotal thành tiền = giá × số lượng
 */
public record OrderLineView(long menuItemId, String name, int quantity, int unitPrice, int lineTotal) {}
