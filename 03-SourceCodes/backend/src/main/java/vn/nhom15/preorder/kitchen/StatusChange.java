package vn.nhom15.preorder.kitchen;

import vn.nhom15.preorder.order.OrderStatus;

/**
 * Kết quả chuyển trạng thái của bếp.
 *
 * @param id mã đơn
 * @param status trạng thái mới
 */
public record StatusChange(long id, OrderStatus status) {}
