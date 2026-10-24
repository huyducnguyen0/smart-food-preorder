-- Dữ liệu mẫu cho môi trường local và demo (T-12). KHÔNG dùng cho môi trường công khai.
-- Mọi tài khoản mẫu dùng chung một mật khẩu, ghi trong 03-SourceCodes/README.md; ở đây chỉ lưu bản băm BCrypt (NFR-06).
-- Migration lặp (R__): chạy lại khi file đổi; ON CONFLICT DO NOTHING giữ nguyên tài khoản đã có.
INSERT INTO user_accounts (login_id, password_hash, display_name, role, locked) VALUES
    ('khach1@example.com', '$2a$10$xop7pPUMgPpIGMP8OQzqqedENTb71LVacSoiPZjH70lz6QZ8NP1a2', 'Khách Một', 'CUSTOMER', false),
    ('0901234567',         '$2a$10$xop7pPUMgPpIGMP8OQzqqedENTb71LVacSoiPZjH70lz6QZ8NP1a2', 'Khách Hai', 'CUSTOMER', false),
    ('khoa@example.com',   '$2a$10$xop7pPUMgPpIGMP8OQzqqedENTb71LVacSoiPZjH70lz6QZ8NP1a2', 'Khách Bị Khóa', 'CUSTOMER', true),
    ('bep1@example.com',   '$2a$10$xop7pPUMgPpIGMP8OQzqqedENTb71LVacSoiPZjH70lz6QZ8NP1a2', 'Bếp Một', 'KITCHEN', false),
    ('quay1@example.com',  '$2a$10$xop7pPUMgPpIGMP8OQzqqedENTb71LVacSoiPZjH70lz6QZ8NP1a2', 'Quầy Một', 'COUNTER', false),
    ('quanly@example.com', '$2a$10$xop7pPUMgPpIGMP8OQzqqedENTb71LVacSoiPZjH70lz6QZ8NP1a2', 'Quản Lý', 'MANAGER', false)
ON CONFLICT (login_id) DO NOTHING;
