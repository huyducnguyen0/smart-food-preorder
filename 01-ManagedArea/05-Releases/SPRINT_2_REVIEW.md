# Sprint Review - Sprint 2
Trạng thái: **Accepted cho bản demo v1.0.0**. AI rà soát kỹ thuật ngày 2026-11-26 theo yêu cầu hoàn thiện của người dùng, dựa trên bằng chứng của Sprint và lượt kiểm thử bản tích hợp.

## Input
- Sprint Goal: cửa hàng tự vận hành không cần dữ liệu mẫu, nhận walk-in dùng chung quota, gợi ý khung khi hết chỗ.
- Increment: PR Sprint 2 (dựa trên PR #22, #23).
- Test results: backend 399, frontend 129 test pass; 61 ca black-box Passed.
- Acceptance Criteria: PB-03, PB-04, PB-19, PB-11, PB-09 trong Product Backlog.

## Kết quả trình bày và đánh giá
| Nội dung | Bằng chứng / kết quả kiểm tra | Accepted / Change-Fix Required |
|---|---|---|
| PB-03 Quản lý món (AC1–AC4) | TC-MNG-01 → 08; chạy tay: thêm món, món hiện ở thực đơn | Accepted |
| PB-04 Giờ và quota (AC1–AC4) | TC-CFG-01 → 09 (gồm BR-11); chạy tay: quota riêng khung 18:00 | Accepted |
| PB-19 Đăng ký (AC1–AC3) | TC-REG-01 → 06; chạy tay: đăng ký rồi vào thực đơn | Accepted |
| PB-11 Walk-in (AC1–AC3), NFR-01 | TC-WALK-01 → 10, TC-CON-08; chạy tay: tạo đơn walk-in khung hiện tại | Accepted |
| PB-09 Khung thay thế (AC1–AC3) | TC-ALT-01 → 05; chưa chạy tay | Accepted |

## Feedback
| Phản hồi | Nội dung cần cập nhật Product Backlog |
|---|---|
| Review kỹ thuật dùng kết quả của [lượt v1.0.0](../04-Testing/Test-Results/README.md). Chưa ghi nhận phản hồi của bên ngoài. | Các chức năng đã được kiểm lại trong phạm vi demo và test hồi quy. |

## Release
Tích hợp vào [bản demo v1.0.0](README.md).
