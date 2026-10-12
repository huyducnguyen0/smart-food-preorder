# UC-xx — <Tên use case>
Trạng thái: Mẫu. Đặc tả thật đặt tên file `UC-xx-<ten-ngan>.md` trong thư mục `specifications/`.

| Mục | Nội dung |
|---|---|
| Actor chính | Actor khởi động use case |
| Mục đích | Mục tiêu actor đạt được |
| Story và AC | `PB-xx` AC n → bước/luồng tương ứng |
| FR, quy tắc nghiệp vụ | `FR-xx`, `BR-xx` trong [SRS](../SRS/SRS.md) |
| NFR | `NFR-xx` nếu có |
| Quan hệ | include / extend / được include bởi |
| Điểm mở rộng | Bước mà use case khác có thể chen vào, kèm điều kiện |
| Phạm vi Sprint | Phần làm trong Sprint hiện tại và phần để sau |

## Precondition
Điều kiện phải đúng trước khi bắt đầu.

## Main Flow
Đánh số bước; mỗi bước bắt đầu bằng chủ thể (`Khách:`, `Hệ thống:`...). Bước của hệ thống ghi quy tắc nghiệp vụ áp dụng.

## Alternative Flow
- **Luồng thay thế `A1`, `A2`...**: cách khác vẫn hợp lệ để đi tiếp hoặc kết thúc.
- **Luồng lỗi `E1`, `E2`...**: điều kiện không thỏa, use case kết thúc không thành công hoặc quay lại bước trước.

Mỗi luồng ghi: rẽ ra từ bước nào, điều kiện, hệ thống làm gì, quay về bước nào hoặc kết thúc. Mã luồng dùng làm cơ sở cho Test Case (T-23).

## Postcondition
- **Thành công**: trạng thái dữ liệu sau khi hoàn tất.
- **Không thành công**: những gì chắc chắn không thay đổi.

## Ghi chú và giới hạn
Giả định, quyết định thiết kế liên quan, phần chưa làm trong Sprint hiện tại. Không bắt buộc.

## Output chuyển giao
Đặc tả dùng cho Activity Diagram, Analysis Class, Sequence Diagram, UI và Test Case.
