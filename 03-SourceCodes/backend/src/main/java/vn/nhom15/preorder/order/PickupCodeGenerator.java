package vn.nhom15.preorder.order;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Sinh mã nhận 6 ký tự (BR-38). Bảng ký tự bỏ I, L, O, 0, 1 để khách đọc cho nhân viên không bị nhầm. Dùng bộ sinh số
 * ngẫu nhiên an toàn để không đoán được mã của đơn khác.
 */
@Component
public class PickupCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 6;

    private final RandomGenerator random;

    public PickupCodeGenerator() {
        this(new SecureRandom());
    }

    PickupCodeGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
