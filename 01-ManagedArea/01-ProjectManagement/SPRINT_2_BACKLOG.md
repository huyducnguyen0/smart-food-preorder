# Sprint 2
Trạng thái: **Bản nháp chờ người dùng duyệt**, AI soạn ngày 2026-10-28 theo các quyết định người dùng chọn cùng ngày (mục "Quyết định khi lập kế hoạch"). Chưa bắt đầu task nào.

## Quyết định khi lập kế hoạch (người dùng chọn, 2026-10-28)
| Câu hỏi | Lựa chọn |
|---|---|
| Quy trình duyệt | **Chặt hơn Sprint 1**: người dùng duyệt kế hoạch, đặc tả từng story, thiết kế từng story và từng PR trước khi merge |
| Phạm vi | PB-03, PB-04, PB-19, PB-11; PB-09 làm thêm nếu kịp |
| Đóng Sprint 1 | Bỏ qua T-27, T-28 của Sprint 1, vào Sprint 2 ngay. Hai task đó để trống, chưa làm |
| Thành viên | Vẫn một mình người dùng, AI hỗ trợ |
| Ngày tháng | Người dùng không dùng hạn ngày làm điều kiện (quyết định trước đó); Sprint kết thúc khi phạm vi cam kết đạt Definition of Done |

## Input
- [Product Backlog](../02-Requirements/Product-Backlog/PRODUCT_BACKLOG.md) thứ tự 11–15 và NFR-01.
- [SRS](../02-Requirements/SRS/SRS.md), [Use Case Model](../02-Requirements/Use-Cases/README.md) (UC-01, UC-07, UC-13, UC-14, UC-18 chưa có đặc tả chi tiết).
- Kết quả Sprint 1: luồng đặt → giữ quota → bếp → thu tiền → bàn giao chạy được; 360 test backend, 111 test frontend ([Test Results](../04-Testing/Test-Results/README.md)).
- Điều Sprint 1 cho thấy (người dùng nhìn lại ngày 2026-10-26, chưa ghi thành Retrospective chính thức): một người làm, AI viết cả code lẫn test và tự review, chưa ai khác xem demo, tốc độ làm nhanh hơn mức người dùng kịp hiểu.

## Điều kiện trước khi bắt đầu
- PR #22 (PB-16 thanh toán) và #23 (PB-15 bàn giao) của Sprint 1 **chưa merge**. PB-11 walk-in cần PB-16 (ghi nhận thanh toán khi xác nhận, BR-19, BR-27), nên phải merge #22 trước khi làm PB-11. Người dùng duyệt và merge.

## Sprint Goal
Cửa hàng **tự vận hành mà không cần dữ liệu mẫu**: quản lý tự quản lý món và cấu hình giờ mở cửa, quota; khách tự đăng ký; nhân viên quầy nhận khách mua trực tiếp (walk-in) dùng **chung một quota** với khách đặt trước, không bao giờ nhận vượt quota khi hai kênh đặt cùng lúc.

## Trách nhiệm thực hiện
Người dùng kiêm Product Owner, Scrum Master và Developer. AI soạn tài liệu, viết code và test, giải thích. Người dùng đọc, chạy thử, quyết định và chịu trách nhiệm kết quả.

Hạn chế đã biết, ghi đúng như vậy: không có người review độc lập; review PR là người dùng tự review kèm AI giải thích; test do AI viết nên có chung điểm mù với code.

## Cách làm mỗi story (quy trình duyệt "chặt hơn")
| Bước | Ai | Đầu ra | Điểm duyệt |
|---|---|---|---|
| 1. Đặc tả use case (luồng chính, thay thế, lỗi) và **ca test black-box viết trước code** | AI soạn | `UC-xx.md`, thêm ca test vào tài liệu test case | **Người dùng duyệt** (PR tài liệu) |
| 2. Thiết kế phần thay đổi: bảng và migration, API và mã lỗi, màn hình | AI soạn | Cập nhật Database Design, Architecture, UI Prototype | **Người dùng duyệt** (PR tài liệu) |
| 3. Code và test, mỗi task một nhánh | AI | PR code | |
| 4. Người dùng đọc PR, chạy thử, giải thích lại phần chính bằng lời của mình | Người dùng | Duyệt hoặc yêu cầu sửa | **Người dùng duyệt** trước khi merge |
| 5. Merge, chuyển task sang Done | Người dùng (hoặc bảo AI) | | |

Mỗi đợt AI chỉ làm 1–2 task rồi dừng để người dùng xem. AI không tự merge.

## Story đã chọn
| Thứ tự | Story | Use case | Estimate | Ghi chú phạm vi |
|---|---|---|---|---|
| 1 | PB-03 Quản lý món và tình trạng bán | UC-13 | M | Thêm, sửa, còn bán/tạm hết, ngừng bán; đơn đã xác nhận giữ giá và điểm cũ (BR-05, BR-08). Không quản lý tồn kho |
| 2 | PB-04 Cấu hình giờ mở cửa và quota | UC-14 | M | Không đặt quota thấp hơn điểm đã giữ; không thu hẹp giờ làm mất khung có đơn (BR-11) |
| 3 | PB-19 Khách tự đăng ký | UC-01 | S | Email hoặc SĐT chưa dùng; mật khẩu băm (NFR-06) |
| 4 | PB-11 Đơn walk-in tại quầy | UC-07 | M | Cần PB-16 đã merge. Gồm kiểm chứng **NFR-01 đầy đủ** khi hai kênh cùng đặt |
| Làm thêm | PB-09 Đề xuất khung thay thế | UC-18 | M | Chỉ bắt đầu khi 4 story trên đã Done |

## Sprint Backlog / Task Board
| ID | Story | Task | Điểm duyệt | Dependency | Trạng thái |
|---|---|---|---|---|---|
| T-31 | — | Lập kế hoạch Sprint 2 (file này) | Người dùng duyệt | — | Review |
| T-32 | PB-03 | Đặc tả UC-13 và ca test black-box | Đặc tả | T-31 | To Do |
| T-33 | PB-03 | Thiết kế: API quản lý món, màn quản lý món, thay đổi database nếu có | Thiết kế | T-32 | To Do |
| T-34 | PB-03 | Backend quản lý món và test | PR | T-33 | To Do |
| T-35 | PB-03 | Màn quản lý món và test | PR | T-34 | To Do |
| T-36 | PB-04 | Đặc tả UC-14 và ca test black-box | Đặc tả | T-31 | To Do |
| T-37 | PB-04 | Thiết kế: API cấu hình, màn cấu hình giờ và quota | Thiết kế | T-36 | To Do |
| T-38 | PB-04 | Backend cấu hình (BR-06, BR-07, BR-11) và test | PR | T-37 | To Do |
| T-39 | PB-04 | Màn cấu hình và test | PR | T-38 | To Do |
| T-40 | PB-19 | Đặc tả UC-01 và ca test black-box | Đặc tả | T-31 | To Do |
| T-41 | PB-19 | Thiết kế: API đăng ký, màn đăng ký | Thiết kế | T-40 | To Do |
| T-42 | PB-19 | Backend và màn đăng ký, test | PR | T-41 | To Do |
| T-43 | PB-11 | Đặc tả UC-07 và ca test black-box | Đặc tả | T-31, PR #22 đã merge | To Do |
| T-44 | PB-11 | Thiết kế: khung dự kiến sớm nhất (BR-18), xác nhận khi khách đồng ý, chống trùng cho walk-in | Thiết kế | T-43 | To Do |
| T-45 | PB-11 | Backend walk-in dùng chung quota và test | PR | T-44 | To Do |
| T-46 | PB-11 | Màn tạo đơn walk-in ở quầy và test | PR | T-45 | To Do |
| T-47 | NFR-01 | Kiểm chứng đầy đủ: 50 yêu cầu đồng thời từ hai kênh vào cùng khung, ghi kết quả | PR | T-45 | To Do |
| T-48 | PB-09 (làm thêm) | Đặc tả và thiết kế UC-18 | Đặc tả, thiết kế | T-31 | Backlog |
| T-49 | PB-09 (làm thêm) | Code đề xuất tối đa 3 khung (BR-16) và test | PR | T-48 | Backlog |
| T-50 | — | Chạy toàn bộ test, ghi Test Results, sửa lỗi | PR | T-35, T-39, T-42, T-46, T-47 | To Do |
| T-51 | — | Daily Scrum theo từng buổi làm việc thật | Người dùng xác nhận | — | To Do |
| T-52 | — | Sprint Review (demo cho **ít nhất một người khác**) và Retrospective | Người dùng | T-50 | To Do |

Trạng thái: Backlog → To Do → In Progress → Review → Done. Task tài liệu ở Review khi đã mở PR chờ duyệt; Done khi người dùng duyệt và merge.

Thứ tự đề xuất: PB-03 trước (bỏ phụ thuộc vào menu mẫu), rồi PB-04, PB-19, cuối cùng PB-11 vì phức tạp nhất và cần PB-16. Các bước đặc tả của nhiều story có thể làm sớm để người dùng duyệt gộp một lần.

## Daily Scrum
Ghi theo **buổi làm việc thật**, không ghi theo lịch. Mỗi buổi 3 ý: đã làm gì hướng tới Sprint Goal, sẽ làm gì, vướng gì. AI soạn nháp từ những gì đã xảy ra trong buổi; người dùng sửa và xác nhận trước khi ghi. Buổi nào không làm thì không ghi.

| Buổi | Đã làm | Sẽ làm | Vướng mắc / điều chỉnh |
|---|---|---|---|

## Definition of Done áp dụng
Như Sprint 1 (README), cộng thêm:
- Đặc tả và thiết kế của story đã được người dùng duyệt **trước** khi code.
- Ca test black-box viết từ đặc tả **trước** khi code, và chạy pass.
- Người dùng đã đọc PR, tự chạy thử, giải thích lại được phần chính trước khi merge.
- Tài liệu liên quan (đặc tả, thiết kế, hợp đồng API, Test Results) cập nhật trong cùng PR.

## Rủi ro chính của Sprint
| Rủi ro | Xử lý |
|---|---|
| Một người làm, review không độc lập | Duyệt ở 3 điểm; nếu có thể, nhờ một người xem demo ở Sprint Review |
| Đổi quota hoặc giờ mở cửa khi đã có đơn giữ quota (BR-11) | Thiết kế ràng buộc ở cả service và database, có test |
| Walk-in và pre-order tranh cùng quota | Dùng lại `QuotaService` (một câu `UPDATE` có điều kiện), kiểm chứng NFR-01 ở T-47 |
| Không có hạn ngày nên Sprint kéo dài | Mỗi đợt dừng lại cho người dùng xem; người dùng có thể thu hẹp phạm vi bất cứ lúc nào |

## Output chuyển giao
Sprint Goal, Sprint Backlog và Task Board để thực hiện Sprint 2. Cập nhật trạng thái task và Daily Scrum trong file này; Sprint Review và Retrospective ghi theo mẫu trong 05-Releases.
