-- T-16 (PB-05): cấu hình cửa hàng và khung giờ. Thiết kế: 01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md.
-- store_settings chỉ có đúng một dòng (id = 1): giờ mở cửa và quota mặc định (BR-06, BR-07).
CREATE TABLE store_settings (
    id                   smallint    NOT NULL,
    open_time            time        NOT NULL,
    close_time           time        NOT NULL,
    default_quota_points integer     NOT NULL,
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_store_settings PRIMARY KEY (id),
    CONSTRAINT ck_store_settings_single_row CHECK (id = 1),
    CONSTRAINT ck_store_settings_default_quota CHECK (default_quota_points >= 0),
    CONSTRAINT ck_store_settings_hours CHECK (open_time < close_time)
);

-- Một dòng cho mỗi khung có quota riêng hoặc có đơn giữ quota. Khung chưa có dòng thì quota là quota mặc định
-- và điểm đang giữ là 0 (QuotaService tạo dòng khi cần).
CREATE TABLE time_slots (
    id             bigint  GENERATED ALWAYS AS IDENTITY,
    slot_date      date    NOT NULL,
    start_time     time    NOT NULL,
    quota_override integer,
    held_points    integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_time_slots PRIMARY KEY (id),
    CONSTRAINT uq_time_slots_slot_date_start_time UNIQUE (slot_date, start_time),
    CONSTRAINT ck_time_slots_quota_override CHECK (quota_override >= 0),
    CONSTRAINT ck_time_slots_held_points CHECK (held_points >= 0),
    -- Lưới an toàn cuối cho BR-09 khi khung có quota riêng; với quota mặc định, điều kiện nằm trong câu UPDATE giữ quota.
    CONSTRAINT ck_time_slots_held_within_override CHECK (quota_override IS NULL OR held_points <= quota_override)
);
