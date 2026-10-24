package vn.nhom15.preorder.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * Cấu hình bảo mật: phiên phía server, chống CSRF bằng cookie, kiểm tra vai trò theo đường dẫn (BR-33, NFR-03).
 * Bảng quyền ở đây phải khớp mục "Yêu cầu phân quyền chung" của UC-02. Kiểm tra sở hữu đơn nằm ở service.
 */
@Configuration
public class SecurityConfig {

    /** Băm mật khẩu bằng BCrypt (NFR-06). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Cookie XSRF-TOKEN đọc được bằng JavaScript; frontend gửi lại ở header X-XSRF-TOKEN cho mọi yêu cầu ghi.
        // Dùng handler không mã hóa (đủ cho SPA gửi nguyên giá trị cookie); CsrfCookieFilter bảo đảm cookie luôn được
        // ghi.
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(Customizer.withDefaults())
                .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, e) ->
                                writeError(response, ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập"))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này")))
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        // UC-02, UC-03: chưa đăng nhập vẫn xem được menu và đăng nhập (BR-34).
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/menu", "/api/menu/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login")
                        .permitAll()
                        .requestMatchers("/api/auth/**")
                        .authenticated()
                        // Khách chỉ dùng được phần của khách; đơn của khách khác được chặn thêm ở service (UC-05 E1).
                        .requestMatchers("/api/slots/**", "/api/orders/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers("/api/kitchen/**")
                        .hasRole("KITCHEN")
                        // Mặc định từ chối: đường dẫn mới phải được khai báo quyền ở đây.
                        .anyRequest()
                        .denyAll());
        return http.build();
    }

    /** Ghi lỗi dạng JSON cùng khuôn với {@code GlobalExceptionHandler} (Problem Details kèm trường code). */
    private static void writeError(HttpServletResponse response, ErrorCode code, String detail) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Chuỗi cố định do ta viết, không chứa ký tự cần escape.
        response.getWriter()
                .write("{\"type\":\"about:blank\",\"title\":\"" + code.status().getReasonPhrase() + "\",\"status\":"
                        + code.status().value() + ",\"code\":\"" + code.name() + "\",\"detail\":\"" + detail + "\"}");
    }
}
