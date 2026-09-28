# Smart Food Pre-order and Pickup Coordination Platform

Dự án học phần IT3180 - Kỹ thuật phần mềm của Nhóm 15.

## Mục tiêu

Xây dựng hệ thống web hỗ trợ đặt món trước và điều phối thời gian nhận món theo năng lực phục vụ của một cửa hàng. Trọng tâm của dự án là bảo đảm luồng thống nhất từ khách hàng, điều phối pickup slot, bếp đến quầy bàn giao; không phát triển thành marketplace hoặc hệ thống giao đồ ăn.

## Trạng thái hiện tại

- Giai đoạn: `00 - Initiation`.
- Đã có: proposal ban đầu và hướng dẫn quy trình môn học.
- Bước kế tiếp: khảo sát tiền khả thi, sau đó lập Project Charter.
- Chưa khóa: technology stack, kiến trúc, database và chi tiết implementation.

Xem [PROJECT_STATUS.md](PROJECT_STATUS.md) để biết điểm kiểm soát hiện tại và [01-ManagedArea/ARTIFACT_REGISTER.md](01-ManagedArea/ARTIFACT_REGISTER.md) để theo dõi toàn bộ tài liệu bàn giao.

## Cấu trúc repository

```text
Project_SE/
├── 00-References/          # Tài liệu môn học và tài liệu tham khảo bất biến
├── 01-ManagedArea/         # Artifact chính thức, được review và version hóa
│   ├── 00-Initiation/
│   ├── 01-ProjectManagement/
│   ├── 02-Requirements/
│   ├── 03-Design/
│   ├── 04-Testing/
│   └── 05-Releases/
├── 02-FreeZone/            # Bản nháp, thử nghiệm, chưa phải tài liệu chính thức
├── 03-SourceCodes/         # Mã nguồn sau khi stack và kiến trúc được phê duyệt
├── 04-Users/               # Không gian làm việc tạm theo thành viên
└── .github/                # Mẫu issue và pull request
```

## Quy trình áp dụng

```text
Nghiên cứu tiền khả thi
→ Project Charter
→ WBS + Risk Register
→ Product Backlog
→ Sprint Planning
→ SRS + Use Case + Activity
→ Analysis + Architecture + Detailed Design
→ UI Prototype
→ Implementation + Review
→ Testing
→ Sprint Review + Release + Feedback
```

Mỗi artifact chỉ được đánh dấu `Approved` khi có người review và output đủ làm input cho bước tiếp theo. Không đưa bản nháp từ `02-FreeZone` vào sử dụng chính thức trước khi chuyển sang `01-ManagedArea`.

## Quy ước truy vết

Sử dụng ID ổn định xuyên suốt dự án:

- `OBJ-xxx`: mục tiêu dự án.
- `STK-xxx`: stakeholder.
- `US-xxx`: user story.
- `FR-xxx`: functional requirement.
- `NFR-xxx`: non-functional requirement.
- `BR-xxx`: business rule.
- `UC-xxx`: use case.
- `TC-xxx`: test case.
- `RISK-xxx`: rủi ro.
- `ADR-xxx`: quyết định kiến trúc.

Quan hệ tối thiểu cần truy vết:

```text
Objective → User Story → Requirement/Business Rule → Use Case
          → Design/Code → Test Case → Test Result → Acceptance
```

## Làm việc với Git

Quy tắc nhánh, commit, review và Definition of Done nằm trong [CONTRIBUTING.md](CONTRIBUTING.md).
