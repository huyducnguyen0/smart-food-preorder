package vn.nhom15.preorder.menu;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Truy vấn các bảng {@code menu_categories} và {@code menu_items}. */
@Repository
public class MenuRepository {

    private static final String ITEM_COLUMNS = """
            i.id, i.category_id, i.name, i.description, i.image_url, i.price,
            i.workload_points, i.available, i.discontinued
            """;

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

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public MenuRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * Các nhóm kèm món chưa ngừng bán (BR-03), nhóm theo thứ tự hiển thị, món theo thứ tự tạo. Nhóm không còn món nào
     * thì không có trong kết quả.
     */
    public List<MenuCategory> findVisibleMenu() {
        record Row(long categoryId, String categoryName, MenuItem item) {}
        List<Row> rows = jdbcTemplate.query(
                """
                        SELECT c.id AS cat_id, c.name AS cat_name, %s
                        FROM menu_items i JOIN menu_categories c ON c.id = i.category_id
                        WHERE NOT i.discontinued
                        ORDER BY c.sort_order, c.id, i.id
                        """.formatted(ITEM_COLUMNS),
                (rs, rowNum) ->
                        new Row(rs.getLong("cat_id"), rs.getString("cat_name"), ITEM_MAPPER.mapRow(rs, rowNum)));

        Map<Long, List<MenuItem>> itemsByCategory = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (Row row : rows) {
            names.put(row.categoryId(), row.categoryName());
            itemsByCategory
                    .computeIfAbsent(row.categoryId(), id -> new java.util.ArrayList<>())
                    .add(row.item());
        }
        return itemsByCategory.entrySet().stream()
                .map(e -> new MenuCategory(e.getKey(), names.get(e.getKey()), List.copyOf(e.getValue())))
                .toList();
    }

    /** Một món theo mã, kể cả món đã ngừng bán (đơn cũ vẫn tham chiếu được). */
    public Optional<MenuItem> findById(long id) {
        return jdbcTemplate
                .query("SELECT " + ITEM_COLUMNS + " FROM menu_items i WHERE i.id = ?", ITEM_MAPPER, id)
                .stream()
                .findFirst();
    }

    /** Nhiều món theo mã, trả về theo mã; mã không tồn tại thì không có trong kết quả. */
    public Map<Long, MenuItem> findByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<MenuItem> found = namedJdbcTemplate.query(
                "SELECT " + ITEM_COLUMNS + " FROM menu_items i WHERE i.id IN (:ids)",
                new MapSqlParameterSource("ids", ids),
                ITEM_MAPPER);
        Map<Long, MenuItem> byId = new LinkedHashMap<>();
        found.forEach(item -> byId.put(item.id(), item));
        return byId;
    }
}
