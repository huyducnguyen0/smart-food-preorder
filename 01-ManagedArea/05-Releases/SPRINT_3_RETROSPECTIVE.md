# Sprint Retrospective - Sprint 3
Trạng thái: **Nháp do AI soạn ngày 2026-10-28 từ diễn biến thật của Sprint, người dùng chưa duyệt.** Người thực hiện: một người dùng, AI hỗ trợ.

## Input
Hành động cải tiến của Sprint 2: trước khi merge, người dùng đọc trang `sprint-2-de-hieu.md` và tự trả lời ít nhất 4/5 câu hỏi. **Chưa có bằng chứng đã làm**: PR Sprint 2 chưa merge và không có ghi nhận người dùng đã tự trả lời; Sprint 3 bắt đầu ngay sau Sprint 2.

## Điều làm tốt
- Xong cả 6 story và đo NFR-04 trong một lần; quota đúng trong mọi ca hủy/no-show, kể cả nhiều yêu cầu hủy đồng thời.
- **Chạy tay trên trình duyệt tìm ra một lỗi thật** mà mọi test tự động đều không bắt được (đăng xuất rồi người sau bị "Không có quyền").
- Dùng lại nhiều: kiểm tra định danh của đăng ký, `AdminSlotService` cho màn theo dõi, khung test black-box.

## Điều chưa tốt
- Hành động cải tiến Sprint 2 chưa được thực hiện mà đã sang Sprint 3: khoảng cách giữa phần đã làm và phần người dùng hiểu tiếp tục tăng.
- Ca test vẫn viết cùng lúc với code, do cùng một AI.
- Lỗi đăng xuất có từ Sprint 1, qua hai Sprint không ai phát hiện vì chưa ai khác dùng thử.
- Một PR rất lớn; ba PR trước (#22, #23, #25) vẫn chưa merge, PR mới chồng lên.

## Hành động cải tiến
Một việc trước khi làm phát hành: người dùng merge lần lượt các PR đang chờ, sau đó **tự đóng vai khách, quầy, quản lý trên trình duyệt** theo một kịch bản ngắn (đặt, hủy, bếp làm, quầy thu tiền và bàn giao, xem màn theo dõi) và ghi lại điều gì khó hiểu hoặc sai. Nhận biết hiệu quả: có một danh sách phản hồi của chính người dùng (kể cả "không có vấn đề") trước khi bắt đầu phát hành.
