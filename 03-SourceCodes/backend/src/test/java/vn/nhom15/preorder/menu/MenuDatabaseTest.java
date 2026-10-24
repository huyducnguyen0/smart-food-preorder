package vn.nhom15.preorder.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;
import vn.nhom15.preorder.support.TestDatabase;

/** Menu trên PostgreSQL thật với dữ liệu mẫu (khớp "Menu kiểm thử" của test case black-box). */
class MenuDatabaseTest {

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static MenuService service;

    @BeforeAll
    static void setUp() {
        dataSource = TestDatabase.create(true);
        jdbc = new JdbcTemplate(dataSource);
        service = new MenuService(new MenuRepository(jdbc));
    }

    @AfterAll
    static void tearDown() {
        dataSource.close();
    }

    private static MenuItem find(List<MenuCategory> menu, String name) {
        return menu.stream()
                .flatMap(c -> c.items().stream())
                .filter(i -> i.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void menuHasThreeCategoriesInDisplayOrder() {
        List<MenuCategory> menu = service.getMenu();

        assertThat(menu).extracting(MenuCategory::name).containsExactly("Burger", "Gà rán", "Đồ uống");
    }

    @Test
    void discontinuedItemsAreHiddenButSoldOutItemsStayVisible() {
        List<MenuCategory> menu = service.getMenu();

        // BR-03: món ngừng bán không hiển thị. BR-04: món tạm hết vẫn hiện, kèm cờ không còn bán.
        assertThat(menu.stream().flatMap(c -> c.items().stream()).map(MenuItem::name))
                .doesNotContain("Combo cũ")
                .contains("Burger gà giòn");
        assertThat(find(menu, "Burger gà giòn").available()).isFalse();
        assertThat(find(menu, "Burger gà giòn").isOrderable()).isFalse();
        assertThat(find(menu, "Burger bò").isOrderable()).isTrue();
    }

    @Test
    void sampleItemsMatchTheBlackBoxTestData() {
        List<MenuCategory> menu = service.getMenu();

        assertThat(find(menu, "Burger bò")).satisfies(i -> {
            assertThat(i.price()).isEqualTo(45000);
            assertThat(i.workloadPoints()).isEqualTo(5);
        });
        assertThat(find(menu, "Gà rán 2 miếng").workloadPoints()).isEqualTo(6);
        assertThat(find(menu, "Trà đào").workloadPoints()).isZero();
        assertThat(find(menu, "Nước suối").imageUrl()).isNull();
    }

    @Test
    void getVisibleItemReturnsSoldOutButRejectsDiscontinuedAndUnknown() {
        long soldOutId = find(service.getMenu(), "Burger gà giòn").id();
        long discontinuedId = jdbc.queryForObject("SELECT id FROM menu_items WHERE name = 'Combo cũ'", Long.class);

        assertThat(service.getVisibleItem(soldOutId).available()).isFalse();
        for (long missing : new long[] {discontinuedId, 999_999L}) {
            assertThatThrownBy(() -> service.getVisibleItem(missing))
                    .isInstanceOfSatisfying(
                            ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.MENU_ITEM_NOT_FOUND));
        }
    }

    @Test
    void findItemsIncludesDiscontinuedItemsSoOrdersCanBeCheckedAgainstTheCart() {
        long discontinuedId = jdbc.queryForObject("SELECT id FROM menu_items WHERE name = 'Combo cũ'", Long.class);
        long burgerId = find(service.getMenu(), "Burger bò").id();

        Map<Long, MenuItem> items = service.findItems(List.of(discontinuedId, burgerId, 999_999L));

        assertThat(items).containsOnlyKeys(discontinuedId, burgerId);
        assertThat(items.get(discontinuedId).discontinued()).isTrue();
        assertThat(service.findItems(List.of())).isEmpty();
    }

    @Test
    void reseedingDoesNotDuplicateItems() {
        // Migration lặp R__ chạy lại khi file đổi; chạy lại phải giữ nguyên số món.
        int before = jdbc.queryForObject("SELECT count(*) FROM menu_items", Integer.class);

        jdbc.execute(
                "INSERT INTO menu_categories (name, sort_order) VALUES ('Burger', 1) ON CONFLICT (name) DO NOTHING");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM menu_items", Integer.class))
                .isEqualTo(before)
                .isEqualTo(8);
    }

    @Test
    void databaseRejectsNegativePriceAndDuplicateCategory() {
        long categoryId = jdbc.queryForObject("SELECT id FROM menu_categories WHERE name = 'Burger'", Long.class);

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO menu_items (category_id, name, price, workload_points) "
                                + "VALUES (?, 'Sai giá', -1, 0)",
                        categoryId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_menu_items_price");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO menu_categories (name) VALUES ('Burger')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_menu_categories_name");
    }
}
