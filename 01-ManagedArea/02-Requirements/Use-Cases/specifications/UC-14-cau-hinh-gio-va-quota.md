# UC-14 — Cấu hình giờ mở cửa và quota
Trạng thái: Đặc tả ngắn lập ở Sprint 2 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Quản lý |
| Mục đích | Giới hạn lượng việc cửa hàng cam kết theo từng khung 15 phút |
| Story và AC | PB-04 AC1 → UC-14.1; AC2 → UC-14.2, 14.3; AC3 → bước 2; AC4 → E1, E2 |
| FR, quy tắc | FR-06; BR-06, BR-07, BR-11 |

## Main Flow
1. Quản lý: mở "Quota".
2. Hệ thống: hiện giờ mở cửa, quota mặc định và **từng khung hôm nay**: quota hiệu lực (đánh dấu nếu là quota riêng), điểm đã giữ, số đơn đang xử lý.
3. Quản lý: đổi giờ mở/đóng cửa (đúng mốc 15 phút) và quota mặc định (UC-14.1, 14.2); hoặc sửa quota riêng một khung hôm nay, hay trả khung về quota mặc định (UC-14.3).
4. Hệ thống: kiểm tra BR-11 rồi lưu; danh sách khung của khách đổi ngay.

## Luồng lỗi
- **E1 — Quota thấp hơn điểm đã giữ** (BR-11): quota mặc định mới thấp hơn điểm của khung đang dùng quota mặc định, hoặc quota riêng thấp hơn điểm khung đang giữ. Từ chối, báo khung nào đang giữ bao nhiêu.
- **E2 — Thu hẹp giờ làm mất khung có đơn** (BR-11): khung có đơn đang xử lý hoặc đang giữ điểm nằm ngoài giờ mới. Từ chối, báo các khung đó.
- **E3 — Dữ liệu sai**: giờ không đúng mốc, mở không trước đóng, không đủ một khung, quota âm, khung ngoài giờ mở cửa.
- **E4 — Không phải quản lý**: từ chối.

## Ghi chú
- Đổi cấu hình và giữ quota được xếp hàng bằng khóa trên dòng cấu hình, nên không có lần đặt nào "lọt" giữa lúc kiểm tra và lúc lưu (BR-11). Sửa quota riêng là một câu lệnh có điều kiện `held_points <= quota mới`.
- Lựa chọn chưa xác nhận: đơn 0 điểm cũng tính là "khung có đơn" khi thu hẹp giờ; được sửa quota cả khung đã qua trong ngày.

## Output chuyển giao
`GET /api/admin/slots/today`, `PUT /api/admin/settings`, `PUT /api/admin/slots/today/{giờ}/quota`, màn `/admin/slots`, test TC-CFG-01 → 09.
