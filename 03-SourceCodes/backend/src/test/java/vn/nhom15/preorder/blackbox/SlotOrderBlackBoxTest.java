package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;

/** Black-box UC-04 chọn khung và đặt đơn, UC-17 giữ quota (T-25): danh sách khung và đặt đơn qua API. */
class SlotOrderBlackBoxTest extends BlackBoxBase {

    private String slotsUrl(String... itemsAndQuantities) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < itemsAndQuantities.length; i += 2) {
            parts.add(item(itemsAndQuantities[i]).id() + ":" + itemsAndQuantities[i + 1]);
        }
        return "/api/slots?lines=" + String.join(",", parts);
    }

    private List<String> slotStarts(Res slots) {
        return slots.at("$.slots[*].start");
    }

    private boolean bookable(Res slots, String hhmm) {
        List<Boolean> found = slots.at("$.slots[?(@.start == '" + slot(hhmm) + "')].bookable");
        assertThat(found).as("khung %s phải có trong danh sách", hhmm).hasSize(1);
        return found.getFirst();
    }

    // ---------- Danh sách khung (UC-04 bước 6) ----------

    @Test
    @DisplayName("TC-SLOT-01 T=11:00: khung đầu 11:30, dài 15 phút, khung cuối 20:45, tất cả đặt được")
    void slot01() throws Exception {
        Res slots = get(slotsUrl("Burger bò", "1"), login("khach1@example.com"));

        List<String> starts = slotStarts(slots);
        assertThat(starts.getFirst()).isEqualTo(slot("11:30"));
        assertThat(starts.getLast()).isEqualTo(slot("20:45"));
        assertThat(starts).hasSize(38);
        List<String> ends = slots.at("$.slots[*].end");
        assertThat(ends.getFirst()).isEqualTo(slot("11:45"));
        assertThat(slots.<List<Boolean>>at("$.slots[*].bookable")).containsOnly(true);
    }

    @Test
    @DisplayName("TC-SLOT-02 Biên đúng 30 phút: có khung 11:30; không có 11:15 và 11:00")
    void slot02() throws Exception {
        List<String> starts = slotStarts(get(slotsUrl("Burger bò", "1"), login("khach1@example.com")));

        assertThat(starts).contains(slot("11:30")).doesNotContain(slot("11:15"), slot("11:00"));
    }

    @Test
    @DisplayName("TC-SLOT-03 T=11:01: không có khung 11:30; khung đầu là 11:45")
    void slot03() throws Exception {
        at("11:01");

        List<String> starts = slotStarts(get(slotsUrl("Burger bò", "1"), login("khach1@example.com")));

        assertThat(starts).doesNotContain(slot("11:30"));
        assertThat(starts.getFirst()).isEqualTo(slot("11:45"));
    }

    @Test
    @DisplayName("TC-SLOT-04 T=09:00 trước giờ mở cửa: khung đầu là 10:00")
    void slot04() throws Exception {
        at("09:00");

        assertThat(slotStarts(get(slotsUrl("Burger bò", "1"), login("khach1@example.com")))
                        .getFirst())
                .isEqualTo(slot("10:00"));
    }

    @Test
    @DisplayName("TC-SLOT-05 T=20:20: không còn khung hôm nay")
    void slot05() throws Exception {
        at("20:20");

        Res slots = get(slotsUrl("Burger bò", "1"), login("khach1@example.com"));

        assertThat(slotStarts(slots)).isEmpty();
        assertThat((String) slots.at("$.unavailableReason")).isEqualTo("NO_SLOT_LEFT_TODAY");
    }

    @Test
    @DisplayName("TC-SLOT-06 Chỉ có khung của hôm nay")
    void slot06() throws Exception {
        for (String hhmm : List.of("09:00", "11:00", "19:00")) {
            at(hhmm);
            Res slots = get(slotsUrl("Burger bò", "1"), login("khach1@example.com"));
            assertThat(slotStarts(slots))
                    .allSatisfy(start -> assertThat(start).startsWith(BlackBoxClockConfig.TODAY.toString()));
            assertThat((String) slots.at("$.date")).isEqualTo(BlackBoxClockConfig.TODAY.toString());
        }
    }

    @Test
    @DisplayName("TC-SLOT-08 Dữ liệu danh sách khung không có quota hay điểm đã giữ")
    void slot08() throws Exception {
        setHeld("12:00", 18);

        String body =
                get(slotsUrl("Burger bò", "1"), login("khach1@example.com")).body();

        assertThat(body)
                .doesNotContainIgnoringCase("quota")
                .doesNotContainIgnoringCase("held")
                .doesNotContainIgnoringCase("point")
                .doesNotContainIgnoringCase("workload");
    }

    @Test
    @DisplayName("TC-SLOT-09 Khung giữ 15, giỏ 5 điểm: đặt được (còn đúng 5)")
    void slot09() throws Exception {
        setHeld("12:00", 15);

        assertThat(bookable(get(slotsUrl("Burger bò", "1"), login("khach1@example.com")), "12:00"))
                .isTrue();
    }

    @Test
    @DisplayName("TC-SLOT-10 Khung giữ 16, giỏ 5 điểm: không đủ chỗ (còn 4)")
    void slot10() throws Exception {
        setHeld("12:00", 16);

        Res slots = get(slotsUrl("Burger bò", "1"), login("khach1@example.com"));

        assertThat(bookable(slots, "12:00")).isFalse();
        assertThat(bookable(slots, "12:15")).isTrue();
    }

    @Test
    @DisplayName("TC-SLOT-11 Mọi khung còn lại đã giữ từ 18 điểm: báo hết chỗ trong ngày")
    void slot11() throws Exception {
        for (int minutes = 11 * 60 + 30; minutes <= 20 * 60 + 45; minutes += 15) {
            setHeld(LocalTime.of(minutes / 60, minutes % 60).toString(), 18);
        }

        Res slots = get(slotsUrl("Burger bò", "1"), login("khach1@example.com"));

        assertThat(slots.<List<Boolean>>at("$.slots[*].bookable")).containsOnly(false);
        assertThat((String) slots.at("$.unavailableReason")).isEqualTo("ALL_SLOTS_FULL");
    }

    @Test
    @DisplayName("TC-SLOT-12 Giỏ 21 điểm: báo đơn quá lớn, không khung nào chọn được")
    void slot12() throws Exception {
        Res slots = get(slotsUrl("Burger phô mai đôi", "3"), login("khach1@example.com"));

        assertThat(slots.<List<Boolean>>at("$.slots[*].bookable")).containsOnly(false);
        assertThat((String) slots.at("$.unavailableReason")).isEqualTo("ORDER_TOO_LARGE");
    }

    @Test
    @DisplayName("TC-SLOT-13 Giỏ 0 điểm, mọi khung đầy: mọi khung vẫn đặt được")
    void slot13() throws Exception {
        for (int minutes = 10 * 60; minutes <= 20 * 60 + 45; minutes += 15) {
            setHeld(LocalTime.of(minutes / 60, minutes % 60).toString(), 20);
        }

        Res slots = get(slotsUrl("Trà đào", "2", "Nước suối", "1"), login("khach1@example.com"));

        assertThat(slots.<List<Boolean>>at("$.slots[*].bookable")).containsOnly(true);
        assertThat(slots.has("$.unavailableReason")).isFalse();
    }

    @Test
    @DisplayName("TC-SLOT-14 Quota riêng khung 13:00 = 4, giỏ 5 điểm: chỉ 13:00 không đủ chỗ")
    void slot14() throws Exception {
        setQuotaOverride("13:00", 4);

        Res slots = get(slotsUrl("Burger bò", "1"), login("khach1@example.com"));

        assertThat(bookable(slots, "13:00")).isFalse();
        assertThat(bookable(slots, "12:45")).isTrue();
        assertThat(bookable(slots, "13:15")).isTrue();
    }

    @Test
    @DisplayName("TC-SLOT-15 Chưa đăng nhập và BẾP gọi danh sách khung: bị từ chối")
    void slot15() throws Exception {
        assertThat(get(slotsUrl("Burger bò", "1"), null).status()).isEqualTo(401);
        assertThat(get(slotsUrl("Burger bò", "1"), login("bep1@example.com")).status())
                .isEqualTo(403);
    }

    // ---------- Đặt đơn (UC-04 bước 7–11) ----------

    @Test
    @DisplayName("TC-ORD-01 Đặt 10 điểm: tạo đúng một đơn CONFIRMED, UNPAID, pre-order, 110.000, có mã nhận")
    void ord01() throws Exception {
        Res result = order(login("khach1@example.com"), "12:00", line("Burger bò", 2), line("Trà đào", 1));

        assertThat(result.status()).isEqualTo(201);
        assertThat((String) result.at("$.status")).isEqualTo("CONFIRMED");
        assertThat((String) result.at("$.paymentStatus")).isEqualTo("UNPAID");
        assertThat((Integer) result.at("$.totalAmount")).isEqualTo(110000);
        assertThat((String) result.at("$.slotStart")).isEqualTo(slot("12:00"));
        assertThat(orderCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT source FROM orders", String.class))
                .isEqualTo("PRE_ORDER");
        assertThat(jdbc.queryForObject("SELECT workload_points FROM orders", Integer.class))
                .isEqualTo(10);
        assertThat(held("12:00")).isEqualTo(10);
        assertThat((String) result.at("$.pickupCode")).hasSize(6);
    }

    @Test
    @DisplayName("TC-ORD-02 Mã nhận 6 ký tự chữ hoa và số, không có 0, O, 1, I, L")
    void ord02() throws Exception {
        Res result = order(login("khach1@example.com"), "12:00", line("Burger bò", 2), line("Trà đào", 1));

        assertThat((String) result.at("$.pickupCode")).matches("[A-Z2-9]{6}").doesNotContain("0", "O", "1", "I", "L");
    }

    @Test
    @DisplayName("TC-ORD-03 100 đơn trong ngày: không có hai mã nhận trùng")
    void ord03() throws Exception {
        List<MockHttpSession> sessions = sessionsFor(createCustomers(34));
        Set<String> codes = new HashSet<>();
        int placed = 0;
        for (MockHttpSession session : sessions) {
            for (int i = 0; i < 3 && placed < 100; i++, placed++) {
                // Đơn 0 điểm để không đụng quota: mục tiêu là kiểm tra tính duy nhất của mã.
                Res result = order(session, "12:00", line("Nước suối", 1));
                assertThat(result.status()).isEqualTo(201);
                codes.add(result.at("$.pickupCode"));
            }
        }

        assertThat(placed).isEqualTo(100);
        assertThat(codes).hasSize(100);
    }

    @Test
    @DisplayName("TC-ORD-04 Đơn 10 + đơn 10 vào cùng khung được nhận (đủ 20); đơn nhỏ sau đó hết chỗ")
    void ord04() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        MockHttpSession kh2 = login("0901234567");
        assertThat(order(kh1, "12:00", line("Burger bò", 2)).status()).isEqualTo(201);

        assertThat(order(kh2, "12:00", line("Burger bò", 2)).status()).isEqualTo(201);
        Res last = order(kh2, "12:00", line("Khoai tây chiên", 1));

        assertThat(last.status()).isEqualTo(409);
        assertThat((String) last.at("$.code")).isEqualTo("SLOT_FULL");
        assertThat(held("12:00")).isEqualTo(20);
        assertThat(orderCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-ORD-07 Đổi giá M1 sau khi đặt: đơn cũ giữ nguyên giá và tổng tiền")
    void ord07() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        long id = ((Number) order(kh1, "12:00", line("Burger bò", 2), line("Trà đào", 1))
                        .json("$.id"))
                .longValue();

        jdbc.update("UPDATE menu_items SET price = 99000 WHERE name = 'Burger bò'");

        Res again = get("/api/orders/" + id, kh1);
        assertThat((Integer) again.at("$.totalAmount")).isEqualTo(110000);
        assertThat(again.<List<Integer>>at("$.lines[*].unitPrice")).containsExactly(45000, 20000);
    }

    @Test
    @DisplayName("TC-ORD-08 Đơn 0 điểm khi khung đã giữ 20: được nhận, điểm giữ vẫn 20")
    void ord08() throws Exception {
        setHeld("12:00", 20);

        Res result = order(login("khach1@example.com"), "12:00", line("Trà đào", 2), line("Nước suối", 1));

        assertThat(result.status()).isEqualTo(201);
        assertThat((String) result.at("$.status")).isEqualTo("CONFIRMED");
        assertThat(held("12:00")).isEqualTo(20);
    }

    @Test
    @DisplayName("TC-ORD-09 Đơn đúng 20 điểm: được nhận, khung giữ đúng 20")
    void ord09() throws Exception {
        assertThat(order(login("khach1@example.com"), "12:00", line("Burger bò", 4))
                        .status())
                .isEqualTo(201);

        assertThat(held("12:00")).isEqualTo(20);
    }

    @Test
    @DisplayName("TC-ORD-10 M1×2 + M4×1 + M5×2 = 20 điểm: điểm đơn lưu 20, khung tăng 20")
    void ord10() throws Exception {
        Res result = order(
                login("khach1@example.com"),
                "12:00",
                line("Burger bò", 2),
                line("Gà rán 2 miếng", 1),
                line("Khoai tây chiên", 2));

        assertThat(result.status()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT workload_points FROM orders", Integer.class))
                .isEqualTo(20);
        assertThat(held("12:00")).isEqualTo(20);
    }

    @Test
    @DisplayName("TC-ORD-11 Gửi 5 lần liên tiếp cùng mã yêu cầu: đúng 1 đơn, cùng mã nhận, khung giữ 10")
    void ord11() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        String body = orderJson(newRequestId(), slot("12:00"), line("Burger bò", 2));

        List<Res> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(post("/api/orders", kh1, body));
        }

        assertThat(results.getFirst().status()).isEqualTo(201);
        assertThat(results.subList(1, 5)).allSatisfy(r -> assertThat(r.status()).isEqualTo(200));
        assertThat(results.stream().map(r -> r.<Integer>at("$.id")).distinct()).hasSize(1);
        assertThat(results.stream().map(r -> r.<String>at("$.pickupCode")).distinct())
                .hasSize(1);
        assertThat(orderCount()).isEqualTo(1);
        assertThat(held("12:00")).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-ORD-12 Gửi 5 lần đồng thời cùng mã yêu cầu: đúng 1 đơn, khung giữ 10")
    void ord12() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        String body = orderJson(newRequestId(), slot("12:00"), line("Burger bò", 2));
        List<Callable<Res>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> post("/api/orders", kh1, body));
        }

        List<Res> results = concurrently(tasks);

        assertThat(results).allSatisfy(r -> assertThat(r.status()).isIn(200, 201));
        assertThat(results.stream().filter(r -> r.status() == 201)).hasSize(1);
        assertThat(results.stream().map(r -> r.<Integer>at("$.id")).distinct()).hasSize(1);
        assertThat(orderCount()).isEqualTo(1);
        assertThat(held("12:00")).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-ORD-13 Gửi lại cùng mã khi khách đủ 3 đơn và khung đầy: vẫn trả đơn cũ")
    void ord13() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        String body = orderJson(newRequestId(), slot("12:00"), line("Burger bò", 2));
        Res first = post("/api/orders", kh1, body);
        assertThat(order(kh1, "12:15", line("Burger bò", 1)).status()).isEqualTo(201);
        assertThat(order(kh1, "12:30", line("Burger bò", 1)).status()).isEqualTo(201);
        setHeld("12:00", 20);
        assertThat(order(kh1, "12:45", line("Burger bò", 1)).status()).isEqualTo(409);

        Res again = post("/api/orders", kh1, body);

        assertThat(again.status()).isEqualTo(200);
        assertThat(again.<Integer>at("$.id")).isEqualTo(first.<Integer>at("$.id"));
    }

    @Test
    @DisplayName("TC-ORD-14 Hai yêu cầu mã khác nhau cùng giỏ: hai đơn, mỗi đơn giữ quota riêng")
    void ord14() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");

        Res a = order(kh1, "12:00", line("Burger bò", 1));
        Res b = order(kh1, "12:00", line("Burger bò", 1));

        assertThat(a.<Integer>at("$.id")).isNotEqualTo(b.<Integer>at("$.id"));
        assertThat(orderCount()).isEqualTo(2);
        assertThat(held("12:00")).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-ORD-15 Đơn thứ 4 khi đang có 3 đơn: bị từ chối, không tạo đơn, quota không đổi")
    void ord15() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        for (String hhmm : List.of("12:00", "12:15", "12:30")) {
            assertThat(order(kh1, hhmm, line("Burger bò", 1)).status()).isEqualTo(201);
        }

        Res fourth = order(kh1, "13:00", line("Burger bò", 1));

        assertThat(fourth.status()).isEqualTo(409);
        assertThat((String) fourth.at("$.code")).isEqualTo("OPEN_ORDER_LIMIT_REACHED");
        assertThat((Integer) fourth.at("$.openOrders")).isEqualTo(3);
        assertThat(orderCount()).isEqualTo(3);
        assertThat(held("13:00")).isZero();
    }

    @Test
    @DisplayName("TC-ORD-16 Có 2 đơn chưa kết thúc, đặt đơn thứ 3: được nhận")
    void ord16() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        order(kh1, "12:00", line("Burger bò", 1));
        order(kh1, "12:15", line("Burger bò", 1));

        assertThat(order(kh1, "12:30", line("Burger bò", 1)).status()).isEqualTo(201);
    }

    @Test
    @DisplayName("TC-ORD-17 Ba đơn ở CONFIRMED, PREPARING, READY: đặt thêm bị từ chối")
    void ord17() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        long a = ((Number) order(kh1, "12:00", line("Burger bò", 1)).json("$.id")).longValue();
        long b = ((Number) order(kh1, "12:15", line("Burger bò", 1)).json("$.id")).longValue();
        order(kh1, "12:30", line("Burger bò", 1));
        MockHttpSession kitchen = login("bep1@example.com");
        post("/api/kitchen/orders/" + a + "/start", kitchen, null);
        post("/api/kitchen/orders/" + b + "/start", kitchen, null);
        post("/api/kitchen/orders/" + b + "/complete", kitchen, null);
        assertThat(List.of(statusOf(a), statusOf(b))).containsExactly("PREPARING", "READY");

        Res more = order(kh1, "13:00", line("Burger bò", 1));

        assertThat(more.status()).isEqualTo(409);
        assertThat((String) more.at("$.code")).isEqualTo("OPEN_ORDER_LIMIT_REACHED");
    }

    @Test
    @DisplayName("TC-ORD-18 2 đơn chưa kết thúc và 5 đơn đã kết thúc: đặt thêm được nhận")
    void ord18() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        List<Long> ids = new ArrayList<>();
        // Đơn 0 điểm để không đụng quota khi dựng 7 đơn cho một khách.
        for (int i = 0; i < 7; i++) {
            ids.add(((Number) order(kh1, "12:00", line("Nước suối", 1)).json("$.id")).longValue());
            if (i < 5) {
                setStatus(
                        ids.getLast(),
                        List.of("PICKED_UP", "PICKED_UP", "CANCELLED", "NO_SHOW", "PICKED_UP")
                                .get(i));
            }
        }

        assertThat(order(kh1, "13:00", line("Nước suối", 1)).status())
                .as("2 đơn mở + 5 đơn đã kết thúc")
                .isEqualTo(201);
    }

    @Test
    @DisplayName("TC-ORD-19 KH1 đủ 3 đơn: KH2 vẫn đặt được")
    void ord19() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        for (String hhmm : List.of("12:00", "12:15", "12:30")) {
            order(kh1, hhmm, line("Burger bò", 1));
        }

        assertThat(order(login("0901234567"), "13:00", line("Burger bò", 1)).status())
                .isEqualTo(201);
    }

    @Test
    @DisplayName("TC-ORD-20 Khung 11:30 chọn lúc 11:00, gửi lúc 11:05: bị từ chối khung không còn đặt được")
    void ord20() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        assertThat(slotStarts(get(slotsUrl("Burger bò", "1"), kh1))).contains(slot("11:30"));
        at("11:05");

        Res result = order(kh1, "11:30", line("Burger bò", 1));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("SLOT_NOT_BOOKABLE");
        assertThat(orderCount()).isZero();
        assertThat(held("11:30")).isZero();
    }

    @Test
    @DisplayName("TC-ORD-21 Khung ngày khác, sai mốc, ngoài giờ mở, 21:00: đều bị từ chối")
    void ord21() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        String tomorrow = BlackBoxClockConfig.TODAY.plusDays(1) + "T12:00";
        String yesterday = BlackBoxClockConfig.TODAY.minusDays(1) + "T12:00";
        for (String start :
                List.of(tomorrow, yesterday, slot("11:07"), slot("09:00"), slot("21:00"), slot("12:00:30"))) {
            Res result = post("/api/orders", kh1, orderJson(newRequestId(), start, line("Burger bò", 1)));
            assertThat(result.status()).as(start).isEqualTo(409);
            assertThat((String) result.at("$.code")).as(start).isEqualTo("SLOT_NOT_BOOKABLE");
        }
        assertThat(orderCount()).isZero();
    }

    @Test
    @DisplayName("TC-ORD-22 Đổi giá M1 sau khi vào giỏ: không tạo đơn, báo giá mới; gửi lại với giá mới thì đặt được")
    void ord22() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        Item m1 = item("Burger bò");
        jdbc.update("UPDATE menu_items SET price = 47000 WHERE id = ?", m1.id());

        Res rejected = order(kh1, "12:00", line(m1.id(), 1, 45000));

        assertThat(rejected.status()).isEqualTo(409);
        assertThat((String) rejected.at("$.code")).isEqualTo("ITEMS_CHANGED");
        assertThat((String) rejected.at("$.items[0].reason")).isEqualTo("PRICE_CHANGED");
        assertThat((Integer) rejected.at("$.items[0].currentPrice")).isEqualTo(47000);
        assertThat(orderCount()).isZero();
        assertThat(held("12:00")).isZero();
        assertThat(order(kh1, "12:00", line(m1.id(), 1, 47000)).status()).isEqualTo(201);
    }

    @Test
    @DisplayName("TC-ORD-23 M4 chuyển tạm hết: không tạo đơn, báo M4 không còn bán")
    void ord23() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        String lines1 = line("Burger bò", 1);
        String lines4 = line("Gà rán 2 miếng", 1);
        jdbc.update("UPDATE menu_items SET available = false WHERE name = 'Gà rán 2 miếng'");

        Res result = order(kh1, "12:00", lines1, lines4);

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("ITEMS_CHANGED");
        assertThat(result.<List<Integer>>at("$.items[*].menuItemId"))
                .containsExactly((int) item("Gà rán 2 miếng").id());
        assertThat((String) result.at("$.items[0].reason")).isEqualTo("UNAVAILABLE");
        assertThat(orderCount()).isZero();
    }

    @Test
    @DisplayName("TC-ORD-24 Giỏ có món ngừng bán: không tạo đơn, báo món không còn bán")
    void ord24() throws Exception {
        Item discontinued = item("Combo cũ");

        Res result = order(login("khach1@example.com"), "12:00", line(discontinued.id(), 1, discontinued.price()));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.items[0].reason")).isEqualTo("UNAVAILABLE");
        assertThat(orderCount()).isZero();
    }

    @Test
    @DisplayName("TC-ORD-25 Nhiều món đổi cùng lúc: báo từng món")
    void ord25() throws Exception {
        Item m1 = item("Burger bò");
        Item m4 = item("Gà rán 2 miếng");
        Item m5 = item("Khoai tây chiên");
        jdbc.update("UPDATE menu_items SET price = 46000 WHERE id = ?", m1.id());
        jdbc.update("UPDATE menu_items SET available = false WHERE id = ?", m4.id());

        Res result = order(
                login("khach1@example.com"),
                "12:00",
                line(m1.id(), 1, m1.price()),
                line(m4.id(), 1, m4.price()),
                line(m5.id(), 1, m5.price()));

        assertThat(result.status()).isEqualTo(409);
        assertThat(result.<List<Integer>>at("$.items[*].menuItemId"))
                .containsExactlyInAnyOrder((int) m1.id(), (int) m4.id());
    }

    @Test
    @DisplayName("TC-ORD-26 KH2 vừa đặt 15 điểm vào khung 12:00, KH1 đặt 10 điểm: hết chỗ, khung vẫn giữ 15")
    void ord26() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        MockHttpSession kh2 = login("0901234567");
        assertThat(slotStarts(get(slotsUrl("Burger bò", "2"), kh1))).contains(slot("12:00"));
        assertThat(order(kh2, "12:00", line("Burger bò", 3)).status()).isEqualTo(201);

        Res result = order(kh1, "12:00", line("Burger bò", 2));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("SLOT_FULL");
        assertThat(orderCount()).isEqualTo(1);
        assertThat(held("12:00")).isEqualTo(15);
        assertThat(bookable(get(slotsUrl("Burger bò", "2"), kh1), "12:00"))
                .as("trạng thái mới")
                .isFalse();
    }

    @Test
    @DisplayName("TC-ORD-27 Giỏ 21 điểm, khung trống: báo đơn quá lớn (khác hết chỗ), quota không đổi")
    void ord27() throws Exception {
        Res result = order(login("khach1@example.com"), "12:00", line("Burger phô mai đôi", 3));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("ORDER_TOO_LARGE");
        assertThat(held("12:00")).isZero();
        assertThat(orderCount()).isZero();
    }

    @Test
    @DisplayName("TC-ORD-28 Quota riêng khung 12:00 = 6, giỏ 8 điểm: đơn quá lớn so với quota riêng")
    void ord28() throws Exception {
        setQuotaOverride("12:00", 6);

        Res result = order(login("khach1@example.com"), "12:00", line("Khoai tây chiên", 4));

        assertThat(result.status()).isEqualTo(409);
        assertThat((String) result.at("$.code")).isEqualTo("ORDER_TOO_LARGE");
    }

    @Test
    @DisplayName(
            "TC-ORD-29 Lỗi database sau khi giữ quota: báo lỗi, không đơn, quota về như cũ, gửi lại tạo đúng một đơn")
    void ord29() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        String body = orderJson(newRequestId(), slot("12:00"), line("Burger bò", 2));
        doThrow(new IllegalStateException("Lỗi database giả lập"))
                .when(orderRepository)
                .insert(any());

        Res failed = post("/api/orders", kh1, body);

        assertThat(failed.status()).isEqualTo(500);
        assertThat((String) failed.at("$.code")).isEqualTo("INTERNAL_ERROR");
        assertThat(orderCount()).isZero();
        assertThat(held("12:00")).as("quota trở về như trước").isZero();

        Mockito.reset(orderRepository);
        Res retried = post("/api/orders", kh1, body);
        assertThat(retried.status()).isEqualTo(201);
        assertThat(orderCount()).isEqualTo(1);
        assertThat(held("12:00")).isEqualTo(10);
    }

    @Test
    @DisplayName("TC-ORD-31 Danh sách món rỗng: bị từ chối dữ liệu không hợp lệ")
    void ord31() throws Exception {
        Res result = post("/api/orders", login("khach1@example.com"), orderJson(newRequestId(), slot("12:00")));

        assertThat(result.status()).isEqualTo(400);
        assertThat((String) result.at("$.code")).isEqualTo("VALIDATION_ERROR");
        assertThat(orderCount()).isZero();
    }

    @Test
    @DisplayName("TC-ORD-32 Số lượng 0, âm, món không tồn tại, thiếu mã yêu cầu: bị từ chối, không tạo đơn")
    void ord32() throws Exception {
        MockHttpSession kh1 = login("khach1@example.com");
        long m1 = item("Burger bò").id();

        for (String body : List.of(
                orderJson(newRequestId(), slot("12:00"), line(m1, 0, 45000)),
                orderJson(newRequestId(), slot("12:00"), line(m1, -2, 45000)),
                "{\"slotStart\":\"" + slot("12:00") + "\",\"lines\":[" + line(m1, 1, 45000) + "]}")) {
            Res result = post("/api/orders", kh1, body);
            assertThat(result.status()).as(body).isEqualTo(400);
            assertThat((String) result.at("$.code")).isEqualTo("VALIDATION_ERROR");
        }
        Res unknown = post("/api/orders", kh1, orderJson(newRequestId(), slot("12:00"), line(999999, 1, 1000)));
        assertThat(unknown.status()).isEqualTo(409);
        assertThat((String) unknown.at("$.code")).isEqualTo("ITEMS_CHANGED");
        assertThat(orderCount()).isZero();
    }

    @Test
    @DisplayName("TC-ORD-33 BẾP, QUẦY, chưa đăng nhập gọi API đặt đơn: bị từ chối")
    void ord33() throws Exception {
        String body = orderJson(newRequestId(), slot("12:00"), line("Burger bò", 1));

        assertThat(post("/api/orders", null, body).status()).isEqualTo(401);
        assertThat(post("/api/orders", login("bep1@example.com"), body).status())
                .isEqualTo(403);
        assertThat(post("/api/orders", login("quay1@example.com"), body).status())
                .isEqualTo(403);
        assertThat(orderCount()).isZero();
    }
}
