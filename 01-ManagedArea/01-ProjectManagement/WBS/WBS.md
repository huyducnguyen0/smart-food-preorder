# Work Breakdown Structure
Trạng thái: Bản phân rã ban đầu ngày 2026-09-29, chưa được người dùng review toàn bộ; chưa estimate hoặc phân công. Các gói triển khai còn phụ thuộc yêu cầu sẽ được chia nhỏ tiếp trước khi chọn vào Sprint.

## Input
- [Quy trình IT3180](../../../00-References/IT3180_PROJECT_PROCESS.md), Bước 2: phân rã theo sản phẩm/quy trình đến work package có thể estimate, assign và track.
- [Project Charter](../Project-Charter/PROJECT_CHARTER.md), mục 4.3–4.4: sản phẩm bàn giao và phạm vi nghiệp vụ đã được người dùng duyệt về nội dung. Chưa coi toàn bộ Charter được phê duyệt.

## Cách phân rã và sử dụng

Phân rã cấp đầu theo nhóm công việc của quy trình; trong phần xây dựng phần mềm, chia tiếp theo chức năng thuộc scope. Mỗi dòng có ID và output để theo dõi, nhưng không tự tạo thêm loại tài liệu.

- WBS trả lời toàn dự án phải làm những phần việc nào. Product Backlog xác định giá trị/ưu tiên; Sprint Backlog chọn và chia task thực hiện trong từng Sprint.
- Số thứ tự là mã phân rã, không phải lịch tuần hay số Sprint. Một Sprint lấy việc yêu cầu, thiết kế, code và test liên quan tới cùng mục tiêu sản phẩm.
- Output thiết kế dùng chung nằm ở nhóm 3; mã nguồn hiện thực nằm ở nhóm 4; thiết kế/bằng chứng kiểm thử chính thức nằm ở nhóm 5. Local test và sửa code trong nhóm 4 hỗ trợ kiểm thử, không thay thế nhóm 5.
- Chưa có cơ sở gán giờ, story point, nhân sự hay công nghệ. Khi yêu cầu rõ hơn, chia các gói lớn thành task vừa sức trong Sprint Backlog; không coi bản WBS này đã hoàn tất estimation/planning.

## Cấu trúc tổng quan

```text
0. Dự án đặt món trước và điều phối nhận món
├── 1. Quản lý và tổ chức thực hiện
├── 2. Yêu cầu và mô hình nghiệp vụ
├── 3. Phân tích và thiết kế
├── 4. Xây dựng phần mềm
├── 5. Kiểm thử và kết quả
└── 6. Review, release và cải tiến
```

## 1. Quản lý và tổ chức thực hiện

| ID | Phần việc | Output / phạm vi công việc |
|---|---|---|
| 1.1 | Tổng hợp tiền khả thi và Charter | Bối cảnh, mục tiêu, scope, stakeholder, giả định/ràng buộc trong Charter; cập nhật thông tin còn thiếu đúng thực tế |
| 1.2 | Phân rã công việc | WBS này; cập nhật khi scope hoặc mức chi tiết thay đổi |
| 1.3 | Nhận diện, phân tích và xử lý rủi ro | Risk Register dựa trên Charter và WBS; theo dõi xuyên dự án |
| 1.4 | Tổ chức repository và quản lý phiên bản | Cấu trúc project, quy ước, truy cập và Git workflow dùng được |
| 1.5 | Lập kế hoạch và theo dõi từng Sprint | Sprint Goal, Sprint Backlog, Task Board; cập nhật tiến độ/blocker và cách phối hợp theo Charter |

## 2. Yêu cầu và mô hình nghiệp vụ

| ID | Phần việc | Output / phạm vi công việc |
|---|---|---|
| 2.1 | Làm rõ nghiệp vụ pre-order, walk-in, bếp và pickup | Business process và quy tắc trong SRS, ghi rõ cơ sở lựa chọn của mô hình |
| 2.2 | Xây và duy trì Product Backlog | User Stories, Acceptance Criteria, priority, estimate và status từ nhu cầu đã làm rõ |
| 2.3 | Đặc tả chức năng | Phần chức năng SRS bao phủ scope Charter, xử lý ngoại lệ và dữ liệu liên quan |
| 2.4 | Đặc tả NFR và constraint | Điều kiện kiểm chứng nhất quán quota, retry, phân quyền, hiệu năng và các ràng buộc đã thống nhất |
| 2.5 | Xây dựng Use Case Model | Actor List, Use Case List, sơ đồ tổng quan/phân rã khi cần |
| 2.6 | Đặc tả từng Use Case thuộc phạm vi | Điều kiện trước/sau, main flow, alternative/error flow liên kết SRS |
| 2.7 | Mô hình hóa luồng hành động | Activity Diagrams từ đặc tả Use Case |

## 3. Phân tích và thiết kế

| ID | Phần việc | Output / phạm vi công việc |
|---|---|---|
| 3.1 | Phân tích trách nhiệm | Analysis Class Model và phân bổ Boundary/Controller/Entity |
| 3.2 | Phân tích tương tác | Sequence Diagrams cho các luồng cần triển khai |
| 3.3 | Thiết kế kiến trúc | Architecture, Package Diagram và trách nhiệm thành phần dựa trên Use Case/NFR |
| 3.4 | Thiết kế dữ liệu khái niệm | ERD với entity, attribute và relationship |
| 3.5 | Thiết kế dữ liệu logic | Relational Schema, khóa và quan hệ phù hợp quy tắc đã đặc tả |
| 3.6 | Thiết kế class chi tiết | Class Diagram với thuộc tính, phương thức, quan hệ và trách nhiệm |
| 3.7 | Thiết kế giao diện và điều hướng | Layout, wireframe, bản phối màu và navigation của các vai trò trong scope |
| 3.8 | Đánh giá và chỉnh prototype | Prototype Evaluation, feedback thực tế và bản UI được chấp thuận khi có bằng chứng |

## 4. Xây dựng phần mềm

Các dòng dưới là gói chức năng ban đầu, không phải kiến trúc module hoặc một task code duy nhất. Phân rã tiếp theo Use Case/thiết kế đã làm rõ, không tự chốt business rule trong WBS.

| ID | Phần việc | Output / giới hạn |
|---|---|---|
| 4.1 | Thống nhất Coding Convention | Convention theo ngôn ngữ/công nghệ được chọn, dùng khi implement và review |
| 4.2 | Đăng nhập và kiểm soát truy cập | Chức năng đăng nhập và quyền thao tác theo SRS |
| 4.3 | Menu và giỏ hàng | Quản lý/xem món, tình trạng bán, thông tin chuẩn bị và giỏ hàng; không quản lý tồn kho |
| 4.4 | Tạo đơn pre-order | Chọn thời gian nhận dự kiến và gửi đơn theo điều kiện được đặc tả; tích hợp kiểm tra/giữ quota ở 4.6 |
| 4.5 | Tiếp nhận walk-in | Quầy nhập đơn, thông báo thời gian dự kiến, xác nhận sau khi khách đồng ý; không mở rộng thành full POS |
| 4.6 | Quota và đề xuất thời gian | Cấu hình/kiểm tra/giữ quota nhất quán cho hai kênh, bảo vệ mọi đơn đã nhận, đề xuất thời gian phù hợp và chống tạo đơn trùng |
| 4.7 | Theo dõi đơn và hủy theo chính sách | Hành vi cập nhật/theo dõi, hủy và xử lý quota theo SRS; không mặc định cách hoàn quota |
| 4.8 | Hàng chờ và tiến độ bếp | Hiển thị đơn sắp tới/cần chuẩn bị, cập nhật tiến độ và sẵn sàng; không quản lý batch/thiết bị |
| 4.9 | Thông báo sẵn sàng | Chức năng thông báo cho khách bằng kênh sẽ được lựa chọn trong yêu cầu/thiết kế |
| 4.10 | Xác minh và bàn giao | Xem đơn sẵn sàng, kiểm tra mã, ghi nhận hoàn tất theo quyền và điều kiện SRS |
| 4.11 | Ghi nhận trạng thái thanh toán | Lưu/cập nhật trạng thái, liên kết quy trình đơn theo SRS; chưa chọn phương thức, không tích hợp gateway production |
| 4.12 | Theo dõi vận hành | Hiển thị tải theo quota, đơn xử lý/đơn trễ và thống kê cơ bản; không tuyên bố đo năng lực bếp thực chính xác |
| 4.13 | Tích hợp và review mã nguồn | Luồng giữa các chức năng hoạt động chung; commit, local test, PR/review/merge đúng hoạt động thực tế |

## 5. Kiểm thử và kết quả

| ID | Phần việc | Output / phạm vi công việc |
|---|---|---|
| 5.1 | Thiết kế white-box | Control Flow Graph, execution paths và test case từ logic code được chọn |
| 5.2 | Thiết kế black-box | Test case theo SRS/Use Case/Acceptance Criteria; luồng thành công, biên và ngoại lệ |
| 5.3 | Kiểm chứng các NFR | Test cho đặt đồng thời, retry, phân quyền và hiệu năng với workload/môi trường xác định trong yêu cầu |
| 5.4 | Chạy và ghi kết quả | Expected/Actual, Passed/Failed và bằng chứng trong Test Results |
| 5.5 | Sửa lỗi và kiểm tra lại | Code/tài liệu liên quan được sửa; kết quả chạy lại và kiểm tra phần bị ảnh hưởng |

## 6. Review, release và cải tiến

| ID | Phần việc | Output / phạm vi công việc |
|---|---|---|
| 6.1 | Sprint Review | Demo increment, đối chiếu mục tiêu/AC, ghi Accepted hoặc Change/Fix Required và feedback thực tế |
| 6.2 | Release increment | Phiên bản chạy/demo có thể sử dụng, gắn với code và kết quả kiểm chứng; chỉ phát hành khi đủ điều kiện theo nguồn |
| 6.3 | Retrospective | Hành động cải tiến cụ thể áp dụng cho Sprint sau |
| 6.4 | Xử lý feedback và công việc tiếp theo | Product Backlog cập nhật; chuyển sang Sprint tiếp theo hoặc công việc bảo trì trong phạm vi khi cần |

## Phụ thuộc chính và điểm chưa được chốt

- Charter và WBS là input cho Risk Management; rủi ro ảnh hưởng ưu tiên/thiết kế sau đó.
- 2.1 cung cấp cơ sở cho 2.2; trong từng Sprint, 2.3–2.7 tiếp tục làm rõ phần yêu cầu được chọn.
- Nhóm 3 dùng yêu cầu/Use Case tương ứng; nhóm 4 cần thiết kế đủ rõ cho phần đang implement.
- Test black-box có thể thiết kế từ yêu cầu trước code; white-box cần logic code; chạy test cần bản phần mềm tương ứng.
- 6.1–6.2 dựa trên increment và kết quả nhóm 5; 6.3–6.4 quay lại kế hoạch/backlog cho vòng tiếp theo.
- Slot, workload, thời điểm chuẩn bị, ưu tiên bếp, hủy/no-show, hoàn quota và phương thức thanh toán vẫn thuộc phần yêu cầu cần làm rõ. WBS chỉ ghi công việc xử lý chúng, không đặt giá trị hoặc công thức.
- Giới hạn quota riêng dành cho pre-order được trao đổi sau Charter vẫn chưa được duyệt; không đưa thành chức năng bắt buộc hoặc thay đổi scope ở bản này.

## Đối chiếu phạm vi và trạng thái

| Đầu ra/phạm vi Charter | Vị trí trong WBS |
|---|---|
| Artifact quản lý, repository và Scrum | 1.1–1.5, 2.2, 6.1, 6.3–6.4 |
| SRS, Use Case, Activity | 2.1, 2.3–2.7 |
| Analysis, Sequence, Architecture, DB, Class, UI | 3.1–3.8 |
| Khách pre-order, walk-in, menu và quyền | 4.2–4.5, 4.7 |
| Quota dùng chung và tính nhất quán khi xác nhận | 4.6, 4.13, 5.3 |
| Bếp, thông báo, pickup | 4.8–4.10 |
| Trạng thái thanh toán, quan sát vận hành | 4.11–4.12 |
| Convention, source code và lịch sử phát triển | 4.1–4.13 |
| White-box/black-box và kết quả | 5.1–5.5 |
| Bản chạy demo, review, release và feedback | 6.1–6.4 |

Không có gói full POS, inventory/batch, tối ưu bếp, delivery, marketplace hoặc gateway production. Các nhóm ở trên không phải tài liệu mới ngoài bộ nguồn.

Đã có khung repository và Charter với nội dung nghiệp vụ được duyệt; WBS hiện là bản phân rã ban đầu. Các dòng còn lại mô tả công việc cần làm, không chứng nhận đã thực hiện. Estimate, owner và task chi tiết sẽ được bổ sung khi đủ đầu vào; chưa coi WBS đã sẵn sàng giao tất cả gói triển khai ngay.

## Output chuyển giao

WBS này phục vụ estimation, planning, risk identification và task decomposition. Bước kế tiếp là lập Risk Register từ Charter và các nhóm công việc trên; sau đó thu thập/phân tích yêu cầu, Product Backlog và Sprint Planning theo nguồn. Khi yêu cầu hoặc scope thay đổi, cập nhật WBS thay vì giữ một bản phân rã không còn phản ánh dự án.
