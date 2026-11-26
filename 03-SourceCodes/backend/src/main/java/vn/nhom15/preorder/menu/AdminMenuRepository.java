package vn.nhom15.preorder.menu;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Ghi bảng {@code menu_items} cho quản lý (UC-13). Module menu là nơi duy nhất sở hữu dữ liệu món. */
@Repository
public class AdminMenuRepository {

    private static final String ITEM_COLUMNS =
            "id, category_id, name, description, image_url, price, workload_points, available, discontinued";

    private static final RowMapper<MenuItem> ITEM_MAPPER = (rs, rowNum) -> new MenuItem(
            rs.getLong("id"),
            rs.getLong("category_id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("image_url"),
            rs.getInt("price"),
            rs.getInt("workload_points"),
            rs.getBoolean("available"),
            rs.getBoolean("discontinued"));

    /** Một nhóm món, không kèm món. */
    public record Category(long id, String name) {}

    /** Dữ liệu sửa được của một món (BR-01). */
    public record ItemFields(
            long categoryId, String name, String description, String imageUrl, int price, int workloadPoints) {}

    private final JdbcTemplate jdbcTemplate;

    public AdminMenuRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Category> findCategories() {
        return jdbcTemplate.query(
                "SELECT id, name FROM menu_categories ORDER BY sort_order, id",
                (rs, rowNum) -> new Category(rs.getLong("id"), rs.getString("name")));
    }

    public boolean categoryExists(long categoryId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM menu_categories WHERE id = ?)", Boolean.class, categoryId));
    }

    /** Mọi món, kể cả món ngừng bán: quản lý cần thấy để bán lại hoặc tra cứu. */
    public List<MenuItem> findAllItems() {
        return jdbcTemplate.query(
                "SELECT " + ITEM_COLUMNS + " FROM menu_items ORDER BY discontinued, category_id, id", ITEM_MAPPER);
    }

    public Optional<MenuItem> findItem(long id) {
        return jdbcTemplate.query("SELECT " + ITEM_COLUMNS + " FROM menu_items WHERE id = ?", ITEM_MAPPER, id).stream()
                .findFirst();
    }

    /** Thêm món mới, mặc định còn bán và chưa ngừng bán. */
    public long insert(ItemFields fields) {
        Long id = jdbcTemplate.queryForObject(
                """
                        INSERT INTO menu_items (category_id, name, description, image_url, price, workload_points)
                        VALUES (?, ?, ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                fields.categoryId(),
                fields.name(),
                fields.description(),
                fields.imageUrl(),
                fields.price(),
                fields.workloadPoints());
        return Objects.requireNonNull(id);
    }

    /**
     * Sửa thông tin món. Đơn đã xác nhận không bị ảnh hưởng vì dòng đơn lưu tên, giá và điểm tại thời điểm đặt (BR-05,
     * BR-08).
     *
     * @return true nếu có món để sửa
     */
    public boolean update(long id, ItemFields fields) {
        return jdbcTemplate.update(
                        """
                                UPDATE menu_items
                                SET category_id = ?, name = ?, description = ?, image_url = ?, price = ?,
                                    workload_points = ?, updated_at = now()
                                WHERE id = ?
                                """,
                        fields.categoryId(),
                        fields.name(),
                        fields.description(),
                        fields.imageUrl(),
                        fields.price(),
                        fields.workloadPoints(),
                        id)
                > 0;
    }

    public boolean setAvailable(long id, boolean available) {
        return jdbcTemplate.update(
                        "UPDATE menu_items SET available = ?, updated_at = now() WHERE id = ?", available, id)
                > 0;
    }

    public boolean setDiscontinued(long id, boolean discontinued) {
        return jdbcTemplate.update(
                        "UPDATE menu_items SET discontinued = ?, updated_at = now() WHERE id = ?", discontinued, id)
                > 0;
    }
}
