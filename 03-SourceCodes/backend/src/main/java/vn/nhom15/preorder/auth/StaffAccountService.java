package vn.nhom15.preorder.auth;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * UC-15 Quản lý tài khoản nhân viên: quản lý tạo, khóa, mở khóa tài khoản quầy và bếp (BR-32). Nhân viên đăng nhập
 * bằng email hoặc số điện thoại như khách; mật khẩu lưu dạng băm (NFR-06).
 */
@Service
public class StaffAccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffAccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AccountRepository.StaffAccount> list() {
        return accountRepository.findStaff();
    }

    /**
     * UC-15.1: tạo tài khoản quầy hoặc bếp.
     *
     * @throws ApiException {@code VALIDATION_ERROR} nếu vai trò không phải quầy/bếp hoặc dữ liệu sai;
     *     {@code LOGIN_ID_TAKEN} nếu định danh đã có tài khoản
     */
    @Transactional
    public AccountRepository.StaffAccount create(String rawLoginId, String displayName, String password, Role role) {
        if (role != Role.COUNTER && role != Role.KITCHEN) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Chỉ tạo được tài khoản nhân viên quầy hoặc bếp");
        }
        String loginId = RegistrationService.validLoginId(rawLoginId);
        RegistrationService.checkPasswordLength(password);
        long id = accountRepository
                .insertIfAbsent(loginId, passwordEncoder.encode(password), displayName.strip(), role)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.LOGIN_ID_TAKEN, "Email hoặc số điện thoại này đã có tài khoản"));
        return new AccountRepository.StaffAccount(id, loginId, displayName.strip(), role, false);
    }

    /**
     * UC-15.2, UC-15.3: khóa hoặc mở khóa. Tài khoản bị khóa không đăng nhập được nữa.
     *
     * @throws ApiException {@code VALIDATION_ERROR} nếu id không phải tài khoản nhân viên quầy/bếp
     */
    @Transactional
    public List<AccountRepository.StaffAccount> setLocked(long id, boolean locked) {
        if (!accountRepository.setStaffLocked(id, locked)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Không có tài khoản nhân viên quầy/bếp này");
        }
        return list();
    }
}
