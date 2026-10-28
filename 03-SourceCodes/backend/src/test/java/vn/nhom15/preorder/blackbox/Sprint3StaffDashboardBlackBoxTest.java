package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box Sprint 3: quản lý tài khoản nhân viên (UC-15, BR-32) và theo dõi vận hành trong ngày (PB-17, FR-20), qua
 * API HTTP thật trên PostgreSQL thật.
 */
class Sprint3StaffDashboardBlackBoxTest extends BlackBoxBase {

    private static String staffJson(String loginId, String role) {
        return "{\"loginId\":\"" + loginId + "\",\"displayName\":\"Nhân viên mới\",\"password\":\"" + PASSWORD
                + "\",\"role\":\"" + role + "\"}";
    }

    private long idOf(String loginId) {
        return jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = ?", Long.class, loginId);
    }

    // ---------- UC-15 tài khoản nhân viên ----------

    @Test
    @DisplayName("TC-STF-01 Quản lý xem danh sách: chỉ tài khoản quầy và bếp, có trạng thái khóa, không có mật khẩu")
    void stf01() throws Exception {
        Res result = get("/api/admin/staff", login("quanly@example.com"));

        assertThat(result.status()).isEqualTo(200);
        assertThat(result.<List<String>>at("$.staff[*].role")).containsOnly("COUNTER", "KITCHEN");
        assertThat(result.<List<String>>at("$.staff[*].loginId")).contains("quay1@example.com", "bep1@example.com");
        assertThat(result.body()).doesNotContain("password");
    }

    @Test
    @DisplayName("TC-STF-02 Tạo tài khoản quầy: 201, đăng nhập được ngay và vào được màn quầy")
    void stf02() throws Exception {
        Res created =
                post("/api/admin/staff", login("quanly@example.com"), staffJson(" BB-Quay2@Example.com ", "COUNTER"));

        assertThat(created.status()).isEqualTo(201);
        assertThat((String) created.at("$.loginId")).isEqualTo("bb-quay2@example.com");
        assertThat((Boolean) created.at("$.locked")).isFalse();
        MockHttpSession staff = login("bb-quay2@example.com");
        assertThat(get("/api/counter/orders", staff).status()).isEqualTo(200);
        assertThat(get("/api/admin/staff", staff).status()).isEqualTo(403);
    }

    @Test
    @DisplayName("TC-STF-03 Tạo tài khoản bếp bằng số điện thoại được; vai trò khách hoặc quản lý bị từ chối")
    void stf03() throws Exception {
        MockHttpSession manager = login("quanly@example.com");
        jdbc.update("DELETE FROM user_accounts WHERE login_id = '0912345678'");

        assertThat(post("/api/admin/staff", manager, staffJson("0912345678", "KITCHEN"))
                        .status())
                .isEqualTo(201);
        jdbc.update("DELETE FROM user_accounts WHERE login_id = '0912345678'");
        assertThat(post("/api/admin/staff", manager, staffJson("bb-x@example.com", "CUSTOMER"))
                        .status())
                .isEqualTo(400);
        assertThat(post("/api/admin/staff", manager, staffJson("bb-y@example.com", "MANAGER"))
                        .status())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("TC-STF-04 Định danh đã có tài khoản, sai dạng, mật khẩu ngắn: bị từ chối đúng mã lỗi")
    void stf04() throws Exception {
        MockHttpSession manager = login("quanly@example.com");

        Res taken = post("/api/admin/staff", manager, staffJson("khach1@example.com", "COUNTER"));
        assertThat(taken.status()).isEqualTo(409);
        assertThat((String) taken.at("$.code")).isEqualTo("LOGIN_ID_TAKEN");
        assertThat(post("/api/admin/staff", manager, staffJson("khong-hop-le", "COUNTER"))
                        .status())
                .isEqualTo(400);
        assertThat(post(
                                "/api/admin/staff",
                                manager,
                                "{\"loginId\":\"bb-z@example.com\",\"displayName\":\"A\",\"password\":\"1234567\","
                                        + "\"role\":\"COUNTER\"}")
                        .status())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("TC-STF-05 Khóa tài khoản: không đăng nhập được; mở khóa: đăng nhập lại được")
    void stf05() throws Exception {
        MockHttpSession manager = login("quanly@example.com");
        assertThat(post("/api/admin/staff", manager, staffJson("bb-k@example.com", "KITCHEN"))
                        .status())
                .isEqualTo(201);
        long id = idOf("bb-k@example.com");

        Res locked = post("/api/admin/staff/" + id + "/lock", manager, null);
        assertThat(locked.status()).isEqualTo(200);
        assertThat(locked.<List<Boolean>>at("$.staff[?(@.id==" + id + ")].locked"))
                .containsExactly(true);
        Res denied = loginRaw("bb-k@example.com", PASSWORD);
        assertThat(denied.status()).isEqualTo(403);
        assertThat((String) denied.at("$.code")).isEqualTo("ACCOUNT_LOCKED");

        assertThat(post("/api/admin/staff/" + id + "/unlock", manager, null).status())
                .isEqualTo(200);
        assertThat(loginRaw("bb-k@example.com", PASSWORD).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("TC-STF-06 Không khóa được tài khoản khách hoặc quản lý qua API nhân viên")
    void stf06() throws Exception {
        MockHttpSession manager = login("quanly@example.com");

        assertThat(post("/api/admin/staff/" + idOf("khach1@example.com") + "/lock", manager, null)
                        .status())
                .isEqualTo(400);
        assertThat(post("/api/admin/staff/" + idOf("quanly@example.com") + "/lock", manager, null)
                        .status())
                .isEqualTo(400);
        assertThat(loginRaw("khach1@example.com", PASSWORD).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("TC-STF-07 Quầy, bếp, khách không gọi được API tài khoản nhân viên (403)")
    void stf07() throws Exception {
        for (String loginId : List.of("quay1@example.com", "bep1@example.com", "khach1@example.com")) {
            MockHttpSession session = login(loginId);
            assertThat(get("/api/admin/staff", session).status()).isEqualTo(403);
            assertThat(post("/api/admin/staff", session, staffJson("bb-h@example.com", "COUNTER"))
                            .status())
                    .isEqualTo(403);
        }
    }

    // ---------- PB-17 theo dõi vận hành ----------

    @Test
    @DisplayName("TC-DSH-01 Bảng theo dõi: điểm đã giữ, số đơn mỗi khung và thống kê theo nguồn, trạng thái")
    void dsh01() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        long a = ((Number) order(customer, "12:00", line("Burger bò", 2)).json("$.id")).longValue();
        long b = ((Number) order(customer, "12:00", line("Khoai tây chiên", 1)).json("$.id")).longValue();
        long c = ((Number) order(customer, "12:15", line("Burger bò", 1)).json("$.id")).longValue();
        assertThat(post("/api/orders/" + c + "/cancel", customer, null).status())
                .isEqualTo(200);
        setStatus(b, "PICKED_UP");
        assertThat(post(
                                "/api/counter/walk-in",
                                login("quay1@example.com"),
                                "{\"requestId\":\"" + newRequestId() + "\",\"slotStart\":\"" + slot("11:15")
                                        + "\",\"paymentMethod\":\"CASH\",\"lines\":[" + line("Khoai tây chiên", 1)
                                        + "]}")
                        .status())
                .isEqualTo(201);

        Res result = get("/api/admin/dashboard", login("quanly@example.com"));

        assertThat(result.status()).isEqualTo(200);
        assertThat(result.<List<Integer>>at("$.slots[?(@.start=='" + slot("12:00") + "')].heldPoints"))
                .containsExactly(12);
        assertThat(result.<List<Integer>>at("$.slots[?(@.start=='" + slot("12:15") + "')].heldPoints"))
                .containsExactly(0);
        assertThat((Integer) result.at("$.stats.preOrders")).isEqualTo(3);
        assertThat((Integer) result.at("$.stats.walkIns")).isEqualTo(1);
        assertThat((Integer) result.at("$.stats.cancelled")).isEqualTo(1);
        assertThat((Integer) result.at("$.stats.pickedUp")).isEqualTo(1);
        assertThat((Integer) result.at("$.stats.heldPoints")).isEqualTo(14);
        assertThat(result.<List<Integer>>at("$.activeOrders[*].id")).contains((int) a);
        assertThat(result.<List<Integer>>at("$.activeOrders[*].id")).doesNotContain((int) b, (int) c);
    }

    @Test
    @DisplayName("TC-DSH-02 Đơn CONFIRMED/PREPARING đã qua giờ bắt đầu khung bị gắn cờ trễ; READY thì không (BR-26)")
    void dsh02() throws Exception {
        MockHttpSession customer = login("khach1@example.com");
        long confirmed =
                ((Number) order(customer, "12:00", line("Burger bò", 1)).json("$.id")).longValue();
        long ready = ((Number) order(customer, "12:00", line("Burger bò", 1)).json("$.id")).longValue();
        long future = ((Number) order(customer, "12:30", line("Burger bò", 1)).json("$.id")).longValue();
        setStatus(ready, "READY");
        at("12:05");

        Res result = get("/api/admin/dashboard", login("quanly@example.com"));

        assertThat(result.<List<Boolean>>at("$.activeOrders[?(@.id==" + confirmed + ")].late"))
                .containsExactly(true);
        assertThat(result.<List<Boolean>>at("$.activeOrders[?(@.id==" + ready + ")].late"))
                .containsExactly(false);
        assertThat(result.<List<Boolean>>at("$.activeOrders[?(@.id==" + future + ")].late"))
                .containsExactly(false);
        assertThat((Integer) result.at("$.stats.lateNow")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-DSH-03 Đếm no-show; tỷ lệ dùng quota = điểm giữ / tổng quota các khung trong ngày")
    void dsh03() throws Exception {
        long id = ((Number) order(login("khach1@example.com"), "12:00", line("Burger bò", 1))
                        .json("$.id"))
                .longValue();
        setStatus(id, "READY");
        at("13:00");
        assertThat(post("/api/counter/orders/" + id + "/no-show", login("quay1@example.com"), null)
                        .status())
                .isEqualTo(200);

        Res result = get("/api/admin/dashboard", login("quanly@example.com"));

        assertThat((Integer) result.at("$.stats.noShow")).isEqualTo(1);
        // 10:00–21:00 là 44 khung × 20 điểm = 880 điểm; giữ 5 điểm → 1 % (làm tròn).
        assertThat((Integer) result.at("$.stats.quotaPoints")).isEqualTo(880);
        assertThat((Integer) result.at("$.stats.quotaUsagePercent")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-DSH-04 Chỉ quản lý xem được bảng theo dõi")
    void dsh04() throws Exception {
        for (String loginId : List.of("quay1@example.com", "bep1@example.com", "khach1@example.com")) {
            assertThat(get("/api/admin/dashboard", login(loginId)).status()).isEqualTo(403);
        }
    }
}
