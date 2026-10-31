# UC-06 — Khách hủy đơn
Trạng thái: Đặc tả ngắn lập ở Sprint 3 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Khách (đã đăng nhập) |
| Mục đích | Trả lại chỗ trong khung khi không cần đơn nữa, trước khi bếp cần chuẩn bị |
| Story và AC | PB-10 AC1 → bước 3–5; AC2 → E1–E3 |
| FR, quy tắc | FR-12; BR-21 (cửa sổ chuẩn bị), BR-23; SRS B.5 `CONFIRMED → CANCELLED` |
| Quan hệ | Bắt đầu từ chi tiết đơn của UC-05; hoàn quota qua `QuotaService` (module `slot`, NFR-05) |

## Main Flow
1. Khách: mở chi tiết một đơn `CONFIRMED` của mình (UC-05) và chọn "Hủy đơn".
2. Hệ thống: hỏi lại để tránh bấm nhầm.
3. Khách: xác nhận hủy.
4. Hệ thống: khóa dòng đơn, kiểm tra đơn của khách, còn `CONFIRMED`, chưa thanh toán và **chưa tới mốc** giờ bắt đầu khung trừ cửa sổ chuẩn bị (15 phút; khung 12:00 thì hủy được tới trước 11:45).
5. Hệ thống: chuyển `CANCELLED`, ghi người hủy và thời điểm, **hoàn toàn bộ điểm** cho khung trong cùng giao dịch (BR-23). Trang hiện "Đã hủy", không còn nút hủy.

## Luồng lỗi
- **E1 — Đã tới mốc cửa sổ chuẩn bị**: từ chối (`CANCEL_NOT_ALLOWED`, kèm `cancelDeadline`); trang báo mốc và hướng dẫn liên hệ cửa hàng. Đơn và quota giữ nguyên.
- **E2 — Đơn không còn `CONFIRMED`** (bếp vừa bắt đầu, đã hủy trước đó): từ chối (`INVALID_STATUS_TRANSITION`).
- **E3 — Đơn không phải của mình hoặc không có**: trả như không tồn tại (BR-33, NFR-03).
- **E4 — Bấm hủy nhiều lần cùng lúc**: khóa dòng đơn nên chỉ một lần thành công, điểm chỉ hoàn một lần.
- **E5 — Đơn đã thanh toán** (quầy đã thu tiền trước): từ chối (`CANCEL_NOT_ALLOWED`, `reason` = `PAID`); trang đơn không hiện nút hủy, hướng dẫn liên hệ cửa hàng để hủy và hoàn tiền (2026-10-31).

## Ghi chú
- Lựa chọn chưa xác nhận: khách tự hủy không cần nhập lý do; nút hủy hiện với mọi đơn `CONFIRMED` chưa thanh toán, mốc thời gian do backend quyết (màn hình không tự tính để khỏi lệch giờ máy khách).
- Đơn hủy không tính vào giới hạn 3 đơn chưa kết thúc (BR-37), nên khách đặt lại được ngay.
- Bếp chỉ bắt đầu đơn từ thời gian chuẩn bị (BR-21 sửa đổi 2026-10-31), nên khách luôn có trọn thời gian tự hủy tới mốc đó.

## Output chuyển giao
`POST /api/orders/{id}/cancel`, migration V9, nút hủy ở trang chi tiết đơn, test TC-CAN-01 → 07.
