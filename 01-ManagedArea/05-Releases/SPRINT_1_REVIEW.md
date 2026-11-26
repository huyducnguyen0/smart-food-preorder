# Sprint Review - Sprint 1

Trạng thái: **Accepted cho bản demo v1.0.0**. Rà soát kỹ thuật ngày 2026-11-26 theo yêu cầu hoàn thiện và phát hành của người dùng. Bằng chứng gồm test của Sprint 1 và kiểm tra lại luồng trên bản tích hợp.

## Input

- [Sprint Goal và Backlog](../01-ProjectManagement/SPRINT_1_BACKLOG.md).
- [Tiêu chí chấp nhận](../02-Requirements/Product-Backlog/PRODUCT_BACKLOG.md), PB-01, 02, 05, 06, 07, 08, 12, 13 cùng PB-16 và PB-15 làm thêm.
- [Test Results](../04-Testing/Test-Results/README.md), lượt Sprint 1 và lượt v1.0.0.

## Kết quả trình bày và đánh giá

| Nội dung | Bằng chứng | Kết quả |
|---|---|---|
| Đăng nhập, phân quyền, menu và giỏ | Test auth/menu, ma trận vai trò, kiểm tra giỏ frontend và demo khách trên Docker | Accepted |
| Chọn giờ, nhận đơn và theo dõi trạng thái | Test API giữ chỗ và chống trùng, khách đặt đơn và xem tự cập nhật trên bản tích hợp | Accepted |
| Hàng chờ bếp, thu tiền và nhận món | Test tiến độ bếp, thanh toán, bàn giao. Demo đi qua `PREPARING`, `READY`, ghi nhận tiền mặt, thử mã sai và bàn giao đúng mã | Accepted |

## Feedback và cải tiến

- Điểm phát hiện ở Sprint 1: phiên hết hạn giữa chừng chưa chuyển về đăng nhập. Đã sửa ở T-25 và có test hồi quy.
- Việc còn mở ở thời điểm Sprint 1: TC-MY-05 về lý do hủy. Đã kiểm bằng TC-STC-01 và test giao diện ở Sprint 3.
- Rút kinh nghiệm khi chốt v1: hoàn thiện luồng bàn giao rồi chạy xuyên suốt qua các vai trò trước khi ghi kết quả Review. Lượt v1 đã áp dụng trên Docker.

## Release

Tích hợp vào [bản demo v1.0.0](README.md).
