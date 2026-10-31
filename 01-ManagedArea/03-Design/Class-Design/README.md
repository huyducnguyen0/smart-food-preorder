# Detailed Class Design
Trạng thái: Lập ở task T-10 (Sprint 1) ngày 2026-10-17 cho backend phần Sprint 1; người dùng chưa review. Tên class, method là thiết kế để code theo; khi code thấy cần đổi (thêm tham số, tách method) thì sửa tài liệu này trong cùng PR.

Bước 13.2 của [quy trình IT3180](../../../00-References/IT3180_PROJECT_PROCESS.md): xác định class, thuộc tính, method, quan hệ và trách nhiệm ở mức lập trình viên bắt đầu code được.

## Input
- [Analysis Model](../Analysis-Model/README.md): trách nhiệm từng Control, sequence đặt đơn.
- [Architecture](../Architecture/ARCHITECTURE.md): module, tầng, quy tắc phụ thuộc, REST API, mã lỗi.
- [Database Design](../Database/DATABASE_DESIGN.md): bảng, ràng buộc, bất biến.

## 1. Sơ đồ lớp
Ký hiệu: `+` public, `-` private, `~` dùng trong package (để test), gạch chân là `static`. `<<record>>` là Java record (dữ liệu bất biến), `<<Service>>`… là annotation Spring tương ứng. Không vẽ DTO `...Request`/`...Response` của controller: chúng là record khớp đúng JSON ở Architecture mục 5.2.

### 1.1. Đặt đơn và giữ quota (`order`, `slot`, `menu`)
![Class Diagram đặt đơn và quota](class-diagram_dat-don-quota.svg)

### 1.2. Xác thực, bếp, xử lý lỗi (`auth`, `kitchen`, `common`, `config`)
![Class Diagram auth, bếp, lỗi](class-diagram_auth-bep-loi.svg)

## 2. Trách nhiệm từng class
| Class | Trách nhiệm | Từ phân tích | Quy tắc | Task |
|---|---|---|---|---|
| `AuthController` | `POST /api/auth/login`, `logout`, `GET /api/auth/me`; lưu `SecurityContext` vào session sau khi `AuthService` xác thực | `LoginScreen` → `AuthControl` | UC-02 | T-12 |
| `AuthService` | Chuẩn hóa định danh, tìm tài khoản, so mật khẩu, **rồi mới** kiểm tra khóa (mục 4.1) | `AuthControl` | BR-32, NFR-06 | T-12 |
| `SecurityConfig` | Quy tắc quyền theo đường dẫn, CSRF, trả 401/403 JSON, `PasswordEncoder` BCrypt | `AuthControl` | BR-33, NFR-03 | T-12 |
| `MenuService` | Menu theo nhóm (bỏ món ngừng bán); một món đang hiển thị (`getVisibleItem`); tải nhiều món theo id | `MenuControl` | BR-03, BR-04 | T-15 |
| `SlotAvailabilityService` | Liệt kê khung hôm nay kèm tình trạng theo giỏ (mục 4.2); kiểm tra một khung còn đặt được | `SlotAvailabilityControl` | BR-06, BR-15, BR-36 | T-18 |
| `QuotaService` | Tính điểm đơn; giữ quota cho khung bằng câu cập nhật có điều kiện (mục 3) | `QuotaControl` | BR-07–BR-13, NFR-01 | T-16 |
| `PlaceOrderService` | Giao dịch đặt đơn theo đúng thứ tự kiểm tra (mục 3) | `PlaceOrderControl` | BR-10, BR-17, BR-35, BR-37, BR-38 | T-18 |
| `PickupCodeGenerator` | Sinh mã 6 ký tự từ 31 ký tự cho phép, dùng `SecureRandom` | `PlaceOrderControl` | BR-38 | T-18 |
| `MyOrderService` | Danh sách, chi tiết đơn **của khách đang đăng nhập** | `MyOrdersControl` | BR-33, UC-05 E1 | T-20 |
| `OrderStatusService` | Chuyển trạng thái theo tiến độ: bếp bắt đầu (kể cả làm trước), hoàn tất; quầy bàn giao. Chỉ chuyển khi trạng thái hiện tại đúng | `OrderProgressControl` | SRS B.5, BR-21, BR-29 | T-22, T-30, 2026-10-31 |
| `OrderCancellationService` | Chuyển ngoại lệ: khách hủy, cửa hàng hủy, không đến nhận. Khóa dòng đơn, kiểm tra quy tắc, đổi trạng thái và hoàn quota trong một giao dịch | — | BR-23, BR-24, BR-25 | Sprint 3 |
| `OrderPaymentService` | Ghi nhận thanh toán `UNPAID → PAID` | — | BR-27 | T-29 |
| `WalkInService` | Ước tính khung sớm nhất, xác nhận đơn walk-in (giữ quota như pre-order) | — | BR-18–BR-20 | Sprint 2 |
| `LateRule` | Quy tắc đơn trễ dùng chung cho bếp và màn theo dõi | — | BR-26 | 2026-10-28 |
| `StaffAccountService` | Tạo, khóa, mở khóa tài khoản quầy, bếp | — | BR-32 | Sprint 3 |
| `InitialManagerProvisioner` | Tạo quản lý đầu tiên từ biến môi trường khi chưa có | — | BR-32 | 2026-10-28 |
| `LockedAccountFilter` | Mỗi yêu cầu đã đăng nhập hỏi `AuthService.isBlocked`; bị khóa thì hủy phiên, trả 401 | `AuthControl` | BR-32 | 2026-10-31 |
| `DashboardService` | Số liệu theo dõi trong ngày, đơn tồn (module `report`, chỉ đọc) | — | FR-20, BR-26 | Sprint 3 |
| `KitchenService` | Hàng chờ ba nhóm, sắp xếp, đánh dấu trễ (mục 4.3); gọi `OrderStatusService` | `KitchenQueueControl`, `OrderProgressControl` | BR-21, BR-22, BR-26 | T-21, T-22 |
| `...Repository` | SQL qua `JdbcTemplate` cho bảng của module mình (Architecture mục 2) | Entity | — | theo module |
| `ApiException`, `ErrorCode` | Lỗi nghiệp vụ có mã; `ErrorCode` biết HTTP status | — | Architecture mục 5.3 | T-12 |
| `GlobalExceptionHandler` | Chuyển mọi exception thành Problem Details; lỗi không lường trước → 500 `INTERNAL_ERROR`, ghi log | — | UC-04 E10 | T-12 |
| `OrderingProperties` | Đọc `app.ordering.*`: độ dài khung, đặt trước tối thiểu, cửa sổ chuẩn bị, số đơn tối đa | — | BR-06, BR-15, BR-21, BR-37 | T-16 |

`OrderStatus.isOpen()` trả `true` với `CONFIRMED`, `PREPARING`, `READY`: "đơn chưa kết thúc" của BR-37 và UC-05 bước 2.

## 3. Giao dịch đặt đơn và giữ quota
`PlaceOrderService.placeOrder` có `@Transactional`: mọi bước dưới đây chung một giao dịch PostgreSQL (mức cô lập mặc định READ COMMITTED). `ApiException` là `RuntimeException`, nên mọi lỗi đều làm Spring hủy giao dịch: quota đã giữ và đơn đã chèn cùng biến mất (BR-10, E10).

| # | Bước | SQL / thao tác | Lỗi | Đặc tả |
|---|---|---|---|---|
| 1 | Khóa tài khoản khách | `SELECT id FROM user_accounts WHERE id = ? FOR UPDATE` | — | Xử lý lần lượt theo khách |
| 2 | Đã có đơn cho mã yêu cầu? | `SELECT id FROM orders WHERE customer_id = ? AND request_id = ?` → có thì trả đơn đó, `created = false` | — | A1, BR-17 |
| 3 | Đếm đơn chưa kết thúc | `SELECT count(*) FROM orders WHERE customer_id = ? AND status IN ('CONFIRMED','PREPARING','READY')` | ≥ N → `OPEN_ORDER_LIMIT_REACHED` | E5, BR-37 |
| 4 | Khung còn đặt được | `SlotAvailabilityService.checkBookable(slotStart)` (mục 4.2) | `SLOT_NOT_BOOKABLE` | E6, BR-15 |
| 5 | Món và giá | `MenuService.findItems(ids)`; mỗi dòng: món tồn tại, `isOrderable()`, `price == unitPrice` | `ITEMS_CHANGED` kèm danh sách | E7, BR-04, BR-35 |
| 6 | Giữ quota | `QuotaService.reserve(...)`, xem bên dưới | `ORDER_TOO_LARGE`, `SLOT_FULL` | E8, E9, UC-17 |
| 7 | Sinh mã nhận | `generate()` lặp tới khi `pickupCodeExists` trả `false` | — | BR-38 |
| 8 | Chèn đơn và dòng đơn | `INSERT INTO orders … RETURNING id`; `INSERT INTO order_lines …` (batch) | — | Bước 10, BR-28, BR-35, BR-08 |
| 9 | Trả kết quả | Đọc lại `OrderDetail`, `created = true` → controller trả 201 | — | Bước 11 |

**`QuotaService.reserve(date, start, lines, walkIn)`** (UC-17, từ Sprint 4 — 2026-10-31, PB-22):
1. `points = orderPoints(lines)` = Σ điểm × số lượng (BR-08).
2. `storeSettingsRepository.lockShared()` (`SELECT … FROM store_settings FOR SHARE`): xếp hàng với quản lý đổi cấu hình (BR-11).
3. `storeSettingsRepository.lockKitchenPlan(date)`: `SELECT pg_advisory_xact_lock(1501, <số ngày kể từ 1970-01-01>)`. Mọi lần nhận đơn của cùng ngày chạy **lần lượt** từ đây tới hết giao dịch; khóa tự nhả khi giao dịch kết thúc.
4. `slotRepository.ensureExists(date, start)`: bảo đảm khung có dòng.
5. `kitchenPlanService.snapshot()`: đọc cấu hình, công suất riêng, các đơn `CONFIRMED`/`PREPARING` và số đơn mỗi khung nhận **sau khi đã có khóa**, nên thấy mọi đơn vừa được nhận trước đó.
6. `pickupHasRoom(start)` sai → `SLOT_FULL`, `reason` = `PICKUP_LIMIT` (BR-41).
7. Dựng việc của đơn: đặt trước làm trong [S − H, S] (`preOrderJob`); walk-in làm từ khoảng hiện tại tới S + 15 (`walkInJob`) (BR-39).
8. `tooLargeEvenWhenEmpty(job)` → `ORDER_TOO_LARGE` (BR-12).
9. `fits(job)` sai → `SLOT_FULL`, `reason` = `KITCHEN`. `fits` chạy `KitchenPlanner.plan` hai lần: mọi đơn với 100% công suất; nếu là đặt trước thì thêm lần riêng các đơn đặt trước với X% (BR-40).
10. `points > 0` thì `addHeld(slotId, points)` (thống kê), trả `QuotaReservation(slotId, points)`.

**`KitchenPlanner.plan(intervals, intervalMinutes, jobs, sharePercent)`.** Sắp việc: đơn trễ (hạn đã qua) trước, rồi theo hạn, rồi theo lúc được bắt đầu. Với từng việc, đổ điểm vào các khoảng còn trống từ khoảng sớm nhất được phép tới trước hạn. Còn thừa điểm thì lịch không kịp (`feasible = false`), trừ đơn trễ: đơn trễ được xếp vào chỗ trống sớm nhất còn lại nhưng không làm lịch thất bại (bếp vẫn phải làm cho xong). Khoảng hiện tại chỉ còn phần công suất tương ứng thời gian còn lại. Cách xếp này đơn giản, không tối ưu tuyệt đối; nó không bao giờ xếp vượt công suất.

**Vì sao khóa lịch bếp theo ngày (NFR-01).** Lịch bếp là điều kiện trên **nhiều dòng** (một đơn mới có thể đẩy đơn khác sang khoảng khác), nên câu `UPDATE` có điều kiện trên một dòng khung như trước Sprint 4 không đủ. Khóa theo ngày làm các lần nhận đơn trong ngày xếp hàng; mỗi lần chỉ mất vài mili giây vì lịch một ngày có vài chục khoảng và vài trăm đơn. Cái giá: hai khách đặt cùng lúc phải chờ nhau một chút; chấp nhận được với một cửa hàng (đo ở NFR-04). Hủy đơn, bếp bắt đầu, bàn giao không lấy khóa này: chúng chỉ làm lịch nhẹ đi.

**Quản lý đổi cấu hình** (`AdminSlotService`): `lockForUpdate()` dòng `store_settings` và `lockKitchenPlan(today)`, dựng `snapshot(cấu hình mới, công suất riêng mới)`; `acceptedOrdersStillFit()` sai thì `QUOTA_BELOW_HELD` (BR-11).

**Trước Sprint 4** (Sprint 1–3), `reserve` giữ quota của từng khung nhận bằng một câu `UPDATE time_slots SET held_points = held_points + :points WHERE … AND held_points + :points <= COALESCE(quota_override, :defaultQuota)`; PostgreSQL kiểm lại `WHERE` trên giá trị mới nhất sau khi chờ khóa dòng nên không bị chen ngang. Cách này được thay vì không cho bếp làm trước (xem [tư liệu phân tích](../../../02-FreeZone/2026-10-31_lich-bep/README.md)).

**Vì sao khóa tài khoản ở bước 1.** Hai lần gửi đồng thời của **cùng một khách** đều có thể đi qua bước 2 và 3 trước khi lần nào kịp chèn đơn: cả hai thấy "chưa có đơn" (vi phạm BR-17) hoặc "mới 2 đơn" (vi phạm BR-37). Khóa dòng tài khoản buộc lần thứ hai chờ lần thứ nhất xong rồi mới đọc, nên lần thứ hai thấy đơn vừa tạo. Ràng buộc `uq_orders_customer_id_request_id` vẫn giữ lại như lớp phòng vệ cuối. Khóa chỉ chặn các lần đặt của cùng khách, khách khác không bị ảnh hưởng.

**Không có deadlock.** Mọi giao dịch đặt đơn khóa theo cùng thứ tự: dòng tài khoản → chia sẻ `store_settings` → lịch bếp của ngày → dòng khung. Walk-in bỏ bước đầu, quản lý đổi cấu hình lấy `store_settings` (ghi) rồi lịch bếp, cùng thứ tự. Bếp và hủy đơn chỉ khóa dòng đơn rồi dòng khung.

**Mã nhận trùng khi đồng thời.** Hai giao dịch có thể cùng sinh một mã chưa tồn tại, giao dịch sau vi phạm `UNIQUE` và bị hủy (lỗi 500). Xác suất cực nhỏ (31⁶ mã); khách gửi lại an toàn nhờ A1 vì giao dịch lỗi không để lại đơn nào. Chấp nhận, không thêm cơ chế thử lại.

**Kiểm chứng bắt buộc** (T-16, T-18): test chạy với PostgreSQL thật, không dùng mock cho repository:
- nhiều luồng cùng giữ chỗ → bếp vẫn xếp được lịch không vượt công suất, quầy không quá giới hạn (NFR-01; TC-CON-08, TC-PLAN-08 từ Sprint 4);
- cùng mã yêu cầu gửi đồng thời → đúng một đơn (NFR-02);
- `held_points` bằng tổng điểm các đơn của khung sau mỗi test (Database Design mục 3).

## 4. Thuật toán khác
### 4.1. Đăng nhập (`AuthService.authenticate`)
1. `normalizeLoginId`: bỏ khoảng trắng đầu cuối; có `@` thì là email, chuyển chữ thường; ngược lại là số điện thoại, bỏ mọi khoảng trắng.
2. Không có tài khoản → vẫn gọi `passwordEncoder.matches` với một chuỗi băm giả rồi báo `INVALID_CREDENTIALS`. So mật khẩu cả khi không có tài khoản để thời gian phản hồi không lộ định danh nào tồn tại.
3. Mật khẩu sai → `INVALID_CREDENTIALS` (UC-02 E1).
4. Mật khẩu đúng nhưng `locked` → `ACCOUNT_LOCKED` (E2).
5. Trả `CurrentUser`; `AuthController` tạo session mới (đổi id phiên khi đăng nhập), lưu `SecurityContext` chứa `CurrentUser` và vai trò.

Tự viết bước 2–4 thay vì để `DaoAuthenticationProvider` của Spring Security làm hết. Lý do: provider có sẵn kiểm tra khóa **trước** khi so mật khẩu, nên người không biết mật khẩu vẫn biết được tài khoản bị khóa, ngược thứ tự của UC-02.

### 4.2. Khung giờ (`SlotAvailabilityService`)
Đặt `now = LocalDateTime.now(clock)`, `today = now.toLocalDate()`, độ dài khung `L = slotMinutes`, đặt trước tối thiểu `lead = minLeadMinutes`.

`candidateStartTimes`: các `t` từ `openTime`, bước `L`, với `t + L ≤ closeTime` và `today.atTime(t) ≥ now + lead`.

`listSlots(lines)`:
1. Danh sách ứng viên rỗng → `unavailableReason = NO_SLOT_LEFT_TODAY` (E2).
2. `points` = Σ điểm × số lượng của các món trong giỏ; món không tồn tại hoặc số lượng < 1 → `VALIDATION_ERROR`. Món tạm hết vẫn tính điểm: lỗi món chỉ báo khi gửi (E7).
3. Tải `time_slots` của `today`; khung không có dòng thì `held = 0`, quota = mặc định.
4. Mỗi khung: `bookable = points ≤ quota − held` (đúng cả khi `points = 0`).
5. Không khung nào `bookable`: nếu `points > quota` ở **mọi** khung → `ORDER_TOO_LARGE` (E4), ngược lại `ALL_SLOTS_FULL` (E3).

`checkBookable(slotStart)`: `slotStart.toLocalDate() == today`, phút chia hết cho `L`, giây bằng 0, và giờ thuộc `candidateStartTimes`. Sai bất kỳ điều kiện nào → `SLOT_NOT_BOOKABLE`.

### 4.3. Hàng chờ bếp (`KitchenService`)
`findActiveOrders(today)`: đơn `CONFIRMED`/`PREPARING` có khung thuộc hôm nay, kèm dòng đơn, sắp theo `start_time`, `confirmed_at` (BR-22).

Với `N = prepWindowMinutes`, `slotStart` là ngày + giờ bắt đầu khung:
| Điều kiện | Nhóm |
|---|---|
| `status = PREPARING` | `PREPARING` (Đang làm) |
| `CONFIRMED` và `now ≥ slotStart − N` | `DUE_SOON` (Cần chuẩn bị, BR-21) |
| `CONFIRMED` còn lại | `UPCOMING` (Sắp tới) |

`late` theo `LateRule.isLate` (module `order`, dùng chung với màn theo dõi): `now ≥ slotStart` với pre-order, `now ≥ slotEnd` với walk-in (BR-26, sửa đổi 2026-10-28). Thứ tự trong nhóm giữ nguyên thứ tự truy vấn. `groupOf` để mức package để unit test với `Clock` cố định.

### 4.4. Chuyển trạng thái (`OrderStatusService`, `OrderCancellationService`)

Từ 2026-10-31, bếp bắt đầu đơn theo cách khóa dòng đơn (`SELECT … FOR UPDATE`), kiểm tra `CONFIRMED`, rồi mới chạy câu cập nhật dưới đây. Hủy và không đến nhận cũng khóa dòng đơn trước, nên bếp bấm "Bắt đầu" cùng lúc khách bấm hủy thì chỉ một bên thắng và quota đúng.
```sql
UPDATE orders SET status = 'PREPARING', preparing_at = :now, preparing_by = :staffId
WHERE id = :id AND status = 'CONFIRMED'
```
Một dòng đổi → thành công. Không dòng nào → đọc trạng thái hiện tại: không có đơn → `ORDER_NOT_FOUND`; có → `INVALID_STATUS_TRANSITION` kèm `currentStatus` (UC-12 E1). Hai bếp cùng bấm: câu lệnh thứ hai chờ câu thứ nhất, rồi kiểm tra lại `status = 'CONFIRMED'` trên giá trị mới, không đổi dòng nào. `markReady` tương tự với `PREPARING → READY`, `ready_at`, `ready_by`.

## 5. Kiểm thử theo class
| Class | Loại test | Nội dung chính |
|---|---|---|
| `AuthService` | Unit (Mockito) | Thứ tự E1/E2, chuẩn hóa định danh |
| `SecurityConfig` + controller | `@WebMvcTest` / tích hợp | 401, 403 cho từng nhóm đường dẫn (T-14, NFR-03) |
| `SlotAvailabilityService` | Unit, `Clock` cố định | Biên 30 phút, giờ đóng cửa, E2/E3/E4, điểm 0 |
| `QuotaService`, `PlaceOrderService` | Tích hợp với PostgreSQL | Mục 3: đồng thời, A1, E5–E10, hủy giao dịch |
| `KitchenService` | Unit, `Clock` cố định | Biên cửa sổ chuẩn bị 15 phút, trễ |
| `OrderStatusService` | Tích hợp | Chuyển hợp lệ, chuyển sai, hai bếp cùng bấm, làm trước rồi khách hủy |
| `OrderCancellationService` | Black-box qua API | Biên thời gian, quyền, hoàn quota, hủy đồng thời, đơn đã thanh toán |
| `PickupCodeGenerator` | Unit | Độ dài, chỉ ký tự cho phép |

Test tích hợp cần PostgreSQL. Cách chạy (Testcontainers hay database của Docker Compose) chốt ở T-16 cùng cấu hình test.

## Công cụ
PlantUML, kiểu chung ở `../_style.iuml`; cách render như [Analysis Model](../Analysis-Model/README.md) mục Công cụ, đổi thư mục làm việc thành `Class-Design`.

## Output chuyển giao
- T-12 → T-22: tạo class đúng tên, package và trách nhiệm ở mục 1–2. Thứ tự kiểm tra ở mục 3 là thứ tự code trong `PlaceOrderService`.
- T-16, T-18: kiểm chứng bắt buộc ở mục 3.
- T-24: các nhánh của mục 3 và 4.2 là đường đi cho Control Flow Graph.
