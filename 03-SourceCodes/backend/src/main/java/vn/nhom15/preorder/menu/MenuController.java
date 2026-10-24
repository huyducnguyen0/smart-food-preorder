package vn.nhom15.preorder.menu;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API xem thực đơn, công khai (BR-34). Không trả điểm công việc của món: đó là thông tin vận hành (UC-03 Ghi chú). */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * Một món như khách nhìn thấy.
     *
     * @param available còn bán (true) hay tạm hết (false)
     */
    public record ItemResponse(
            long id, String name, String description, String imageUrl, int price, boolean available) {

        static ItemResponse of(MenuItem item) {
            return new ItemResponse(
                    item.id(), item.name(), item.description(), item.imageUrl(), item.price(), item.available());
        }
    }

    public record CategoryResponse(long id, String name, List<ItemResponse> items) {}

    public record MenuResponse(List<CategoryResponse> categories) {}

    /** UC-03 bước 1–2: món đang bán theo nhóm. */
    @GetMapping
    public MenuResponse menu() {
        List<CategoryResponse> categories = menuService.getMenu().stream()
                .map(c -> new CategoryResponse(
                        c.id(),
                        c.name(),
                        c.items().stream().map(ItemResponse::of).toList()))
                .toList();
        return new MenuResponse(categories);
    }

    /** UC-04 E1: tình trạng và giá hiện tại của một món. */
    @GetMapping("/items/{id}")
    public ItemResponse item(@PathVariable long id) {
        return ItemResponse.of(menuService.getVisibleItem(id));
    }
}
