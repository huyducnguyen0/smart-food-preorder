package vn.nhom15.preorder.menu;

import java.util.List;

/**
 * Một nhóm món cùng các món đang hiển thị trong nhóm đó.
 *
 * @param id khóa chính của nhóm
 * @param name tên nhóm
 * @param items các món chưa ngừng bán, theo thứ tự hiển thị
 */
public record MenuCategory(long id, String name, List<MenuItem> items) {}
