# UC-16 — Theo dõi vận hành
Trạng thái: Đặc tả ngắn lập ở Sprint 3 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Quản lý |
| Mục đích | Nhìn tải theo quota, phát hiện đơn trễ và nguy cơ quá tải trong ngày |
| Story và AC | PB-17 AC1 → bước 2; AC2 → bước 3; AC3 → bước 4; AC4 → bước 5 |
| FR, quy tắc | FR-20; BR-26; Charter NT9 |
| Quan hệ | Chỉ đọc; module `report` không ghi bảng nào |

## Main Flow
1. Quản lý: mở tab "Theo dõi" (`/admin`). Trang tự làm mới mỗi 30 giây.
2. Hệ thống: hiện điểm dự kiến làm trên công suất của khoảng làm và số đơn trong khung nhận. Màn hình liệt kê các khoảng có công việc xếp lịch hoặc có đơn nhận.
3. Hệ thống: **đơn đang xử lý** (`CONFIRMED`, `PREPARING`, `READY`) với nguồn và trạng thái; đơn `CONFIRMED`/`PREPARING` đã qua giờ bắt đầu khung (walk-in: giờ kết thúc khung) gắn nhãn **Trễ** (BR-26, chỉ hiển thị, không chặn; cùng quy tắc với hàng chờ bếp).
4. Hệ thống: **thống kê ngày** gồm số đơn pre-order, walk-in, đã nhận, đang trễ, đã hủy và không đến nhận. **Tải bếp còn lại** bằng tổng điểm còn được xếp lịch chia cho công suất bếp từ hiện tại đến lúc đóng cửa, làm tròn phần trăm. API vẫn cung cấp tổng điểm đã giữ theo khung nhận.
5. Hệ thống: có đơn chưa kết thúc của ngày đã qua thì nhắc "Có N đơn tồn từ ngày trước", dẫn tới màn Quầy (2026-10-31).
6. Hệ thống: ghi rõ quota là mức tải cửa hàng tự đặt, **không phải năng lực bếp đo được** (NT9).

## Luồng lỗi
- **E1 — Không tải được**: báo lỗi; nếu đang có số liệu cũ thì ghi "có thể đã cũ".
- **E2 — Không phải quản lý**: 403.

## Ghi chú
- Thống kê đơn tính theo **ngày của khung nhận**. Điểm đã giữ gồm cả đơn đã nhận và no-show, trừ đơn hủy từ `CONFIRMED`.
- Sprint 4 (2026-10-31, PB-22): mỗi khung hiện "Bếp làm X/Y · nhận Z đơn" (điểm bếp dự kiến làm trên công suất, số đơn nhận).
- Chưa có xem ngày khác, xuất file hay biểu đồ theo giờ.

## Output chuyển giao
`GET /api/admin/dashboard`, màn `/admin`, test TC-DSH-01 → 04.
