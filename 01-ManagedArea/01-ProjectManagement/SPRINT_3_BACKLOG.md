# Sprint 3
Trạng thái: Thực hiện ngày 2026-10-28 theo chế độ **"mỗi lần prompt một Sprint"** (như Sprint 2): AI lập kế hoạch gọn, làm hết các story, chạy test, soạn nháp Review và Retrospective trong một lần; người dùng duyệt một PR ở cuối.

## Sprint Goal
Xử lý **ngoại lệ của đơn** (khách hủy, cửa hàng hủy kèm lý do, khách không đến nhận) mà quota luôn đúng; khách **không phải tải lại** để biết đơn đã sẵn sàng; quản lý **tự quản lý nhân viên** và **nhìn được tải trong ngày**. Đo hiệu năng NFR-04.

## Trách nhiệm
Một người dùng kiêm mọi vai trò; AI soạn đặc tả, thiết kế, code, test và tài liệu. Không có review độc lập, không họp, chưa ai khác xem demo. Dựa trên nhánh Sprint 2 (PR #25) và PR #22, #23; các PR đó chưa merge lúc làm Sprint 3.

## Story và task
| Story | Use case | Task | Trạng thái |
|---|---|---|---|
| PB-10 Khách hủy đơn | [UC-06](../02-Requirements/Use-Cases/specifications/UC-06-khach-huy-don.md) | Đặc tả, migration V9, `QuotaService.release`, `POST /api/orders/{id}/cancel`, nút hủy ở trang đơn, test TC-CAN | Review |
| PB-21 Cửa hàng hủy đơn | [UC-11](../02-Requirements/Use-Cases/specifications/UC-11-cua-hang-huy-don.md) | Đặc tả, API hủy kèm lý do theo quyền, ô lý do ở màn Quầy, lý do ở trang đơn khách, test TC-STC | Review |
| PB-18 No-show | [UC-10](../02-Requirements/Use-Cases/specifications/UC-10-danh-dau-no-show.md) | Đặc tả, cấu hình thời gian chờ, cờ `overdue`, API no-show, nút ở màn Quầy, test TC-NOS | Review |
| PB-14 Tự cập nhật trang đơn | [UC-05](../02-Requirements/Use-Cases/specifications/UC-05-xem-don-cua-toi.md) (Ghi chú) | Gọi lại mỗi 15 giây khi còn đơn chưa kết thúc, thông báo khi `READY`, test frontend | Review |
| PB-20 Tài khoản nhân viên | [UC-15](../02-Requirements/Use-Cases/specifications/UC-15-quan-ly-tai-khoan-nhan-vien.md) | Đặc tả, API `/api/admin/staff`, màn `/admin/staff`, test TC-STF | Review |
| PB-17 Theo dõi vận hành | [UC-16](../02-Requirements/Use-Cases/specifications/UC-16-theo-doi-van-hanh.md) | Đặc tả, module `report`, `GET /api/admin/dashboard`, màn `/admin`, gom tab quản lý, test TC-DSH | Review |
| NFR-04 | UC-03, UC-04, UC-12, UC-09 | Test đo 50 người dùng đồng thời qua HTTP thật (TC-PERF-01) | Review |
| — | — | Chạy toàn bộ test, Test Results, nháp Review và Retro, trang "để hiểu" | Review |

## Daily Scrum
Không tổ chức. Sprint làm trong một buổi (2026-10-28) bằng một lần giao việc cho AI.

## Definition of Done áp dụng
Như Sprint 2.

## Kết quả
Xem [Test Results](../04-Testing/Test-Results/README.md), lượt "2026-10-28: Sprint 3". Review và Retro (nháp): [05-Releases](../05-Releases/).
