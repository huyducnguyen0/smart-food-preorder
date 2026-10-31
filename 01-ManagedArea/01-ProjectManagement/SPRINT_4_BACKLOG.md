# Sprint 4
Trạng thái: Thực hiện ngày 2026-10-31 theo chế độ **"mỗi lần prompt một Sprint"**. Sprint này sinh ra từ phản hồi của người dùng sau review luồng: giờ đông bếp không làm kịp nếu chỉ được làm đơn trong 15 phút trước giờ nhận, nhiều khách walk-in không được phục vụ. Người dùng chọn hướng "lịch bếp + dành chỗ cho walk-in" với giá trị mặc định do AI đề xuất (lệnh "1 làm, 2 làm 3 có lưu vào làm tư liệu").

## Sprint Goal
Hệ thống nhận đơn theo **khả năng làm thật của bếp**: bếp làm trước lúc vắng để giờ đông bớt quá tải, khách tại quầy luôn còn phần công suất dành riêng, quầy không bị dồn quá nhiều khách một khung; không bao giờ nhận vượt công suất khi nhiều người đặt cùng lúc.

## Trách nhiệm
Một người dùng kiêm mọi vai trò; AI soạn phân tích, đặc tả, thiết kế, code, test và tài liệu. Không có review độc lập, không họp. Dựa trên nhánh `fix/review-findings` (PR #28); các PR #22 → #28 chưa merge lúc làm Sprint 4.

## Input
- Phân tích và mô phỏng: [02-FreeZone/2026-10-31_lich-bep](../../02-FreeZone/2026-10-31_lich-bep/README.md). Mô phỏng (mô hình giả định, không phải số đo cửa hàng thật) cho thấy với nhu cầu 25 đặt trước + 25 walk-in, số đơn phục vụ được tăng từ khoảng 37 lên 43, walk-in từ khoảng 13 lên 20.
- Quyết định của người dùng (2026-10-31): làm trước tối đa H = 45 phút, đặt trước dùng tối đa 70% công suất, quầy tối đa 8 đơn mỗi khung, công suất giữ như quota hiện tại (20 điểm/15 phút); quản lý đổi được cả bốn.

## Story và task
| Story | Use case | Task | Trạng thái |
|---|---|---|---|
| PB-22 Lịch bếp | [UC-17](../02-Requirements/Use-Cases/specifications/UC-17-kiem-tra-va-giu-quota.md), UC-04, UC-07, UC-12, UC-14, UC-16 | BR-09/11/12/14/18 sửa, BR-39–41 mới; migration V12; `KitchenPlanner`, `KitchenPlanService`; viết lại `QuotaService.reserve`, khung hợp lệ, walk-in, cấu hình của quản lý, hàng chờ bếp; màn quản lý và bếp; test TC-PLAN, sửa ca cũ | Review |
| — | — | Architecture, Database, Class Design, white-box, Test Results, nháp Review và Retro, trang "để hiểu" | Review |

## Daily Scrum
Không tổ chức. Sprint làm trong một buổi (2026-10-31) bằng một lần giao việc cho AI.

## Definition of Done áp dụng
Như Sprint 2. Thêm: ca đồng thời chạy với cấu hình mặc định mới (TC-PLAN-08).

## Kết quả
Xem [Test Results](../04-Testing/Test-Results/README.md), lượt "2026-10-31: Sprint 4". Review và Retro (nháp): [05-Releases](../05-Releases/).
