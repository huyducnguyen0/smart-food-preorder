package vn.nhom15.preorder.menu;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import vn.nhom15.preorder.common.ApiException;
import vn.nhom15.preorder.common.ErrorCode;

/** Thực đơn (UC-03) và tra cứu món cho các use case khác (UC-04). Module menu là nơi duy nhất sở hữu dữ liệu món. */
@Service
public class MenuService {

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /** Menu đang hiển thị: bỏ món ngừng bán (BR-03); món tạm hết vẫn có, kèm cờ {@code available = false} (BR-04). */
    public List<MenuCategory> getMenu() {
        return menuRepository.findVisibleMenu();
    }

    /**
     * Một món đang hiển thị, để frontend kiểm tra tình trạng và giá hiện tại trước khi thêm vào giỏ (UC-04 E1).
     *
     * @throws ApiException {@code MENU_ITEM_NOT_FOUND} nếu món không có hoặc đã ngừng bán
     */
    public MenuItem getVisibleItem(long id) {
        return menuRepository
                .findById(id)
                .filter(item -> !item.discontinued())
                .orElseThrow(() -> new ApiException(ErrorCode.MENU_ITEM_NOT_FOUND, "Món không có trong thực đơn"));
    }

    /**
     * Nhiều món theo mã (kể cả món ngừng bán), để use case đặt đơn so với giỏ; mã không có thì không nằm trong
     * kết quả.
     */
    public Map<Long, MenuItem> findItems(Collection<Long> ids) {
        return menuRepository.findByIds(ids);
    }
}
