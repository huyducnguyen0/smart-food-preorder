package vn.nhom15.preorder.slot;

/**
 * Một dòng của giỏ nhìn từ phía chọn khung: mã món và số lượng.
 *
 * @param menuItemId mã món
 * @param quantity số lượng, ít nhất 1
 */
public record CartLine(long menuItemId, int quantity) {}
