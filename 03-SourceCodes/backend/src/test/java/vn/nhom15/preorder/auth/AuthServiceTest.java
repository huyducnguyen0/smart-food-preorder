package vn.nhom15.preorder.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/** Thứ tự kiểm tra của UC-02 bước 3–5 và các luồng lỗi E1, E2. */
class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AccountRepository repository;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repository = mock(AccountRepository.class);
        service = new AuthService(repository, encoder);
    }

    private Account account(boolean locked) {
        return new Account(7, "khach@example.com", encoder.encode("mat-khau-dung"), "Khách", Role.CUSTOMER, locked);
    }

    private static void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void correctPasswordReturnsCurrentUserWithoutPassword() {
        when(repository.findByLoginId("khach@example.com")).thenReturn(Optional.of(account(false)));

        CurrentUser user = service.authenticate("khach@example.com", "mat-khau-dung");

        assertThat(user).isEqualTo(new CurrentUser(7, "Khách", Role.CUSTOMER));
    }

    @Test
    void unknownLoginIdAndWrongPasswordGiveTheSameError() {
        when(repository.findByLoginId("khach@example.com")).thenReturn(Optional.of(account(false)));
        when(repository.findByLoginId("khong-co@example.com")).thenReturn(Optional.empty());

        // UC-02 E1: không cho biết sai định danh hay sai mật khẩu.
        assertCode(() -> service.authenticate("khach@example.com", "sai"), ErrorCode.INVALID_CREDENTIALS);
        assertCode(() -> service.authenticate("khong-co@example.com", "mat-khau-dung"), ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void lockedAccountIsReportedOnlyWhenPasswordIsCorrect() {
        when(repository.findByLoginId("khach@example.com")).thenReturn(Optional.of(account(true)));

        // UC-02 E2 chỉ xảy ra khi mật khẩu đúng; sai mật khẩu thì không lộ tài khoản đang bị khóa.
        assertCode(() -> service.authenticate("khach@example.com", "mat-khau-dung"), ErrorCode.ACCOUNT_LOCKED);
        assertCode(() -> service.authenticate("khach@example.com", "sai"), ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void passwordLongerThan72BytesIsRejectedWithoutErrors() {
        when(repository.findByLoginId("khach@example.com")).thenReturn(Optional.of(account(false)));

        assertCode(() -> service.authenticate("khach@example.com", "a".repeat(73)), ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginIdIsNormalizedBeforeLookup() {
        when(repository.findByLoginId("khach@example.com")).thenReturn(Optional.of(account(false)));

        assertThat(service.authenticate("  Khach@Example.COM  ", "mat-khau-dung")
                        .id())
                .isEqualTo(7);
    }

    @Test
    void normalizeLoginIdHandlesEmailAndPhone() {
        assertThat(AuthService.normalizeLoginId("  Ten.Ho@Mail.VN ")).isEqualTo("ten.ho@mail.vn");
        assertThat(AuthService.normalizeLoginId(" 0901 234  567 ")).isEqualTo("0901234567");
    }
}
