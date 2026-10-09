# Nhóm 15 - Project IT3180
Hệ thống đặt món trước và điều phối nhận món theo năng lực phục vụ.

## Làm việc cùng AI

[AGENTS.md](AGENTS.md) quy định vai trò kỹ sư thực thi kiêm người hướng dẫn intern, nguồn quy trình và cách cộng tác trong repository. Đây là chỉ dẫn cho công cụ, không phải artifact bàn giao của môn. Trạng thái ở phần dưới; quyết định nghiệp vụ và kỹ thuật nằm trong tài liệu tương ứng.

Khi mở chat mới trong dự án, có thể yêu cầu: “Tiếp tục theo AGENTS.md, đọc trạng thái trong README và hướng dẫn tao làm bước tiếp theo.”

## Nguồn và trạng thái
- [Quy trình project IT3180](00-References/IT3180_PROJECT_PROCESS.md): nguồn dùng để tổ chức và thực hiện dự án.
- [Proposal Nhóm 15](00-References/Proposal_Nhom15.pdf): nội dung đề xuất ban đầu; chưa có bằng chứng phê duyệt trong repository.
- Đã cập nhật [Project Charter](01-ManagedArea/01-ProjectManagement/Project-Charter/PROJECT_CHARTER.md) theo nội dung người dùng duyệt ngày 2026-09-29: một cửa hàng fast-food, pre-order và walk-in tối giản dùng chung quota, bảo vệ mọi đơn đã xác nhận/giữ quota; walk-in đồng ý thời gian dự kiến trước khi xác nhận. Ghi nhận thanh toán trong phạm vi, gateway production ngoài phạm vi.
- Đây là mô hình dự án do người dùng lựa chọn, không phải quy trình thương hiệu đã được khảo sát xác nhận. Charter đã có nội dung bối cảnh/phạm vi được duyệt trong trao đổi, chưa có phê duyệt toàn bộ tài liệu sau cập nhật.
- Đã lập [WBS ban đầu](01-ManagedArea/01-ProjectManagement/WBS/WBS.md) từ Charter: quản lý, yêu cầu, thiết kế, xây dựng phần mềm, kiểm thử, review/release. Chưa estimate/phân công; các gói triển khai sẽ được chia nhỏ tiếp khi yêu cầu rõ.
- Đã lập [Risk Register sơ bộ](01-ManagedArea/01-ProjectManagement/Risk-Management/RISK_REGISTER.md): 12 rủi ro, nhận định định tính, phòng ngừa và ứng phó, liên kết WBS. Chưa review toàn bộ, chưa xác nhận owner và chưa có bằng chứng xử lý rủi ro. Các tài liệu nghiệp vụ/thiết kế/test còn lại vẫn là mẫu.
- Đã lập [Product Backlog nháp](01-ManagedArea/02-Requirements/Product-Backlog/PRODUCT_BACKLOG.md) ngày 2026-10-08: 21 story xếp theo thứ tự ưu tiên (PB-01–21) và 6 NFR, có priority, estimate cỡ S-M-L, AC tham chiếu quy tắc SRS và nguồn từng story. Người dùng chưa duyệt thứ tự; chưa xác nhận với stakeholder ngoài. Chưa Sprint Planning.
- Ngày 2026-10-08 người dùng chốt các quy tắc nghiệp vụ mở (quota theo điểm công việc, khung 15 phút, đặt trong ngày trước ≥30 phút, trả tại quầy và chỉ bàn giao khi đã trả, hủy trước cửa sổ chuẩn bị, cửa hàng hủy kèm lý do, bếp cập nhật theo đơn, khách tự đăng ký). Đã ghi vào [SRS nháp](01-ManagedArea/02-Requirements/SRS/SRS.md): BR-01–33, state machine, FR-01–20, NFR với ngưỡng nháp. Chưa có Use Case.
- Đã lập [Sprint 1 Backlog](01-ManagedArea/01-ProjectManagement/SPRINT_1_BACKLOG.md) ngày 2026-10-08: Sprint 2 tuần (12/10–25/10/2026), chọn PB-01, 02, 05, 06, 07, 08, 12, 13 (làm thêm PB-16, 15), 30 task. Công nghệ người dùng chọn: Spring Boot, PostgreSQL, React. Người dùng tạm duyệt kế hoạch và cách thực hiện task ngày 2026-10-09.
- Bước 4 (Git) thực hiện ngày 2026-10-09 qua task T-01: commit toàn bộ tài liệu, tạo remote GitHub public, chốt quy ước nhánh/commit/PR (mục Git workflow).
- Ngày 2026-10-09 người dùng bỏ estimate theo giờ khỏi Sprint 1; lượng việc theo cỡ S-M-L và Task Board.
- T-03 (khung dự án) thực hiện ngày 2026-10-09 trên nhánh `chore/T-03-project-skeleton`: Spring Boot 4.1 (Java 25), React + Vite (TypeScript), PostgreSQL 18 bằng Docker Compose, Flyway, `/api/health`; hướng dẫn chạy ở [03-SourceCodes](03-SourceCodes/README.md). Đã merge (PR #2).
- T-02 (coding convention) thực hiện ngày 2026-10-09 trên nhánh `chore/T-02-coding-convention`: [CODING_CONVENTION.md](03-SourceCodes/CODING_CONVENTION.md) theo 8 nhóm của Bước 15; Java dùng Spotless + Palantir (4 space, 120 ký tự) và Checkstyle, TypeScript dùng Prettier và oxlint; `.gitattributes` buộc LF. Chờ người dùng review và merge PR.
- Tiếp theo: T-04–T-11 (Use Case → thiết kế). Giới hạn quota riêng cho pre-order mới là đề xuất, chưa đưa vào phạm vi đã duyệt. Không dùng hạn nộp hoặc phân công nhân sự làm điều kiện chặn việc này.
- Công nghệ đã chọn (Spring Boot, PostgreSQL, React); kiến trúc (T-08), thiết kế database (T-09) và chi tiết nghiệp vụ còn lại chưa chốt.

## Cấu trúc
Cấu trúc chính bám mục 8 của nguồn. `00-References` giữ nguyên tài liệu đầu vào; các file cấu hình Git/editor phục vụ làm việc, không phải tài liệu bàn giao môn học.

```text
Project_SE/
├── AGENTS.md
├── 00-References/
├── 01-ManagedArea/
│   ├── 01-ProjectManagement/
│   │   ├── Project-Charter/
│   │   ├── WBS/
│   │   └── Risk-Management/
│   ├── 02-Requirements/
│   │   ├── Product-Backlog/
│   │   ├── SRS/
│   │   ├── Use-Cases/
│   │   └── Activity-Diagrams/
│   ├── 03-Design/
│   │   ├── Analysis-Model/
│   │   ├── Architecture/
│   │   ├── Database/
│   │   ├── Class-Design/
│   │   └── UI-Prototype/
│   ├── 04-Testing/
│   │   ├── Test-Cases/
│   │   └── Test-Results/
│   └── 05-Releases/
├── 02-FreeZone/
├── 03-SourceCodes/
├── 04-Users/
└── README.md
```

## Định hướng thực hiện đã thống nhất

Chuẩn bị yêu cầu và thiết kế tổng thể tương đối kỹ, sau đó triển khai bằng Agile/Scrum gọn cho 1–2 người. Bộ tài liệu vẫn bám nguồn IT3180; không thêm loại tài liệu riêng cho cách tổ chức này.

- Trước Sprint đầu: làm rõ bài toán, Charter, WBS, rủi ro, Product Backlog, SRS và thiết kế tổng thể đủ để triển khai. Phần đã rõ được ghi đầy đủ; không cần hoàn thiện mọi chi tiết của mọi chức năng mới được bắt đầu.
- Mỗi Sprint: chọn một mục tiêu và phần backlog vừa sức; làm rõ yêu cầu/thiết kế liên quan, code, test và tạo phần sản phẩm chạy được theo Definition of Done.
- Planning chọn mục tiêu và công việc; Daily Scrum kiểm tra tiến độ và điều chỉnh kế hoạch; Review kiểm tra sản phẩm và việc tiếp theo; Retrospective chọn cải tiến cụ thể. Thực hiện ngắn, ghi kết quả có ích trong các mẫu sẵn có.
- Dùng lại SRS và thiết kế hiện hành, chỉ cập nhật phần bị ảnh hưởng. Không viết lại bộ tài liệu mỗi Sprint và không cố tạo thay đổi yêu cầu.
- Giữ các trách nhiệm Product Owner, Scrum Master và Developers; ghi đúng việc kiêm nhiệm/người thực hiện khi tổ chức Sprint. Nếu chỉ một người, ghi rõ hạn chế về phối hợp và phản hồi độc lập; không dựng cuộc họp hay review của người khác.
- Không bắt buộc story point, velocity hoặc cuộc họp riêng ngoài những hoạt động đã thống nhất. Estimate và Task Board vẫn phục vụ chọn lượng việc thực tế.
- Kết thúc khi phạm vi đã thống nhất đáp ứng yêu cầu và chất lượng, không kéo dài vòng lặp chỉ để đủ hình thức.

Hướng chia dự kiến, chưa phải Sprint Backlog hoặc lịch cam kết:

| Sprint | Mục tiêu dự kiến |
|---|---|
| 1 | Luồng tối giản từ đặt món theo slot đến bếp xử lý và quầy bàn giao |
| 2 | Hoàn thiện điều phối capacity, đề xuất slot và xử lý request đồng thời |
| 3 | Hoàn thiện hủy đơn, ngoại lệ và các chức năng còn lại được chọn trong phạm vi |

Giới hạn slot cơ bản và kiểm thử có từ Sprint đầu; Sprint sau mở rộng độ đầy đủ. Chốt độ dài Sprint và khối lượng khi lập Sprint Planning, không mặc định mỗi dòng trên chắc chắn vừa một Sprint.

Nghiệp vụ do người thực hiện chủ động xác định từ quan sát hoặc tham khảo. Ghi rõ đâu là quan sát, đâu là nguồn tham khảo và đâu là quy tắc lựa chọn cho mô hình dự án; không bắt buộc phỏng vấn cửa hàng hoặc tạo bằng chứng xác nhận không có thật.

## Quy trình theo nguồn
```text
Nghiên cứu tiền khả thi
→ Project Charter
→ WBS + Risk Management
→ Thiết lập cấu trúc project + repository
→ Thu thập và phân tích yêu cầu
→ Product Backlog
→ Sprint Planning + Sprint Backlog
→ SRS
→ Use Case Model + Use Case Specification
→ Activity Diagram
→ Analysis Model + Sequence Diagram
→ Architecture / Package Design
→ Database Design + Detailed Class Design
→ UI Prototype
→ Implementation
→ Testing
→ Sprint Review / Release
→ Feedback → cập nhật Product Backlog
```

Sprint Retrospective tạo hành động cải tiến cách làm cho Sprint tiếp theo. Khi còn backlog, quay lại Sprint Planning; khi hoàn thành phạm vi chính, chuyển sang vận hành/bảo trì.

Thu thập yêu cầu bắt đầu trước Product Backlog. Trong Sprint, tiếp tục làm rõ yêu cầu và cập nhật SRS cùng tài liệu liên quan. Nguồn không yêu cầu hoàn thiện SRS toàn hệ thống trước Sprint đầu tiên.

## Input và output từng bước
Bám phần quy trình chi tiết Bước 0–19 trong nguồn; nơi lưu là cách bố trí các đầu ra đã được nguồn nêu.

| Bước | Input | Công việc / Output | Nơi lưu |
|---|---|---|---|
| 0. Tiền khả thi | Nhu cầu, bối cảnh, stakeholder, nguồn lực | Thu thập dữ liệu; làm rõ mục tiêu, scope, giả định, ràng buộc và khả thi để viết Charter | Thông tin đầu vào và nội dung liên quan trong Charter |
| 1. Charter | Kết quả nghiên cứu ban đầu | Thống nhất thông tin dự án, đội ngũ, stakeholder, phạm vi, truyền thông, phê duyệt → Project Charter | [Project Charter](01-ManagedArea/01-ProjectManagement/Project-Charter/PROJECT_CHARTER.md) |
| 2. WBS | Charter, scope, deliverable | Phân rã thành work package → WBS | [WBS](01-ManagedArea/01-ProjectManagement/WBS/WBS.md) |
| 3. Risk | Charter, WBS, nguồn lực, giả định | Nhận diện, đánh giá, xử lý → Risk Register | [Risk Register](01-ManagedArea/01-ProjectManagement/Risk-Management/RISK_REGISTER.md) |
| 4. Repository | Artifact, team, workflow | Tổ chức thư mục, version, quyền truy cập, Git workflow | Repository này |
| 5. Backlog | Nhu cầu stakeholder, business process, scope | Thu thập yêu cầu, viết story, estimate, ưu tiên, acceptance criteria → Product Backlog | [Product Backlog](01-ManagedArea/02-Requirements/Product-Backlog/PRODUCT_BACKLOG.md) |
| 6. Sprint Planning | Backlog, capacity nhóm, dependency, risk | Chọn story, chia task, estimate, phân công → Sprint Goal, Sprint Backlog, Task Board | [Sprint Backlog mẫu](01-ManagedArea/01-ProjectManagement/SPRINT_BACKLOG_TEMPLATE.md) |
| 7. SRS | Story, acceptance criteria, nghiệp vụ | Khảo sát, phân rã chức năng, đặc tả chức năng/NFR → SRS | [SRS](01-ManagedArea/02-Requirements/SRS/SRS.md) |
| 8. Use Case Model | SRS, chức năng, vai trò | Xác định actor, use case, vẽ tổng quan/phân rã | [Use Cases](01-ManagedArea/02-Requirements/Use-Cases/README.md) |
| 9. Use Case Specification | Model, SRS, business rules | Đặc tả điều kiện, luồng chính, thay thế và lỗi | [Mẫu đặc tả](01-ManagedArea/02-Requirements/Use-Cases/USE_CASE_TEMPLATE.md) |
| 10. Activity | Luồng Use Case, điều kiện nghiệp vụ | Vẽ action, decision, branch, merge, start/end | [Activity Diagrams](01-ManagedArea/02-Requirements/Activity-Diagrams/README.md) |
| 11. Analysis | Use Case, đặc tả, Activity | Boundary/Controller/Entity, phân bổ trách nhiệm, Sequence | [Analysis Model](01-ManagedArea/03-Design/Analysis-Model/README.md) |
| 12. Architecture | Analysis, Use Case, NFR, constraint | Tổ chức kiến trúc/package và trách nhiệm | [Architecture](01-ManagedArea/03-Design/Architecture/ARCHITECTURE.md) |
| 13. Detailed Design | Architecture, analysis, sequence, SRS | ERD, relational schema, detailed class diagram | [Database](01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md), [Class Design](01-ManagedArea/03-Design/Class-Design/README.md) |
| 14. UI | SRS, Use Case, Activity, actor | Layout, wireframe, phối màu, navigation, đánh giá và chỉnh sửa | [UI Prototype](01-ManagedArea/03-Design/UI-Prototype/README.md) |
| 15. Implementation | Sprint Backlog, design, prototype, convention | Code, local test, commit, PR, review/merge | [Source Codes](03-SourceCodes/README.md) |
| 16. Testing | SRS, Use Case, AC, code, hệ thống chạy | White-box/black-box; thiết kế test, chạy và ghi kết quả | [Test Cases](01-ManagedArea/04-Testing/Test-Cases/README.md), [Test Results](01-ManagedArea/04-Testing/Test-Results/README.md) |
| 17. Review/Release | Increment, test results, AC, Sprint Goal | Demo, chấp nhận/yêu cầu sửa, release và feedback | [Releases](01-ManagedArea/05-Releases/README.md) |
| 18. Retrospective | Dữ liệu Sprint, blocker, trải nghiệm nhóm | Xác định điều tốt/chưa tốt và hành động cải tiến | [Mẫu Retrospective](01-ManagedArea/05-Releases/RETROSPECTIVE_TEMPLATE.md) |
| 19. Tiếp tục/bảo trì | Feedback, backlog còn lại | Cập nhật backlog, Sprint tiếp theo hoặc bảo trì | Product Backlog và công việc tương ứng |

## Quy tắc chuyển giao
Theo mục 9 của nguồn:
- Requirement → Design: yêu cầu, Use Case, main/alternative flow và business rules đủ rõ.
- Design → Implementation: hiểu data model, responsibility, interaction và UI flow.
- Implementation → Testing: có code, yêu cầu kiểm thử được và expected behavior.
- Testing → Release: test bắt buộc pass, acceptance criteria đạt, review chấp nhận.

## Git workflow
Chọn feature/bug → checkout nhánh phù hợp → pull mới nhất → tạo nhánh → code và commit → push → Pull Request → review/merge.

Quy ước áp dụng (chốt ở task T-01, Sprint 1):
- Remote: [github.com/huyducnguyen0/smart-food-preorder](https://github.com/huyducnguyen0/smart-food-preorder) (public). `main` luôn ở trạng thái chạy được; không commit thẳng vào `main`.
- Nhánh: `feature/<PB-id>-<tên-ngắn>` cho story, `fix/<mô-tả>` cho lỗi, `docs/<mô-tả>` cho tài liệu, `chore/<T-id>-<mô-tả>` cho thiết lập. Ví dụ: `feature/PB-07-place-order`.
- Commit theo Conventional Commits: `feat|fix|docs|test|refactor|chore(<phạm vi>): <mô tả>`, ghi mã PB/BR/T liên quan khi có.
- Pull Request vào `main`: mô tả làm gì, liên kết story/task, cách đã kiểm tra. Người dùng tự review theo checklist, có AI hỗ trợ review; không có reviewer độc lập (RISK-08). Merge kiểu squash để mỗi PR là một commit trên `main`, rồi xóa nhánh.
- Không đưa bí mật lên repo; `.env` nằm trong `.gitignore`, chỉ commit `.env.example`.

## Definition of Done
Theo mục 11 của nguồn:
- [ ] Requirement rõ.
- [ ] User Story có Acceptance Criteria.
- [ ] Use Case/flow được làm rõ nếu cần.
- [ ] Design đủ để implement.
- [ ] Code tuân thủ convention.
- [ ] Code đã commit/push.
- [ ] Pull Request đã review.
- [ ] Test Cases đã thiết kế.
- [ ] Test bắt buộc đã pass.
- [ ] Acceptance Criteria đạt.
- [ ] Tài liệu liên quan cập nhật.
- [ ] Feature demo được trong Sprint Review.

## Truy vết
Theo mục 5 của nguồn, liên kết nhu cầu → story → Sprint Backlog → SRS → Use Case → Activity → Analysis/Sequence → Design/UI → code → test → acceptance.
Ghi liên kết tới tài liệu, mục, commit/PR và test tương ứng ngay trong nội dung làm việc. Không bắt buộc thêm tài liệu truy vết riêng.
