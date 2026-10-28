package vn.nhom15.preorder.menu;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/**
 * UC-13 Quản lý món: thêm, sửa, còn bán/tạm hết, ngừng bán thay cho xóa (BR-01–BR-05). Không quản lý tồn kho nguyên
 * liệu. Thay đổi chỉ ảnh hưởng đơn tạo sau đó.
 */
@Service
public class AdminMenuService {

    /** Toàn bộ dữ liệu màn quản lý món. */
    public record AdminMenu(List<AdminMenuRepository.Category> categories, List<MenuItem> items) {}

    private final AdminMenuRepository repository;

    public AdminMenuService(AdminMenuRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AdminMenu getMenu() {
        return new AdminMenu(repository.findCategories(), repository.findAllItems());
    }

    /** UC-13.1 Thêm món. */
    @Transactional
    public MenuItem create(AdminMenuRepository.ItemFields fields) {
        requireCategory(fields.categoryId());
        return load(repository.insert(clean(fields)));
    }

    /** UC-13.2 Sửa thông tin món. */
    @Transactional
    public MenuItem update(long id, AdminMenuRepository.ItemFields fields) {
        requireCategory(fields.categoryId());
        if (!repository.update(id, clean(fields))) {
            throw notFound();
        }
        return load(id);
    }

    /** UC-13.3 Đổi còn bán ↔ tạm hết (BR-04). */
    @Transactional
    public MenuItem setAvailable(long id, boolean available) {
        if (!repository.setAvailable(id, available)) {
            throw notFound();
        }
        return load(id);
    }

    /** UC-13.4 Ngừng bán (ẩn khỏi menu, giữ dữ liệu đơn cũ, BR-03); {@code false} để bán lại món đã ngừng. */
    @Transactional
    public MenuItem setDiscontinued(long id, boolean discontinued) {
        if (!repository.setDiscontinued(id, discontinued)) {
            throw notFound();
        }
        return load(id);
    }

    private void requireCategory(long categoryId) {
        if (!repository.categoryExists(categoryId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Nhóm món không tồn tại")
                    .with("errors", List.of(Map.of("field", "categoryId", "message", "Nhóm món không tồn tại")));
        }
    }

    /** Bỏ khoảng trắng thừa; mô tả và ảnh để trống thì lưu null (tùy chọn theo BR-01). */
    private static AdminMenuRepository.ItemFields clean(AdminMenuRepository.ItemFields fields) {
        return new AdminMenuRepository.ItemFields(
                fields.categoryId(),
                fields.name().strip(),
                blankToNull(fields.description()),
                blankToNull(fields.imageUrl()),
                fields.price(),
                fields.workloadPoints());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private MenuItem load(long id) {
        return repository.findItem(id).orElseThrow(AdminMenuService::notFound);
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.MENU_ITEM_NOT_FOUND, "Không tìm thấy món");
    }
}
