# UC-17 — Kiểm tra và giữ quota
Trạng thái: Đặc tả cho Sprint 1, lập ở T-05 ngày 2026-10-10; người dùng chưa review.

| Mục | Nội dung |
|---|---|
| Actor chính | Không có actor trực tiếp; được include bởi use case tạo đơn |
| Mục đích | Chỉ xác nhận đơn khi khung còn đủ quota, và việc giữ quota xảy ra cùng lúc với việc tạo đơn, kể cả khi nhiều request tới đồng thời |
| Story và AC | PB-05 AC1 → bước 1, 6; AC2 → bước 4; AC3 → bước 5–6, E3; AC4 → E1; AC5 → Ghi chú; AC6 → bước 4–5, NFR-01 |
| FR, quy tắc nghiệp vụ | FR-07; BR-07–BR-14 |
| NFR | **NFR-01** (nhất quán quota khi đồng thời), NFR-05 (tách trách nhiệm quota khỏi luồng đặt đơn) |
| Quan hệ | Được include bởi UC-04.2 Chọn khung giờ và gửi đơn; sau này bởi UC-07 Tạo đơn walk-in |
| Phạm vi Sprint | Sprint 1: toàn bộ, thiết kế đúng khi đồng thời ngay từ đầu (T-16). Kiểm chứng NFR-01 đầy đủ ở Sprint 2 |

## Precondition
1. Use case gọi đã kiểm tra: khung thuộc ngày hiện tại và nằm trong giờ mở cửa; các món đều còn bán và đúng giá (UC-04 bước 8).
2. Đầu vào: khung giờ đã chọn; danh sách dòng đơn (món, số lượng).

## Main Flow
1. Hệ thống: tính **điểm đơn** = Σ (điểm món hiện tại × số lượng) (BR-08).
2. Hệ thống: nếu điểm đơn = 0 thì đơn không chiếm quota (BR-13); bỏ qua bước 3–5, đi tới bước 6.
3. Hệ thống: lấy **quota của khung**: quota riêng của khung nếu quản lý đã sửa cho ngày hiện tại, ngược lại là quota mặc định (BR-07).
4. Hệ thống: kiểm tra điểm đơn ≤ quota của khung − tổng điểm đang giữ của khung (BR-09). Pre-order và walk-in trừ vào **cùng một** quota (NT1).
5. Hệ thống: cộng điểm đơn vào điểm đang giữ của khung. Bước 4 và 5 là **một thao tác không thể chen ngang**: giữa lúc kiểm tra và lúc giữ, không request nào khác được giữ quota của cùng khung dựa trên số liệu cũ (NFR-01).
6. Hệ thống: trả kết quả "giữ được" cùng điểm đơn để use case gọi lưu vào đơn. Việc giữ quota và việc tạo đơn được ghi nhận **cùng nhau**: nếu bước tạo đơn của use case gọi thất bại thì phần quota vừa giữ cũng không còn (BR-10).

## Alternative Flow
- **E1 — Đơn lớn hơn quota của cả khung** (bước 4): điểm đơn > quota của khung, kể cả khi khung chưa có đơn nào. Không giữ quota; trả kết quả "đơn vượt sức chứa một khung" để use case gọi báo khách liên hệ cửa hàng (BR-12). Kết thúc không thành công.
- **E2 — Không đủ quota còn lại** (bước 4): điểm đơn ≤ quota của khung nhưng > phần còn lại. Không giữ quota; trả kết quả "khung không đủ chỗ". Use case gọi quyết định bước tiếp: UC-04 báo khung vừa hết chỗ (Sprint 1), sau này UC-18 đề xuất khung thay thế. Kết thúc không thành công.
- **E3 — Lỗi khi đang giữ hoặc khi use case gọi tạo đơn** (bước 5–6): lỗi hệ thống, mất kết nối database. Mọi thay đổi bị hủy: không có đơn, quota của khung trở về như trước (BR-10). Trả lỗi hệ thống. Kết thúc không thành công.

## Postcondition
- **Thành công**: điểm đang giữ của khung tăng đúng bằng điểm đơn (không đổi nếu điểm đơn = 0); điểm đơn được lưu cùng đơn; tổng điểm đang giữ của khung ≤ quota của khung.
- **Không thành công**: điểm đang giữ của khung không đổi; không có đơn nào được tạo.
- **Bất biến luôn đúng**, kể cả khi có nhiều request đồng thời: với mọi khung, tổng điểm các đơn đang giữ quota ≤ quota của khung (BR-09, BR-11, NFR-01).

## Ghi chú và giới hạn
- Hoàn quota (BR-14) không thuộc use case này: thực hiện khi hủy đơn (UC-06, UC-11, Sprint sau). Đơn `CONFIRMED`, `PREPARING`, `READY`, `PICKED_UP`, `NO_SHOW` tiếp tục giữ quota.
- Điểm món dùng ở bước 1 là giá trị tại thời điểm xác nhận; sửa điểm món sau đó không làm đổi điểm đơn đã lưu (BR-08).
- Cách hiện thực "không thể chen ngang" ở bước 4–5 (khóa dòng, câu cập nhật có điều kiện...) chọn ở T-16, phải chứng minh bằng test đồng thời theo NFR-01, không chỉ bằng lập luận.

## Output chuyển giao
Dùng cho Analysis/Sequence giữ quota (T-07), Database (T-09), service quota và unit test (T-16), white-box test (T-24).
