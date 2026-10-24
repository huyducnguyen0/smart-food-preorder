-- Dữ liệu mẫu cho local và demo (T-15). Bảng món này trùng "Menu kiểm thử" trong
-- 01-ManagedArea/04-Testing/Test-Cases/BLACK_BOX_TEST_CASES.md mục 2; sửa ở đây thì sửa cả ở đó.
-- Ảnh minh họa lấy từ Unsplash (giấy phép Unsplash); cửa hàng thật dùng ảnh của mình qua image_url (không bắt buộc, BR-01).
INSERT INTO menu_categories (name, sort_order) VALUES
    ('Burger', 1),
    ('Gà rán', 2),
    ('Đồ uống', 3)
ON CONFLICT (name) DO NOTHING;

-- Chỉ chèn món khi bảng còn trống để chạy lại file này không nhân đôi món (món không có khóa tự nhiên duy nhất).
-- Cột ord ép thứ tự chèn, vì thứ tự hiển thị trong nhóm theo mã món (thứ tự tạo).
INSERT INTO menu_items (category_id, name, description, image_url, price, workload_points, available, discontinued)
SELECT c.id, v.name, v.description, v.image_url, v.price, v.workload_points, v.available, v.discontinued
FROM (VALUES
    (1, 'Burger', 'Burger bò', 'Bò nướng, phô mai cheddar, rau tươi, sốt nhà làm',
        'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&h=400&fit=crop&q=70&auto=format', 45000, 5, true, false),
    (2, 'Burger', 'Burger phô mai đôi', 'Hai lớp bò, phô mai, cà chua, xà lách',
        'https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=400&h=400&fit=crop&q=70&auto=format', 59000, 7, true, false),
    (3, 'Burger', 'Burger gà giòn', 'Gà chiên giòn, bắp cải, sốt cay',
        'https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=400&h=400&fit=crop&q=70&auto=format', 39000, 5, false, false),
    (4, 'Gà rán', 'Gà rán 2 miếng', 'Giòn rụm, ướp 12 tiếng',
        'https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=400&h=400&fit=crop&q=70&auto=format', 55000, 6, true, false),
    (5, 'Gà rán', 'Khoai tây chiên', 'Cỡ vừa, rắc muối biển',
        'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400&h=400&fit=crop&q=70&auto=format', 25000, 2, true, false),
    (6, 'Đồ uống', 'Trà đào', 'Trà đen, đào ngâm, đá',
        'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=400&h=400&fit=crop&q=70&auto=format', 20000, 0, true, false),
    (7, 'Đồ uống', 'Nước suối', 'Chai 500 ml', NULL, 10000, 0, true, false),
    (8, 'Burger', 'Combo cũ', 'Món đã ngừng bán, dùng để kiểm tra BR-03', NULL, 99000, 10, true, true)
) AS v (ord, category_name, name, description, image_url, price, workload_points, available, discontinued)
JOIN menu_categories c ON c.name = v.category_name
WHERE NOT EXISTS (SELECT 1 FROM menu_items)
ORDER BY v.ord;
