# UC-10 — Đánh dấu không đến nhận (no-show)
Trạng thái: Đặc tả ngắn lập ở Sprint 3 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Nhân viên quầy; quản lý |
| Mục đích | Dọn đơn sẵn sàng tồn đọng khi khách không đến |
| Story và AC | PB-18 AC1 → bước 1–2; AC2 → bước 3–4; AC3 → E1 |
| FR, quy tắc | FR-19; BR-25; SRS B.5 `READY → NO_SHOW` |
| Quan hệ | Thực hiện ở màn Quầy |

## Main Flow
1. Hệ thống: ở danh sách đơn của Quầy, gắn cờ **quá hạn nhận** cho đơn `READY` đã qua giờ kết thúc khung cộng thời gian chờ (30 phút, cấu hình `app.ordering.no-show-grace-minutes`). Khung 12:00–12:15 thì quá hạn từ 12:45.
2. Quầy: thấy thẻ đơn có viền đỏ và nút "Không đến nhận".
3. Quầy: bấm nút.
4. Hệ thống: khóa dòng đơn, kiểm tra lại `READY` và đã quá hạn; chuyển `NO_SHOW`, ghi người và thời điểm. **Không hoàn quota** (khung đã qua). Đơn rời danh sách quầy; khách thấy "Không đến nhận".

## Luồng lỗi
- **E1 — Chưa quá hạn**: từ chối (`NO_SHOW_TOO_EARLY`, kèm `allowedFrom`).
- **E2 — Đơn không còn `READY`** (vừa bàn giao, chưa làm xong): từ chối (`INVALID_STATUS_TRANSITION`).
- **E3 — Bếp, khách gọi API này**: từ chối (403).

## Ghi chú
- Lựa chọn chưa xác nhận: chỉ đánh dấu bằng tay, không tự động; đơn chưa trả tiền vẫn đánh dấu được (không thu được gì).

## Output chuyển giao
`POST /api/counter/orders/{id}/no-show`, trường `overdue` trong `GET /api/counter/orders`, test TC-NOS-01 → 03.
