# UC-03 — Xem menu
Trạng thái: Đặc tả cho Sprint 1, lập ở T-05 ngày 2026-10-10; người dùng chưa review.

| Mục | Nội dung |
|---|---|
| Actor chính | Khách vãng lai, Khách, Nhân viên quầy (và Quản lý qua tổng quát hóa) |
| Mục đích | Biết cửa hàng đang bán món gì, giá bao nhiêu, món nào đang tạm hết, để chọn món |
| Story và AC | PB-02 AC1 → bước 2; AC2 → bước 2, A2; AC3 → bước 2; AC4 → A3 |
| FR, quy tắc nghiệp vụ | FR-05; BR-01, BR-03, BR-04, BR-34 |
| NFR | NFR-04 (p95 ≤ 1 giây cho xem menu, kiểm chứng ở Sprint sau) |
| Quan hệ | Là điểm bắt đầu của UC-04 Đặt pre-order (thêm món vào giỏ, UC-04.1); sau này dùng cho UC-07 Tạo đơn walk-in |
| Phạm vi Sprint | Sprint 1: toàn bộ, menu là dữ liệu mẫu. Quản lý món (UC-13) ở Sprint sau |

## Precondition
Không cần đăng nhập (BR-34).

## Main Flow
1. Actor: mở trang menu.
2. Hệ thống: hiển thị các món **đang bán** (bỏ món ngừng bán, BR-03), nhóm theo nhóm món. Mỗi món có: tên, giá, tình trạng **còn bán** hoặc **tạm hết**; mô tả và hình ảnh nếu có (BR-01). Món tạm hết vẫn hiện nhưng được đánh dấu và không có thao tác thêm vào giỏ (BR-04).
3. Actor: xem, có thể lọc theo nhóm món.
4. Khách (đã đăng nhập): chọn "Thêm vào giỏ" ở một món còn bán → tiếp tục ở UC-04, phần UC-04.1.

## Alternative Flow
- **A1 — Menu chưa có món nào đang bán** (bước 2): hệ thống hiển thị thông báo "Hiện chưa có món", không có lỗi.
- **A2 — Món tạm hết** (bước 2–4): món hiển thị nhãn "Tạm hết", không chọn được. Nếu actor vẫn gửi yêu cầu thêm (ví dụ trang đang mở từ trước khi món chuyển tạm hết), xử lý ở UC-04 E1 (khi thêm vào giỏ) và UC-04 E7 (khi gửi đơn).
- **A3 — Khách vãng lai chọn thêm vào giỏ** (bước 4): hệ thống yêu cầu đăng nhập (UC-02 E4); đăng nhập xong quay lại trang menu. Món chưa được tự thêm vào giỏ, khách chọn lại.
- **E1 — Không tải được menu** (bước 2): lỗi mạng hoặc hệ thống. Hiển thị thông báo lỗi và nút thử lại; không hiển thị dữ liệu cũ như dữ liệu hiện tại.

## Postcondition
Không thay đổi dữ liệu. Actor thấy menu tại thời điểm tải; menu có thể thay đổi sau đó (giá, tình trạng), và được kiểm tra lại khi gửi đơn (BR-35).

## Ghi chú và giới hạn
- Không hiển thị điểm công việc của món cho khách (thông tin vận hành nội bộ).
- Khách vãng lai thấy cùng nội dung với khách đã đăng nhập.

## Output chuyển giao
Dùng cho UI menu (T-11), API menu (T-15), Test Case (T-23).
