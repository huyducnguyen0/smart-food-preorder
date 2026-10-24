package vn.nhom15.preorder.kitchen;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Hàng chờ bếp chia ba nhóm (UC-12 bước 2).
 *
 * @param serverTime thời điểm máy chủ dựng danh sách, để màn hình ghi "cập nhật lúc"
 * @param preparing đang làm
 * @param dueSoon cần chuẩn bị (đã tới cửa sổ chuẩn bị của khung, BR-21)
 * @param upcoming sắp tới (chưa tới cửa sổ chuẩn bị)
 */
public record KitchenQueue(
        OffsetDateTime serverTime,
        List<KitchenOrder> preparing,
        List<KitchenOrder> dueSoon,
        List<KitchenOrder> upcoming) {}
