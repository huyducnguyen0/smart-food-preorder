-- Cấu hình cửa hàng mặc định là dữ liệu bắt buộc để hệ thống chạy (danh sách khung đọc dòng id = 1), không phải dữ
-- liệu demo, nên phải có cả khi triển khai không nạp db/seed. Quản lý sửa lại ở màn "Giờ mở cửa và quota" (UC-14).
-- Database đã có dòng này (từ R__seed_store_settings.sql) thì giữ nguyên.
INSERT INTO store_settings (id, open_time, close_time, default_quota_points)
VALUES (1, '10:00', '21:00', 20)
ON CONFLICT (id) DO NOTHING;
