-- T-29 (PB-16): ghi nhận thanh toán tại quầy. Thiết kế: 01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md mục 5.
-- payment_status và payment_method đã có từ V5; thêm thời điểm và người ghi nhận.
ALTER TABLE orders
    ADD COLUMN paid_at timestamptz,
    ADD COLUMN paid_by bigint,
    ADD CONSTRAINT fk_orders_paid_by FOREIGN KEY (paid_by) REFERENCES user_accounts (id),
    -- Đã thanh toán thì có thời điểm và người ghi nhận, chưa thì không có (BR-27).
    ADD CONSTRAINT ck_orders_paid_audit CHECK (
        (paid_at IS NULL) = (paid_by IS NULL) AND (payment_status = 'PAID') = (paid_at IS NOT NULL));
