package vn.nhom15.preorder.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Các con số cấu hình của nghiệp vụ đặt món, đọc từ {@code app.ordering.*} trong application.yaml. Đây là cấu hình hệ
 * thống, không đổi khi vận hành (khác với giờ mở cửa và quota lưu trong database).
 *
 * @param slotMinutes độ dài một khung giờ, phút (BR-06)
 * @param minLeadMinutes phải đặt trước giờ bắt đầu khung ít nhất bấy nhiêu phút (BR-15)
 * @param prepWindowMinutes đơn vào nhóm "cần chuẩn bị" từ bấy nhiêu phút trước giờ bắt đầu khung (BR-21)
 * @param maxOpenOrdersPerCustomer số đơn chưa kết thúc tối đa của một khách (BR-37)
 * @param noShowGraceMinutes đơn sẵn sàng quá giờ kết thúc khung bấy nhiêu phút thì được đánh dấu không đến nhận (BR-25)
 */
@Validated
@ConfigurationProperties(prefix = "app.ordering")
public record OrderingProperties(
        @Min(1) int slotMinutes,
        @Min(0) int minLeadMinutes,
        @Min(0) int prepWindowMinutes,
        @Min(1) int maxOpenOrdersPerCustomer,
        @Min(0) int noShowGraceMinutes) {}
