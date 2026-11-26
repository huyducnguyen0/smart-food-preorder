# UC-18 — Đề xuất khung thay thế
Trạng thái: Đặc tả ngắn lập ở Sprint 2 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Khách (qua UC-04) |
| Mục đích | Vẫn đặt được khi khung đã chọn vừa hết chỗ, không phải thử lần lượt |
| Story và AC | PB-09 AC1 → bước 2; AC2 → bước 4; AC3 → E1 |
| FR, quy tắc | BR-15, BR-16 |
| Quan hệ | Extend UC-04 tại điểm mở rộng "Khung không đủ quota" (UC-04 E8) |

## Main Flow
1. Khách xác nhận đặt nhưng khung đã chọn vừa hết chỗ (UC-04 E8).
2. Hệ thống: không tạo đơn; đề xuất **tối đa 3 khung** hợp lệ theo BR-15, còn đủ quota cho giỏ, gần khung đã chọn nhất cả trước và sau; cùng khoảng cách thì **khung sau** đứng trước (BR-16).
3. Khách: chọn một khung gợi ý.
4. Hệ thống: khách xác nhận lại; đơn được kiểm tra lại qua UC-17 như mọi lần đặt.

## Luồng lỗi
- **E1 — Không còn khung nào**: danh sách gợi ý rỗng; báo hết chỗ, khách có thể bớt món.

## Ghi chú
Gợi ý chỉ đúng tại thời điểm hiển thị (BR-36); khung gợi ý cũng có thể vừa hết chỗ.

## Output chuyển giao
Thuộc tính `alternatives` của lỗi `SLOT_FULL` ở `POST /api/orders`; khung gợi ý ở màn chọn giờ; test TC-ALT-01 → 05.
