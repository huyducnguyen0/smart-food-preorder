# Sprint 1
Trạng thái: Kế hoạch lập ngày 2026-10-08, người dùng tạm duyệt ngày 2026-10-09 (có thể điều chỉnh khi thực hiện). T-01 và T-03 được làm sớm ngày 2026-10-09, trước ngày bắt đầu Sprint.

Quyết định 2026-10-09: người dùng bỏ estimate theo giờ khỏi Sprint 1 (sức chứa giờ, estimate giờ từng task, giờ thực tế). Lượng việc chọn theo cỡ S-M-L trong Product Backlog và theo dõi qua Task Board. Đây là lựa chọn của người dùng, gọn hơn mục "chia task, estimate" của Bước 6 trong nguồn.

## Input
- [Product Backlog](../02-Requirements/Product-Backlog/PRODUCT_BACKLOG.md) thứ tự 1–10, priority, estimate.
- [SRS](../02-Requirements/SRS/SRS.md) mục B.4–B.5, C, D.
- Công nghệ người dùng chọn ngày 2026-10-08: backend Java Spring Boot, database PostgreSQL, frontend React. Người dùng hiểu khái niệm, chủ yếu lập trình cùng AI, chưa tự code tay; frontend là phần yếu nhất.
- Rủi ro liên quan: RISK-04, RISK-07 (thiết kế quota và phân quyền từ đầu), RISK-08 (một người, chọn việc vừa sức), RISK-09 (backend và frontend tách rời → dựng khung chạy được end-to-end sớm), RISK-12 (môi trường tái lập được).
- Môi trường hiện có (kiểm tra 2026-10-08): Java 25, Node 25, Docker. Repository Git chưa có remote; working tree còn thay đổi chưa commit.

## Sprint Goal
Khách đăng nhập, xem menu, đặt pre-order cho một khung giờ trong ngày và chỉ được xác nhận khi còn quota; khách thấy đơn và mã nhận; bếp thấy hàng chờ và đưa đơn tới READY. Toàn bộ chạy được trên máy local bằng một hướng dẫn.

Mục tiêu dừng ở READY, chưa gồm thanh toán và bàn giao tại quầy như hướng dự kiến trong README. Lý do: Sprint đầu gánh thêm phần thiết lập và thiết kế nền. PB-16 và PB-15 là phần làm thêm nếu dư thời gian, nếu không thì đứng đầu Sprint 2.

## Khoảng thời gian Sprint
- Bắt đầu: Thứ Hai 2026-10-12. Kết thúc: Chủ nhật 2026-10-25. Độ dài: 2 tuần.
- Sprint Review và Retrospective: ngày cuối Sprint.

## Trách nhiệm thực hiện
Người dùng tự thực hiện và kiêm Product Owner, Scrum Master và Developer. AI hỗ trợ soạn tài liệu, viết code, viết test và hướng dẫn; người dùng đọc hiểu, chạy, quyết định và chịu trách nhiệm kết quả.

Cách thực hiện mỗi task đã thống nhất ngày 2026-10-09: AI giải thích khái niệm bằng ví dụ dự án → làm cùng → người dùng tự chạy và kiểm tra → người dùng giải thích lại được rồi mới commit/merge. Code người dùng chưa giải thích được thì không merge.

Hạn chế đã biết: không có người review độc lập. Review PR là tự review kèm AI review, ghi đúng như vậy. Sprint Review là tự đánh giá theo AC, trừ khi thực tế có người khác tham gia.

## Story đã chọn
| Thứ tự | Story | Estimate backlog | Ghi chú phạm vi Sprint |
|---|---|---|---|
| 1 | PB-01 Đăng nhập và phân quyền | M | Tài khoản khách và nhân viên là dữ liệu mẫu; đăng ký (PB-19) ở Sprint sau |
| 2 | PB-02 Xem menu | S | Menu là dữ liệu mẫu; màn quản lý menu (PB-03) ở Sprint sau |
| 3 | PB-05 Giữ quota dùng chung | M | Giờ mở cửa, quota mặc định và điểm món là dữ liệu mẫu; màn cấu hình (PB-04) ở Sprint sau. Thiết kế giữ quota đúng cả khi đồng thời ngay từ đầu; kiểm chứng NFR-01 đầy đủ ở Sprint 2 |
| 4 | PB-06 Giỏ hàng | S | |
| 5 | PB-07 Đặt pre-order | M | Gồm NFR-02 (chống trùng) |
| 6 | PB-08 Xem đơn và mã nhận | S | |
| 7 | PB-12 Hàng chờ bếp | M | |
| 8 | PB-13 Bếp cập nhật tiến độ | S | |
| Làm thêm | PB-16 Ghi nhận thanh toán, PB-15 Bàn giao | S, M | Chỉ bắt đầu khi task cam kết đã Done |

PB-03 và PB-04 vẫn là High nhưng chuyển xuống sau luồng cốt lõi trong Product Backlog: dữ liệu mẫu thay thế được màn quản lý để chạy luồng đặt → bếp, còn giá trị chính của Sprint là luồng đó.

## Sprint Backlog / Task Board
| ID | User Story | Task / nội dung công việc | Người thực hiện | Dependency | Trạng thái |
|---|---|---|---|---|---|
| T-01 | — (WBS 1.4) | Commit trạng thái tài liệu hiện tại; tạo remote GitHub; chốt quy ước nhánh `feature/<PB-id>-<tên>` và PR vào `main` (quy ước ghi ở README mục Git workflow) | Người dùng, AI hỗ trợ | — | Done |
| T-02 | — (WBS 4.1) | Chọn coding convention Java và TypeScript; cấu hình formatter/linter; cập nhật CODING_CONVENTION.md | Người dùng, AI hỗ trợ | T-03 | To Do |
| T-03 | — (WBS 1.4, 4.13) | Khung dự án: Spring Boot, React, PostgreSQL bằng Docker Compose, migration database; frontend gọi được backend (`/api/health`); hướng dẫn chạy trong [03-SourceCodes](../../03-SourceCodes/README.md). Dữ liệu mẫu chuyển sang các task tạo bảng (T-12, T-15, T-16) vì schema thiết kế ở T-09 | Người dùng, AI hỗ trợ | T-01 | Review |
| T-04 | Tất cả | Use Case Model: Actor List, Use Case List toàn hệ thống, sơ đồ tổng quan | Người dùng, AI hỗ trợ | — | To Do |
| T-05 | PB-01, 02, 06, 07, 08, 12, 13 | Use Case Specification: Đăng nhập; Đặt pre-order (gồm xem menu, giỏ); Xem đơn; Xử lý đơn tại bếp | Người dùng, AI hỗ trợ | T-04 | To Do |
| T-06 | PB-07, PB-13 | Activity Diagram: Đặt pre-order; Xử lý đơn tại bếp | Người dùng, AI hỗ trợ | T-05 | To Do |
| T-07 | PB-05, PB-07 | Analysis Model (Boundary/Controller/Entity) và Sequence Diagram cho Đặt pre-order có giữ quota | Người dùng, AI hỗ trợ | T-05 | To Do |
| T-08 | Tất cả | Architecture: tầng backend, module frontend, REST API, Package Diagram, trách nhiệm thành phần (NFR-05) | Người dùng, AI hỗ trợ | T-07 | To Do |
| T-09 | PB-01, 02, 05, 07 | ERD và Relational Schema: tài khoản, món, cấu hình giờ mở cửa/quota, khung, đơn, dòng đơn | Người dùng, AI hỗ trợ | T-07 | To Do |
| T-10 | PB-05, PB-07, PB-13 | Detailed Class Diagram backend phần Sprint 1 | Người dùng, AI hỗ trợ | T-08, T-09 | To Do |
| T-11 | PB-01, 02, 06, 07, 08, 12 | UI wireframe và navigation: đăng nhập, menu và giỏ, chọn khung và đặt, đơn của tôi, hàng chờ bếp | Người dùng, AI hỗ trợ | T-05 | To Do |
| T-12 | PB-01 | Backend: xác thực, mật khẩu băm (NFR-06), vai trò, kiểm tra quyền tại API, tài khoản mẫu | Người dùng, AI hỗ trợ | T-03, T-09 | To Do |
| T-13 | PB-01 | Frontend: đăng nhập, đăng xuất, điều hướng theo vai trò | Người dùng, AI hỗ trợ | T-12, T-11 | To Do |
| T-14 | PB-01 | Test tự động phân quyền: gọi API ngoài vai trò, đơn của khách khác (NFR-03) | Người dùng, AI hỗ trợ | T-12 | To Do |
| T-15 | PB-02 | Dữ liệu menu mẫu, API menu, màn xem menu (BR-04) | Người dùng, AI hỗ trợ | T-12 | To Do |
| T-16 | PB-05 | Service tính điểm và giữ quota trong một transaction (BR-08–BR-14); dữ liệu mẫu giờ mở cửa và quota; unit test | Người dùng, AI hỗ trợ | T-09, T-10 | To Do |
| T-17 | PB-06 | Giỏ hàng frontend | Người dùng, AI hỗ trợ | T-15 | To Do |
| T-18 | PB-07 | API danh sách khung hợp lệ và đặt đơn: BR-12, BR-15, chống trùng BR-17, cấp mã nhận; test | Người dùng, AI hỗ trợ | T-16 | To Do |
| T-19 | PB-07 | Màn chọn khung, gửi đơn, hiển thị kết quả và lỗi | Người dùng, AI hỗ trợ | T-17, T-18 | To Do |
| T-20 | PB-08 | API và màn "Đơn của tôi": trạng thái, mã nhận, trạng thái thanh toán | Người dùng, AI hỗ trợ | T-18 | To Do |
| T-21 | PB-12 | API và màn hàng chờ bếp: nhóm sắp tới / cần chuẩn bị / đang làm, sắp xếp, đánh dấu trễ (BR-21, BR-22, BR-26) | Người dùng, AI hỗ trợ | T-18 | To Do |
| T-22 | PB-13 | Chuyển trạng thái CONFIRMED → PREPARING → READY, từ chối chuyển sai; nút thao tác; test | Người dùng, AI hỗ trợ | T-21 | To Do |
| T-23 | Story đã chọn | Black-box Test Cases từ AC | Người dùng, AI hỗ trợ | T-05 | To Do |
| T-24 | PB-05 | White-box: Control Flow Graph, execution paths, test case cho logic giữ quota | Người dùng, AI hỗ trợ | T-16 | To Do |
| T-25 | Story đã chọn | Chạy test, ghi Test Results, sửa lỗi phát hiện | Người dùng, AI hỗ trợ | T-14, T-22, T-23, T-24 | To Do |
| T-26 | — | Sprint Planning (kế hoạch tạm duyệt 2026-10-09) | Người dùng | — | Done |
| T-27 | — | Daily Scrum, khoảng 10 phút mỗi ngày làm việc | Người dùng | — | To Do |
| T-28 | — | Sprint Review và Retrospective | Người dùng | T-25 | To Do |
| T-29 | PB-16 (làm thêm) | Ghi nhận thanh toán UNPAID → PAID | Người dùng, AI hỗ trợ | T-20 | Backlog |
| T-30 | PB-15 (làm thêm) | Danh sách READY, xác minh mã, bàn giao (BR-29) | Người dùng, AI hỗ trợ | T-22, T-29 | Backlog |

Trạng thái theo nguồn: Backlog → To Do → In Progress → Review → Done. Task code ở Review khi đã mở PR; Done khi đã merge và đạt phần DoD liên quan.

Thứ tự gợi ý: tuần 1 làm T-01–T-16 (thiết lập, thiết kế, đăng nhập, menu, quota); tuần 2 làm T-17–T-25 (đặt đơn, đơn của tôi, bếp, kiểm thử). Thiết kế chỉ ở mức đủ cho phạm vi Sprint, bổ sung ở Sprint sau.

## Daily Scrum
Ghi ngắn mỗi ngày làm việc: đã làm gì hướng tới Sprint Goal, sẽ làm gì, vướng gì. Người dùng tự thực hiện nên đây là tự kiểm tra tiến độ.

| Ngày | Đã làm | Sẽ làm | Vướng mắc / điều chỉnh |
|---|---|---|---|

Nếu đến cuối tuần 1 (2026-10-18) chưa xong T-16, xem lại phạm vi: giữ Sprint Goal, cân nhắc thu gọn T-21 (bỏ đánh dấu trễ sang Sprint 2) trước khi cắt story.

## Definition of Done áp dụng
Theo README: requirement rõ, có AC, Use Case/flow rõ, design đủ implement, code theo convention, đã commit/push, PR đã review (tự review kèm AI review), test case đã thiết kế, test bắt buộc pass, AC đạt, tài liệu liên quan cập nhật, demo được trong Sprint Review.

## Output chuyển giao
Sprint Goal, Sprint Backlog và Task Board để thực hiện Sprint 1. Cập nhật trạng thái task và Daily Scrum trong file này; Sprint Review và Retrospective ghi theo mẫu trong 05-Releases.
