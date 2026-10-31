# Sprint Retrospective - Sprint 4
Trạng thái: **Nháp do AI soạn ngày 2026-10-31 từ diễn biến thật của Sprint, người dùng chưa duyệt.** Người thực hiện: một người dùng, AI hỗ trợ.

## Input
Hành động cải tiến của Sprint 3: người dùng merge lần lượt các PR đang chờ, rồi tự đóng vai khách, quầy, quản lý trên trình duyệt và ghi lại phản hồi. **Làm được một phần**: người dùng chưa merge PR nào (#22 → #28 vẫn mở), chưa có ghi nhận đóng vai; nhưng người dùng đã tự xem xét luồng nghiệp vụ và đưa ra phản hồi vận hành thật (bếp phải làm trước giờ đông), dẫn tới Sprint này.

## Điều làm tốt
- Phản hồi của người dùng xuất phát từ hiểu biết vận hành, không phải từ test; nó bắt được một lỗi thiết kế mà mọi test đều "pass" (quy tắc 15 phút làm giờ đông quá tải).
- Phân tích bằng mô phỏng trước khi code, lưu thành tư liệu; quyết định dựa trên số liệu thay vì cảm tính.
- Thiết kế giữ được ca cũ: cấu hình tương đương mô hình cũ làm các ca Sprint 1–3 giữ nguyên ý nghĩa.

## Điều chưa tốt
- Một sửa lỗi ở review (sửa ⑤, chỉ cho bếp làm từ 15 phút trước khung) đúng về quy tắc nhưng sai về vận hành, phải hoàn tác. AI đã không hỏi người dùng về giờ đông trước khi đề xuất.
- Đổi nghĩa cột cũ (`default_quota_points`, `quota_override`, `held_points`) thay vì đổi tên: ít thay đổi hơn nhưng người đọc database dễ hiểu nhầm.
- PR chồng thêm một tầng (7 PR mở nối tiếp nhau).
- Chưa chạy tay trên trình duyệt; Docker tắt.

## Hành động cải tiến
Trước khi sửa một quy tắc ảnh hưởng vận hành, AI hỏi người dùng một câu về tình huống thực tế (giờ đông, ai làm gì lúc đó). Nhận biết hiệu quả: các thay đổi quy tắc ở Sprint sau có ghi câu hỏi và câu trả lời của người dùng trong đặc tả hoặc tư liệu.
