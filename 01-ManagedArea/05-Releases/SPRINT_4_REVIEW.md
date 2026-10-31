# Sprint Review - Sprint 4
Trạng thái: **Nháp do AI soạn ngày 2026-10-31, người dùng chưa duyệt.** Đây là tự đánh giá theo Acceptance Criteria; chưa có ai khác xem demo.

## Input
- Sprint Goal: nhận đơn theo khả năng làm thật của bếp, làm trước lúc vắng, dành chỗ cho walk-in, không vượt công suất khi đồng thời.
- Increment: PR Sprint 4 (dựa trên PR #28).
- Test results: backend 442, frontend 150 test pass; 8 ca TC-PLAN và 5 ca cũ đổi nội dung Passed.
- Acceptance Criteria: PB-22 trong Product Backlog.

## Kết quả trình bày và đánh giá
| Nội dung | Bằng chứng / kết quả kiểm tra | Accepted / Change-Fix Required |
|---|---|---|
| AC1 Nhận đơn khi bếp còn xếp kịp trong cửa sổ [S − H, S] | TC-PLAN-01 (khung 12:00 nhận 40 điểm thay vì 20), TC-PLAN-07; `KitchenPlannerTest` | Chờ người dùng quyết |
| AC2 Đặt trước tối đa X%, walk-in dùng phần còn lại | TC-PLAN-02; `KitchenPlannerTest#preOrderShare` | Chờ người dùng quyết |
| AC3 Quầy tối đa N đơn mỗi khung | TC-PLAN-03 | Chờ người dùng quyết |
| AC4 Bếp biết nên làm lúc nào; quản lý xem và chỉnh lịch bếp | TC-PLAN-04, 05, 06; test frontend; **chưa chạy tay trên trình duyệt** | Chờ người dùng quyết |
| AC5 Không vượt công suất khi đồng thời | TC-PLAN-08 (80 yêu cầu), TC-CON-08 (50 yêu cầu) | Chờ người dùng quyết |
| NFR-04 sau thay đổi | p95 đặt đơn 583 ms (trước 448 ms), vẫn dưới 1 giây | Chờ người dùng quyết |

## Feedback
| Phản hồi | Nội dung cần cập nhật Product Backlog |
|---|---|
| Người dùng (2026-10-31, trước Sprint): "nếu đến 11h45 mới làm thì nó rất tệ, đó là h đông khách"; "việc làm trước là phải giúp quán giảm quá tải, giúp khách bớt chờ lâu" | Đã thành PB-22 và là nội dung của Sprint này |
| Chưa có phản hồi sau khi xem bản đã làm | — |

## Điểm cần người dùng quyết khi duyệt
- Bốn giá trị mặc định (45 phút, 70%, 8 đơn, 20 điểm) là đề xuất, chưa đối chiếu cửa hàng thật.
- Cách xếp lịch đơn giản, hiếm khi từ chối một đơn mà cách xếp khéo hơn vẫn nhận được (không bao giờ nhận vượt).

## Release
Chưa có release. Sau khi các PR được merge, cần chạy lại bản Docker và `scripts/demo-data.sh` dưới cấu hình mới.
