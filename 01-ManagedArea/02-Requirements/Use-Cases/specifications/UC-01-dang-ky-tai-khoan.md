# UC-01 — Đăng ký tài khoản
Trạng thái: Đặc tả ngắn lập ở Sprint 2 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Khách vãng lai |
| Mục đích | Tự tạo tài khoản khách để đặt món trước |
| Story và AC | PB-19 AC1 → bước 2–5; AC2 → E1; AC3 → bước 4 (NFR-06) |
| FR, quy tắc | FR-01; BR-31; NFR-06 |
| Quan hệ | Sau khi đăng ký là đã đăng nhập (như UC-02 bước 5–6) |

## Main Flow
1. Khách vãng lai: mở "Đăng ký" (từ trang đăng nhập).
2. Khách vãng lai: nhập email hoặc số điện thoại, tên hiển thị, mật khẩu.
3. Hệ thống: chuẩn hóa định danh như UC-02 bước 3 (bỏ khoảng trắng; email chữ thường); kiểm tra dạng: email có `@` và tên miền có dấu chấm, hoặc số điện thoại bắt đầu bằng 0, 10–11 chữ số.
4. Hệ thống: tạo tài khoản vai trò Khách, mật khẩu lưu dạng băm BCrypt (NFR-06).
5. Hệ thống: đăng nhập luôn và chuyển tới thực đơn.

## Luồng lỗi
- **E1 — Định danh đã có tài khoản**: từ chối "đã có tài khoản", không tạo gì. Hai người cùng đăng ký một định danh thì chỉ một tài khoản được tạo.
- **E2 — Dữ liệu sai**: thiếu trường, định danh sai dạng, mật khẩu dưới 8 ký tự hoặc quá 72 byte: báo theo từng trường.
- **E3 — Thiếu token chống CSRF**: từ chối.

## Ghi chú
- Lựa chọn chưa xác nhận: mật khẩu tối thiểu 8 ký tự; tên hiển thị bắt buộc; đăng ký xong đăng nhập luôn; không xác minh email hay số điện thoại (không gửi mã).

## Output chuyển giao
`POST /api/auth/register`, màn `/register`, test `Sprint2AdminBlackBoxTest` (TC-REG-01 → 06).
