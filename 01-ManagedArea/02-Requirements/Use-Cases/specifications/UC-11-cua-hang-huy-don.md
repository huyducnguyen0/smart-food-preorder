# UC-11 — Cửa hàng hủy đơn
Trạng thái: Đặc tả ngắn lập ở Sprint 3 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Quản lý; nhân viên quầy (chỉ đơn walk-in) |
| Mục đích | Xử lý ngoại lệ: cửa hàng không phục vụ được, hoặc khách walk-in đổi ý |
| Story và AC | PB-21 AC1 → bước 2–4, E1–E3; AC2 → bước 5; AC3 → bước 6 |
| FR, quy tắc | FR-18; BR-24, BR-30; SRS B.5 |
| Quan hệ | Thực hiện ở màn Quầy; hoàn quota qua `QuotaService` khi hủy từ `CONFIRMED` |

## Main Flow
1. Quản lý hoặc quầy: ở màn Quầy, chọn "Hủy đơn" trên một đơn được phép hủy.
2. Hệ thống: hiện ô lý do; nút xác nhận chỉ bật khi đã nhập lý do.
3. Người dùng: nhập lý do (tối đa 500 ký tự) và xác nhận.
4. Hệ thống: khóa dòng đơn, kiểm tra quyền theo BR-24: quản lý hủy được đơn `CONFIRMED` hoặc `PREPARING`; quầy chỉ hủy được đơn **walk-in** `CONFIRMED`.
5. Hệ thống: chuyển `CANCELLED`, ghi người hủy, thời điểm, lý do. Hủy từ `CONFIRMED` thì **hoàn đủ điểm**; từ `PREPARING` thì **không hoàn** (bếp đã làm).
6. Khách pre-order thấy "Cửa hàng đã hủy đơn. Lý do: …" ở trang đơn (UC-05). Hoàn tiền cho đơn đã thu làm ngoài hệ thống (BR-30).

## Luồng lỗi
- **E1 — Thiếu lý do hoặc chỉ có khoảng trắng**: từ chối (`VALIDATION_ERROR`).
- **E2 — Quầy hủy đơn pre-order**: từ chối (`CANCEL_NOT_ALLOWED`); cần quản lý.
- **E3 — Trạng thái không cho hủy** (`READY` trở đi, hoặc quầy hủy walk-in đã `PREPARING`): từ chối (`INVALID_STATUS_TRANSITION`).
- **E4 — Bếp, khách gọi API này**: từ chối (403).

## Ghi chú
- Lựa chọn chưa xác nhận: đơn `READY` không ai hủy được trong hệ thống (BR-24 chỉ nêu `CONFIRMED`/`PREPARING`); khách không đến thì dùng UC-10.
- Màn hình chỉ hiện nút hủy ở đơn người dùng có quyền hủy; backend vẫn kiểm tra lại.

## Output chuyển giao
`POST /api/counter/orders/{id}/cancel` với `{ "reason": "..." }`, ô lý do ở màn Quầy, test TC-STC-01 → 06.
