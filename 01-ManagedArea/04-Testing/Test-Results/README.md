# Test Results

Lưu kết quả theo build/release, không ghi đè bằng chứng cũ.

Ghi kết quả white-box và black-box theo test tương ứng:

- Test Case ID và liên kết test;
- Input và Precondition;
- Expected Outcome;
- Actual Outcome;
- Result: Passed / Failed sau khi chạy;
- Note và bằng chứng liên quan.

Kết quả dùng để đối chiếu Acceptance Criteria, xử lý lỗi blocking và Sprint Review. Kết quả theo lượt chạy, mới nhất ở cuối.

## Lượt 2026-10-24: white-box logic giữ quota (T-24)
Kiểm chứng test thiết kế ở [WHITE_BOX_QUOTA.md](../Test-Cases/WHITE_BOX_QUOTA.md). Test là test tự động trong `03-SourceCodes/backend/src/test/java/vn/nhom15/preorder/slot/`, chạy cùng toàn bộ test của backend.

| Mục | Kết quả |
|---|---|
| Lệnh | `./mvnw clean verify` trên nhánh `docs/T-24-white-box-quota`, môi trường: Windows 11, Java 25, PostgreSQL 18.6 nhúng trong test |
| Tổng số test của backend | 82, **Passed 82**, Failed 0, Skipped 0 |
| Test thuộc white-box quota | `QuotaServiceTest` 11, `QuotaDatabaseTest` 12, `SlotRepositoryTest` 15 (kể cả 10 dòng giá trị biên của điều kiện SQL): tất cả **Passed** |
| Đường độc lập của `QuotaService.reserve` | P1, P2, P3, P4, P5: tất cả có test và **Passed** |
| Độ phủ (JaCoCo) | `QuotaService` 24/24 dòng, 8/8 nhánh; `QuotaLine` 4/4 nhánh; `TimeSlot` 2/2 nhánh; `StoreSettingsRepository` 2/2 nhánh |
| Giới hạn | Chỉ logic quota; đồng thời ở bản rút gọn tại tầng dịch vụ; chưa có kiểm chứng NFR-01 đầy đủ qua API (Sprint 2) |

Đây chưa phải kết quả của các test black-box ở [BLACK_BOX_TEST_CASES.md](../Test-Cases/BLACK_BOX_TEST_CASES.md) (chạy ở T-25).

## Lượt 2026-10-25: black-box Sprint 1 (T-25)
Chạy các test ở [BLACK_BOX_TEST_CASES.md](../Test-Cases/BLACK_BOX_TEST_CASES.md) trên nhánh `test/T-25-run-tests` (dựng từ `main` sau PR #20). Bảng từng ca, kèm bằng chứng: [2026-10-25_black-box.md](2026-10-25_black-box.md).

| Mục | Kết quả |
|---|---|
| Môi trường | Windows 11, Java 25, PostgreSQL 18.6 nhúng trong test; Node, Vite dev cho phần chạy tay. Luồng `docker compose` kiểm riêng ở mục "Bổ sung" bên dưới |
| Lệnh | `./mvnw clean verify` (backend), `npm test`, `tsc -b`, `oxlint`, `prettier --check`, `npm run build` (frontend) |
| Backend | 323 test, **Passed 323**, Failed 0. Trong đó 102 test black-box mang ID ca (`AuthMenuBlackBoxTest`, `SlotOrderBlackBoxTest`, `ConcurrencyMyKitchenBlackBoxTest`), gọi API HTTP thật |
| Frontend | 96 test, **Passed 96**, Failed 0 |
| Chạy tay trên trình duyệt thật | Menu, giỏ, đặt đơn, hàng chờ bếp, bắt đầu và hoàn tất, hết phiên giữa chừng (ngày 2026-10-24 và 2026-10-25) |
| **Tổng 131 ca** | **Passed 130**, Chưa chạy 1, Failed 0 |

**Ca chưa chạy:** TC-MY-05 (hiện lý do hủy đơn): thuộc Sprint sau, chưa có chức năng hủy đơn.

**Độ phủ Acceptance Criteria:** mọi AC ở mục 10 của tài liệu test case đều có ít nhất một ca Passed, trừ phần "hoàn quota khi hủy" của PB-05 AC5 (Sprint sau, đã ghi trong tài liệu test case).

**Lỗi phát hiện khi chạy T-25 và đã sửa (cùng PR):**
| Lỗi | Phát hiện bởi | Sửa |
|---|---|---|
| Phiên hết hạn giữa chừng ở màn bếp, "Đơn của tôi" chỉ hiện thông báo lỗi chung, không chuyển tới đăng nhập (vi phạm UC-02 E4, TC-AUTH-13, TC-KIT-24) | Đối chiếu ca test giao diện với màn hình; sau đó kiểm tay trên trình duyệt | `apiFetch` báo "hết phiên" khi nhận 401 `UNAUTHENTICATED`, `AuthProvider` chuyển về chưa đăng nhập, `RequireRole` đưa tới đăng nhập; có test `sessionExpiry.test.tsx` |

**Giới hạn cần biết**
- Bộ test black-box API do chính người viết code Sprint 1 viết sau khi cài đặt, dù bám đặc tả ở tài liệu test case. Nó chưa được ai khác chạy hay review độc lập. Một người khác chạy lại bằng lệnh trên sẽ cho bằng chứng đáng tin hơn.
- Phần giao diện chủ yếu được kiểm bằng test thành phần (jsdom), không phải trình duyệt thật. Chạy tay trên trình duyệt thật chỉ phủ các luồng chính ghi ở cột bằng chứng; chưa kiểm bằng mắt bố cục (CSS) vì cửa sổ trình duyệt bị ẩn nên không chụp được màn hình, chưa thử trên thiết bị thật, chưa thử đóng và mở lại trình duyệt (TC-CART-09).
- Menu mẫu không có món 3 điểm hay 1 điểm như ví dụ trong đặc tả, nên một số ca đồng thời chỉnh điểm của một món thử trong database test (TC-CON-01: 3 điểm, TC-CON-02: 8 điểm, TC-CON-03: 1 điểm nhân số lượng 1–6) và TC-ORD-04 dùng đơn nhỏ 2 điểm thay vì 1 điểm. Kết quả vẫn kiểm đúng điều kiện của ca (TC-CON-01: đúng 6 đơn, giữ 18 điểm).
- Đồng hồ và lỗi database được giả lập ở môi trường test (TC-ORD-29, TC-KIT-23 tiêm lỗi vào tầng lưu đơn), không phải lỗi database thật.
- Chưa kiểm chứng NFR-01 ở mức 50 yêu cầu qua toàn bộ hệ thống triển khai thật, NFR-04 (hiệu năng p95 ≤ 1 giây) và NFR-05 (bảo trì); chưa đo.

### Bổ sung 2026-10-25: chạy qua Docker Compose
Trước đó ghi là "Docker Desktop không khởi động". Nguyên nhân thực ra là Docker Desktop chưa được bật; khi bật lên, luồng chính thức chạy được.

| Bước | Kết quả |
|---|---|
| `docker compose up -d` (PostgreSQL 18 trong container, cổng 5433, đọc `.env`) | Container `healthy` |
| `./mvnw spring-boot:run` (backend đọc `.env`, không dùng PostgreSQL nhúng) | Flyway áp dụng V1–V5 và 3 file dữ liệu mẫu; `/api/health` trả `database: UP`, `schemaVersion: 5` |
| Đăng nhập khach1, đặt 1 đơn (2 × Burger bò, khung 23:30) | 201; bảng `orders` có 1 dòng (`CONFIRMED`, 10 điểm, 90.000đ); `time_slots` giữ 10 điểm |
| `docker restart` container database | Đơn và 10 điểm giữ vẫn còn nguyên: dữ liệu bền, khác với PostgreSQL nhúng |

Để thử được, tôi tạm đổi giờ đóng cửa trong database (lúc chạy là 22:29, quá 21:00) rồi trả về 21:00 và xóa đơn thử. Chưa chạy lại cả 131 ca trên database Docker; bộ test tự động vẫn dùng PostgreSQL nhúng.

## Lượt 2026-10-28: Sprint 2
Chạy trên nhánh `feature/sprint-2` (dựa trên PR #22, #23). Bảng ca: [BLACK_BOX_TEST_CASES_SPRINT_2.md](../Test-Cases/BLACK_BOX_TEST_CASES_SPRINT_2.md).

| Mục | Kết quả |
|---|---|
| Môi trường | Windows 11, Java 25, PostgreSQL 18.6 nhúng trong test; Vite dev và backend dev cho phần chạy tay |
| Backend | `./mvnw clean verify`: 399 test, **Passed 399**, Failed 0; Checkstyle sạch |
| Frontend | `npm test`: 129 test, **Passed 129**; `tsc -b`, `oxlint`, `prettier --check`, `npm run build` sạch |
| Ca black-box Sprint 2 (kèm T-29, T-30) | 61 ca, **Passed 61** |
| NFR-01 hai kênh | TC-CON-08: 25 pre-order + 25 walk-in 3 điểm cùng lúc vào khung quota 20: đúng 6 đơn, giữ 18 điểm, khớp tổng điểm đơn |
| Chạy tay trên trình duyệt thật | Đăng ký; quản lý thêm món; quota riêng một khung; quầy tạo đơn walk-in. Chưa chạy tay: khung gợi ý, các nhánh lỗi |

**Lỗi phát hiện khi chạy và đã sửa:** 3 test cũ của trang đăng nhập (dạng `@WebMvcTest`) thiếu bean đăng ký mới; 1 test cũ chèn đơn walk-in thiếu người tạo (ràng buộc mới của V8 bắt được). Không có lỗi ở code tính năng.

**Giới hạn:** ca test viết cùng lúc với code bởi cùng AI; chưa ai khác chạy lại hay review; chưa chạy qua `docker compose` cho lượt này.

## Lượt 2026-10-28: Sprint 3
Chạy trên nhánh `feature/sprint-3` (dựa trên nhánh Sprint 2). Bảng ca: [BLACK_BOX_TEST_CASES_SPRINT_3.md](../Test-Cases/BLACK_BOX_TEST_CASES_SPRINT_3.md).

| Mục | Kết quả |
|---|---|
| Môi trường | Windows 11, Java 25, PostgreSQL 18 nhúng trong test; backend dev và Vite dev cho phần chạy tay |
| Backend | `./mvnw clean verify`: 427 test, **Passed 427**, Failed 0; Checkstyle sạch |
| Frontend | `npm test`: 144 test, **Passed 144**; `tsc -b`, `oxlint`, `prettier --check`, `npm run build` sạch |
| Ca black-box Sprint 3 | 28 ca (27 ca chức năng + TC-PERF-01), **Passed 28**. TC-MY-05 còn treo từ Sprint 1 nay được kiểm bằng TC-STC-01 và test frontend |
| Chạy tay trên trình duyệt thật | Khách đặt rồi hủy đơn; đơn thứ hai được bếp làm xong qua API, trang khách **tự** hiện "đã sẵn sàng" sau một lượt 15 giây; màn Theo dõi đúng số (đơn hủy đã trả điểm); quản lý tạo rồi khóa tài khoản quầy, tài khoản bị khóa đăng nhập nhận `ACCOUNT_LOCKED`. Chưa chạy tay: hủy và no-show ở màn Quầy |

**NFR-04 (TC-PERF-01)**, lượt verify cuối cùng (số thay đổi chút ít giữa các lần chạy; lần chạy riêng trước đó p95 đặt đơn 270 ms):

| Yêu cầu | Số mẫu | p50 | p95 | Lớn nhất |
|---|---|---|---|---|
| `GET /api/menu` | 500 | 6 ms | 48 ms | 160 ms |
| `POST /api/orders` | 100 | 78 ms | 155 ms | 162 ms |
| `GET /api/kitchen/queue` | 500 | 15 ms | 70 ms | 157 ms |
| `POST /api/counter/orders/{id}/handover` | 100 | 14 ms | 37 ms | 39 ms |

Mọi yêu cầu trả thành công (200/201); p95 mọi loại dưới ngưỡng 1 giây. **Giới hạn của số đo:** client, server và database chạy chung một máy phát triển, database nhúng, không qua mạng; có làm nóng 20 lượt trước khi đo. Đây là bằng chứng ở môi trường dev, chưa phải "môi trường được công bố" mà NFR-04 nói tới; đo lại khi có máy chủ triển khai.

**Lỗi phát hiện khi chạy và đã sửa:**
- Test cũ `OrderingPropertiesTest` thiếu thuộc tính mới `no-show-grace-minutes` (cấu hình thiếu thì Spring gán 0).
- 4 dòng vượt 120 ký tự (Checkstyle).
- **Lỗi thật ở giao diện, có từ Sprint 1**, phát hiện khi chạy tay: đăng xuất khi đang ở trang chỉ khách vào được (ví dụ `/orders/2`) thì người đăng nhập tiếp theo với vai trò khác bị đưa tới "Không có quyền". Đã sửa (chuyển trang trước rồi mới đăng xuất) và kiểm lại trên trình duyệt; chưa có test tự động cho lỗi này.

**Giới hạn:** ca test viết cùng lúc với code bởi cùng AI; chưa ai khác chạy lại hay review; chưa chạy qua `docker compose` cho lượt này.

## Lượt 2026-10-28: chạy toàn hệ thống bằng Docker (chuẩn bị phát hành)
Nhánh `release/v1-prep` (dựa trên nhánh Sprint 3). Mục đích: kiểm tra bản đóng gói chạy được trọn vẹn, không thay cho bộ test tự động ở lượt Sprint 3.

| Mục | Kết quả |
|---|---|
| Môi trường | Windows 11, Docker Engine 29.6.2; image `eclipse-temurin:25`, `node:24-alpine`, `nginx:1.29-alpine`, `postgres:18-alpine` |
| Build và khởi động | `docker compose --profile app up -d --build --wait`: cả ba container chạy, `backend` `healthy`. Database dev có sẵn (schema V5) được Flyway nâng lên V9 khi backend khởi động |
| Đường đi qua nginx (cổng 8000) | `/api/health` trả `UP`, schema 9; mở thẳng `/orders/2` trả trang React (định tuyến phía trình duyệt) |
| Vòng đời đơn qua HTTP | Script thử 14 bước qua cổng 8000: khách đăng nhập, xem menu, đặt đơn; bếp bắt đầu, hoàn tất; khách thấy `READY`; quầy thu tiền mặt, bàn giao đúng mã; đặt đơn thứ hai rồi khách hủy; quản lý xem theo dõi và nhân viên; khách bị chặn ở API quản lý (403); đăng xuất. **14/14 đúng** |
| Giữ dữ liệu | Khởi động lại container `backend`: đơn đã tạo vẫn còn (database ở volume `pgdata`) |
| Trình duyệt | Quản lý đăng nhập qua cổng 8000, mở thẳng `/admin` vẫn giữ phiên, số liệu khớp (1 đã nhận, 1 đã hủy) |
| Kích thước image | backend 531 MB, frontend 94 MB |

**Lỗi phát hiện và đã sửa:** healthcheck của backend ban đầu chạy bằng `sh` (không hỗ trợ `/dev/tcp`) nên container bị báo `unhealthy` dù backend chạy đúng; đổi sang `bash`.

**Giới hạn:** script thử 14 bước chạy tay, chưa đưa vào repository; chưa thử chế độ không có dữ liệu mẫu (`FLYWAY_LOCATIONS`); không có HTTPS; chưa chạy trên máy khác ngoài máy phát triển.

### Bổ sung 2026-10-28: chế độ triển khai không có dữ liệu mẫu, dữ liệu demo
Khi rà soát database theo yêu cầu của người dùng, phát hiện chế độ triển khai không nạp dữ liệu mẫu (`FLYWAY_LOCATIONS=classpath:db/migration`, hướng dẫn ở lượt trên) **không dùng được**: không có tài khoản quản lý nào và không có dòng cấu hình cửa hàng. Đã sửa bằng migration V10 (cấu hình cửa hàng mặc định) và tạo quản lý đầu tiên từ biến môi trường `INITIAL_MANAGER_*` lúc khởi động (BR-32), kèm 5 test (`InitialManagerProvisionerTest`) trên PostgreSQL thật không có dữ liệu mẫu. Backend `./mvnw clean verify`: **432 test, Passed 432**, Checkstyle sạch. Một test cũ (`SlotRepositoryTest`) giả định database không dữ liệu mẫu thì thiếu cấu hình cửa hàng; đã sửa test để tự xóa dòng cấu hình trước khi kiểm nhánh lỗi.

| Kiểm tra | Kết quả |
|---|---|
| Project Docker riêng, database mới, không dữ liệu mẫu (cổng 8001, xóa sau khi thử) | 10 migration; log "Đã tạo tài khoản quản lý đầu tiên"; quản lý đăng nhập được; tài khoản mẫu `khach1` không tồn tại (401); khung hôm nay có từ cấu hình V10; quản lý tạo được tài khoản quầy; khách tự đăng ký được |
| Khởi động lại với `INITIAL_MANAGER_*` khác | Không tạo quản lý thứ hai |
| Database có sẵn của người dùng (cổng 8000) | Nâng lên V10, dữ liệu giữ nguyên, không tạo thêm quản lý |
| Nhóm món khi không có dữ liệu mẫu | Ban đầu không có nhóm nào nên quản lý không thêm được món. Người dùng quyết định (2026-10-28): luôn có nhóm "Món khác"; đã thêm migration V11 và thử lại (dòng dưới) |
| `scripts/demo-data.sh` trên project riêng có dữ liệu mẫu (cổng 8002, xóa sau khi thử; giờ đóng cửa dời sang 23:45 vì lúc thử đã gần 21:00) | 21/21 bước OK: 9 đơn ở đủ trạng thái; lý do hủy tiếng Việt lưu đúng; màn theo dõi đếm 7 pre-order, 2 walk-in, 1 đã nhận, 2 đã hủy |
| Lần chạy đầu của script trên database của người dùng | Script tự dồn 21 điểm vào một khung quota 20; hệ thống **từ chối đúng** đơn thứ ba; đã sửa script (giảm điểm, bỏ qua các bước sau của đơn đặt hỏng). Database local của người dùng giữ 8 đơn demo của lần chạy này |

**Quan sát và quyết định:** đơn walk-in xếp vào khung hiện tại (đã bắt đầu) nên bị gắn "Trễ" ngay khi tạo. Người dùng quyết định (2026-10-28): walk-in chỉ trễ từ giờ kết thúc khung. Đã sửa BR-26 trong SRS, gom quy tắc trễ của hàng chờ bếp và màn theo dõi về một chỗ (`LateRule`), thêm TC-LATE-01, 02 và TC-MNG-09. Backend `./mvnw clean verify`: **437 test, Passed 437**, Checkstyle sạch.

**Thử lại chế độ triển khai thật sau V11** (project Docker riêng, database mới, cổng 8001, xóa sau khi thử): 11 migration; quản lý đầu tiên được tạo; danh sách nhóm chỉ có "Món khác"; quản lý thêm món "Bánh mì trứng" vào nhóm đó, khách vãng lai thấy món trên thực đơn; khách tự đăng ký; quản lý tạo tài khoản bếp; khách đặt 2 phần; bếp bắt đầu, hoàn tất; thu tiền mặt, bàn giao đúng mã; khách thấy "đã nhận" tổng 40.000đ; màn theo dõi đếm 1 đơn đã nhận. **14/14 đúng.** Giờ đóng cửa được dời sang 23:45 bằng chức năng của quản lý vì lúc thử đã 20:57. Database của người dùng (cổng 8000) lên V11, có thêm nhóm "Món khác", 10 đơn cũ giữ nguyên.

**Lưu ý khi thử bằng `curl` trên Git Bash:** gõ tiếng Việt trực tiếp trong lệnh có thể không được gửi dạng UTF-8 (ví dụ tên thành "Qu?y"); gửi bằng file UTF-8 thì đúng. Đây là lỗi của cách thử, không phải của ứng dụng; script demo gửi bằng file.

## Lượt 2026-10-31: review luồng sản phẩm và sửa lỗi
Nhánh `fix/review-findings` (dựa trên `release/v1-prep`). Người dùng yêu cầu review lại thiết kế luồng, backend, database; AI đọc lại toàn bộ code backend, đối chiếu tài liệu, chạy thử những chỗ nghi ngờ bằng một test tạm (đã xóa), rồi sửa theo đề xuất người dùng đồng ý.

**Lỗi tìm thấy** (đều đã tái hiện bằng chạy thật trước khi sửa, trừ ghi chú):
| # | Lỗi | Cách tái hiện | Sửa |
|---|---|---|---|
| ① | Khách đặt 3 đơn, cửa hàng không xử lý, sang hôm sau khách bị chặn đặt đơn mới, không tự hủy được, và màn quầy, bếp, theo dõi đều không thấy 3 đơn đó | Dời đồng hồ sang hôm sau | BR-37 chỉ đếm đơn từ hôm nay; màn quầy có mục "Đơn tồn từ ngày trước"; màn theo dõi nhắc |
| ② | Quầy thu tiền đơn đặt trước khi còn "đã xác nhận", khách vẫn tự hủy được, đơn biến mất khỏi màn quầy nên không ai biết phải hoàn tiền | Thu tiền rồi khách hủy | Khách không tự hủy đơn đã thanh toán (BR-23 sửa đổi) |
| ③ | Khóa tài khoản nhân viên không có tác dụng với màn đang mở (màn quầy, bếp tự làm mới nên phiên không hết hạn) | Đọc code; sau khi sửa đã thử trên ứng dụng thật: khóa bếp đang mở màn → yêu cầu kế tiếp 401 | Kiểm tra tài khoản ở mỗi yêu cầu đã đăng nhập |
| ④ | Quản lý rút giờ đóng cửa đúng lúc khách đặt khung cuối: đơn vẫn được tạo ngoài giờ mới | Lúc review đánh giá "hiếm, chưa tái hiện"; TC-FIX-06 cho thấy xảy ra ngay vòng đầu khi chưa sửa | Khóa cấu hình trước khi kiểm tra khung |
| ⑤ | Bếp bắt đầu đơn khung 20:00 từ 11:00 làm khách mất quyền tự hủy | Bếp bấm sớm rồi khách hủy | Bếp chỉ bắt đầu từ thời gian chuẩn bị (BR-21 sửa đổi) |

Trong lúc sửa còn phát hiện chính bản sửa ③ ban đầu vi phạm quy tắc kiến trúc (module `config` gọi thẳng repository của `auth`); đã đổi sang gọi `AuthService`.

| Mục | Kết quả |
|---|---|
| Backend | `./mvnw clean verify`: **444 test, Passed 444**; Checkstyle sạch. 15 test cũ sửa theo quy tắc mới (mục Test Cases) |
| Frontend | **149 test, Passed 149**; `tsc -b`, `oxlint`, `prettier --check`, `npm run build` sạch |
| Ca mới | TC-FIX-01 → 07 Passed; test frontend đơn đã thanh toán, đơn tồn, cảnh báo đơn tồn |
| Chạy thật (backend với PostgreSQL nhúng, cổng 8080) | `scripts/demo-data.sh` đã sửa: 23/23 bước đúng; khóa tài khoản bếp đang mở màn: 401 ngay |
| NFR-04 (lượt verify cuối) | p95: xem menu 214 ms, đặt đơn 448 ms, hàng chờ bếp 296 ms, bàn giao 94 ms; vẫn dưới 1 giây nhưng cao hơn lượt Sprint 3 (48, 155, 70, 37 ms) |

**Giới hạn:** số đo NFR-04 lần này chạy khi một backend thử khác cũng đang chạy trên máy; ngoài ra mỗi yêu cầu đã đăng nhập thêm một truy vấn kiểm tra tài khoản (sửa ③). Chưa tách được hai nguyên nhân; nên đo lại trên máy rảnh. Docker Desktop tắt trong lượt này nên chưa chạy lại bản Docker.

