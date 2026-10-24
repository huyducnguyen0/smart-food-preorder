-- T-18 (PB-07): đơn và dòng đơn. Thiết kế: 01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md.
-- Chỉ tạo các cột Sprint 1 cần; cột cho thanh toán, bàn giao, hủy thêm bằng migration của task tương ứng.
CREATE TABLE orders (
    id              bigint      GENERATED ALWAYS AS IDENTITY,
    customer_id     bigint,
    source          varchar(10) NOT NULL,
    time_slot_id    bigint      NOT NULL,
    status          varchar(12) NOT NULL,
    payment_status  varchar(6)  NOT NULL,
    payment_method  varchar(15),
    workload_points integer     NOT NULL,
    total_amount    integer     NOT NULL,
    pickup_code     char(6)     NOT NULL,
    request_id      uuid,
    confirmed_at    timestamptz NOT NULL,
    preparing_at    timestamptz,
    preparing_by    bigint,
    ready_at        timestamptz,
    ready_by        bigint,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_customer_id FOREIGN KEY (customer_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_orders_time_slot_id FOREIGN KEY (time_slot_id) REFERENCES time_slots (id),
    CONSTRAINT fk_orders_preparing_by FOREIGN KEY (preparing_by) REFERENCES user_accounts (id),
    CONSTRAINT fk_orders_ready_by FOREIGN KEY (ready_by) REFERENCES user_accounts (id),
    CONSTRAINT ck_orders_source CHECK (source IN ('PRE_ORDER', 'WALK_IN')),
    CONSTRAINT ck_orders_status CHECK (status IN ('CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT ck_orders_payment_status CHECK (payment_status IN ('UNPAID', 'PAID')),
    CONSTRAINT ck_orders_payment_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER')),
    CONSTRAINT ck_orders_workload_points CHECK (workload_points >= 0),
    CONSTRAINT ck_orders_total_amount CHECK (total_amount >= 0),
    -- Mã nhận (BR-38): 23 chữ cái bỏ I, L, O và 8 chữ số bỏ 0, 1. Duy nhất toàn bảng, chặt hơn "không trùng trong ngày".
    CONSTRAINT ck_orders_pickup_code CHECK (pickup_code ~ '^[A-HJKMNP-Z2-9]{6}$'),
    CONSTRAINT uq_orders_pickup_code UNIQUE (pickup_code),
    -- BR-17: một mã yêu cầu của một khách tạo tối đa một đơn (lưới an toàn sau khóa tài khoản khi đặt).
    CONSTRAINT uq_orders_customer_id_request_id UNIQUE (customer_id, request_id),
    CONSTRAINT ck_orders_source_customer CHECK (
        (source = 'PRE_ORDER' AND customer_id IS NOT NULL AND request_id IS NOT NULL)
        OR (source = 'WALK_IN' AND customer_id IS NULL)),
    -- BR-27: đã thanh toán thì có phương thức, chưa thì không.
    CONSTRAINT ck_orders_payment CHECK ((payment_status = 'PAID') = (payment_method IS NOT NULL)),
    CONSTRAINT ck_orders_preparing CHECK ((preparing_at IS NULL) = (preparing_by IS NULL)),
    CONSTRAINT ck_orders_ready CHECK ((ready_at IS NULL) = (ready_by IS NULL))
);

CREATE INDEX ix_orders_customer_id ON orders (customer_id);
CREATE INDEX ix_orders_time_slot_id ON orders (time_slot_id);

-- Tên và giá là giá trị tại thời điểm đặt, khác với giá hiện tại của món (BR-35).
CREATE TABLE order_lines (
    id           bigint       GENERATED ALWAYS AS IDENTITY,
    order_id     bigint       NOT NULL,
    menu_item_id bigint       NOT NULL,
    item_name    varchar(100) NOT NULL,
    unit_price   integer      NOT NULL,
    quantity     integer      NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_order_lines_order_id FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_lines_menu_item_id FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT ck_order_lines_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_lines_quantity CHECK (quantity >= 1),
    CONSTRAINT uq_order_lines_order_id_menu_item_id UNIQUE (order_id, menu_item_id)
);
