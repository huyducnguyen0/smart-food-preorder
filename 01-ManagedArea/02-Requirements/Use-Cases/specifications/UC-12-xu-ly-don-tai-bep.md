# UC-12 — Xử lý đơn tại bếp
Gồm UC-12.1 Xem hàng chờ bếp (bước 1–3) và UC-12.2 Cập nhật tiến độ đơn (bước 4–7).

Trạng thái: Đặc tả cho Sprint 1, lập ở T-05 ngày 2026-10-10; người dùng chưa review.

| Mục | Nội dung |
|---|---|
| Actor chính | Nhân viên bếp |
| Mục đích | Biết cần làm đơn nào tiếp theo và báo cho quầy, khách khi đơn sẵn sàng |
| Story và AC | PB-12 AC1 → bước 2; AC2 → bước 2; AC3 → bước 2. PB-13 AC1 → bước 4–7; AC2 → E1; AC3 → E2 |
| FR, quy tắc nghiệp vụ | FR-14, FR-15; BR-21, BR-22, BR-26, BR-33; SRS B.5 |
| NFR | NFR-03, NFR-04 (p95 ≤ 1 giây cho xem hàng chờ) |
| Quan hệ | UC-12 include UC-12.1; UC-12.2 extend UC-12.1 (thao tác trên từng đơn trong hàng chờ) |
| Phạm vi Sprint | Sprint 1: toàn bộ. Đơn walk-in xuất hiện khi có UC-07 (Sprint sau) |

## Precondition
Nhân viên bếp đã đăng nhập (UC-02).

## Main Flow
**UC-12.1 — Xem hàng chờ bếp**
1. Nhân viên bếp: mở "Hàng chờ bếp".
2. Hệ thống: hiển thị các đơn của **ngày hiện tại**, từ cả pre-order và walk-in (NT7), chia ba nhóm:
   | Nhóm | Đơn thuộc nhóm |
   |---|---|
   | Đang làm | Trạng thái `PREPARING` |
   | Cần chuẩn bị | `CONFIRMED` và thời điểm hiện tại ≥ giờ bắt đầu khung − N phút, N = 15 cấu hình được (BR-21) |
   | Sắp tới | `CONFIRMED` còn lại |

   Trong mỗi nhóm, sắp theo giờ bắt đầu khung tăng dần; cùng khung thì theo thời điểm xác nhận (BR-22), không theo thời điểm tạo trên giao diện. Mỗi đơn hiển thị: mã nhận, khung nhận, nguồn (pre-order/walk-in), danh sách món và số lượng, thời điểm xác nhận. Đơn `CONFIRMED` hoặc `PREPARING` đã qua giờ bắt đầu khung được **đánh dấu trễ** (BR-26), chỉ để hiển thị, không chặn thao tác.
3. Hệ thống: danh sách phản ánh dữ liệu tại thời điểm tải; được làm mới khi bếp tải lại hoặc tự làm mới định kỳ (cách làm chọn ở UI, T-11).

**UC-12.2 — Cập nhật tiến độ đơn**

4. Nhân viên bếp: chọn "Bắt đầu" ở một đơn `CONFIRMED`, thuộc nhóm cần chuẩn bị hoặc sắp tới (bếp được bắt đầu đơn sắp tới, BR-21).
5. Hệ thống: kiểm tra đơn **đang** ở `CONFIRMED`, chuyển sang `PREPARING`, ghi thời điểm và người thực hiện. Đơn chuyển sang nhóm "Đang làm".
6. Nhân viên bếp: khi làm xong cả đơn, chọn "Hoàn tất" ở đơn `PREPARING`.
7. Hệ thống: kiểm tra đơn **đang** ở `PREPARING`, chuyển sang `READY`, ghi thời điểm và người thực hiện. Đơn rời hàng chờ bếp; khách thấy "Sẵn sàng nhận" ở UC-05; quầy thấy đơn ở danh sách bàn giao (UC-09, khi có).

Cập nhật theo **cả đơn**, không theo từng món (BR-22).

## Alternative Flow
- **A1 — Hàng chờ trống** (bước 2): hiển thị "Chưa có đơn cần làm".
- **E1 — Chuyển trạng thái không hợp lệ** (bước 5 hoặc 7): trạng thái hiện tại của đơn không còn như màn hình đang hiển thị. Ví dụ: hai nhân viên bếp cùng bấm "Bắt đầu" một đơn; đơn đã bị cửa hàng hủy (Sprint sau); màn hình đã cũ. Hệ thống không thay đổi đơn, báo trạng thái hiện tại và làm mới hàng chờ. Chỉ các chuyển `CONFIRMED → PREPARING` và `PREPARING → READY` được phép cho bếp (SRS B.5).
- **E2 — Không phải nhân viên bếp** (bước 4–7): tài khoản vai trò khác gọi thao tác chuyển trạng thái. Hệ thống từ chối, không thay đổi đơn (BR-33, PB-13 AC3, NFR-03).
- **E3 — Không tải được hàng chờ hoặc lỗi khi cập nhật** (bước 2, 5, 7): báo lỗi, cho thử lại; trạng thái đơn không đổi nếu cập nhật thất bại.
- **E4 — Phiên đăng nhập hết hạn**: UC-02 E4.

## Postcondition
- **Thành công (bước 5)**: đơn ở `PREPARING`, có thời điểm và người bắt đầu; quota vẫn giữ (BR-14).
- **Thành công (bước 7)**: đơn ở `READY`, có thời điểm và người hoàn tất; quota vẫn giữ.
- **Không thành công**: trạng thái đơn không đổi.

## Ghi chú và giới hạn
- Không có thao tác hoàn tác (ví dụ `PREPARING → CONFIRMED` khi bấm nhầm) vì SRS B.5 không cho phép chuyển trạng thái khác. Bấm nhầm thì xử lý ngoài hệ thống. Đây là hệ quả của quy tắc hiện có; cần người dùng xác nhận chấp nhận được.
- Bếp không thấy giá và trạng thái thanh toán (không cần cho việc chế biến).
- Đơn `READY` không còn trong hàng chờ bếp; đơn bị hủy (Sprint sau) tự rời hàng chờ.

## Output chuyển giao
Dùng cho Activity "Xử lý đơn tại bếp" (T-06), UI hàng chờ bếp (T-11), API và màn hình (T-21), chuyển trạng thái và test (T-22), Test Case (T-23).
