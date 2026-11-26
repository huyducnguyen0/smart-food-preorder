# UC-14 — Cấu hình giờ mở cửa và quota
Trạng thái: Đặc tả ngắn lập ở Sprint 2 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Quản lý |
| Mục đích | Cấu hình công suất làm của bếp và giới hạn nhận món ở quầy |
| Story và AC | PB-04 AC1 → UC-14.1; AC2 → UC-14.2, 14.3; AC3 → bước 2; AC4 → E1, E2 |
| FR, quy tắc | FR-06; BR-06, BR-07, BR-11, BR-39–BR-41 |

## Main Flow
1. Quản lý: mở "Quota".
2. Hệ thống: trong giờ mở cửa hôm nay, hiện công suất và điểm bếp dự kiến làm theo khoảng 15 phút, cùng số đơn nhận trong mỗi khung.
3. Quản lý: đổi giờ mở/đóng cửa (đúng mốc 15 phút), công suất mặc định, giới hạn làm trước, phần đặt trước hoặc giới hạn quầy. Có thể sửa công suất riêng của một khoảng hoặc trả về mặc định.
4. Hệ thống: kiểm tra BR-11 rồi lưu; danh sách khung của khách đổi ngay.

## Luồng lỗi
- **E1 — Bếp không còn kịp làm đơn đã nhận** (BR-11): với cấu hình mới, lịch dùng công suất tổng không còn xếp kịp các đơn đã nhận. Từ chối và giữ cấu hình cũ.
- **E2 — Thu hẹp giờ làm mất khung có đơn** (BR-11): khung có đơn đang xử lý hoặc đang giữ điểm nằm ngoài giờ mới. Từ chối, báo các khung đó.
- **E3 — Dữ liệu sai**: giờ không đúng mốc, mở không trước đóng, không đủ một khung, quota âm, khung ngoài giờ mở cửa.
- **E4 — Không phải quản lý**: từ chối.

## Ghi chú
- Đổi cấu hình và nhận đơn dùng khóa cấu hình và khóa lịch bếp khi kiểm tra và lưu (BR-11). Điểm đã giữ ghi theo khung nhận để thống kê. Nếu chỉ giảm phần đặt trước, các đơn đã nhận vẫn được bảo vệ theo BR-40.
- Sprint 4 (2026-10-31, PB-22): "quota" đổi nghĩa thành **công suất bếp mỗi khoảng làm 15 phút**. Thêm thời gian làm trước tối đa, phần cho đặt trước (%), giới hạn đơn mỗi khung nhận. Màn hình hiện điểm bếp dự kiến làm và số đơn nhận mỗi khung. Giảm công suất hay đổi cấu hình tới mức bếp không còn kịp đơn đã nhận thì bị từ chối (BR-11).
- Lựa chọn chưa xác nhận: đơn 0 điểm cũng tính là "khung có đơn" khi thu hẹp giờ; được sửa quota cả khung đã qua trong ngày.

## Output chuyển giao
`GET /api/admin/slots/today`, `PUT /api/admin/settings`, `PUT /api/admin/slots/today/{giờ}/quota`, màn `/admin/slots`, test TC-CFG-01 → 09.
