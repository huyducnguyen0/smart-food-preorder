-- Nhóm "Món khác" luôn có (quyết định 2026-10-28): quản lý luôn có ít nhất một nhóm để thêm món, kể cả khi triển khai
-- không nạp dữ liệu mẫu (chưa có chức năng tạo nhóm). Xếp cuối danh sách. Thực đơn của khách chỉ hiện nhóm có món
-- đang bán, nên nhóm trống không hiện ra.
INSERT INTO menu_categories (name, sort_order)
VALUES ('Món khác', 1000)
ON CONFLICT (name) DO NOTHING;
