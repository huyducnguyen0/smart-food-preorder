# UC-04 — Đặt pre-order
Gồm UC-04.1 Quản lý giỏ hàng (bước 1–4) và UC-04.2 Chọn khung giờ và gửi đơn (bước 5–11).

Trạng thái: Đặc tả cho Sprint 1, lập ở T-05 ngày 2026-10-10; người dùng chưa review.

| Mục | Nội dung |
|---|---|
| Actor chính | Khách (đã đăng nhập) |
| Mục đích | Đặt món cho một khung giờ nhận trong ngày và được xác nhận ngay khi cửa hàng còn khả năng phục vụ |
| Story và AC | PB-06 AC1 → bước 1–4; AC2 → E1, bước 8. PB-07 AC1 → bước 6, E6; AC2 → bước 9–11; AC3 → E6, E7; AC4 → A1; AC5 → E3, E8; AC6 → E5, E7 |
| FR, quy tắc nghiệp vụ | FR-08, FR-09; BR-04, BR-06, BR-08, BR-10, BR-12, BR-15, BR-17, BR-27, BR-28, BR-35, BR-36, BR-37, BR-38 |
| NFR | NFR-01 (qua UC-17), NFR-02 (không trùng đơn khi gửi lại), NFR-04 (p95 ≤ 1 giây cho đặt đơn) |
| Quan hệ | Bắt đầu từ UC-03 Xem menu; UC-04.2 include UC-17 Kiểm tra và giữ quota |
| Điểm mở rộng | **Khung không đủ quota** (bước 9, luồng E8): UC-18 Đề xuất khung thay thế mở rộng tại đây (Sprint sau) |
| Phạm vi Sprint | Sprint 1: toàn bộ trừ UC-18; khung hết chỗ thì chỉ báo để khách chọn khung khác (PB-07 AC5) |

## Precondition
1. Khách đã đăng nhập với vai trò Khách (UC-02).
2. Cửa hàng đã có giờ mở cửa, quota mặc định và menu (Sprint 1: dữ liệu mẫu).

## Main Flow
**UC-04.1 — Quản lý giỏ hàng**
1. Khách: ở trang menu (UC-03), chọn "Thêm vào giỏ" ở một món còn bán.
2. Hệ thống: thêm món vào giỏ với số lượng 1, hoặc tăng số lượng nếu món đã có trong giỏ; ghi nhận giá đang hiển thị của món.
3. Khách: mở giỏ; có thể đổi số lượng (số nguyên ≥ 1) hoặc bỏ món.
4. Hệ thống: hiển thị từng dòng (tên món, số lượng, đơn giá, thành tiền) và tổng tiền; cập nhật ngay khi khách thay đổi.

**UC-04.2 — Chọn khung giờ và gửi đơn**

5. Khách: chọn "Chọn giờ nhận".
6. Hệ thống: liệt kê các khung 15 phút trong ngày hiện tại, trong giờ mở cửa, có giờ bắt đầu cách thời điểm hiện tại ít nhất 30 phút (BR-06, BR-15). Mỗi khung hiển thị "Đặt được" hoặc "Không đủ chỗ" theo điểm của giỏ so với quota còn lại, không hiển thị con số quota (BR-36). Khung không đủ chỗ không chọn được.
7. Khách: chọn một khung "Đặt được", xem lại tóm tắt (món, số lượng, tổng tiền, khung nhận, thanh toán tại quầy khi nhận) và chọn "Xác nhận đặt".
8. Hệ thống: kiểm tra lại tại thời điểm nhận yêu cầu:
   - a. Khách chưa có đủ N đơn chưa kết thúc (BR-37).
   - b. Khung vẫn thỏa BR-15.
   - c. Từng món vẫn còn bán và giá đúng bằng giá khách đã thấy trong giỏ (BR-04, BR-35).
   - d. Yêu cầu này chưa được xử lý trước đó (BR-17, xem A1).
9. Hệ thống: thực hiện **UC-17 Kiểm tra và giữ quota** cho khung đã chọn.
10. Hệ thống: tạo đơn trạng thái `CONFIRMED`, thanh toán `UNPAID` (BR-28), nguồn pre-order, lưu từng dòng đơn với giá tại thời điểm xác nhận (BR-35) và điểm đơn (BR-08), cấp mã nhận (BR-38). Tạo đơn và giữ quota thành công cùng nhau (BR-10).
11. Hệ thống: hiển thị kết quả đặt thành công: **mã nhận**, khung nhận, món, tổng tiền, trạng thái "Đã xác nhận", nhắc thanh toán tại quầy khi nhận (BR-27). Xóa giỏ.

## Alternative Flow
**Luồng thay thế**
- **A1 — Gửi lại cùng yêu cầu** (bước 8d): khách bấm "Xác nhận đặt" nhiều lần, hoặc gửi lại sau khi mất kết nối không nhận được kết quả, kể cả nhiều lần gửi tới cùng lúc. Mỗi lần xác nhận đặt mang một **mã yêu cầu** riêng tạo khi khách vào bước 7; các lần gửi lại dùng cùng mã. Nếu mã đã có đơn: không tạo đơn mới, không giữ thêm quota, trả lại kết quả của đơn đã tạo → bước 11 (BR-17, NFR-02).
- **A2 — Giỏ trống** (bước 4–5): không có món nào; thao tác "Chọn giờ nhận" không khả dụng. Khách quay lại menu.

**Luồng lỗi**
- **E1 — Món không còn bán khi thêm vào giỏ** (bước 1–2): trang menu đã cũ, món vừa chuyển tạm hết hoặc ngừng bán. Hệ thống không thêm món, báo món không còn bán, làm mới menu (BR-04).
- **E2 — Không còn khung hợp lệ trong ngày** (bước 6): đã quá giờ hoặc khung cuối cách hiện tại dưới 30 phút. Hệ thống báo "Hôm nay không còn khung nhận"; giỏ được giữ. Kết thúc.
- **E3 — Mọi khung hợp lệ đều không đủ chỗ** (bước 6): hệ thống báo hết chỗ trong ngày; giỏ được giữ để khách có thể bớt món. Quay lại bước 3 hoặc kết thúc.
- **E4 — Giỏ lớn hơn quota của mọi khung** (bước 6): điểm giỏ vượt quota của từng khung, kể cả khung chưa có đơn. Hệ thống báo đơn quá lớn cho một khung và đề nghị liên hệ cửa hàng (BR-12). Quay lại bước 3 hoặc kết thúc.
- **E5 — Đã đủ N đơn chưa kết thúc** (bước 8a): hệ thống từ chối, báo số đơn đang chờ và hướng dẫn xem ở "Đơn của tôi" (UC-05); giỏ được giữ (BR-37). Kết thúc.
- **E6 — Khung không còn hợp lệ** (bước 8b): khách để quá lâu, khung đã cách hiện tại dưới 30 phút. Hệ thống từ chối, báo khung không còn đặt được, quay lại bước 6 với danh sách mới.
- **E7 — Món thay đổi so với giỏ** (bước 8c): có món đổi giá, tạm hết hoặc ngừng bán. Hệ thống không tạo đơn, báo từng món đã thay đổi, cập nhật giỏ (giá mới, bỏ món không còn bán) và quay lại bước 4 để khách xem lại rồi gửi lại (BR-35). Khách không bao giờ bị tính giá chưa thấy.
- **E8 — Khung vừa hết chỗ** (bước 9, UC-17 E2): khung đặt được lúc hiển thị nhưng quota đã bị đơn khác giữ trước. Hệ thống không tạo đơn, báo khung vừa hết chỗ, quay lại bước 6 với trạng thái mới. *Điểm mở rộng UC-18 (Sprint sau): đề xuất tối đa 3 khung thay thế (BR-16).*
- **E9 — Đơn vượt sức chứa một khung** (bước 9, UC-17 E1): báo đơn quá lớn cho một khung, đề nghị liên hệ cửa hàng (BR-12). Quay lại bước 3 hoặc kết thúc.
- **E10 — Lỗi hệ thống** (bước 9–10, UC-17 E3): hệ thống báo lỗi, không có đơn nào được tạo, quota không đổi (BR-10); giỏ được giữ. Khách gửi lại an toàn nhờ A1.
- **E11 — Phiên đăng nhập hết hạn** (bất kỳ bước nào): chuyển tới đăng nhập (UC-02 E4); giỏ vẫn còn trên trình duyệt.

## Postcondition
- **Thành công**: có đúng một đơn mới của khách: `CONFIRMED`, `UNPAID`, nguồn pre-order, gắn khung đã chọn, có mã nhận không trùng trong ngày, giá từng dòng và điểm đơn đã lưu; quota khung tăng đúng bằng điểm đơn. Đơn xuất hiện ở "Đơn của tôi" (UC-05) và hàng chờ bếp (UC-12). Giỏ trống.
- **Không thành công**: không có đơn mới; quota không đổi; giỏ giữ nguyên (hoặc đã được cập nhật theo E7).

## Ghi chú và giới hạn
- Quyết định 2026-10-10: giỏ hàng lưu **trên trình duyệt** của khách (không lưu trên server); đổi thiết bị hoặc xóa dữ liệu trình duyệt thì mất giỏ. Vì giỏ do trình duyệt giữ, mọi giá trị trong giỏ chỉ là "khách đã thấy"; backend luôn kiểm tra lại ở bước 8.
- Tổng tiền đơn = Σ (giá lưu của món × số lượng). Không có giảm giá, phí hay thuế trong phạm vi.
- Tình trạng "Đặt được" ở bước 6 chỉ đúng tại thời điểm hiển thị; quyết định cuối cùng ở bước 9 (BR-36).
- Thời điểm sinh mã yêu cầu ở A1 và cách lưu để chặn trùng khi đồng thời: thiết kế ở T-08, T-09, hiện thực ở T-18.

## Output chuyển giao
Dùng cho Activity "Đặt pre-order" (T-06), Analysis/Sequence (T-07), Database (T-09), UI giỏ và đặt đơn (T-11), API đặt đơn (T-18), màn chọn khung và gửi đơn (T-19), Test Case (T-23).
