package vn.nhom15.preorder.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Mã nhận (BR-38): 6 ký tự, không có ký tự dễ nhầm, khớp ràng buộc của database. */
class PickupCodeGeneratorTest {

    @Test
    void codesHaveSixCharactersFromTheAllowedAlphabetOnly() {
        PickupCodeGenerator generator = new PickupCodeGenerator();

        for (int i = 0; i < 2000; i++) {
            // Cùng biểu thức với ràng buộc ck_orders_pickup_code trong V5__create_orders.sql.
            assertThat(generator.generate()).matches("[A-HJKMNP-Z2-9]{6}");
        }
    }

    @Test
    void alphabetHasThirtyOneDistinctCharactersWithoutConfusingOnes() {
        assertThat(PickupCodeGenerator.ALPHABET).hasSize(31);
        Set<Character> distinct = new HashSet<>();
        PickupCodeGenerator.ALPHABET.chars().forEach(c -> distinct.add((char) c));
        assertThat(distinct).hasSize(31).doesNotContain('I', 'L', 'O', '0', '1');
    }

    @Test
    void codesAreNotAllTheSame() {
        PickupCodeGenerator generator = new PickupCodeGenerator();
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            codes.add(generator.generate());
        }
        assertThat(codes.size()).isGreaterThan(90);
    }
}
