# Sprint Review - Sprint 4
Trạng thái: **Accepted cho bản demo v1.0.0** sau lượt sửa và kiểm thử ngày 2026-11-26. AI thực hiện review kỹ thuật theo yêu cầu hoàn thiện và phát hành của người dùng.

## Input
- Sprint Goal: nhận đơn theo khả năng làm thật của bếp, làm trước lúc vắng, dành chỗ cho walk-in, không vượt công suất khi đồng thời.
- Increment: Sprint 4 và bản sửa PB-22 tích hợp v1.0.0.
- Test results: backend 443, frontend 151, 9 ca TC-PLAN pass. Demo 4 vai trò qua Docker.
- Acceptance Criteria: PB-22 trong Product Backlog.

## Kết quả trình bày và đánh giá
| Nội dung | Bằng chứng / kết quả kiểm tra | Accepted / Change-Fix Required |
|---|---|---|
| AC1 Nhận đơn khi bếp còn xếp kịp trong cửa sổ [S − H, S] | TC-PLAN-01, 04, 07 và `KitchenPlannerTest` | Accepted |
| AC2 Đặt trước tối đa X%, walk-in dùng phần còn lại | TC-PLAN-02, 05 và 09 kiểm một lịch chung, gồm ca 18 + 22 điểm | Accepted |
| AC3 Quầy tối đa N đơn mỗi khung | TC-PLAN-03 | Accepted |
| AC4 Bếp biết nên làm lúc nào, quản lý xem và chỉnh lịch bếp | TC-PLAN-04, 05, 06, test frontend và demo trình duyệt | Accepted |
| AC5 Không vượt công suất khi đồng thời | TC-PLAN-08, TC-CON-08 và ca tranh 10 điểm trên Docker | Accepted |
| NFR-04 sau thay đổi | Bốn thao tác đều dưới 1 giây ở p95 trong môi trường thử nghiệm | Accepted |

## Feedback
| Phản hồi | Nội dung cần cập nhật Product Backlog |
|---|---|
| Người dùng (2026-10-31, trước Sprint): "nếu đến 11h45 mới làm thì nó rất tệ, đó là h đông khách"; "việc làm trước là phải giúp quán giảm quá tải, giúp khách bớt chờ lâu" | Đã thành PB-22 và là nội dung của Sprint này |
| Rà soát kỹ thuật ngày 2026-11-26 phát hiện hai phép kiểm có thể cùng pass nhưng lịch chung bất khả thi. | Đã sửa PB-22 và thêm TC-PLAN-09. |

## Giới hạn của bản demo

- 45 phút, 70%, 8 đơn và 20 điểm là mặc định của mô hình học phần. Hiệu quả vận hành tại cửa hàng còn cần số liệu thực tế.
- Cách xếp theo hạn sớm có thể bỏ qua một phương án nhận thêm đơn. Kết quả nhận dựa trên lịch mà hệ thống tìm được.

## Release
[v1.0.0](README.md). Số liệu và phạm vi chạy trong [Test Results](../04-Testing/Test-Results/README.md).
