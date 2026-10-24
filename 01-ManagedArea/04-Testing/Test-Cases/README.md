# Test Cases
Trạng thái: Black-box Sprint 1 đã thiết kế ở T-23 (2026-10-24): [BLACK_BOX_TEST_CASES.md](BLACK_BOX_TEST_CASES.md), 131 test case, chưa chạy. White-box cho logic giữ quota đã thiết kế ở T-24: [WHITE_BOX_QUOTA.md](WHITE_BOX_QUOTA.md) (Control Flow Graph, 5 đường độc lập, bảng giá trị biên, độ phủ đo được).

## White-box
Input: source code / control logic.
Thực hiện: đọc code → xây Control Flow Graph → xác định execution paths → chọn dữ liệu → thiết kế test → chạy → ghi kết quả.
Lưu Control Flow Graph, Execution Paths và White-box Test Cases tại đây; kết quả ở Test-Results.

## Black-box
Input: SRS, Use Case, Acceptance Criteria và business rules.
Thực hiện: xác định input, expected output và điều kiện → chọn trường hợp hợp lệ/không hợp lệ → viết test → chạy → so sánh expected/actual.
Lưu Black-box Test Cases tại đây; kết quả ở Test-Results.

Liên kết mỗi test với yêu cầu hoặc logic được kiểm chứng. Chỉ ghi Passed/Failed khi đã chạy.
