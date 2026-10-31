# Phân tích: làm trước để giảm quá tải và giảm chờ (2026-10-31)

Tư liệu nháp theo yêu cầu người dùng ngày 2026-10-31, **không phải source of truth** (xem [02-FreeZone](../README.md)). Quyết định đã chốt được ghi vào SRS và Product Backlog; tài liệu này giữ lại lập luận và số liệu mô phỏng.

## Bối cảnh
Sau review ngày 2026-10-31, AI đổi quy tắc để bếp chỉ bắt đầu đơn từ 15 phút trước khung nhận (bảo vệ quyền tự hủy của khách). Người dùng chỉ ra sai về vận hành: 11:45–12:00 là giờ đông khách, bếp phải tranh thủ làm trước lúc vắng, nếu không khách walk-in sẽ không được phục vụ. Người dùng yêu cầu phân tích chuyên sâu để cải thiện thiết kế.

## Vấn đề gốc
Quota của một khung nhận đang gộp ba thứ khác nhau:

| Thứ bị gộp | Bản chất |
|---|---|
| Công suất bếp | Bếp làm được bao nhiêu điểm trong mỗi 15 phút đang làm |
| Năng lực quầy | Quầy giao được bao nhiêu khách trong mỗi 15 phút khách đến lấy |
| Độ ngon của món | Món làm xong giữ được tối đa bao lâu |

Mô hình hiện tại ngầm giả định bếp chỉ làm trong đúng 15 phút trước giờ nhận, nên:
1. Làm trước lúc vắng không được ghi nhận: khung vẫn bị coi là đầy.
2. Bếp rảnh lúc vắng nhưng hệ thống vẫn từ chối đơn lúc đông.
3. Đơn đặt trước nhận từ sáng chiếm hết công suất cao điểm trước khi walk-in tới.

## Thiết kế đề xuất: lịch bếp + dành chỗ cho walk-in
1. Tách hai giới hạn: **công suất bếp theo khung làm** (ví dụ 20 điểm/15 phút) và **giới hạn số đơn mỗi khung nhận** (ví dụ 8 đơn/15 phút).
2. Mỗi đơn có **cửa sổ làm** [giờ nhận − H, giờ nhận], H là thời gian món còn ngon (ví dụ 45 phút).
3. **Nhận đơn khi còn xếp được lịch làm**: xếp mọi đơn theo hạn, làm sớm nhất có thể, không khung làm nào vượt công suất.
4. **Dành chỗ cho walk-in**: đơn đặt trước dùng tối đa X% công suất mỗi khung làm (ví dụ 70%).
5. Màn bếp thành **kế hoạch làm**: "nên làm ngay", "có thể làm trước".
6. Khách tự hủy khi bếp chưa bắt đầu làm, chậm nhất 15 phút trước giờ nhận; trang đơn ghi rõ.
7. Walk-in được báo giờ theo lịch bếp thật.

Ví dụ: công suất 20 điểm/15 phút, H = 45 phút, 30 điểm đơn đặt trước cho khung nhận 12:00. Mô hình cũ từ chối 10 điểm. Lịch bếp làm 20 điểm lúc 11:15–11:30, 10 điểm lúc 11:30–11:45, nhận đủ 30, và khung làm 11:45–12:00 còn trống cho walk-in.

## Mô phỏng
Chạy: `python mo_phong_lich_bep.py` (Python 3, không cần thư viện ngoài). Giả định: bán 10:00–14:00, cao điểm 11:45–12:15, đơn trung bình 6 điểm, bếp 20 điểm/15 phút (320 điểm cả buổi), walk-in bỏ đi nếu chờ quá khoảng 30–45 phút. Trung bình 300 ngày; tổng số đơn được phục vụ (trong ngoặc: số walk-in):

| Nhu cầu (đặt trước + walk-in) | A: hiện tại | A+: dành 30% cho walk-in | B: lịch bếp | B+: lịch bếp + dành 30% |
|---|---|---|---|---|
| 20 + 20 | 33,1 (13,5) | 33,4 (15,9) | 38,3 (18,3) | 38,4 (18,5) |
| 25 + 25 | 36,8 (13,2) | 37,6 (17,5) | 43,0 (18,0) | 43,2 (19,6) |
| 30 + 30 | 39,7 (12,8) | 41,2 (18,9) | 45,8 (16,0) | 46,5 (20,3) |

- Lịch bếp phục vụ thêm khoảng 15–17% đơn với cùng một bếp.
- Chỉ dành chỗ cho walk-in mà không đổi mô hình thì chủ yếu chuyển khách từ đặt trước sang walk-in.
- B+ tốt nhất; walk-in được phục vụ tăng khoảng 40–60% so với hiện tại.
- Thời gian chờ đo theo đơn vị khung 15 phút nên quá thô để thấy khác biệt.

Giới hạn: số giả định, công suất cố định, không tính hủy, không đến nhận, chưa mô phỏng mở lại phần dành riêng sát giờ. Kết luận là xu hướng, không phải con số chính xác.

## Quyết định của người dùng (2026-10-31)
1. Làm ngay: bếp được làm trước; khách tự hủy khi bếp chưa làm, chậm nhất 15 phút trước giờ nhận; trang đơn ghi rõ.
2. Làm lịch bếp + dành chỗ cho walk-in thành Sprint 4, dùng số mặc định: H = 45 phút, đơn đặt trước tối đa 70% công suất, quầy tối đa 8 đơn mỗi khung nhận.
3. Lưu phân tích này làm tư liệu.

## Đã hiện thực
Quyết định 2 làm ở Sprint 4 (PB-22), nhánh `feature/sprint-4`: [Sprint 4 Backlog](../../01-ManagedArea/01-ProjectManagement/SPRINT_4_BACKLOG.md). Quy tắc chính thức nằm ở SRS (BR-09, BR-39–BR-41) và UC-17; tư liệu này chỉ giữ lý do và mô phỏng.
