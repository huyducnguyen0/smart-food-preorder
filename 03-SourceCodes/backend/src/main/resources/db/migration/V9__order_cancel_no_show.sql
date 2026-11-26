-- Sprint 3 (PB-10, PB-21, PB-18): hủy đơn và đánh dấu không đến nhận. Thiết kế: DATABASE_DESIGN.md mục 5.
ALTER TABLE orders
    ADD COLUMN cancelled_at timestamptz,
    ADD COLUMN cancelled_by bigint,
    ADD COLUMN cancel_reason varchar(500),
    ADD COLUMN no_show_at timestamptz,
    ADD COLUMN no_show_by bigint,
    ADD CONSTRAINT fk_orders_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES user_accounts (id),
    ADD CONSTRAINT fk_orders_no_show_by FOREIGN KEY (no_show_by) REFERENCES user_accounts (id),
    -- Có thời điểm thì có người thực hiện. Lý do chỉ có khi đã hủy (cửa hàng hủy bắt buộc có lý do, BR-24).
    ADD CONSTRAINT ck_orders_cancelled_audit CHECK ((cancelled_at IS NULL) = (cancelled_by IS NULL)),
    ADD CONSTRAINT ck_orders_cancel_reason CHECK (cancel_reason IS NULL OR cancelled_at IS NOT NULL),
    ADD CONSTRAINT ck_orders_no_show_audit CHECK ((no_show_at IS NULL) = (no_show_by IS NULL));
