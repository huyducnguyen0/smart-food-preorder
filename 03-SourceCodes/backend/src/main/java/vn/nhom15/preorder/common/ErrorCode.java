package vn.nhom15.preorder.common;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi nghiệp vụ trả về cho frontend trong trường {@code code} của phản hồi lỗi. Frontend chọn câu thông báo tiếng
 * Việt theo mã này. Bảng mã và đặc tả liên quan: Architecture mục 5.3.
 */
public enum ErrorCode {
    /** Dữ liệu gửi lên thiếu hoặc sai định dạng (UC-02 E3, UC-04 A2). */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    /** Chưa đăng nhập hoặc phiên hết hạn (UC-02 E4). */
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    /** Sai định danh hoặc mật khẩu; một mã chung để không lộ tài khoản nào tồn tại (UC-02 E1). */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    /** Mật khẩu đúng nhưng tài khoản bị khóa (UC-02 E2). */
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN),
    /** Sai vai trò (UC-02 E5, UC-12 E2). */
    FORBIDDEN(HttpStatus.FORBIDDEN),
    /** Món không có hoặc đã ngừng bán (UC-04 E1). */
    MENU_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND),
    /** Đơn không có hoặc không phải của khách (UC-05 E1). */
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    /** Khách đã đủ số đơn chưa kết thúc tối đa (UC-04 E5, BR-37). */
    OPEN_ORDER_LIMIT_REACHED(HttpStatus.CONFLICT),
    /** Khung giờ không còn đặt được (UC-04 E6, BR-15). */
    SLOT_NOT_BOOKABLE(HttpStatus.CONFLICT),
    /** Món đổi giá, tạm hết hoặc ngừng bán so với giỏ (UC-04 E7, BR-35). */
    ITEMS_CHANGED(HttpStatus.CONFLICT),
    /** Điểm đơn lớn hơn quota của cả khung (UC-04 E9, UC-17 E1, BR-12). */
    ORDER_TOO_LARGE(HttpStatus.CONFLICT),
    /** Khung không còn đủ quota (UC-04 E8, UC-17 E2, BR-09). */
    SLOT_FULL(HttpStatus.CONFLICT),
    /** Đơn không ở trạng thái cho phép chuyển (UC-12 E1, SRS B.5). */
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
    /** Đơn đã được ghi nhận thanh toán rồi (UC-08 E1, BR-27). */
    ALREADY_PAID(HttpStatus.CONFLICT),
    /** Đơn đã kết thúc hoặc bị hủy nên không ghi nhận thanh toán được (UC-08 E2). */
    ORDER_NOT_PAYABLE(HttpStatus.CONFLICT),
    /** Mã nhận quầy nhập không khớp với đơn (UC-09 E2, BR-29). */
    PICKUP_CODE_MISMATCH(HttpStatus.CONFLICT),
    /** Đơn chưa thanh toán nên chưa bàn giao được (UC-09 E3, BR-29). */
    ORDER_NOT_PAID(HttpStatus.CONFLICT),
    /** Lỗi hệ thống không lường trước; giao dịch đã hủy (UC-04 E10). */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    /** Mã HTTP tương ứng. */
    public HttpStatus status() {
        return status;
    }
}
