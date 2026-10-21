-- T-12 (PB-01): tài khoản người dùng. Thiết kế: 01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md.
-- login_id lưu dạng đã chuẩn hóa (email viết thường hoặc số điện thoại bỏ khoảng trắng), xem UC-02 bước 3.
CREATE TABLE user_accounts (
    id            bigint GENERATED ALWAYS AS IDENTITY,
    login_id      varchar(100) NOT NULL,
    password_hash varchar(100) NOT NULL,
    display_name  varchar(100) NOT NULL,
    role          varchar(20)  NOT NULL,
    locked        boolean      NOT NULL DEFAULT false,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_accounts PRIMARY KEY (id),
    CONSTRAINT uq_user_accounts_login_id UNIQUE (login_id),
    CONSTRAINT ck_user_accounts_role CHECK (role IN ('CUSTOMER', 'COUNTER', 'KITCHEN', 'MANAGER'))
);
