-- Sprint 4 (PB-22): lịch bếp thay cho quota theo khung nhận. Thiết kế: DATABASE_DESIGN.md mục 2, 5.
-- Nghĩa mới của cột cũ (giữ tên để không phải đổi dữ liệu và code không liên quan):
--   store_settings.default_quota_points: công suất bếp mặc định (điểm) của mỗi khoảng làm 15 phút.
--   time_slots.quota_override: công suất bếp riêng của khoảng làm bắt đầu lúc start_time.
--   time_slots.held_points: tổng điểm các đơn NHẬN trong khung này còn giữ chỗ (thống kê, đối chiếu), không còn là
--   giới hạn; nên bỏ ràng buộc held_points <= quota_override.
ALTER TABLE store_settings
    -- Làm trước tối đa bao lâu so với giờ nhận (món còn ngon); bếp xếp lịch đơn đặt trước trong cửa sổ này.
    ADD COLUMN hold_minutes integer NOT NULL DEFAULT 45,
    -- Phần trăm công suất mỗi khoảng làm mà đơn đặt trước được dùng; phần còn lại dành cho khách tại quầy.
    ADD COLUMN preorder_share_percent integer NOT NULL DEFAULT 70,
    -- Số đơn tối đa nhận trong một khung nhận 15 phút (quầy không bị dồn khách).
    ADD COLUMN pickup_limit_orders integer NOT NULL DEFAULT 8,
    ADD CONSTRAINT ck_store_settings_hold_minutes CHECK (hold_minutes > 0),
    ADD CONSTRAINT ck_store_settings_preorder_share CHECK (preorder_share_percent BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_store_settings_pickup_limit CHECK (pickup_limit_orders >= 1);

ALTER TABLE time_slots DROP CONSTRAINT ck_time_slots_held_within_override;
