package vn.nhom15.preorder.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.nhom15.preorder.auth.AccountRepository;
import vn.nhom15.preorder.auth.CurrentUser;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * BR-32: tài khoản bị khóa không dùng được nữa, kể cả phiên đang mở. Màn quầy và bếp tự làm mới mỗi 30 giây nên phiên
 * không bao giờ hết hạn; chỉ chặn lúc đăng nhập là không đủ. Mỗi yêu cầu đã đăng nhập kiểm tra lại tài khoản (một truy
 * vấn theo khóa chính); bị khóa hoặc không còn tồn tại thì hủy phiên và trả 401 để giao diện đưa về trang đăng nhập.
 */
class LockedAccountFilter extends OncePerRequestFilter {

    private final AccountRepository accountRepository;

    LockedAccountFilter(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof CurrentUser user
                && accountRepository.isBlocked(user.id())) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            SecurityConfig.writeError(
                    response, ErrorCode.UNAUTHENTICATED, "Tài khoản đã bị khóa, vui lòng liên hệ quản lý");
            return;
        }
        chain.doFilter(request, response);
    }
}
