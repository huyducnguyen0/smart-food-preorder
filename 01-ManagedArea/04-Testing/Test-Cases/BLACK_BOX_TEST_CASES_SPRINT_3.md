# Black-box Test Cases — Sprint 3
Trạng thái: Thiết kế và chạy ở Sprint 3 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. **28 ca, tất cả Passed** (cộng 3 ca bước chuẩn bị phát hành và 7 ca sửa sau review 2026-10-31, cùng Passed) ở lượt chạy ngày 2026-10-28 ([Test Results](../Test-Results/README.md)).

Như Sprint 2: ca test viết cùng lúc với code, mỗi ca **là** một test tự động gọi API HTTP thật trên PostgreSQL thật, tên test bắt đầu bằng ID ca; bảng dưới là tóm tắt. Dữ liệu và tài khoản như mục 2 của [Sprint 1](BLACK_BOX_TEST_CASES.md); giờ hiện tại mặc định 11:00, cửa sổ chuẩn bị 15 phút, thời gian chờ no-show 30 phút. Phần giao diện (nút hủy, tự cập nhật, màn quầy, nhân viên, theo dõi) kiểm bằng test frontend `features/sprint3.test.tsx` (15 test), không lặp lại ở đây.

## UC-06 Khách hủy đơn (PB-10)
Đặc tả: [UC-06](../../02-Requirements/Use-Cases/specifications/UC-06-khach-huy-don.md). Test: `Sprint3CancelNoShowBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-CAN-01 | Hủy đơn `CONFIRMED` trước cửa sổ chuẩn bị: `CANCELLED`, hoàn đủ điểm, ghi người và lúc hủy | Passed |
| TC-CAN-02 | Biên: khung 12:00 hủy lúc 11:44 được; đúng 11:45 bị từ chối kèm `cancelDeadline`, điểm giữ nguyên | Passed |
| TC-CAN-03 | Đơn `PREPARING` hoặc đã hủy: không hủy được, quota không đổi | Passed |
| TC-CAN-04 | Hủy đơn của khách khác: 404, đơn giữ nguyên (NFR-03) | Passed |
| TC-CAN-05 | Sau khi hủy, khung vừa đầy đặt lại được | Passed |
| TC-CAN-06 | 5 yêu cầu hủy cùng lúc: đúng 1 thành công, điểm hoàn một lần | Passed |
| TC-CAN-07 | Đơn đã hủy không còn ở hàng chờ bếp, bếp không bắt đầu làm được | Passed |

## UC-11 Cửa hàng hủy đơn (PB-21)
Đặc tả: [UC-11](../../02-Requirements/Use-Cases/specifications/UC-11-cua-hang-huy-don.md). Test: `Sprint3CancelNoShowBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-STC-01 | Quản lý hủy đơn `CONFIRMED` có lý do: hoàn điểm; khách thấy lý do ở chi tiết đơn (thay ca TC-MY-05 còn treo từ Sprint 1) | Passed |
| TC-STC-02 | Quản lý hủy đơn `PREPARING`: không hoàn điểm | Passed |
| TC-STC-03 | Thiếu lý do hoặc lý do toàn khoảng trắng: 400 | Passed |
| TC-STC-04 | Quầy hủy được walk-in `CONFIRMED` (hoàn điểm), không hủy được pre-order (`CANCEL_NOT_ALLOWED`) | Passed |
| TC-STC-05 | Quầy không hủy được walk-in `PREPARING`; không ai hủy được đơn `READY` | Passed |
| TC-STC-06 | Bếp và khách gọi API hủy của cửa hàng: 403 | Passed |

## UC-10 Đánh dấu no-show (PB-18)
Đặc tả: [UC-10](../../02-Requirements/Use-Cases/specifications/UC-10-danh-dau-no-show.md). Test: `Sprint3CancelNoShowBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-NOS-01 | Đơn `READY` khung 12:00–12:15, lúc 12:45: quầy đánh dấu `NO_SHOW`, không hoàn điểm, khách thấy trạng thái | Passed |
| TC-NOS-02 | Biên: lúc 12:44 bị từ chối kèm `allowedFrom`, cờ `overdue` = false; lúc 12:45 cờ = true | Passed |
| TC-NOS-03 | Đơn chưa `READY` không đánh dấu được; quản lý đánh dấu được | Passed |

## UC-15 Quản lý tài khoản nhân viên (PB-20)
Đặc tả: [UC-15](../../02-Requirements/Use-Cases/specifications/UC-15-quan-ly-tai-khoan-nhan-vien.md). Test: `Sprint3StaffDashboardBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-STF-01 | Danh sách chỉ có quầy và bếp, có trạng thái khóa, không có mật khẩu | Passed |
| TC-STF-02 | Tạo tài khoản quầy (định danh được chuẩn hóa): đăng nhập được ngay, vào được màn quầy, không vào được màn quản lý | Passed |
| TC-STF-03 | Tạo tài khoản bếp bằng số điện thoại được; vai trò khách hoặc quản lý bị từ chối | Passed |
| TC-STF-04 | Định danh đã có (`LOGIN_ID_TAKEN`), sai dạng, mật khẩu 7 ký tự: bị từ chối | Passed |
| TC-STF-05 | Khóa: đăng nhập bị từ chối `ACCOUNT_LOCKED`; mở khóa: đăng nhập được | Passed |
| TC-STF-06 | Không khóa được tài khoản khách hoặc quản lý qua API nhân viên | Passed |
| TC-STF-07 | Quầy, bếp, khách gọi API tài khoản nhân viên: 403 | Passed |

## UC-16 Theo dõi vận hành (PB-17)
Đặc tả: [UC-16](../../02-Requirements/Use-Cases/specifications/UC-16-theo-doi-van-hanh.md). Test: `Sprint3StaffDashboardBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-DSH-01 | Điểm đã giữ, số đơn mỗi khung; thống kê theo nguồn và trạng thái (đơn hủy hoàn điểm, đơn đã nhận vẫn giữ) | Passed |
| TC-DSH-02 | Đơn `CONFIRMED` qua giờ bắt đầu khung bị gắn trễ; `READY` và khung chưa tới thì không (BR-26) | Passed |
| TC-DSH-03 | Đếm no-show; tỉ lệ dùng quota = điểm giữ / tổng quota các khung (5/880 → 1 %) | Passed |
| TC-DSH-04 | Chỉ quản lý xem được | Passed |

## NFR-04 Hiệu năng
Test: `Nfr04PerformanceTest` (một test, Tomcat thật trên cổng ngẫu nhiên, client `HttpClient`).

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-PERF-01 | 50 người dùng đồng thời; mỗi người xem menu 10 lần, đặt 2 đơn; 50 phiên bếp xem hàng chờ 10 lần; 50 phiên quầy bàn giao 2 đơn. Mọi yêu cầu thành công và p95 từng loại ≤ 1 giây | Passed trên máy phát triển (số đo ở Test Results) |

## PB-14 Trang đơn tự cập nhật
Không có API mới (dùng `GET /api/orders/{id}` đã kiểm ở Sprint 1). Kiểm bằng test frontend: đơn chuyển `READY` sau một lượt 15 giây thì hiện thông báo nổi bật; đơn đã kết thúc thì không hỏi lại backend.

## Bổ sung ở bước chuẩn bị phát hành (2026-10-28)
Theo hai quyết định của người dùng cùng ngày. Test: `ReleasePrepBlackBoxTest`. TC-MNG-01 (Sprint 2) sửa theo: danh sách nhóm có thêm "Món khác" ở cuối.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-LATE-01 | Walk-in khung 11:00 tạo lúc 11:00: chưa trễ lúc 11:05 và 11:14; từ 11:15 (hết khung) mới trễ, ở cả màn theo dõi và hàng chờ bếp (BR-26 sửa đổi) | Passed |
| TC-LATE-02 | Pre-order giữ quy tắc cũ: khung 11:30 chưa trễ lúc 11:29, trễ từ 11:30, ở cả hai màn | Passed |
| TC-MNG-09 | Nhóm "Món khác" luôn có; thực đơn khách không hiện nhóm trống; quản lý thêm món vào nhóm này thì khách thấy | Passed |

## Sửa sau buổi review (2026-10-31)
Các lỗi tìm thấy khi review lại luồng sản phẩm và cách sửa người dùng đã duyệt. Test: `ReviewFixesBlackBoxTest`. Test frontend: `features/reviewFixes.test.tsx`.

| ID | Lỗi được kiểm | Kiểm chứng | Kết quả |
|---|---|---|---|
| TC-FIX-01 | Đơn tồn chặn khách (BR-37 sửa đổi) | 3 đơn chưa kết thúc của hôm qua không chặn khách đặt đơn hôm nay | Passed |
| TC-FIX-02 | Đơn tồn không ai thấy | Màn quầy trả đơn tồn kèm cờ `stale`; màn theo dõi đếm `staleOpen`; quầy đánh dấu không đến nhận đơn sẵn sàng, quản lý hủy đơn chưa làm; dọn xong thì hết | Passed |
| TC-FIX-03 | — | Đơn hôm nay không bị gắn cờ tồn | Passed |
| TC-FIX-04 | Khách tự hủy đơn đã trả tiền (BR-23 sửa đổi) | Khách bị từ chối (`reason` = `PAID`), đơn và điểm giữ nguyên; cửa hàng hủy kèm lý do được, hoàn điểm | Passed |
| TC-FIX-05 | Khóa tài khoản không có tác dụng với phiên đang mở (BR-32) | Bếp đang mở màn hình, quản lý khóa: yêu cầu kế tiếp 401, phiên bị hủy; người khác không ảnh hưởng; mở khóa thì đăng nhập lại được | Passed |
| TC-FIX-06 | Đổi giờ cùng lúc đặt đơn lọt đơn ngoài giờ (BR-11) | 15 vòng: khách đặt khung 20:45 đồng thời quản lý rút giờ đóng cửa về 20:45; không vòng nào cả hai cùng thành công. **Gỡ chỗ sửa thì test thất bại ngay vòng 1** | Passed |
| TC-FIX-07 | Bếp làm trước làm khách mất quyền hủy mà không biết (BR-21, BR-23) | Chi tiết đơn có `cancelDeadline` (khung 12:00 → 11:45); bếp làm trước đơn thứ nhất thì khách không hủy được đơn đó, vẫn hủy được đơn chưa làm | Passed |

Ca cũ sửa theo quy tắc mới: **TC-KIT-25** vẫn là "bếp được làm trước đơn sắp tới", thêm kiểm tra khách hết quyền tự hủy sau đó (giữa chừng có lúc đổi thành "bị từ chối", đã khôi phục cùng ngày); **TC-PAY-11** danh sách quầy có thêm đơn tồn đứng đầu; TC-KIT-13 → 23, TC-HAND-11, TC-ORD-17 đưa đồng hồ thử tới thời gian chuẩn bị trước khi bếp bấm "Bắt đầu".

