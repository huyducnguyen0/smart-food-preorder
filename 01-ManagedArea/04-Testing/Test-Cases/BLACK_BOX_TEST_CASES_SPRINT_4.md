# Black-box Test Cases — Sprint 4
Trạng thái: Thiết kế và chạy ở Sprint 4 ngày 2026-10-31 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. **8 ca mới, tất cả Passed** ở lượt chạy ngày 2026-10-31 ([Test Results](../Test-Results/README.md)).

Như Sprint 2–3: mỗi ca **là** một test tự động gọi API HTTP thật trên PostgreSQL thật, tên test bắt đầu bằng ID ca. Dữ liệu và tài khoản như mục 2 của [Sprint 1](BLACK_BOX_TEST_CASES.md); giờ hiện tại 11:00. Cấu hình lịch bếp mặc định: công suất 20 điểm mỗi khoảng làm 15 phút, làm trước tối đa 45 phút, đặt trước 70% (14 điểm mỗi khoảng), quầy tối đa 8 đơn mỗi khung. Burger bò 5 điểm, Khoai tây chiên 2 điểm, Trà đào 0 điểm.

## PB-22 Lịch bếp
Đặc tả: [UC-17](../../02-Requirements/Use-Cases/specifications/UC-17-kiem-tra-va-giu-quota.md), BR-09, BR-39–BR-41, NFR-01. Test: `Sprint4KitchenPlanBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-PLAN-01 | Làm trước: khung 12:00 nhận 4 đơn × 10 điểm = 40 điểm đặt trước (mô hình cũ chỉ 20); đơn thứ 5 (tổng 50 > 3 khoảng × 14 = 42) bị `SLOT_FULL`, `reason` = `KITCHEN` | Passed |
| TC-PLAN-02 | Dành chỗ cho walk-in: đặt trước đã dùng hết phần 70% của khung 12:00, khách tại quầy vẫn được nhận khung 12:00 (15 điểm) | Passed |
| TC-PLAN-03 | Quầy tối đa 8 đơn: đơn thứ 9 (chỉ đồ uống, 0 điểm) bị `SLOT_FULL`, `reason` = `PICKUP_LIMIT`; khung 12:15 vẫn nhận; danh sách khung báo 12:00 không đặt được | Passed |
| TC-PLAN-04 | Hàng chờ bếp: đơn khung 12:00 có `prepareFrom` và `startAllowedFrom` = 11:15. Bắt đầu lúc 11:14 trả 409 `PREPARATION_TOO_EARLY`; bắt đầu lúc 11:15 thành công | Passed |
| TC-PLAN-05 | Màn quản lý: đơn đặt trước 30 điểm khung 12:00 xếp 14, 14, 2 điểm lần lượt vào 11:15, 11:30, 11:45; `pickupOrders` = 1 | Passed |
| TC-PLAN-06 | Đổi thông số (làm trước 30 phút, 60%, 5 đơn) được lưu; bỏ công suất 11:45 vẫn kịp; giảm 11:30 xuống 10 thì bếp không còn kịp → `QUOTA_BELOW_HELD` | Passed |
| TC-PLAN-07 | Hủy đơn trả lại chỗ trong lịch: khung vừa hết chỗ lại nhận được đơn | Passed |
| TC-PLAN-08 | 60 khách đặt trước và 20 walk-in cùng lúc vào 12:00–12:15: mọi phản hồi là 201 hoặc 409 hết chỗ/quá lớn; mỗi khung ≤ 8 đơn; điểm giữ khớp tổng điểm đơn (NFR-01) | Passed |
| TC-PLAN-09 | Lúc 11:30, walk-in 18 điểm nhận 11:30–11:45, pre-order 22 điểm nhận 12:00 bị `SLOT_FULL/KITCHEN` và không giữ điểm. Pre-order 16 điểm cùng khung thì thành công | Passed |

## Ca cũ đổi theo mô hình mới
Các ca Sprint 1–3 về "khung còn bao nhiêu" chạy với cấu hình tương đương mô hình cũ (làm trước 15 phút, đặt trước 100%, quầy 1000 đơn), nên giữ nguyên ý nghĩa và kết quả. Riêng các ca dưới đây đổi nội dung vì quy tắc đổi:
| ID | Trước | Sau Sprint 4 | Kết quả |
|---|---|---|---|
| TC-CFG-06 | Sửa quota riêng một khung | Sửa công suất riêng một khoảng làm; áp dụng ngay cho khách; `null` về mặc định | Passed |
| TC-CFG-07 | Quota riêng thấp hơn điểm đang giữ bị từ chối | Giảm công suất tới mức bếp không còn kịp đơn đã nhận bị từ chối; vừa đủ thì được (BR-11) | Passed |
| TC-WALK-02 | Khung hiện tại không đủ quota thì lấy khung sớm nhất còn đủ | Bếp không kịp ở khung hiện tại thì lấy khung sớm nhất còn kịp | Passed |
| TC-WALK-03 | Không còn khung đủ quota, sau giờ đóng cửa, đơn quá lớn | Bếp kín lịch, đơn quá lớn so với thời gian còn lại, sau giờ đóng cửa | Passed |
| TC-CON-08 | 25 pre-order và 25 walk-in vào một khung quota 20: đúng 6 đơn | Cùng tải, bếp còn 40 điểm cho khung 12:00: không bao giờ vượt công suất | Passed |

Phần giao diện (nhãn "Bếp làm X/Y · nhận N đơn", ô cấu hình lịch bếp, gợi ý "Theo lịch bếp: làm từ …") kiểm bằng test frontend `features/sprint2.test.tsx`, `features/sprint3.test.tsx`, `features/kitchen/KitchenPage.test.tsx` (đã sửa fixture và nhãn).
