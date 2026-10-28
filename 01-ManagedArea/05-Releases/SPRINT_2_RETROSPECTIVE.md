# Sprint Retrospective - Sprint 2
Trạng thái: **Nháp do AI soạn ngày 2026-10-28 từ diễn biến thật của Sprint, người dùng chưa duyệt.** Người thực hiện: một người dùng, AI hỗ trợ.

## Input
Sprint 1 cho thấy (người dùng tự nhìn lại ngày 2026-10-26): một người làm, AI viết cả code lẫn test và tự review, chưa ai khác xem demo, tốc độ nhanh hơn mức người dùng kịp hiểu. Sprint 2 người dùng chọn làm còn nhanh hơn: một lần giao việc cho cả Sprint.

## Điều làm tốt
- Xong cả 5 story trong một lần, mọi test pass; NFR-01 được kiểm chứng với hai kênh cùng đặt.
- Dùng lại thiết kế Sprint 1 (`QuotaService`, kiểm tra món, chống trùng) nên phần walk-in ít code mới.
- Mỗi lựa chọn AI tự quyết được ghi trong đặc tả, mục "Ghi chú", để người dùng xác nhận sau.

## Điều chưa tốt
- Ca test viết cùng lúc với code, do cùng một AI: dễ có chung điểm mù.
- Một PR rất lớn cho cả Sprint: người dùng khó đọc hết, thực tế duyệt dựa trên kết quả test.
- Khoảng cách giữa phần đã làm và phần người dùng hiểu được tiếp tục tăng.
- Vẫn chưa ai khác xem demo; Review và Retro chỉ là nháp.

## Hành động cải tiến
Một việc cho Sprint sau: trước khi merge PR Sprint, người dùng đọc trang ghi chú cá nhân `04-Users/NguyenHuyDuc/sprint-2-de-hieu.md` (thư mục cá nhân, không đưa lên Git) và tự trả lời được các câu hỏi ở cuối trang mà không nhìn đáp án. Nhận biết hiệu quả: trả lời được ít nhất 4/5 câu.
