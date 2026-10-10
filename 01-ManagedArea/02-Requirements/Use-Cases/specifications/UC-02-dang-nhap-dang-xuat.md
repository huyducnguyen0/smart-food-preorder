# UC-02 — Đăng nhập, đăng xuất
Trạng thái: Đặc tả cho Sprint 1, lập ở T-05 ngày 2026-10-10; người dùng chưa review.

| Mục | Nội dung |
|---|---|
| Actor chính | Người dùng: Khách, Nhân viên quầy, Nhân viên bếp, Quản lý |
| Mục đích | Xác thực để dùng đúng các chức năng của vai trò mình; kết thúc phiên khi xong |
| Story và AC | PB-01 AC1 → bước 3–6, E1, E2; AC2 → mục Yêu cầu phân quyền chung, E5; AC3 → bước 7–8; AC4 → Precondition |
| FR, quy tắc nghiệp vụ | FR-02; BR-32, BR-33, BR-34 |
| NFR | NFR-03 (phân quyền), NFR-06 (mật khẩu không lưu dạng đọc được) |
| Quan hệ | Không include/extend. Là điều kiện trước của mọi use case cần tài khoản |
| Phạm vi Sprint | Sprint 1: toàn bộ, với tài khoản mẫu. Đăng ký (UC-01) và quản lý tài khoản nhân viên (UC-15) ở Sprint sau |

## Precondition
1. Người dùng chưa đăng nhập trên trình duyệt đang dùng.
2. Tài khoản đã tồn tại: khách tự đăng ký (UC-01), nhân viên do quản lý tạo (UC-15), tài khoản quản lý đầu tiên có sẵn khi cài đặt (BR-32). Trong Sprint 1, mọi tài khoản là dữ liệu mẫu.

## Main Flow
**Đăng nhập**
1. Người dùng: mở trang đăng nhập (tự mở, hoặc được chuyển tới khi thao tác cần đăng nhập, xem E4).
2. Người dùng: nhập định danh đăng nhập (email hoặc số điện thoại) và mật khẩu, chọn "Đăng nhập".
3. Hệ thống: chuẩn hóa định danh (bỏ khoảng trắng đầu cuối; email không phân biệt hoa thường) và tìm tài khoản.
4. Hệ thống: kiểm tra mật khẩu bằng cách so với bản băm đã lưu (NFR-06).
5. Hệ thống: kiểm tra tài khoản không bị khóa (BR-32), tạo phiên đăng nhập gắn với tài khoản và vai trò.
6. Hệ thống: chuyển người dùng tới màn hình chính của vai trò; nếu được chuyển tới từ một thao tác cần đăng nhập (E4) thì quay về trang trước đó. Màn hình chính từng vai trò xác định ở UI Prototype (T-11).

**Đăng xuất**
7. Người dùng: chọn "Đăng xuất".
8. Hệ thống: hủy phiên đăng nhập và chuyển về trang menu. Sau đó mọi thao tác cần đăng nhập đều bị từ chối cho tới khi đăng nhập lại.

## Alternative Flow
- **E1 — Sai định danh hoặc mật khẩu** (bước 3 hoặc 4): không tìm thấy tài khoản hoặc mật khẩu không khớp. Hệ thống báo chung "Thông tin đăng nhập không đúng", **không** cho biết sai định danh hay sai mật khẩu (tránh dò tài khoản tồn tại), không tạo phiên, giữ định danh đã nhập và xóa ô mật khẩu. Quay lại bước 2.
- **E2 — Tài khoản bị khóa** (bước 5): mật khẩu đúng nhưng tài khoản bị khóa. Hệ thống báo "Tài khoản đã bị khóa, liên hệ quản lý", không tạo phiên. Kết thúc.
- **E3 — Thiếu thông tin** (bước 2): bỏ trống định danh hoặc mật khẩu. Hệ thống chỉ ra trường còn thiếu, không gửi xác thực. Quay lại bước 2.
- **E4 — Thao tác cần đăng nhập khi chưa đăng nhập hoặc phiên đã hết hạn** (bất kỳ use case nào ngoài UC-01, UC-03): hệ thống không thực hiện thao tác, chuyển tới trang đăng nhập (bước 1); đăng nhập xong quay lại trang trước (bước 6) (BR-34).
- **E5 — Thao tác ngoài quyền của vai trò** (bất kỳ use case nào): hệ thống từ chối và báo không có quyền, không thay đổi dữ liệu (BR-33, NFR-03). Ví dụ: khách gọi thao tác chuyển trạng thái của bếp; nhân viên bếp gọi thao tác đặt đơn.

## Yêu cầu phân quyền chung
Áp dụng cho mọi use case, kiểm tra **tại nơi xử lý yêu cầu**, không chỉ ẩn nút trên giao diện (BR-33):
| Vai trò | Được dùng (Sprint 1) |
|---|---|
| Chưa đăng nhập | UC-03 Xem menu, UC-02 Đăng nhập (UC-01 Đăng ký ở Sprint sau) |
| Khách | UC-03, UC-04, UC-05; chỉ với **đơn của chính mình** |
| Nhân viên bếp | UC-12 |
| Nhân viên quầy | UC-03 (UC-08, UC-09 nếu làm thêm) |
| Quản lý | Mọi use case của Nhân viên quầy và use case quản lý (Sprint sau) |

## Postcondition
- **Thành công (đăng nhập)**: có một phiên đăng nhập gắn tài khoản và vai trò; người dùng ở màn hình chính của vai trò hoặc trang trước đó.
- **Thành công (đăng xuất)**: phiên bị hủy, không dùng lại được.
- **Không thành công**: không có phiên nào được tạo; dữ liệu tài khoản không thay đổi.

## Ghi chú và giới hạn
- Quyết định 2026-10-10: Sprint 1 **chưa** tạm khóa tài khoản khi nhập sai mật khẩu nhiều lần; chỉ khóa khi quản lý khóa. Rủi ro đoán mật khẩu ghi ở RISK-13, xét lại trước khi triển khai công khai.
- Giả định: tài khoản nhân viên và quản lý cũng đăng nhập bằng email hoặc số điện thoại như khách; xác nhận khi đặc tả UC-15.
- Cơ chế phiên (session hay token), thời gian hết hạn phiên: quyết định ở Architecture (T-08) và T-12.

## Output chuyển giao
Dùng cho Activity, Analysis/Sequence, UI đăng nhập (T-11), backend xác thực (T-12), frontend (T-13), test phân quyền (T-14) và Test Case (T-23).
