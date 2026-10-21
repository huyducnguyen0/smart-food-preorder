package vn.nhom15.preorder.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Đăng nhập, đăng xuất và xem người dùng hiện tại (UC-02). Phiên lưu phía server (Architecture mục 3). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** UC-02 bước 1–6: xác thực rồi tạo phiên gắn với tài khoản và vai trò. */
    @PostMapping("/login")
    public CurrentUser login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        CurrentUser user = authService.authenticate(request.loginId(), request.password());

        // Đổi mã phiên khi đăng nhập để chống tấn công cố định phiên (session fixation).
        if (http.getSession(false) != null) {
            http.changeSessionId();
        }
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, authorities));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, http, response);
        return user;
    }

    /** UC-02 bước 7–8: hủy phiên; sau đó mọi thao tác cần đăng nhập đều bị từ chối. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /** Frontend gọi khi tải trang để biết đã đăng nhập chưa. Chưa đăng nhập thì Spring Security trả 401. */
    @GetMapping("/me")
    public CurrentUser me(@AuthenticationPrincipal CurrentUser user) {
        return user;
    }
}
