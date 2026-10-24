-- Dữ liệu mẫu cho local và demo (T-16): giờ mở cửa 10:00-21:00, quota mặc định 20 điểm mỗi khung 15 phút.
-- Chỉ chèn khi chưa có; sửa số ở đây không đổi dữ liệu đã có (quản lý đổi bằng màn cấu hình, PB-04).
INSERT INTO store_settings (id, open_time, close_time, default_quota_points)
VALUES (1, '10:00', '21:00', 20)
ON CONFLICT (id) DO NOTHING;
