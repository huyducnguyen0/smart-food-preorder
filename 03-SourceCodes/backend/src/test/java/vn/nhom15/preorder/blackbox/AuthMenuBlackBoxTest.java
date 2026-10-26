package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/** Black-box UC-02 Đăng nhập và UC-03 Xem menu: phần chạy được qua API và database (T-25). */
class AuthMenuBlackBoxTest extends BlackBoxBase {

    @Test
    @DisplayName("TC-AUTH-01 Đăng nhập KH1 đúng: nhận tên và vai trò, không kèm mật khẩu")
    void auth01() throws Exception {
        Res result = loginRaw("khach1@example.com", PASSWORD);

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.displayName")).isEqualTo("Khách Một");
        assertThat((String) result.at("$.role")).isEqualTo("CUSTOMER");
        assertThat(result.body()).doesNotContainIgnoringCase("password").doesNotContain("$2");
    }

    @Test
    @DisplayName("TC-AUTH-03 Định danh có khoảng trắng và chữ hoa vẫn đăng nhập được")
    void auth03() throws Exception {
        Res result = loginRaw("  KHACH1@Example.COM ", PASSWORD);

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.displayName")).isEqualTo("Khách Một");
    }

    @Test
    @DisplayName("TC-AUTH-04 Số điện thoại có khoảng trắng đăng nhập đúng KH2")
    void auth04() throws Exception {
        Res result = loginRaw(" 0901 234 567 ", PASSWORD);

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.displayName")).isEqualTo("Khách Hai");
    }

    @Test
    @DisplayName("TC-AUTH-05 Sai mật khẩu: lỗi chung, không tạo phiên")
    void auth05() throws Exception {
        Res result = loginRaw("khach1@example.com", "sai-mat-khau");

        assertThat(result.status()).isEqualTo(401);
        assertThat((String) result.at("$.code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat(get("/api/auth/me", null).status()).isEqualTo(401);
    }

    @Test
    @DisplayName("TC-AUTH-06 Định danh không tồn tại: phản hồi giống hệt sai mật khẩu")
    void auth06() throws Exception {
        Res wrongPassword = loginRaw("khach1@example.com", "sai-mat-khau");
        Res unknown = loginRaw("khong-co@example.com", "sai-mat-khau");

        assertThat(unknown.status()).isEqualTo(wrongPassword.status());
        assertThat(unknown.body()).isEqualTo(wrongPassword.body());
    }

    @Test
    @DisplayName("TC-AUTH-07 Tài khoản khóa, mật khẩu đúng: báo bị khóa, không tạo phiên")
    void auth07() throws Exception {
        Res result = loginRaw("khoa@example.com", PASSWORD);

        assertThat(result.status()).isEqualTo(403);
        assertThat((String) result.at("$.code")).isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    @DisplayName("TC-AUTH-08 Tài khoản khóa, mật khẩu sai: lỗi chung, không lộ việc bị khóa")
    void auth08() throws Exception {
        Res locked = loginRaw("khoa@example.com", "sai-mat-khau");
        Res generic = loginRaw("khach1@example.com", "sai-mat-khau");

        assertThat(locked.body()).isEqualTo(generic.body());
        assertThat(locked.body()).doesNotContain("ACCOUNT_LOCKED");
    }

    @Test
    @DisplayName("TC-AUTH-09 Bỏ trống định danh, mật khẩu hoặc cả hai: báo từng trường thiếu")
    void auth09() throws Exception {
        for (String body : List.of(
                "{\"loginId\":\"\",\"password\":\"x\"}",
                "{\"loginId\":\"khach1@example.com\",\"password\":\"\"}",
                "{\"loginId\":\"\",\"password\":\"\"}")) {
            Res result = post("/api/auth/login", null, body);
            assertThat(result.status()).as(body).isEqualTo(400);
            assertThat((String) result.at("$.code")).isEqualTo("VALIDATION_ERROR");
            List<String> fields = result.at("$.errors[*].field");
            if (body.contains("\"loginId\":\"\"")) {
                assertThat(fields).contains("loginId");
            }
            if (body.contains("\"password\":\"\"")) {
                assertThat(fields).contains("password");
            }
        }
    }

    @Test
    @DisplayName("TC-AUTH-10 Mật khẩu 73 byte trở lên: từ chối như sai mật khẩu, không lỗi hệ thống")
    void auth10() throws Exception {
        Res result = loginRaw("khach1@example.com", "a".repeat(73));
        Res longer = loginRaw("khach1@example.com", "é".repeat(200));

        assertThat(result.status()).isEqualTo(401);
        assertThat((String) result.at("$.code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat(longer.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("TC-AUTH-11 Đăng xuất: phiên cũ bị từ chối")
    void auth11() throws Exception {
        MockHttpSession session = login("khach1@example.com");
        assertThat(get("/api/auth/me", session).status()).isEqualTo(200);

        assertThat(post("/api/auth/logout", session, null).status()).isEqualTo(204);

        assertThat(get("/api/auth/me", session).status()).isEqualTo(401);
    }

    @Test
    @DisplayName("TC-AUTH-12 Chưa đăng nhập gọi API giỏ/đơn/bếp: trả chưa đăng nhập")
    void auth12() throws Exception {
        for (String path : List.of("/api/orders", "/api/slots?lines=1:1", "/api/kitchen/queue")) {
            Res result = get(path, null);
            assertThat(result.status()).as(path).isEqualTo(401);
            assertThat((String) result.at("$.code")).isEqualTo("UNAUTHENTICATED");
        }
    }

    @Test
    @DisplayName("TC-AUTH-14 KH1 gọi API bếp: bị từ chối, đơn không đổi")
    void auth14() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        Res placed = order(customer, "12:00", line("Burger bò", 1));
        long id = ((Number) placed.json("$.id")).longValue();

        for (Res result : List.of(
                get("/api/kitchen/queue", customer),
                post("/api/kitchen/orders/" + id + "/start", customer, null),
                post("/api/kitchen/orders/" + id + "/complete", customer, null))) {
            assertThat(result.status()).isEqualTo(403);
            assertThat((String) result.at("$.code")).isEqualTo("FORBIDDEN");
        }
        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("TC-AUTH-15 BẾP gọi API của khách: bị từ chối")
    void auth15() throws Exception {
        MockHttpSession kitchen = login("bep1@example.com");

        assertThat(order(kitchen, "12:00", line("Burger bò", 1)).status()).isEqualTo(403);
        assertThat(get("/api/slots?lines=" + item("Burger bò").id() + ":1", kitchen)
                        .status())
                .isEqualTo(403);
        assertThat(get("/api/orders", kitchen).status()).isEqualTo(403);
        assertThat(orderCount()).isZero();
    }

    @Test
    @DisplayName("TC-AUTH-16 QUẦY và QL gọi API của khách và bếp: bị từ chối trong Sprint 1")
    void auth16() throws Exception {
        for (String loginId : List.of("quay1@example.com", "quanly@example.com")) {
            MockHttpSession session = login(loginId);
            assertThat(order(session, "12:00", line("Burger bò", 1)).status())
                    .as(loginId)
                    .isEqualTo(403);
            assertThat(get("/api/orders", session).status()).as(loginId).isEqualTo(403);
            assertThat(get("/api/kitchen/queue", session).status()).as(loginId).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("TC-AUTH-17 Đường dẫn /api không tồn tại: chưa đăng nhập 401, đã đăng nhập 403")
    void auth17() throws Exception {
        assertThat(get("/api/khong-co", null).status()).isEqualTo(401);
        for (String loginId :
                List.of("khach1@example.com", "bep1@example.com", "quay1@example.com", "quanly@example.com")) {
            assertThat(get("/api/khong-co", login(loginId)).status())
                    .as(loginId)
                    .isEqualTo(403);
        }
    }

    @Test
    @DisplayName("TC-AUTH-18 Không kèm token CSRF: đăng nhập, đặt đơn, đăng xuất đều bị từ chối")
    void auth18() throws Exception {
        MockHttpSession customer = login("khach1@example.com");

        assertThat(postWithoutCsrf(
                                "/api/auth/login",
                                null,
                                "{\"loginId\":\"khach1@example.com\",\"password\":\"" + PASSWORD + "\"}")
                        .status())
                .isEqualTo(403);
        assertThat(postWithoutCsrf(
                                "/api/orders", customer, orderJson(newRequestId(), slot("12:00"), line("Burger bò", 1)))
                        .status())
                .isEqualTo(403);
        assertThat(postWithoutCsrf("/api/auth/logout", customer, null).status()).isEqualTo(403);

        assertThat(orderCount()).isZero();
        assertThat(get("/api/auth/me", customer).status())
                .as("phiên vẫn còn sau lần đăng xuất bị chặn")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("TC-AUTH-19 Cài mới có sẵn tài khoản quản lý đăng nhập được")
    void auth19() throws Exception {
        Res result = loginRaw("quanly@example.com", PASSWORD);

        assertThat(result.status()).isEqualTo(200);
        assertThat((String) result.at("$.role")).isEqualTo("MANAGER");
    }

    @Test
    @DisplayName("TC-AUTH-20 Database chỉ lưu chuỗi băm, không có mật khẩu gốc")
    void auth20() {
        List<String> hashes = jdbc.queryForList("SELECT password_hash FROM user_accounts", String.class);

        assertThat(hashes).isNotEmpty().allSatisfy(hash -> {
            assertThat(hash).startsWith("$2");
            assertThat(hash).doesNotContain(PASSWORD);
        });
    }

    // ---------- UC-03 Menu ----------

    @Test
    @DisplayName("TC-MENU-01 Chưa đăng nhập xem menu: M1–M7 theo nhóm, có tên và giá")
    void menu01() throws Exception {
        Res result = get("/api/menu", null);

        assertThat(result.status()).isEqualTo(200);
        List<String> categories = result.at("$.categories[*].name");
        assertThat(categories).containsExactly("Burger", "Gà rán", "Đồ uống");
        List<String> names = result.at("$.categories[*].items[*].name");
        assertThat(names)
                .containsExactly(
                        "Burger bò",
                        "Burger phô mai đôi",
                        "Burger gà giòn",
                        "Gà rán 2 miếng",
                        "Khoai tây chiên",
                        "Trà đào",
                        "Nước suối");
        List<Integer> prices = result.at("$.categories[*].items[*].price");
        assertThat(prices).containsExactly(45000, 59000, 39000, 55000, 25000, 20000, 10000);
    }

    @Test
    @DisplayName("TC-MENU-03 Món ngừng bán (M8) không có ở bất kỳ đâu trong dữ liệu menu")
    void menu03() throws Exception {
        Res menu = get("/api/menu", null);
        long m8 = item("Combo cũ").id();

        assertThat(menu.body()).doesNotContain("Combo cũ");
        assertThat(get("/api/menu/items/" + m8, null).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("TC-MENU-02 (phần API) Món tạm hết vẫn có trong menu, đánh dấu không còn bán")
    void menu02Api() throws Exception {
        Res menu = get("/api/menu", null);

        List<Boolean> available = menu.at("$.categories[*].items[?(@.name == 'Burger gà giòn')].available");
        assertThat(available).containsExactly(false);
    }

    @Test
    @DisplayName("TC-MENU-06 Dữ liệu menu không có điểm công việc")
    void menu06() throws Exception {
        Res menu = get("/api/menu", null);
        Res single = get("/api/menu/items/" + item("Burger bò").id(), null);

        assertThat(menu.body()).doesNotContainIgnoringCase("workload").doesNotContainIgnoringCase("point");
        assertThat(single.body()).doesNotContainIgnoringCase("workload").doesNotContainIgnoringCase("point");
    }

    @Test
    @DisplayName("TC-MENU-09 (phần API) Menu giống nhau khi đã và chưa đăng nhập")
    void menu09Api() throws Exception {
        assertThat(get("/api/menu", login("khach1@example.com")).body())
                .isEqualTo(get("/api/menu", null).body());
    }

    @Test
    @DisplayName("TC-MENU-10 (phần API) Món vừa tạm hết: tra cứu cho biết không còn bán")
    void menu10Api() throws Exception {
        Item burger = item("Burger bò");
        assertThat((Boolean) get("/api/menu/items/" + burger.id(), null).json("$.available"))
                .isTrue();

        jdbc.update("UPDATE menu_items SET available = false WHERE id = ?", burger.id());

        assertThat((Boolean) get("/api/menu/items/" + burger.id(), null).json("$.available"))
                .isFalse();
    }

    @Test
    @DisplayName("TC-CART-08 (phần API) M3 tạm hết và M8 ngừng bán không thêm vào giỏ được: tra cứu báo không còn bán")
    void cart08Api() throws Exception {
        Item soldOut = item("Burger gà giòn");
        Item discontinued = item("Combo cũ");

        // Giỏ nằm trên trình duyệt; trước khi thêm, giao diện hỏi lại API này (UC-04 E1).
        assertThat((Boolean) get("/api/menu/items/" + soldOut.id(), null).json("$.available"))
                .isFalse();
        assertThat(get("/api/menu/items/" + discontinued.id(), null).status()).isEqualTo(404);
        // Cố đặt thẳng cả hai món qua API cũng bị từ chối, không tạo đơn.
        Res order = order(
                login("khach1@example.com"),
                "12:00",
                line(soldOut.id(), 1, soldOut.price()),
                line(discontinued.id(), 1, discontinued.price()));
        assertThat(order.status()).isEqualTo(409);
        assertThat(order.<List<String>>at("$.items[*].reason"))
                .containsOnly("UNAVAILABLE")
                .hasSize(2);
        assertThat(orderCount()).isZero();
    }
}
