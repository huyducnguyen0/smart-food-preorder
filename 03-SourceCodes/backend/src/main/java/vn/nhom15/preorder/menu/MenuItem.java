package vn.nhom15.preorder.menu;

/**
 * Một món đọc từ bảng {@code menu_items}.
 *
 * @param id khóa chính
 * @param categoryId nhóm món
 * @param name tên món
 * @param description mô tả, có thể null (BR-01)
 * @param imageUrl đường dẫn ảnh, có thể null (BR-01)
 * @param price giá hiện tại, đồng
 * @param workloadPoints điểm công việc của một phần (BR-01); thông tin vận hành, không hiển thị cho khách
 * @param available còn bán (true) hoặc tạm hết (false) (BR-04)
 * @param discontinued đã ngừng bán: ẩn khỏi menu nhưng giữ dữ liệu đơn cũ (BR-03)
 */
public record MenuItem(
        long id,
        long categoryId,
        String name,
        String description,
        String imageUrl,
        int price,
        int workloadPoints,
        boolean available,
        boolean discontinued) {

    /** Đặt được: còn bán và chưa ngừng bán (BR-04). */
    public boolean isOrderable() {
        return available && !discontinued;
    }
}
