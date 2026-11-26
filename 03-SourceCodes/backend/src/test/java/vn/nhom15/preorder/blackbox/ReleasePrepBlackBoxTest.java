package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box các quyết định ngày 2026-10-28 ở bước chuẩn bị phát hành: walk-in chỉ trễ từ giờ kết thúc khung (BR-26
 * sửa đổi), nhóm "Món khác" luôn có (V11). Giờ hiện tại mặc định 11:00.
 */
class ReleasePrepBlackBoxTest extends BlackBoxBase {

    private long walkIn(String hhmm) throws Exception {
        Res created = post(
                "/api/counter/walk-in",
                login("quay1@example.com"),
                "{\"requestId\":\"" + newRequestId() + "\",\"slotStart\":\"" + slot(hhmm)
                        + "\",\"paymentMethod\":\"CASH\",\"lines\":[" + line("Burger bò", 1) + "]}");
        assertThat(created.status()).isEqualTo(201);
        return ((Number) created.json("$.id")).longValue();
    }

    private Boolean dashboardLate(long id) throws Exception {
        List<Boolean> late =
                get("/api/admin/dashboard", login("quanly@example.com")).at("$.activeOrders[?(@.id==" + id + ")].late");
        return late.getFirst();
    }

    private Boolean kitchenLate(long id) throws Exception {
        Res queue = get("/api/kitchen/queue", login("bep1@example.com"));
        List<Boolean> late = queue.at("$..[?(@.id==" + id + ")].late");
        return late.getFirst();
    }

    @Test
    @DisplayName("TC-LATE-01 Walk-in khung 11:00 tạo lúc 11:00: chưa trễ tới 11:14; từ 11:15 (hết khung) mới trễ")
    void late01() throws Exception {
        long id = walkIn("11:00");

        at("11:05");
        assertThat(dashboardLate(id)).isFalse();
        assertThat(kitchenLate(id)).isFalse();

        at("11:14");
        assertThat(dashboardLate(id)).isFalse();

        at("11:15");
        assertThat(dashboardLate(id)).isTrue();
        assertThat(kitchenLate(id)).isTrue();
    }

    @Test
    @DisplayName("TC-LATE-02 Pre-order giữ quy tắc cũ: trễ từ giờ bắt đầu khung, ở cả màn theo dõi và hàng chờ bếp")
    void late02() throws Exception {
        long id = ((Number) order(login("khach1@example.com"), "11:30", line("Burger bò", 1))
                        .json("$.id"))
                .longValue();

        at("11:29");
        assertThat(dashboardLate(id)).isFalse();
        assertThat(kitchenLate(id)).isFalse();

        at("11:30");
        assertThat(dashboardLate(id)).isTrue();
        assertThat(kitchenLate(id)).isTrue();
    }

    @Test
    @DisplayName("TC-MNG-09 Nhóm 'Món khác' luôn có; thực đơn khách không hiện nhóm trống, có món thì hiện")
    void mng09() throws Exception {
        MockHttpSession manager = login("quanly@example.com");
        jdbc.update(
                "DELETE FROM menu_items WHERE category_id = (SELECT id FROM menu_categories WHERE name = 'Món khác')");

        Res admin = get("/api/admin/menu", manager);
        List<Integer> ids = admin.at("$.categories[?(@.name=='Món khác')].id");
        assertThat(ids).hasSize(1);
        assertThat(get("/api/menu", null).body()).doesNotContain("Món khác");

        Res created = post(
                "/api/admin/menu/items",
                manager,
                "{\"categoryId\":" + ids.getFirst() + ",\"name\":\"Bánh flan\",\"description\":\"\",\"imageUrl\":\"\","
                        + "\"price\":15000,\"workloadPoints\":1}");
        assertThat(created.status()).isEqualTo(201);
        assertThat(get("/api/menu", null).<List<String>>at("$.categories[?(@.name=='Món khác')].items[*].name"))
                .containsExactly("Bánh flan");
    }
}
