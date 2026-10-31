# UC-17 — Kiểm tra và giữ chỗ (lịch bếp)
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
Từ Sprint 4 (2026-10-31, PB-22) việc giữ chỗ dựa trên **lịch bếp** thay cho quota riêng từng khung nhận.

1. Hệ thống: tính **điểm đơn** = Σ (điểm món hiện tại × số lượng) (BR-08).
2. Hệ thống: khóa lịch bếp của ngày (các lần nhận đơn của cùng ngày lần lượt chạy, NFR-01).
3. Hệ thống: kiểm tra **khung nhận còn chỗ ở quầy**: số đơn không bị hủy của khung < N (BR-41). Áp dụng cả đơn 0 điểm.
4. Hệ thống: nếu điểm đơn = 0 thì không tốn công bếp (BR-13); đi tới bước 7.
5. Hệ thống: dựng lịch bếp: các khoảng làm 15 phút với công suất (BR-07); các đơn đã nhận còn phải làm, mỗi đơn có cửa sổ làm (BR-39); thêm đơn mới. Xếp theo hạn sớm làm trước, làm sớm nhất có thể.
6. Hệ thống: nhận khi (a) mọi đơn xếp kịp trong toàn bộ công suất và (b) nếu là đơn đặt trước: riêng các đơn đặt trước xếp kịp trong phần X% dành cho đặt trước (BR-40).
7. Hệ thống: cộng điểm đơn vào điểm của khung nhận (thống kê), trả kết quả "giữ được" để use case gọi lưu đơn. Giữ chỗ và tạo đơn được ghi nhận **cùng nhau** (BR-10).

## Alternative Flow
- **E1 — Đơn quá lớn** (bước 5): bếp trống hoàn toàn cũng không làm kịp đơn này trong cửa sổ của nó. Trả "đơn quá lớn", báo khách liên hệ cửa hàng (BR-12).
- **E2 — Không đủ chỗ** (bước 3 hoặc 6): quầy đủ đơn (`reason` = `PICKUP_LIMIT`) hoặc bếp không còn kịp (`reason` = `KITCHEN`). Trả "khung không đủ chỗ"; UC-04 đề xuất khung khác (UC-18), UC-07 báo khung mới.
- **E3 — Lỗi khi đang giữ hoặc khi use case gọi tạo đơn**: mọi thay đổi bị hủy, không có đơn (BR-10).

## Postcondition
- **Thành công**: đơn được tạo; điểm của khung nhận tăng đúng bằng điểm đơn.
- **Không thành công**: không có đơn nào được tạo, không gì thay đổi.
- **Bất biến luôn đúng**, kể cả khi đồng thời: các đơn còn phải làm luôn xếp được lịch không khoảng làm nào vượt công suất; số đơn mỗi khung nhận ≤ N (NFR-01).

## Ghi chú và giới hạn
- Hoàn quota (BR-14) không thuộc use case này: thực hiện khi hủy đơn (UC-06, UC-11, Sprint sau). Đơn `CONFIRMED`, `PREPARING`, `READY`, `PICKED_UP`, `NO_SHOW` tiếp tục giữ quota.
- Điểm món dùng ở bước 1 là giá trị tại thời điểm xác nhận; sửa điểm món sau đó không làm đổi điểm đơn đã lưu (BR-08).
- Cách hiện thực "không thể chen ngang": khóa lịch bếp của ngày (`pg_advisory_xact_lock`) tới hết giao dịch đặt đơn ([Class Design](../../../03-Design/Class-Design/README.md) mục 3). Trước Sprint 4 dùng câu cập nhật có điều kiện trên dòng khung. Kiểm chứng: TC-CON-08, TC-PLAN-08.
- Cách xếp đơn giản (hạn sớm làm trước, làm sớm nhất) không tối ưu tuyệt đối: hiếm khi từ chối một đơn mà cách xếp khéo hơn vẫn nhận được; không bao giờ nhận vượt công suất.
- Đặt H = 15 phút, X = 100%, N rất lớn thì hành vi với đơn đặt trước giống quota theo khung nhận trước Sprint 4.

## Output chuyển giao
Dùng cho Analysis/Sequence giữ quota (T-07), Database (T-09), service quota và unit test (T-16), white-box test (T-24).
