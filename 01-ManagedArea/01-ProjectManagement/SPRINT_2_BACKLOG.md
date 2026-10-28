# Sprint 2
Trạng thái: Thực hiện ngày 2026-10-28 theo chế độ **"mỗi lần prompt một Sprint"** người dùng chọn cùng ngày: AI lập kế hoạch gọn, làm hết các story, chạy test, soạn nháp Review và Retrospective trong một lần; người dùng duyệt một PR ở cuối. Kế hoạch kiểu duyệt từng bước (PR #24) đã đóng, không dùng.

## Sprint Goal
Cửa hàng **tự vận hành không cần dữ liệu mẫu** (quản lý tự sửa món, giờ mở cửa, quota; khách tự đăng ký) và **nhận khách tại quầy (walk-in) dùng chung quota** với khách đặt trước, không bao giờ nhận vượt quota khi hai kênh cùng đặt. Khách được gợi ý khung khác khi khung đã chọn vừa hết chỗ.

## Trách nhiệm
Một người dùng kiêm mọi vai trò; AI soạn đặc tả, thiết kế, code, test và tài liệu. Không có review độc lập, không họp, chưa ai khác xem demo. Dựa trên PR #22, #23 (T-29, T-30) vì PB-11 cần thanh toán; hai PR đó chưa merge lúc làm Sprint 2.

## Story và task
| Story | Use case | Task | Trạng thái |
|---|---|---|---|
| PB-03 Quản lý món | [UC-13](../02-Requirements/Use-Cases/specifications/UC-13-quan-ly-mon.md) | Đặc tả, API `/api/admin/menu/**`, màn `/admin/menu`, test TC-MNG | Review |
| PB-04 Cấu hình giờ và quota | [UC-14](../02-Requirements/Use-Cases/specifications/UC-14-cau-hinh-gio-va-quota.md) | Đặc tả, API cấu hình, khóa chống chen ngang (BR-11), màn `/admin/slots`, test TC-CFG | Review |
| PB-19 Đăng ký | [UC-01](../02-Requirements/Use-Cases/specifications/UC-01-dang-ky-tai-khoan.md) | Đặc tả, `POST /api/auth/register`, màn `/register`, test TC-REG | Review |
| PB-11 Walk-in | [UC-07](../02-Requirements/Use-Cases/specifications/UC-07-tao-don-walk-in.md) | Đặc tả, migration V8, API ước tính và xác nhận, khung tạo đơn ở màn Quầy, test TC-WALK | Review |
| NFR-01 | UC-07, UC-17 | 25 pre-order + 25 walk-in cùng lúc vào một khung (TC-CON-08) | Review |
| PB-09 Khung thay thế | [UC-18](../02-Requirements/Use-Cases/specifications/UC-18-de-xuat-khung-thay-the.md) | Đặc tả, `alternatives` trong lỗi `SLOT_FULL`, khung gợi ý ở màn chọn giờ, test TC-ALT | Review |
| — | — | Chạy toàn bộ test, Test Results, nháp Review và Retro, trang "để hiểu" | Review |

## Daily Scrum
Không tổ chức. Sprint làm trong một buổi (2026-10-28) bằng một lần giao việc cho AI.

## Definition of Done áp dụng
Như Sprint 1, với hai điểm khác ghi đúng thực tế: ca test viết cùng lúc với code (không trước); PR là một PR cho cả Sprint, người dùng duyệt một lần.

## Kết quả
Backend 399 test, frontend 129 test, tất cả pass; 61 ca black-box Sprint 2 (kèm ca T-29, T-30) Passed. Chi tiết: [Test Results](../04-Testing/Test-Results/README.md). Review và Retro (nháp): [05-Releases](../05-Releases/).
