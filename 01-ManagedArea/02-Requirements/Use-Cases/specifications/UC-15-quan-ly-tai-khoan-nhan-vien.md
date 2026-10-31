# UC-15 — Quản lý tài khoản nhân viên
Trạng thái: Đặc tả ngắn lập ở Sprint 3 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Quản lý |
| Mục đích | Kiểm soát ai được thao tác ở quầy và bếp |
| Story và AC | PB-20 AC1 → UC-15.1; AC2 → UC-15.2, 15.3; AC3 → E4 |
| FR, quy tắc | FR-03; BR-31 (định danh), BR-32; NFR-06 |
| Quan hệ | UC-15.1–15.3 mở rộng màn danh sách (`<<extend>>`) |

## Main Flow
1. Quản lý: mở "Quản lý" → "Tài khoản nhân viên".
2. Hệ thống: liệt kê tài khoản **quầy và bếp** (tên, định danh, vai trò, đã khóa hay chưa). Không có mật khẩu, không có khách hay quản lý.
3. **UC-15.1 Tạo**: quản lý nhập email hoặc số điện thoại, tên, mật khẩu ban đầu (≥ 8 ký tự), chọn vai trò Quầy hoặc Bếp. Hệ thống chuẩn hóa định danh như UC-01, lưu mật khẩu dạng băm, tài khoản dùng được ngay.
4. **UC-15.2 Khóa**: tài khoản bị khóa không đăng nhập được nữa (`ACCOUNT_LOCKED`); phiên đang mở bị hủy ở thao tác kế tiếp.
5. **UC-15.3 Mở khóa**: tài khoản đăng nhập lại được.

## Luồng lỗi
- **E1 — Định danh đã có tài khoản**: `LOGIN_ID_TAKEN`.
- **E2 — Định danh sai dạng, thiếu tên, mật khẩu ngắn hoặc quá 72 byte**: `VALIDATION_ERROR`.
- **E3 — Vai trò khác Quầy/Bếp, hoặc khóa tài khoản khách/quản lý**: `VALIDATION_ERROR`.
- **E4 — Không phải quản lý**: 403.

## Ghi chú
- Khóa tài khoản có hiệu lực ngay cả với phiên đang mở (2026-10-31): mỗi yêu cầu đã đăng nhập kiểm tra lại tài khoản; bị khóa thì phiên bị hủy, trả 401 và màn hình chuyển về trang đăng nhập. Trước đó màn quầy và bếp tự làm mới 30 giây/lần nên phiên không bao giờ hết hạn, khóa không có tác dụng với màn đang mở.
- Chưa có đổi mật khẩu, sửa tên hay xóa tài khoản (ngoài BR-32).

## Output chuyển giao
`GET/POST /api/admin/staff`, `POST /api/admin/staff/{id}/lock`, `/unlock`, màn `/admin/staff`, test TC-STF-01 → 07.
