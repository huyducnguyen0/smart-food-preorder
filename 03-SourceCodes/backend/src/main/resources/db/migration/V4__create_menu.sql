-- T-15 (PB-02): nhóm món và món. Thiết kế: 01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md.
CREATE TABLE menu_categories (
    id         bigint      GENERATED ALWAYS AS IDENTITY,
    name       varchar(50) NOT NULL,
    sort_order integer     NOT NULL DEFAULT 0,
    CONSTRAINT pk_menu_categories PRIMARY KEY (id),
    CONSTRAINT uq_menu_categories_name UNIQUE (name)
);

-- Món không bị xóa: ngừng bán thì đặt discontinued = true để ẩn khỏi menu và giữ dữ liệu đơn cũ (BR-03).
-- available = false nghĩa là tạm hết: vẫn hiện nhưng không đặt được (BR-04).
CREATE TABLE menu_items (
    id              bigint       GENERATED ALWAYS AS IDENTITY,
    category_id     bigint       NOT NULL,
    name            varchar(100) NOT NULL,
    description     varchar(500),
    image_url       varchar(500),
    price           integer      NOT NULL,
    workload_points integer      NOT NULL,
    available       boolean      NOT NULL DEFAULT true,
    discontinued    boolean      NOT NULL DEFAULT false,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_menu_items PRIMARY KEY (id),
    CONSTRAINT fk_menu_items_category_id FOREIGN KEY (category_id) REFERENCES menu_categories (id),
    CONSTRAINT ck_menu_items_price CHECK (price >= 0),
    CONSTRAINT ck_menu_items_workload_points CHECK (workload_points >= 0)
);

CREATE INDEX ix_menu_items_category_id ON menu_items (category_id);
