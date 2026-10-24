package vn.nhom15.preorder.order;

/** Trạng thái đơn (SRS B.5). */
public enum OrderStatus {
    CONFIRMED,
    PREPARING,
    READY,
    PICKED_UP,
    CANCELLED,
    NO_SHOW;

    /** Đơn chưa kết thúc: tính vào giới hạn số đơn của một khách (BR-37) và hiện ở hàng chờ. */
    public boolean isOpen() {
        return this == CONFIRMED || this == PREPARING || this == READY;
    }
}
