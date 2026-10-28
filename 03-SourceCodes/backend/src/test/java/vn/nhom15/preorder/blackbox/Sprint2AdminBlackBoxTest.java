package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box Sprint 2: UC-13 Quản lý món (PB-03), UC-14 Cấu hình giờ mở cửa và quota (PB-04), UC-01 Đăng ký (PB-19),
 * qua API HTTP thật trên PostgreSQL thật. ID ca theo BLACK_BOX_TEST_CASES_SPRINT_2.md.
 */
class Sprint2AdminBlackBoxTest extends BlackBoxBase {

    private static final String PUT = "PUT";

    private Res put(String path, MockHttpSession session, String json) throws Exception {
        return send(PUT, path, session, json);
    }

    private static String itemJson(long categoryId, String name, int price, int points) {
        return "{\"categoryId\":" + categoryId + ",\"name\":\"" + name + "\",\"description\":\"Món thử\","
                + "\"imageUrl\":\"\",\"price\":" + price + ",\"workloadPoints\":" + points + "}";
    }

    private long burgerCategory() {
        return jdbc.queryForObject("SELECT id FROM menu_categories WHERE name = 'Burger'", Long.class);
    }

    // ---------- UC-13 Quản lý món ----------

    @Test
    @DisplayName("TC-MNG-01 Quản lý xem mọi món kể cả ngừng bán, có điểm công việc và danh sách nhóm")
    void mng01() throws Exception {
        Res menu = get("/api/admin/menu", login("quanly@example.com"));

        assertThat(menu.status()).isEqualTo(200);
        // "Món khác" luôn có từ V11 (quyết định 2026-10-28), xếp cuối.
        assertThat(menu.<List<String>>at("$.categories[*].name"))
                .containsExactly("Burger", "Gà rán", "Đồ uống", "Món khác");
        assertThat(menu.<List<String>>at("$.items[*].name"))
                .contains("Combo cũ")
                .hasSize(8);
        assertThat(menu.<List<Integer>>at("$.items[?(@.name == 'Burger bò')].workloadPoints"))
                .containsExactly(5);
    }

    @Test
    @DisplayName("TC-MNG-02 Thêm món: xuất hiện ngay ở menu khách, còn bán, đặt được")
    void mng02() throws Exception {
        MockHttpSession manager = login("quanly@example.com");

        Res created = post("/api/admin/menu/items", manager, itemJson(burgerCategory(), "Burger cá", 42000, 4));

        assertThat(created.status()).isEqualTo(201);
        long id = ((Number) created.json("$.id")).longValue();
        assertThat((Boolean) created.at("$.available")).isTrue();
        assertThat((Boolean) created.at("$.discontinued")).isFalse();
        assertThat(get("/api/menu", null).<List<String>>at("$.categories[0].items[*].name"))
                .contains("Burger cá");
        Res order = order(login("khach1@example.com"), "12:00", line(id, 1, 42000));
        assertThat(order.status()).isEqualTo(201);
        assertThat(held("12:00")).isEqualTo(4);
    }

    @Test
    @DisplayName("TC-MNG-03 Sửa giá và điểm: đơn đã xác nhận giữ giá và điểm cũ (BR-05, BR-08)")
    void mng03() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        Res placed = order(customer, "12:00", line("Burger bò", 2));
        long orderId = ((Number) placed.json("$.id")).longValue();
        Item burger = item("Burger bò");

        Res updated = put(
                "/api/admin/menu/items/" + burger.id(),
                login("quanly@example.com"),
                itemJson(burgerCategory(), "Burger bò đặc biệt", 50000, 6));

        assertThat(updated.status()).isEqualTo(200);
        assertThat((Integer) updated.at("$.price")).isEqualTo(50000);
        Res old = get("/api/orders/" + orderId, customer);
        assertThat((Integer) old.at("$.totalAmount")).isEqualTo(90000);
        assertThat(old.<List<String>>at("$.lines[*].name")).containsExactly("Burger bò");
        assertThat(jdbc.queryForObject("SELECT workload_points FROM orders WHERE id = ?", Integer.class, orderId))
                .isEqualTo(10);
        assertThat(held("12:00")).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-MNG-04 Đổi tạm hết rồi còn bán: khách thấy nhãn tạm hết, không đặt được; bán lại thì đặt được")
    void mng04() throws Exception {
        MockHttpSession manager = login("quanly@example.com");
        Item burger = item("Burger bò");

        Res off = post("/api/admin/menu/items/" + burger.id() + "/availability", manager, "{\"available\":false}");

        assertThat((Boolean) off.at("$.available")).isFalse();
        assertThat(get("/api/menu/items/" + burger.id(), null).<Boolean>at("$.available"))
                .isFalse();
        assertThat(order(login("khach1@example.com"), "12:00", line("Burger bò", 1))
                        .status())
                .isEqualTo(409);

        post("/api/admin/menu/items/" + burger.id() + "/availability", manager, "{\"available\":true}");
        assertThat(order(login("khach1@example.com"), "12:00", line("Burger bò", 1))
                        .status())
                .isEqualTo(201);
    }

    @Test
    @DisplayName("TC-MNG-05 Ngừng bán: món biến mất khỏi menu khách, đơn cũ vẫn xem được; bán lại thì hiện lại")
    void mng05() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        long orderId = ((Number) order(customer, "12:00", line("Burger bò", 1)).json("$.id")).longValue();
        MockHttpSession manager = login("quanly@example.com");
        Item burger = item("Burger bò");

        Res result = post("/api/admin/menu/items/" + burger.id() + "/discontinue", manager, null);

        assertThat((Boolean) result.at("$.discontinued")).isTrue();
        assertThat(get("/api/menu", null).body()).doesNotContain("\"Burger bò\"");
        assertThat(get("/api/orders/" + orderId, customer).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM menu_items WHERE id = ?", Integer.class, burger.id()))
                .as("không xóa món (BR-03)")
                .isEqualTo(1);

        post("/api/admin/menu/items/" + burger.id() + "/restore", manager, null);
        assertThat(get("/api/menu", null).body()).contains("\"Burger bò\"");
    }

    @Test
    @DisplayName("TC-MNG-06 Dữ liệu món sai: thiếu tên, giá hoặc điểm âm, nhóm không có, ảnh không phải http")
    void mng06() throws Exception {
        MockHttpSession manager = login("quanly@example.com");
        long category = burgerCategory();

        for (String body : List.of(
                itemJson(category, "", 10000, 1),
                itemJson(category, "A", -1, 1),
                itemJson(category, "A", 10000, -1),
                itemJson(999999, "A", 10000, 1),
                "{\"categoryId\":" + category + ",\"name\":\"A\",\"imageUrl\":\"ftp://x\",\"price\":1,"
                        + "\"workloadPoints\":1}",
                "{\"name\":\"A\",\"price\":1,\"workloadPoints\":1}")) {
            Res result = post("/api/admin/menu/items", manager, body);
            assertThat(result.status()).as(body).isEqualTo(400);
            assertThat((String) result.at("$.code")).isEqualTo("VALIDATION_ERROR");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM menu_items", Integer.class))
                .isEqualTo(8);
    }

    @Test
    @DisplayName("TC-MNG-07 Sửa hoặc đổi tình trạng món không có: không tìm thấy")
    void mng07() throws Exception {
        MockHttpSession manager = login("quanly@example.com");

        assertThat(put("/api/admin/menu/items/999999", manager, itemJson(burgerCategory(), "A", 1, 1))
                        .status())
                .isEqualTo(404);
        assertThat(post("/api/admin/menu/items/999999/discontinue", manager, null)
                        .status())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("TC-MNG-08 Chỉ quản lý quản lý món; khách, quầy, bếp, chưa đăng nhập bị từ chối")
    void mng08() throws Exception {
        String body = itemJson(burgerCategory(), "Món lạ", 1000, 1);

        assertThat(get("/api/admin/menu", null).status()).isEqualTo(401);
        for (String loginId : List.of("khach1@example.com", "quay1@example.com", "bep1@example.com")) {
            MockHttpSession session = login(loginId);
            assertThat(get("/api/admin/menu", session).status()).as(loginId).isEqualTo(403);
            assertThat(post("/api/admin/menu/items", session, body).status())
                    .as(loginId)
                    .isEqualTo(403);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM menu_items", Integer.class))
                .isEqualTo(8);
    }

    // ---------- UC-14 Cấu hình giờ mở cửa và quota ----------

    private static String settingsJson(String open, String close, int quota) {
        return "{\"openTime\":\"" + open + "\",\"closeTime\":\"" + close + "\",\"defaultQuotaPoints\":" + quota + "}";
    }

    @Test
    @DisplayName("TC-CFG-01 Quản lý xem khung hôm nay với quota, quota riêng, điểm đã giữ, số đơn")
    void cfg01() throws Exception {
        order(login("khach1@example.com"), "12:00", line("Burger bò", 2));
        setQuotaOverride("13:00", 8);

        Res day = get("/api/admin/slots/today", login("quanly@example.com"));

        assertThat(day.status()).isEqualTo(200);
        assertThat((String) day.at("$.openTime")).isEqualTo("10:00");
        assertThat((String) day.at("$.closeTime")).isEqualTo("21:00");
        assertThat((Integer) day.at("$.defaultQuotaPoints")).isEqualTo(20);
        assertThat(day.<List<Object>>at("$.slots")).hasSize(44);
        assertThat(day.<List<Integer>>at("$.slots[?(@.start == '" + slot("12:00") + "')].heldPoints"))
                .containsExactly(10);
        assertThat(day.<List<Integer>>at("$.slots[?(@.start == '" + slot("12:00") + "')].openOrders"))
                .containsExactly(1);
        assertThat(day.<List<Integer>>at("$.slots[?(@.start == '" + slot("13:00") + "')].quota"))
                .containsExactly(8);
    }

    @Test
    @DisplayName("TC-CFG-02 Đổi giờ mở cửa và quota mặc định: danh sách khung của khách đổi theo")
    void cfg02() throws Exception {
        Res updated = put("/api/admin/settings", login("quanly@example.com"), settingsJson("11:00", "14:00", 10));

        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.<List<Object>>at("$.slots")).hasSize(12);
        Res slots = get("/api/slots?lines=" + item("Burger bò").id() + ":3", login("khach1@example.com"));
        assertThat(slots.<List<String>>at("$.slots[*].start")).first().isEqualTo(slot("11:30"));
        assertThat(slots.<List<String>>at("$.slots[*].start")).last().isEqualTo(slot("13:45"));
        assertThat((String) slots.at("$.unavailableReason"))
                .as("15 điểm > quota mới 10")
                .isEqualTo("ORDER_TOO_LARGE");
    }

    @Test
    @DisplayName("TC-CFG-03 Quota mặc định thấp hơn điểm khung đang giữ: bị từ chối (BR-11)")
    void cfg03() throws Exception {
        order(login("khach1@example.com"), "12:00", line("Burger bò", 3));

        Res result = put("/api/admin/settings", login("quanly@example.com"), settingsJson("10:00", "21:00", 14));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("QUOTA_BELOW_HELD");
        assertThat(jdbc.queryForObject("SELECT default_quota_points FROM store_settings", Integer.class))
                .isEqualTo(20);
        assertThat(put("/api/admin/settings", login("quanly@example.com"), settingsJson("10:00", "21:00", 15))
                        .status())
                .as("đúng bằng điểm đang giữ thì được")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("TC-CFG-04 Thu hẹp giờ mở cửa làm mất khung có đơn: bị từ chối; không làm mất thì được (BR-11)")
    void cfg04() throws Exception {
        order(login("khach1@example.com"), "12:00", line("Trà đào", 1));
        MockHttpSession manager = login("quanly@example.com");

        Res result = put("/api/admin/settings", manager, settingsJson("12:15", "21:00", 20));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code"))
                .as("đơn 0 điểm vẫn tính là có đơn")
                .isEqualTo("HOURS_CONFLICT_WITH_ORDERS");
        assertThat(put("/api/admin/settings", manager, settingsJson("12:00", "12:15", 20))
                        .status())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("TC-CFG-05 Giờ sai: không đúng mốc 15 phút, mở không trước đóng, thiếu trường")
    void cfg05() throws Exception {
        MockHttpSession manager = login("quanly@example.com");

        for (String body : List.of(
                settingsJson("10:10", "21:00", 20),
                settingsJson("21:00", "10:00", 20),
                settingsJson("10:00", "10:00", 20),
                settingsJson("10:00", "21:00", -1),
                "{\"openTime\":\"10:00\"}")) {
            Res result = put("/api/admin/settings", manager, body);
            assertThat(result.status()).as(body).isEqualTo(400);
        }
    }

    @Test
    @DisplayName("TC-CFG-06 Sửa quota riêng một khung: áp dụng ngay cho khách; trả về mặc định bằng null")
    void cfg06() throws Exception {
        MockHttpSession manager = login("quanly@example.com");

        Res result = put("/api/admin/slots/today/12:00/quota", manager, "{\"quota\":4}");

        assertThat(result.status()).isEqualTo(200);
        Res slots = get("/api/slots?lines=" + item("Burger bò").id() + ":1", login("khach1@example.com"));
        assertThat(slots.<List<Boolean>>at("$.slots[?(@.start == '" + slot("12:00") + "')].bookable"))
                .containsExactly(false);

        put("/api/admin/slots/today/12:00/quota", manager, "{\"quota\":null}");
        Res again = get("/api/slots?lines=" + item("Burger bò").id() + ":1", login("khach1@example.com"));
        assertThat(again.<List<Boolean>>at("$.slots[?(@.start == '" + slot("12:00") + "')].bookable"))
                .containsExactly(true);
    }

    @Test
    @DisplayName("TC-CFG-07 Quota riêng thấp hơn điểm đang giữ: bị từ chối; đúng bằng thì được (BR-11)")
    void cfg07() throws Exception {
        order(login("khach1@example.com"), "12:00", line("Burger bò", 2));
        MockHttpSession manager = login("quanly@example.com");

        Res low = put("/api/admin/slots/today/12:00/quota", manager, "{\"quota\":9}");

        assertThat(low.status()).isEqualTo(409);
        assertThat((String) low.at("$.code")).isEqualTo("QUOTA_BELOW_HELD");
        assertThat((Integer) low.at("$.heldPoints")).isEqualTo(10);
        assertThat(put("/api/admin/slots/today/12:00/quota", manager, "{\"quota\":10}")
                        .status())
                .isEqualTo(200);
        assertThat(order(login("0901234567"), "12:00", line("Khoai tây chiên", 1))
                        .status())
                .as("khung đã đầy theo quota riêng")
                .isEqualTo(409);
    }

    @Test
    @DisplayName("TC-CFG-08 Khung ngoài giờ mở cửa hoặc sai mốc: không sửa quota được")
    void cfg08() throws Exception {
        MockHttpSession manager = login("quanly@example.com");

        assertThat(put("/api/admin/slots/today/09:00/quota", manager, "{\"quota\":5}")
                        .status())
                .isEqualTo(400);
        assertThat(put("/api/admin/slots/today/12:05/quota", manager, "{\"quota\":5}")
                        .status())
                .isEqualTo(400);
        assertThat(put("/api/admin/slots/today/12:00/quota", manager, "{\"quota\":-1}")
                        .status())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("TC-CFG-09 Chỉ quản lý cấu hình được")
    void cfg09() throws Exception {
        for (String loginId : List.of("khach1@example.com", "quay1@example.com", "bep1@example.com")) {
            MockHttpSession session = login(loginId);
            assertThat(get("/api/admin/slots/today", session).status())
                    .as(loginId)
                    .isEqualTo(403);
            assertThat(put("/api/admin/settings", session, settingsJson("10:00", "21:00", 5))
                            .status())
                    .isEqualTo(403);
        }
        assertThat(jdbc.queryForObject("SELECT default_quota_points FROM store_settings", Integer.class))
                .isEqualTo(20);
    }

    // ---------- UC-01 Đăng ký ----------

    private Res register(String loginId, String name, String password) throws Exception {
        return post(
                "/api/auth/register",
                null,
                "{\"loginId\":\"" + loginId + "\",\"displayName\":\"" + name + "\",\"password\":\"" + password + "\"}");
    }

    @Test
    @DisplayName("TC-REG-01 Đăng ký bằng email: tạo tài khoản khách, đăng nhập luôn, mật khẩu lưu dạng băm")
    void reg01() throws Exception {
        Res result = register("  Bb-Moi@Example.COM ", "Khách Mới", "matkhau123");

        assertThat(result.status()).isEqualTo(201);
        assertThat((String) result.at("$.role")).isEqualTo("CUSTOMER");
        assertThat((String) result.at("$.displayName")).isEqualTo("Khách Mới");
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM user_accounts WHERE login_id = 'bb-moi@example.com'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain("matkhau123");
        assertThat(loginRaw("bb-moi@example.com", "matkhau123").status()).isEqualTo(200);
    }

    @Test
    @DisplayName("TC-REG-02 Đăng ký bằng số điện thoại có khoảng trắng")
    void reg02() throws Exception {
        Res result = register("098 765 4321", "Khách SĐT", "matkhau123");

        assertThat(result.status()).isEqualTo(201);
        assertThat(loginRaw("0987654321", "matkhau123").status()).isEqualTo(200);
        jdbc.update("DELETE FROM user_accounts WHERE login_id = '0987654321'");
    }

    @Test
    @DisplayName("TC-REG-03 Email hoặc số điện thoại đã có tài khoản: bị từ chối kèm lý do")
    void reg03() throws Exception {
        Res result = register("KHACH1@example.com", "Ai đó", "matkhau123");

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("LOGIN_ID_TAKEN");
        assertThat(register("0901234567", "Ai đó", "matkhau123").status()).isEqualTo(409);
    }

    @Test
    @DisplayName("TC-REG-04 Dữ liệu sai: định danh sai dạng, mật khẩu ngắn hoặc quá dài, thiếu tên")
    void reg04() throws Exception {
        for (String[] input : List.of(
                new String[] {"khong-phai-email", "Tên", "matkhau123"},
                new String[] {"12345", "Tên", "matkhau123"},
                new String[] {"bb-a@example.com", "Tên", "ngan"},
                new String[] {"bb-b@example.com", "Tên", "a".repeat(73)},
                new String[] {"bb-c@example.com", "", "matkhau123"})) {
            Res result = register(input[0], input[1], input[2]);
            assertThat(result.status()).as(String.join("|", input)).isEqualTo(400);
            assertThat((String) result.at("$.code")).isEqualTo("VALIDATION_ERROR");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_accounts WHERE login_id LIKE 'bb-%'", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("TC-REG-05 Hai người cùng đăng ký một email: đúng một tài khoản được tạo")
    void reg05() throws Exception {
        List<Res> results = concurrently(List.of(
                () -> register("bb-trung@example.com", "Một", "matkhau123"),
                () -> register("bb-trung@example.com", "Hai", "matkhau123")));

        assertThat(results.stream().filter(r -> r.status() == 201)).hasSize(1);
        assertThat(results.stream().filter(r -> r.status() == 409)).hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM user_accounts WHERE login_id = 'bb-trung@example.com'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("TC-REG-06 Không kèm token CSRF: bị từ chối, không tạo tài khoản")
    void reg06() throws Exception {
        Res result = postWithoutCsrf(
                "/api/auth/register",
                null,
                "{\"loginId\":\"bb-csrf@example.com\",\"displayName\":\"A\",\"password\":\"matkhau123\"}");

        assertThat(result.status()).isEqualTo(403);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM user_accounts WHERE login_id = 'bb-csrf@example.com'", Integer.class))
                .isZero();
    }
}
