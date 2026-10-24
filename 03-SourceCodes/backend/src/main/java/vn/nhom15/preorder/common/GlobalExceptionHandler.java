package vn.nhom15.preorder.common;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Chuyển mọi lỗi thành JSON theo chuẩn Problem Details (RFC 9457) kèm trường {@code code}. Các lỗi web thông thường
 * (không tìm thấy đường dẫn, sai phương thức...) giữ cách xử lý chuẩn của Spring.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException e) {
        ProblemDetail body = problem(e.code(), e.getMessage());
        e.properties().forEach(body::setProperty);
        return ResponseEntity.status(e.code().status()).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message", String.valueOf(f.getDefaultMessage())))
                .toList();
        ProblemDetail body = problem(ErrorCode.VALIDATION_ERROR, "Dữ liệu gửi lên không hợp lệ");
        body.setProperty("errors", errors);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status()).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(ErrorCode.VALIDATION_ERROR, "Dữ liệu gửi lên không đọc được");
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status()).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(ErrorCode.VALIDATION_ERROR, "Thiếu tham số: " + e.getParameterName());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status()).body(body);
    }

    /** Lỗi không lường trước: ghi log chi tiết, chỉ trả thông báo chung để không lộ thông tin nội bộ (UC-04 E10). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        // Lỗi quyền do Spring Security xử lý ở tầng filter; không nuốt thành lỗi 500.
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
            throw (RuntimeException) e;
        }
        log.error("Lỗi không lường trước", e);
        ProblemDetail body = problem(ErrorCode.INTERNAL_ERROR, "Có lỗi hệ thống, vui lòng thử lại");
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status()).body(body);
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(code.status(), detail);
        body.setProperty("code", code.name());
        return body;
    }
}
