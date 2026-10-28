# UC-07 — Tạo đơn walk-in
Trạng thái: Đặc tả ngắn lập ở Sprint 2 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Nhân viên quầy (quản lý làm được, SRS B.2) |
| Mục đích | Nhận khách mua trực tiếp dùng **chung quota** với pre-order; khách tự quyết định sau khi biết giờ nhận |
| Story và AC | PB-11 AC1 → bước 2; AC2 → bước 3–4; AC3 → bước 5–7, E3 |
| FR, quy tắc | FR-13; BR-09, BR-10, BR-18, BR-19, BR-20, BR-27; NFR-01, NFR-02 |
| Quan hệ | Include UC-17 Giữ quota (bước 6) |

## Main Flow
1. Quầy: mở "Quầy", phần "Tạo đơn tại quầy".
2. Quầy: chọn món và số lượng từ món còn bán. Không cần thông tin khách (BR-20).
3. Hệ thống: tính khung dự kiến **sớm nhất tính từ khung hiện tại** còn đủ quota (BR-18, không áp mốc 30 phút) và số tiền cần thu. Chưa giữ quota (BR-19).
4. Quầy: báo khách; khách đồng ý thì thu tiền và chọn "Đã thu tiền mặt" hoặc "Đã thu chuyển khoản".
5. Hệ thống: kiểm tra lại món và giá, giữ quota cho khung đã báo qua UC-17 (BR-09, BR-10).
6. Hệ thống: tạo đơn nguồn walk-in, `CONFIRMED`, **`PAID`** kèm phương thức (BR-20), ghi nhân viên tạo, cấp mã nhận.
7. Hệ thống: hiện mã nhận để đọc cho khách. Đơn vào hàng chờ bếp và danh sách quầy như pre-order.

## Luồng lỗi
- **E1 — Không còn khung đủ chỗ hôm nay / đơn lớn hơn quota mọi khung**: báo, không tạo đơn.
- **E2 — Món đổi giá hoặc hết hàng**: báo từng món.
- **E3 — Khung đã báo vừa hết chỗ** (BR-19): không tạo đơn; hiện **khung mới** để báo lại khách, khách đồng ý thì xác nhận lại.
- **E4 — Gửi lại** (bấm lặp, mất mạng): cùng mã yêu cầu chỉ tạo một đơn và trả lại đơn đó (NFR-02).
- **E5 — Khung không hợp lệ**: đã kết thúc, ngày khác, sai mốc, ngoài giờ.
- **E6 — Không phải quầy hoặc quản lý**: từ chối.

## Ghi chú
- Kiểm chứng NFR-01 với hai kênh: 25 pre-order và 25 walk-in cùng lúc vào một khung quota 20, mỗi đơn 3 điểm: đúng 6 đơn được nhận (TC-CON-08).
- Lựa chọn chưa xác nhận: phải trả tiền trước khi xác nhận (không có walk-in "chưa thanh toán"); tối đa 99 phần một món trên màn hình.

## Output chuyển giao
`POST /api/counter/walk-in/estimate`, `POST /api/counter/walk-in`, migration V8, test TC-WALK-01 → 10, TC-CON-08.
