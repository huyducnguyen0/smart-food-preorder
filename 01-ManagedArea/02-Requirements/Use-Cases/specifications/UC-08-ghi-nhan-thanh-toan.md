# UC-08 — Ghi nhận thanh toán
Trạng thái: Đặc tả ngắn lập ở T-29 ngày 2026-10-26 cho phần làm thêm của Sprint 1; người dùng chưa review. Các quyết định chi tiết ở mục "Ghi chú" là lựa chọn hiện thực của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Nhân viên quầy (quản lý làm được thao tác của quầy, SRS B.2) |
| Mục đích | Ghi nhận đơn đã được trả tiền và phương thức, để chỉ giao món đã trả |
| Story và AC | PB-16 AC1 → bước 3–5; AC2 → Postcondition (đơn mới `UNPAID`); AC3 → Ghi chú (không cổng thanh toán, không hoàn tiền) |
| FR, quy tắc nghiệp vụ | FR-16; BR-27, BR-28, BR-30, BR-33 |
| NFR | NFR-03 (chỉ quầy và quản lý) |
| Quan hệ | Không include/extend. Là điều kiện của UC-09 Bàn giao (BR-29) |
| Phạm vi Sprint | Làm thêm Sprint 1. Pre-order trả khi nhận; walk-in trả khi đặt thuộc UC-07 (Sprint sau) |

## Precondition
Nhân viên quầy hoặc quản lý đã đăng nhập (UC-02). Có đơn hôm nay đang xử lý (`CONFIRMED`, `PREPARING`, `READY`) ở trạng thái `UNPAID`.

## Main Flow
1. Quầy: mở màn "Quầy".
2. Hệ thống: hiển thị các đơn **hôm nay** còn đang xử lý, sắp theo giờ khung rồi giờ xác nhận. Mỗi đơn có mã nhận, khung nhận, tên khách (đơn walk-in không có), nguồn, món và số lượng, trạng thái, **số tiền cần thu**, trạng thái thanh toán. Quầy có thể lọc theo mã nhận.
3. Quầy: nhận tiền từ khách, chọn "Tiền mặt" hoặc "Chuyển khoản" ở đơn `UNPAID`.
4. Hệ thống: kiểm tra đơn **đang** `UNPAID` và còn đang xử lý, chuyển sang `PAID` kèm phương thức, ghi thời điểm và người ghi nhận (BR-27).
5. Hệ thống: làm mới danh sách; đơn hiện "Đã thanh toán bằng …". Khách thấy trạng thái thanh toán mới ở "Đơn của tôi" (UC-05).

## Alternative Flow
**Luồng lỗi**
- **E1 — Đơn đã ghi nhận** (bước 4): hai nhân viên cùng bấm hoặc màn hình cũ. Hệ thống không đổi gì, báo đã ghi nhận, làm mới danh sách. Không ghi đè phương thức đã ghi.
- **E2 — Đơn không còn ghi nhận được** (bước 4): đơn đã bàn giao, hủy hoặc vắng mặt. Hệ thống từ chối, báo trạng thái hiện tại.
- **E3 — Đơn không tồn tại** (bước 4): trả "không tìm thấy".
- **E4 — Không phải quầy hoặc quản lý** (bước 2–4): bị từ chối "không có quyền"; không đổi dữ liệu (BR-33, NFR-03).
- **E5 — Lỗi hệ thống hoặc mất kết nối** (bước 2, 4): báo lỗi, cho thử lại; đơn không đổi.
- **E6 — Phiên hết hạn**: UC-02 E4.

## Postcondition
- **Thành công**: đơn `PAID`, có phương thức, thời điểm và người ghi nhận; trạng thái đơn và quota không đổi.
- **Không thành công**: đơn giữ nguyên.
- Đơn pre-order mới tạo luôn là `UNPAID` (BR-28).

## Ghi chú và giới hạn
- Hệ thống chỉ **ghi nhận**, không tích hợp cổng thanh toán, không xử lý tiền và không hoàn tiền (BR-27, BR-30).
- Lựa chọn hiện thực chưa được xác nhận: cho ghi nhận ở mọi trạng thái đang xử lý (xác nhận, đang chuẩn bị, sẵn sàng) vì pre-order trả khi nhận; không có thao tác hoàn tác thanh toán (SRS không định nghĩa).
- Danh sách của quầy chỉ có đơn hôm nay.

## Output chuyển giao
Dùng cho API và màn "Quầy" (T-29), điều kiện của UC-09 (T-30), test tự động `CounterPaymentBlackBoxTest` (TC-PAY-01 → 11).
