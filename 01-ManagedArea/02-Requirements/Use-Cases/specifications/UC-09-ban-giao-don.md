# UC-09 — Bàn giao đơn
Trạng thái: Đặc tả ngắn lập ở T-30 ngày 2026-10-26 cho phần làm thêm của Sprint 1; người dùng chưa review. Các quyết định ở mục "Ghi chú" là lựa chọn hiện thực của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Nhân viên quầy (quản lý làm được thao tác của quầy, SRS B.2) |
| Mục đích | Giao đúng đơn cho đúng người, chỉ khi món đã sẵn sàng và đã trả tiền |
| Story và AC | PB-15 AC1 → bước 1–2; AC2 → bước 4–5, E1–E3; AC3 → E4 |
| FR, quy tắc nghiệp vụ | FR-17; BR-29, BR-14, BR-33; SRS B.5 (`READY → PICKED_UP`) |
| NFR | NFR-03 |
| Quan hệ | Cần UC-12 (đơn `READY`) và UC-08 (đơn `PAID`) |
| Phạm vi Sprint | Làm thêm Sprint 1 |

## Precondition
Quầy hoặc quản lý đã đăng nhập (UC-02). Có đơn hôm nay `READY`.

## Main Flow
1. Quầy: mở màn "Quầy".
2. Hệ thống: hiển thị đơn hôm nay đang xử lý; đơn `READY` và `PAID` có ô nhập mã và nút "Bàn giao", đơn `READY` chưa `PAID` nhắc thu tiền trước.
3. Khách: đến quầy, đọc mã nhận (BR-38).
4. Quầy: nhập mã khách đọc, chọn "Bàn giao". Mã so không phân biệt chữ hoa thường và bỏ khoảng trắng hai đầu.
5. Hệ thống: kiểm tra đơn **đang** `READY`, mã đúng và đã `PAID` (BR-29), chuyển sang `PICKED_UP`, ghi thời điểm và người thực hiện.
6. Hệ thống: đơn rời danh sách của quầy; khách thấy "Đã nhận" ở UC-05.

## Alternative Flow
- **E1 — Đơn chưa `READY`** (bước 5): bếp chưa làm xong, hoặc đơn đã hủy, vắng mặt. Hệ thống từ chối, báo trạng thái hiện tại.
- **E2 — Sai mã nhận** (bước 5): từ chối "mã không khớp", không tiết lộ mã đúng; đơn giữ nguyên.
- **E3 — Chưa thanh toán** (bước 5): từ chối, nhắc ghi nhận thanh toán trước (UC-08); đơn giữ nguyên.
- **E4 — Đã bàn giao** (bước 5): đơn `PICKED_UP` không bàn giao lại được; báo trạng thái hiện tại. Hai nhân viên cùng bấm thì chỉ một người thành công.
- **E5 — Đơn không tồn tại**: trả "không tìm thấy".
- **E6 — Thiếu mã**: từ chối dữ liệu không hợp lệ.
- **E7 — Không phải quầy hoặc quản lý**: từ chối "không có quyền"; không đổi dữ liệu (BR-33, NFR-03).
- **E8 — Lỗi hệ thống hoặc phiên hết hạn**: báo lỗi hoặc UC-02 E4; đơn không đổi.

Thứ tự kiểm tra ở hệ thống: tồn tại, trạng thái `READY`, mã khớp, đã thanh toán.

## Postcondition
- **Thành công**: đơn `PICKED_UP` có thời điểm và người bàn giao; điểm quota vẫn giữ (BR-14, thống kê tải).
- **Không thành công**: đơn giữ nguyên.

## Ghi chú và giới hạn
- Lựa chọn hiện thực chưa được xác nhận: quầy gõ lại mã khách đọc (không dùng quét mã); không có thao tác hoàn tác bàn giao; không ràng buộc ở database rằng `status = PICKED_UP` khi và chỉ khi có thời điểm bàn giao, vì các đường chuyển sang trạng thái kết thúc khác (hủy, vắng mặt) chưa có.
- Đánh dấu vắng mặt (UC-11) và hủy đơn (UC-06, UC-10) thuộc Sprint sau.

## Output chuyển giao
Dùng cho API và màn "Quầy" (T-30), test tự động `CounterHandoverBlackBoxTest` (TC-HAND-01 → 11).
