# UC-05 — Xem đơn của tôi
Trạng thái: Đặc tả cho Sprint 1, lập ở T-05 ngày 2026-10-10; người dùng chưa review.

| Mục | Nội dung |
|---|---|
| Actor chính | Khách (đã đăng nhập) |
| Mục đích | Biết đơn của mình đang ở đâu và có mã nhận khi tới quầy |
| Story và AC | PB-08 AC1 → bước 2, 4; AC2 → bước 4, bảng trạng thái. PB-14 → Ghi chú (Sprint sau) |
| FR, quy tắc nghiệp vụ | FR-11; BR-24, BR-27, BR-28, BR-33; SRS B.5 |
| NFR | NFR-03 (không xem được đơn của khách khác) |
| Quan hệ | Không include/extend. Là nơi khách vào để hủy đơn (UC-06, Sprint sau) |
| Phạm vi Sprint | Sprint 1: danh sách, chi tiết, khách tải lại để thấy trạng thái mới. Tự cập nhật khi `READY` (PB-14) ở Sprint sau |

## Precondition
Khách đã đăng nhập với vai trò Khách (UC-02).

## Main Flow
1. Khách: mở "Đơn của tôi" (từ menu điều hướng, hoặc từ màn kết quả đặt đơn UC-04 bước 11).
2. Hệ thống: hiển thị **chỉ các đơn của khách đang đăng nhập**: đơn chưa kết thúc (`CONFIRMED`, `PREPARING`, `READY`) ở trên, sắp theo giờ nhận tăng dần; sau đó các đơn đã kết thúc, mới nhất trước. Mỗi đơn có: mã nhận, ngày, khung nhận, trạng thái, trạng thái thanh toán, tổng tiền.
3. Khách: chọn một đơn.
4. Hệ thống: hiển thị chi tiết: từng món với số lượng và giá đã lưu khi xác nhận, tổng tiền, khung nhận, trạng thái, **mã nhận**, trạng thái thanh toán (`UNPAID` kèm nhắc "Thanh toán tại quầy khi nhận", hoặc `PAID` kèm phương thức), thời điểm đặt. Đơn bị cửa hàng hủy hiển thị **lý do hủy** (BR-24).
5. Khách: tải lại trang để xem trạng thái mới nhất.

Tên trạng thái hiển thị cho khách:
| Trạng thái (SRS B.5) | Hiển thị | Ghi chú cho khách |
|---|---|---|
| `CONFIRMED` | Đã xác nhận | Cửa hàng đã nhận đơn, sẽ chuẩn bị gần giờ nhận |
| `PREPARING` | Đang chuẩn bị | |
| `READY` | Sẵn sàng nhận | Tới quầy, đọc mã nhận, thanh toán nếu chưa |
| `PICKED_UP` | Đã nhận | |
| `CANCELLED` | Đã hủy | Kèm lý do nếu cửa hàng hủy |
| `NO_SHOW` | Không đến nhận | |

## Alternative Flow
- **A1 — Chưa có đơn nào** (bước 2): hiển thị "Bạn chưa có đơn" kèm liên kết tới menu.
- **E1 — Mở đơn không phải của mình** (bước 3–4): ví dụ sửa mã đơn trên địa chỉ trang hoặc gọi thẳng API. Hệ thống từ chối và trả kết quả như đơn không tồn tại, không tiết lộ đơn đó có thật hay không (BR-33, NFR-03).
- **E2 — Phiên đăng nhập hết hạn** (bất kỳ bước nào): UC-02 E4.
- **E3 — Không tải được dữ liệu** (bước 2 hoặc 4): báo lỗi và cho thử lại; không hiển thị trạng thái cũ như trạng thái hiện tại.

## Postcondition
Không thay đổi dữ liệu. Khách thấy trạng thái đơn tại thời điểm tải.

## Ghi chú và giới hạn
- PB-14 (trang đơn tự cập nhật khi `READY`, không cần khách thao tác) để Sprint sau; cơ chế (gọi định kỳ hay đẩy từ server) quyết định khi đó.
- Đơn walk-in không thuộc tài khoản khách nên không xuất hiện ở đây.
- Trong Sprint 1, khách chỉ thấy `CONFIRMED`, `PREPARING`, `READY` trong thực tế; các trạng thái còn lại xuất hiện khi có UC-09, UC-06/UC-11, UC-10.

## Output chuyển giao
Dùng cho UI "Đơn của tôi" (T-11), API và màn hình (T-20), test phân quyền đơn (T-14), Test Case (T-23).
