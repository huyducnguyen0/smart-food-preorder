-- T-30 (PB-15): bàn giao đơn tại quầy. Thiết kế: 01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md mục 5.
ALTER TABLE orders
    ADD COLUMN picked_up_at timestamptz,
    ADD COLUMN picked_up_by bigint,
    ADD CONSTRAINT fk_orders_picked_up_by FOREIGN KEY (picked_up_by) REFERENCES user_accounts (id),
    -- Có thời điểm thì có người thực hiện. Chưa ràng buộc "status = PICKED_UP khi và chỉ khi có thời điểm" vì các lần
    -- chuyển sang trạng thái kết thúc khác (UC-06, UC-10, UC-11) chưa có; thêm khi các use case đó hoàn chỉnh.
    ADD CONSTRAINT ck_orders_picked_up_audit CHECK ((picked_up_at IS NULL) = (picked_up_by IS NULL));
