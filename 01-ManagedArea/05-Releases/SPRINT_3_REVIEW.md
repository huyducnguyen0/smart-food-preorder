# Sprint Review - Sprint 3
Trạng thái: **Nháp do AI soạn ngày 2026-10-28, người dùng chưa duyệt.** Đây là tự đánh giá theo Acceptance Criteria; chưa có ai khác xem demo, nên chưa có phản hồi thật.

## Input
- Sprint Goal: xử lý ngoại lệ của đơn mà quota luôn đúng; trang đơn tự cập nhật; quản lý tự quản lý nhân viên và theo dõi tải; đo NFR-04.
- Increment: PR Sprint 3 (dựa trên PR Sprint 2).
- Test results: backend 427, frontend 144 test pass; 28 ca black-box Passed.
- Acceptance Criteria: PB-10, PB-21, PB-18, PB-14, PB-20, PB-17, NFR-04 trong Product Backlog.

## Kết quả trình bày và đánh giá
| Nội dung | Bằng chứng / kết quả kiểm tra | Accepted / Change-Fix Required |
|---|---|---|
| PB-10 Khách hủy đơn (AC1–AC2) | TC-CAN-01 → 07 (gồm biên 11:44/11:45, 5 yêu cầu hủy đồng thời); chạy tay: hủy trên trang đơn | Chờ người dùng quyết |
| PB-21 Cửa hàng hủy đơn (AC1–AC3) | TC-STC-01 → 06; chưa chạy tay ở màn Quầy | Chờ người dùng quyết |
| PB-18 No-show (AC1–AC3) | TC-NOS-01 → 03 (biên 12:44/12:45); chưa chạy tay | Chờ người dùng quyết |
| PB-14 Tự cập nhật (AC1–AC2) | Test frontend; chạy tay: trang khách tự hiện "đã sẵn sàng" | Chờ người dùng quyết |
| PB-20 Tài khoản nhân viên (AC1–AC3) | TC-STF-01 → 07; chạy tay: tạo, khóa, đăng nhập bị từ chối | Chờ người dùng quyết |
| PB-17 Theo dõi (AC1–AC4) | TC-DSH-01 → 04; chạy tay: số liệu đúng sau khi hủy | Chờ người dùng quyết |
| NFR-04 | TC-PERF-01: p95 cao nhất 155–270 ms ở máy dev (giới hạn xem Test Results) | Chờ người dùng quyết |

## Feedback
| Phản hồi | Nội dung cần cập nhật Product Backlog |
|---|---|
| Chưa có (chưa ai khác xem) | — |

## Release
Chưa có release. Product Backlog hiện không còn story nào ở trạng thái Backlog; việc còn lại là phát hành (Docker hóa toàn hệ thống, chạy test lần cuối, tài liệu bàn giao).
