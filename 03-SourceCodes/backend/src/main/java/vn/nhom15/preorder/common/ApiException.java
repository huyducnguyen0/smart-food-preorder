package vn.nhom15.preorder.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lỗi nghiệp vụ có mã. Service ném lỗi này; {@link GlobalExceptionHandler} chuyển thành phản hồi JSON. Là
 * RuntimeException nên làm Spring hủy giao dịch đang chạy (BR-10).
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    /** Thêm thông tin kèm theo vào phản hồi, ví dụ danh sách món đã đổi giá. */
    public ApiException with(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> properties() {
        return Map.copyOf(properties);
    }
}
