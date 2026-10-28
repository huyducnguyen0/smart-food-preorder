package vn.nhom15.preorder.blackbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * NFR-04: với 50 người dùng đồng thời, 95 % yêu cầu xem menu, đặt đơn, xem hàng đợi bếp và bàn giao trả lời trong 1
 * giây. Đo qua HTTP thật (Tomcat thật trên cổng ngẫu nhiên, PostgreSQL nhúng) trên máy phát triển; client, server và
 * database chạy chung một máy nên kết quả chỉ là bằng chứng ở môi trường dev, không thay cho đo trên máy chủ thật.
 * Mỗi lần chạy ghi kết quả vào {@code target/nfr04-performance.txt}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Nfr04PerformanceTest extends BlackBoxBase {

    private static final int USERS = 50;
    private static final long LIMIT_MS = 1000;

    @LocalServerPort
    int port;

    /** Một người dùng HTTP có cookie phiên và cookie chống CSRF riêng. */
    private final class User {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> get(String path) throws IOException, InterruptedException {
            return http.send(
                    HttpRequest.newBuilder(uri(path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        HttpResponse<String> post(String path, String json) throws IOException, InterruptedException {
            if (csrf() == null) {
                get("/api/auth/me");
            }
            return http.send(
                    HttpRequest.newBuilder(uri(path))
                            .header("Content-Type", "application/json")
                            .header("X-XSRF-TOKEN", csrf())
                            .POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        private String csrf() {
            return cookies.getCookieStore().getCookies().stream()
                    .filter(c -> c.getName().equals("XSRF-TOKEN"))
                    .map(HttpCookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        User login(String loginId) throws Exception {
            HttpResponse<String> response =
                    post("/api/auth/login", "{\"loginId\":\"" + loginId + "\",\"password\":\"" + PASSWORD + "\"}");
            assertThat(response.statusCode()).as("đăng nhập %s", loginId).isEqualTo(200);
            return this;
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    /** Một mẫu đo: thời gian (ms) và mã HTTP. */
    private record Sample(long millis, int status) {}

    private interface Call {
        HttpResponse<String> run() throws Exception;
    }

    private static Sample timed(Call call) throws Exception {
        long start = System.nanoTime();
        HttpResponse<String> response = call.run();
        return new Sample((System.nanoTime() - start) / 1_000_000, response.statusCode());
    }

    private List<Sample> runAll(List<Callable<List<Sample>>> tasks) throws Exception {
        List<Sample> all = new ArrayList<>();
        concurrently(tasks).forEach(all::addAll);
        return all;
    }

    private static long percentile(List<Sample> samples, double p) {
        List<Long> sorted = new ArrayList<>(samples.stream().map(Sample::millis).toList());
        Collections.sort(sorted);
        int index = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(index, 0));
    }

    @Test
    @DisplayName("NFR-04 50 người dùng đồng thời: p95 menu, đặt đơn, hàng đợi bếp, bàn giao ≤ 1 giây")
    void fiftyConcurrentUsers() throws Exception {
        List<String> customerIds = createCustomers(USERS);
        List<User> customers = new ArrayList<>();
        List<User> kitchens = new ArrayList<>();
        List<User> counters = new ArrayList<>();
        for (int i = 0; i < USERS; i++) {
            customers.add(new User().login(customerIds.get(i)));
            kitchens.add(new User().login("bep1@example.com"));
            counters.add(new User().login("quay1@example.com"));
        }
        // Làm nóng: JVM biên dịch dần mã nóng; không tính các lời gọi này.
        for (int i = 0; i < 20; i++) {
            customers.get(i).get("/api/menu");
            kitchens.get(i).get("/api/kitchen/queue");
        }
        Item fries = item("Khoai tây chiên");

        // 1. Xem menu: 50 người, mỗi người 10 lần liên tiếp.
        List<Callable<List<Sample>>> menu = new ArrayList<>();
        for (User user : customers) {
            menu.add(() -> {
                List<Sample> samples = new ArrayList<>();
                for (int k = 0; k < 10; k++) {
                    samples.add(timed(() -> user.get("/api/menu")));
                }
                return samples;
            });
        }
        List<Sample> menuSamples = runAll(menu);

        // 2. Đặt đơn: 50 người cùng đặt, 2 lượt; rải đều 10 khung từ 12:00 để không khung nào hết quota.
        List<Sample> orderSamples = new ArrayList<>();
        for (int round = 0; round < 2; round++) {
            List<Callable<List<Sample>>> orders = new ArrayList<>();
            for (int i = 0; i < USERS; i++) {
                User user = customers.get(i);
                String start =
                        slot(LocalTime.of(12, 0).plusMinutes(15L * (i % 10)).toString());
                orders.add(() -> List.of(timed(() -> user.post(
                        "/api/orders", orderJson(newRequestId(), start, line(fries.id(), 1, fries.price()))))));
            }
            orderSamples.addAll(runAll(orders));
        }

        // 3. Hàng đợi bếp: 50 phiên bếp, mỗi phiên 10 lần (đặt "bây giờ" để mọi đơn đều vào cửa sổ chuẩn bị).
        at("11:50");
        List<Callable<List<Sample>>> queue = new ArrayList<>();
        for (User user : kitchens) {
            queue.add(() -> {
                List<Sample> samples = new ArrayList<>();
                for (int k = 0; k < 10; k++) {
                    samples.add(timed(() -> user.get("/api/kitchen/queue")));
                }
                return samples;
            });
        }
        List<Sample> queueSamples = runAll(queue);

        // 4. Bàn giao: dựng sẵn mọi đơn READY và đã thanh toán, 50 phiên quầy mỗi phiên giao 2 đơn.
        long counterId =
                jdbc.queryForObject("SELECT id FROM user_accounts WHERE login_id = 'quay1@example.com'", Long.class);
        jdbc.update(
                "UPDATE orders SET status = 'READY', preparing_at = now(), preparing_by = ?, ready_at = now(),"
                        + " ready_by = ?, payment_status = 'PAID', payment_method = 'CASH', paid_at = now(),"
                        + " paid_by = ?",
                kitchenId(),
                kitchenId(),
                counterId);
        List<Map<String, Object>> ready = jdbc.queryForList("SELECT id, pickup_code FROM orders ORDER BY id");
        List<Callable<List<Sample>>> handover = new ArrayList<>();
        for (int i = 0; i < USERS; i++) {
            User user = counters.get(i);
            List<Map<String, Object>> mine = List.of(ready.get(2 * i), ready.get(2 * i + 1));
            handover.add(() -> {
                List<Sample> samples = new ArrayList<>();
                for (Map<String, Object> row : mine) {
                    samples.add(timed(() -> user.post(
                            "/api/counter/orders/" + row.get("id") + "/handover",
                            "{\"pickupCode\":\"" + row.get("pickup_code") + "\"}")));
                }
                return samples;
            });
        }
        List<Sample> handoverSamples = runAll(handover);

        Map<String, List<Sample>> results = new java.util.LinkedHashMap<>();
        results.put("GET /api/menu", menuSamples);
        results.put("POST /api/orders", orderSamples);
        results.put("GET /api/kitchen/queue", queueSamples);
        results.put("POST /api/counter/orders/{id}/handover", handoverSamples);
        StringBuilder report = new StringBuilder("NFR-04, " + USERS + " người dùng đồng thời, máy phát triển\n");
        for (Map.Entry<String, List<Sample>> entry : results.entrySet()) {
            List<Sample> samples = entry.getValue();
            report.append(String.format(
                    "%-40s n=%3d  p50=%4d ms  p95=%4d ms  max=%4d ms%n",
                    entry.getKey(),
                    samples.size(),
                    percentile(samples, 0.50),
                    percentile(samples, 0.95),
                    percentile(samples, 1.0)));
        }
        Files.writeString(Path.of("target", "nfr04-performance.txt"), report, StandardCharsets.UTF_8);
        System.out.print(report);

        // Mọi yêu cầu phải thành công thật (không tính lỗi nhanh là "đạt").
        assertThat(menuSamples).allMatch(s -> s.status() == 200);
        assertThat(orderSamples).allMatch(s -> s.status() == 201);
        assertThat(queueSamples).allMatch(s -> s.status() == 200);
        assertThat(handoverSamples).allMatch(s -> s.status() == 200);
        for (Map.Entry<String, List<Sample>> entry : results.entrySet()) {
            assertThat(percentile(entry.getValue(), 0.95)).as(entry.getKey()).isLessThanOrEqualTo(LIMIT_MS);
        }
        assertHeldMatchesOrders();
    }
}
