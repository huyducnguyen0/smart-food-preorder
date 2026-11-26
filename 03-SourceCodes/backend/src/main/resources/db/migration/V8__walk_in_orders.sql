-- Sprint 2 (PB-11): đơn walk-in do nhân viên quầy tạo. Thiết kế: 01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md.
ALTER TABLE orders
    ADD COLUMN created_by bigint,
    ADD CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by) REFERENCES user_accounts (id),
    -- Walk-in luôn có người tạo; pre-order do chính khách tạo nên không ghi.
    ADD CONSTRAINT ck_orders_walk_in_creator CHECK (source <> 'WALK_IN' OR created_by IS NOT NULL);

-- NFR-02 cho walk-in: một mã yêu cầu chỉ tạo tối đa một đơn walk-in (lưới an toàn sau khóa tài khoản nhân viên).
CREATE UNIQUE INDEX uq_orders_walk_in_request_id ON orders (request_id) WHERE source = 'WALK_IN';
