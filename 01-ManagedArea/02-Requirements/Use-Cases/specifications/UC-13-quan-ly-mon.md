# UC-13 — Quản lý món
Trạng thái: Đặc tả ngắn lập ở Sprint 2 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. Các lựa chọn ở mục "Ghi chú" là của AI, chưa được người dùng xác nhận.

| Mục | Nội dung |
|---|---|
| Actor chính | Quản lý |
| Mục đích | Menu và quota phản ánh khả năng phục vụ mà không cần dữ liệu mẫu |
| Story và AC | PB-03 AC1 → UC-13.1, 13.2; AC2 → UC-13.3, 13.4; AC3 → Postcondition; AC4 → Ghi chú |
| FR, quy tắc | FR-04; BR-01 → BR-05, BR-08 |
| Gồm | UC-13.1 Thêm món, 13.2 Sửa món, 13.3 Còn bán / tạm hết, 13.4 Ngừng bán |

## Main Flow
1. Quản lý: mở "Món".
2. Hệ thống: hiện mọi món theo nhóm, **kể cả món ngừng bán**, có giá, điểm công việc, tình trạng.
3. Quản lý: thêm món (nhóm, tên, giá, điểm, mô tả và ảnh tùy chọn; BR-01, BR-02) hoặc sửa món.
4. Quản lý: đổi còn bán ↔ tạm hết (BR-04), ngừng bán (ẩn khỏi menu, không xóa, BR-03) hoặc bán lại.
5. Hệ thống: lưu; thực đơn của khách đổi ngay.

## Luồng lỗi
- **E1 — Dữ liệu sai**: thiếu tên hoặc nhóm, giá hoặc điểm âm, nhóm không tồn tại, ảnh không bắt đầu bằng http(s): báo theo trường.
- **E2 — Món không tồn tại**: "không tìm thấy".
- **E3 — Không phải quản lý**: từ chối.

## Postcondition
Đơn đã xác nhận không đổi: dòng đơn lưu tên, giá; đơn lưu điểm lúc xác nhận (BR-05, BR-08).

## Ghi chú
- Không quản lý tồn kho nguyên liệu (PB-03 AC4). Không quản lý nhóm món: nhóm mẫu (Burger, Gà rán, Đồ uống) chỉ có khi nạp dữ liệu mẫu; nhóm **"Món khác"** luôn có (migration V11, quyết định của người dùng ngày 2026-10-28) để quản lý luôn thêm được món, kể cả khi triển khai không có dữ liệu mẫu. Thực đơn của khách không hiện nhóm chưa có món đang bán.
- Lựa chọn chưa xác nhận: có thao tác "bán lại" món đã ngừng bán (SRS không cấm, giúp sửa khi bấm nhầm).

## Output chuyển giao
`/api/admin/menu/**`, màn `/admin/menu`, test TC-MNG-01 → 08.
