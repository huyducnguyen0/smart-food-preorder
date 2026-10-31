package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * Black-box Sprint 4, PB-22 lịch bếp, với cấu hình mặc định: bếp 20 điểm mỗi khoảng làm 15 phút, làm trước tối đa 45
 * phút, đơn đặt trước dùng tối đa 70% (14 điểm) mỗi khoảng, quầy tối đa 8 đơn mỗi khung nhận. Giờ hiện tại 11:00.
 * Burger bò 5 điểm, Khoai tây chiên 2 điểm, Trà đào 0 điểm.
 */
class Sprint4KitchenPlanBlackBoxTest extends BlackBoxBase {

    @BeforeEach
    void sprint4Settings() {
        useSprint4Settings();
    }

    private Res walkIn(MockHttpSession staff, String hhmm, String... lines) throws Exception {
        return post(
                "/api/counter/walk-in",
                staff,
                "{\"requestId\":\"" + newRequestId() + "\",\"slotStart\":\"" + slot(hhmm)
                        + "\",\"paymentMethod\":\"CASH\",\"lines\":[" + String.join(",", lines) + "]}");
    }

    private long id(Res res) {
        return ((Number) res.json("$.id")).longValue();
    }

    @Test
    @DisplayName("TC-PLAN-01 Làm trước: khung 12:00 nhận 40 điểm đặt trước (mô hình cũ chỉ 20); vượt 42 thì hết chỗ")
    void plan01() throws Exception {
        List<MockHttpSession> customers = sessionsFor(createCustomers(5));
        for (int i = 0; i < 4; i++) {
            assertThat(order(customers.get(i), "12:00", line("Burger bò", 2)).status())
                    .as("đơn %d", i + 1)
                    .isEqualTo(201);
        }

        Res fifth = order(customers.get(4), "12:00", line("Burger bò", 2));

        assertThat(fifth.status()).isEqualTo(409);
        assertThat((String) fifth.at("$.code")).isEqualTo("SLOT_FULL");
        assertThat((String) fifth.at("$.reason")).isEqualTo("KITCHEN");
        assertThat(held("12:00")).isEqualTo(40);
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName(
            "TC-PLAN-02 Dành chỗ cho walk-in: đặt trước đã dùng hết phần 70%, khách tại quầy vẫn được nhận khung 12:00")
    void plan02() throws Exception {
        List<MockHttpSession> customers = sessionsFor(createCustomers(4));
        for (int i = 0; i < 4; i++) {
            order(customers.get(i), "12:00", line("Burger bò", 2));
        }
        assertThat(order(login("khach1@example.com"), "12:00", line("Khoai tây chiên", 2))
                        .status())
                .as("đặt trước đã hết phần của mình")
                .isEqualTo(409);

        Res walkIn = walkIn(login("quay1@example.com"), "12:00", line("Burger bò", 3));

        assertThat(walkIn.status()).isEqualTo(201);
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName("TC-PLAN-03 Quầy tối đa 8 đơn mỗi khung: đơn thứ 9 (kể cả chỉ đồ uống) hết chỗ, khung 12:15 vẫn nhận")
    void plan03() throws Exception {
        List<MockHttpSession> customers = sessionsFor(createCustomers(9));
        for (int i = 0; i < 8; i++) {
            assertThat(order(customers.get(i), "12:00", line("Trà đào", 1)).status())
                    .isEqualTo(201);
        }

        Res ninth = order(customers.get(8), "12:00", line("Trà đào", 1));

        assertThat(ninth.status()).isEqualTo(409);
        assertThat((String) ninth.at("$.reason")).isEqualTo("PICKUP_LIMIT");
        assertThat(order(customers.get(8), "12:15", line("Trà đào", 1)).status())
                .isEqualTo(201);
        Res list = get("/api/slots?lines=" + item("Trà đào").id() + ":1", login("khach1@example.com"));
        assertThat(list.<List<Boolean>>at("$.slots[?(@.start == '" + slot("12:00") + "')].bookable"))
                .containsExactly(false);
    }

    @Test
    @DisplayName(
            "TC-PLAN-04 Hàng chờ bếp theo lịch: đơn khung 12:00 nên làm từ 11:15 (sớm nhất), tới 11:15 thì cần làm")
    void plan04() throws Exception {
        long id = id(order(login("khach1@example.com"), "12:00", line("Burger bò", 1)));
        MockHttpSession kitchen = login("bep1@example.com");

        Res early = get("/api/kitchen/queue", kitchen);
        assertThat(early.<List<Integer>>at("$.upcoming[*].id")).containsExactly((int) id);
        assertThat(early.<List<String>>at("$.upcoming[*].prepareFrom")).containsExactly(slot("11:15"));

        at("11:15");
        assertThat(get("/api/kitchen/queue", kitchen).<List<Integer>>at("$.dueSoon[*].id"))
                .containsExactly((int) id);
    }

    @Test
    @DisplayName("TC-PLAN-05 Màn quản lý: điểm bếp dự kiến làm theo khoảng, số đơn mỗi khung nhận, thông số lịch bếp")
    void plan05() throws Exception {
        order(login("khach1@example.com"), "12:00", line("Burger bò", 6)); // 30 điểm

        Res day = get("/api/admin/slots/today", login("quanly@example.com"));

        assertThat((Integer) day.at("$.holdMinutes")).isEqualTo(45);
        assertThat((Integer) day.at("$.preOrderSharePercent")).isEqualTo(70);
        assertThat((Integer) day.at("$.pickupLimitOrders")).isEqualTo(8);
        assertThat(day.<List<Integer>>at("$.slots[?(@.start == '" + slot("11:15") + "')].plannedPoints"))
                .containsExactly(20);
        assertThat(day.<List<Integer>>at("$.slots[?(@.start == '" + slot("11:30") + "')].plannedPoints"))
                .containsExactly(10);
        assertThat(day.<List<Integer>>at("$.slots[?(@.start == '" + slot("12:00") + "')].pickupOrders"))
                .containsExactly(1);
    }

    @Test
    @DisplayName("TC-PLAN-06 Đổi thông số lịch bếp; giảm công suất tới mức bếp không kịp đơn đã nhận thì bị từ chối")
    void plan06() throws Exception {
        MockHttpSession manager = login("quanly@example.com");
        Res updated = send(
                "PUT",
                "/api/admin/settings",
                manager,
                "{\"openTime\":\"10:00\",\"closeTime\":\"21:00\",\"defaultQuotaPoints\":20,"
                        + "\"holdMinutes\":30,\"preOrderSharePercent\":60,\"pickupLimitOrders\":5}");
        assertThat(updated.status()).isEqualTo(200);
        assertThat((Integer) updated.at("$.holdMinutes")).isEqualTo(30);
        assertThat((Integer) updated.at("$.preOrderSharePercent")).isEqualTo(60);
        assertThat((Integer) updated.at("$.pickupLimitOrders")).isEqualTo(5);

        // Làm trước 30 phút: khung 12:00 làm được trong 11:30, 11:45 (2 × 12 điểm cho đặt trước).
        order(login("khach1@example.com"), "12:00", line("Burger bò", 4)); // 20 điểm

        Res tooLow = send("PUT", "/api/admin/slots/today/11:45/quota", manager, "{\"quota\":0}");
        assertThat(tooLow.status())
                .as("bỏ khoảng 11:45 thì 11:30 (20 điểm) vẫn đủ cho đơn 20 điểm")
                .isEqualTo(200);
        Res breaking = send("PUT", "/api/admin/slots/today/11:30/quota", manager, "{\"quota\":10}");
        assertThat(breaking.status()).isEqualTo(409);
        assertThat((String) breaking.at("$.code")).isEqualTo("QUOTA_BELOW_HELD");
    }

    @Test
    @DisplayName("TC-PLAN-07 Hủy đơn trả lại chỗ trong lịch bếp: khung vừa hết chỗ lại nhận được")
    void plan07() throws Exception {
        List<MockHttpSession> customers = sessionsFor(createCustomers(5));
        long first = id(order(customers.get(0), "12:00", line("Burger bò", 2)));
        for (int i = 1; i < 4; i++) {
            order(customers.get(i), "12:00", line("Burger bò", 2));
        }
        assertThat(order(customers.get(4), "12:00", line("Burger bò", 2)).status())
                .isEqualTo(409);

        assertThat(post("/api/orders/" + first + "/cancel", customers.get(0), null)
                        .status())
                .isEqualTo(200);

        assertThat(order(customers.get(4), "12:00", line("Burger bò", 2)).status())
                .isEqualTo(201);
        assertHeldMatchesOrders();
    }

    @Test
    @DisplayName(
            "TC-PLAN-08 60 khách đặt trước và 20 walk-in cùng lúc vào 12:00–12:15: bếp không bao giờ vượt công suất,"
                    + " quầy không quá 8 đơn mỗi khung (NFR-01)")
    void plan08() throws Exception {
        List<MockHttpSession> customers = sessionsFor(createCustomers(60));
        MockHttpSession counter = login("quay1@example.com");
        MockHttpSession manager = login("quanly@example.com");
        Random random = new Random(20261031);
        List<Callable<Res>> tasks = new ArrayList<>();
        for (MockHttpSession customer : customers) {
            String hhmm = random.nextBoolean() ? "12:00" : "12:15";
            int burgers = 1 + random.nextInt(3);
            tasks.add(() -> order(customer, hhmm, line("Burger bò", burgers)));
        }
        for (int i = 0; i < 20; i++) {
            MockHttpSession staff = i % 2 == 0 ? counter : manager;
            String hhmm = i % 2 == 0 ? "12:00" : "12:15";
            tasks.add(() -> walkIn(staff, hhmm, line("Khoai tây chiên", 2)));
        }

        List<Res> results = concurrently(tasks);

        assertThat(results).allSatisfy(r -> assertThat(r.status()).isIn(201, 409));
        assertThat(results.stream().filter(r -> r.status() == 409))
                .allSatisfy(r -> assertThat((String) r.at("$.code")).isIn("SLOT_FULL", "ORDER_TOO_LARGE"));
        for (String hhmm : List.of("12:00", "12:15")) {
            int orders = jdbc.queryForObject(
                    "SELECT count(*) FROM orders o JOIN time_slots s ON s.id = o.time_slot_id"
                            + " WHERE s.slot_date = ? AND s.start_time = ?::time",
                    Integer.class,
                    BlackBoxClockConfig.TODAY,
                    hhmm);
            assertThat(orders).as("số đơn khung %s", hhmm).isLessThanOrEqualTo(8);
        }
        assertHeldMatchesOrders();
    }
}
