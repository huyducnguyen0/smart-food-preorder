package vn.nhom15.preorder.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Buộc Spring Security ghi cookie {@code XSRF-TOKEN} ở mọi phản hồi. Mặc định token chỉ được tạo khi có code đọc
 * đến nó, nên trang đăng nhập (chưa gọi API nào ghi dữ liệu) sẽ không có token để gửi lại ở header
 * {@code X-XSRF-TOKEN}.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            // Đọc giá trị để kích hoạt việc tạo token và ghi cookie.
            token.getToken();
        }
        chain.doFilter(request, response);
    }
}
