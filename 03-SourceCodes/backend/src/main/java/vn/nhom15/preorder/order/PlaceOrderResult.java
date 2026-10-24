package vn.nhom15.preorder.order;

/**
 * Kết quả đặt đơn.
 *
 * @param order đơn
 * @param created true nếu vừa tạo mới (HTTP 201); false nếu mã yêu cầu đã có đơn từ trước và trả lại đơn đó (HTTP 200,
 *     A1)
 */
public record PlaceOrderResult(OrderDetail order, boolean created) {}
