# Black-box Test Cases — Sprint 2
Trạng thái: Thiết kế và chạy ở Sprint 2 ngày 2026-10-28 theo chế độ "mỗi lần prompt một Sprint"; người dùng chưa review. **61 ca, tất cả Passed** ở lượt chạy ngày 2026-10-28 ([Test Results](../Test-Results/README.md)).

Khác Sprint 1: ca test được viết cùng lúc với code (không viết trước rồi mới code), và mỗi ca **là** một test tự động gọi API HTTP thật trên PostgreSQL thật, tên test bắt đầu bằng ID ca. Mô tả đầy đủ điều kiện trước, bước và kết quả mong đợi nằm trong thân test; bảng dưới là tóm tắt. Dữ liệu và tài khoản như mục 2 của [Sprint 1](BLACK_BOX_TEST_CASES.md); giờ hiện tại mặc định 11:00.

## UC-13 Quản lý món (PB-03)
Đặc tả: [UC-13](../../02-Requirements/Use-Cases/specifications/UC-13-quan-ly-mon.md). Test: `Sprint2AdminBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-MNG-01 | Quản lý xem mọi món kể cả ngừng bán, có điểm công việc và danh sách nhóm | Passed |
| TC-MNG-02 | Thêm món: xuất hiện ngay ở menu khách, còn bán, đặt được | Passed |
| TC-MNG-03 | Sửa giá và điểm: đơn đã xác nhận giữ giá và điểm cũ (BR-05, BR-08) | Passed |
| TC-MNG-04 | Đổi tạm hết rồi còn bán: khách thấy nhãn tạm hết, không đặt được; bán lại thì đặt được | Passed |
| TC-MNG-05 | Ngừng bán: món biến mất khỏi menu khách, đơn cũ vẫn xem được; bán lại thì hiện lại | Passed |
| TC-MNG-06 | Dữ liệu món sai: thiếu tên, giá hoặc điểm âm, nhóm không có, ảnh không phải http | Passed |
| TC-MNG-07 | Sửa hoặc đổi tình trạng món không có: không tìm thấy | Passed |
| TC-MNG-08 | Chỉ quản lý quản lý món; khách, quầy, bếp, chưa đăng nhập bị từ chối | Passed |

## UC-14 Cấu hình giờ mở cửa và quota (PB-04)
Đặc tả: [UC-14](../../02-Requirements/Use-Cases/specifications/UC-14-cau-hinh-gio-va-quota.md). Test: `Sprint2AdminBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-CFG-01 | Quản lý xem khung hôm nay với quota, quota riêng, điểm đã giữ, số đơn | Passed |
| TC-CFG-02 | Đổi giờ mở cửa và quota mặc định: danh sách khung của khách đổi theo | Passed |
| TC-CFG-03 | Quota mặc định thấp hơn điểm khung đang giữ: bị từ chối (BR-11) | Passed |
| TC-CFG-04 | Thu hẹp giờ mở cửa làm mất khung có đơn: bị từ chối; không làm mất thì được (BR-11) | Passed |
| TC-CFG-05 | Giờ sai: không đúng mốc 15 phút, mở không trước đóng, thiếu trường | Passed |
| TC-CFG-06 | Sửa quota riêng một khung: áp dụng ngay cho khách; trả về mặc định bằng null | Passed |
| TC-CFG-07 | Quota riêng thấp hơn điểm đang giữ: bị từ chối; đúng bằng thì được (BR-11) | Passed |
| TC-CFG-08 | Khung ngoài giờ mở cửa hoặc sai mốc: không sửa quota được | Passed |
| TC-CFG-09 | Chỉ quản lý cấu hình được | Passed |

## UC-01 Đăng ký (PB-19)
Đặc tả: [UC-01](../../02-Requirements/Use-Cases/specifications/UC-01-dang-ky-tai-khoan.md). Test: `Sprint2AdminBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-REG-01 | Đăng ký bằng email: tạo tài khoản khách, đăng nhập luôn, mật khẩu lưu dạng băm | Passed |
| TC-REG-02 | Đăng ký bằng số điện thoại có khoảng trắng | Passed |
| TC-REG-03 | Email hoặc số điện thoại đã có tài khoản: bị từ chối kèm lý do | Passed |
| TC-REG-04 | Dữ liệu sai: định danh sai dạng, mật khẩu ngắn hoặc quá dài, thiếu tên | Passed |
| TC-REG-05 | Hai người cùng đăng ký một email: đúng một tài khoản được tạo | Passed |
| TC-REG-06 | Không kèm token CSRF: bị từ chối, không tạo tài khoản | Passed |

## UC-07 Đơn walk-in (PB-11)
Đặc tả: [UC-07](../../02-Requirements/Use-Cases/specifications/UC-07-tao-don-walk-in.md). Test: `Sprint2WalkInBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-WALK-01 | Khung dự kiến là khung hiện tại (không áp mốc 30 phút), kèm số tiền cần thu (BR-18) | Passed |
| TC-WALK-02 | Khung hiện tại không đủ chỗ thì lấy khung sớm nhất còn đủ | Passed |
| TC-WALK-03 | Không còn khung đủ chỗ, sau giờ đóng cửa, hoặc đơn quá lớn: báo lý do | Passed |
| TC-WALK-04 | Xác nhận: đơn walk-in đã thanh toán, không có khách, ghi nhân viên tạo, giữ quota | Passed |
| TC-WALK-05 | Gửi lại cùng mã yêu cầu (liên tiếp và đồng thời): đúng một đơn, giữ quota một lần | Passed |
| TC-WALK-06 | Khung đã báo khách vừa hết chỗ: không tạo đơn, trả khung mới để báo lại khách (BR-19) | Passed |
| TC-WALK-07 | Khung đã qua, ngày khác, sai mốc, ngoài giờ: không xác nhận được | Passed |
| TC-WALK-08 | Món đổi giá hoặc hết hàng; thiếu phương thức thanh toán; không có món: bị từ chối | Passed |
| TC-WALK-09 | Chỉ quầy và quản lý tạo walk-in; khách và bếp bị từ chối | Passed |
| TC-WALK-10 | Vòng đời walk-in: tạo (đã trả), bếp làm, quầy bàn giao bằng mã | Passed |

## NFR-01 hai kênh cùng đặt
Đặc tả: [UC-07](../../02-Requirements/Use-Cases/specifications/UC-07-tao-don-walk-in.md). Test: `Sprint2WalkInBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-CON-08 | 25 pre-order và 25 walk-in 3 điểm cùng lúc vào một khung quota 20: đúng 6 đơn, giữ 18 | Passed |

## UC-18 Đề xuất khung thay thế (PB-09)
Đặc tả: [UC-18](../../02-Requirements/Use-Cases/specifications/UC-18-de-xuat-khung-thay-the.md). Test: `Sprint2WalkInBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-ALT-01 | Khung chọn hết chỗ: đề xuất 3 khung gần nhất, cùng khoảng cách thì khung sau trước (BR-16) | Passed |
| TC-ALT-02 | Khung lân cận cũng đầy: bỏ qua, lấy khung kế tiếp | Passed |
| TC-ALT-03 | Chỉ đề xuất khung còn đặt trước được (BR-15) và tối đa 3 | Passed |
| TC-ALT-04 | Không còn khung nào đủ chỗ: danh sách đề xuất rỗng | Passed |
| TC-ALT-05 | Chọn khung được đề xuất: đặt được, quota kiểm tra lại qua PB-05 | Passed |

## Bổ sung Sprint 1: UC-08 Ghi nhận thanh toán (PB-16, T-29)
Đặc tả: [UC-08](../../02-Requirements/Use-Cases/specifications/UC-08-ghi-nhan-thanh-toan.md). Test: `CounterPaymentBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-PAY-01 | Đơn mới của khách mặc định chưa thanh toán (BR-28) | Passed |
| TC-PAY-02 | Quầy ghi nhận tiền mặt: PAID, ghi người và thời điểm, khách thấy đã thanh toán | Passed |
| TC-PAY-03 | Quầy ghi nhận chuyển khoản tại quầy | Passed |
| TC-PAY-04 | Ghi nhận được ở mọi bước đang xử lý: xác nhận, đang chuẩn bị, sẵn sàng (trả khi nhận, BR-27) | Passed |
| TC-PAY-05 | Ghi nhận lần hai: bị từ chối, giữ nguyên phương thức đã ghi | Passed |
| TC-PAY-06 | Đơn đã bàn giao, hủy hoặc vắng mặt: không ghi nhận thanh toán được | Passed |
| TC-PAY-07 | Đơn không tồn tại: không tìm thấy; phương thức thiếu hoặc lạ: dữ liệu không hợp lệ | Passed |
| TC-PAY-08 | Chỉ quầy và quản lý ghi nhận được; khách, bếp, chưa đăng nhập bị từ chối | Passed |
| TC-PAY-09 | Hai nhân viên cùng bấm: đúng một lần thành công | Passed |
| TC-PAY-10 | Lỗi database khi ghi nhận: báo lỗi, đơn vẫn chưa thanh toán, thử lại được | Passed |
| TC-PAY-11 | Danh sách của quầy: đơn hôm nay đang xử lý, đủ thông tin thu tiền, sắp theo khung | Passed |

## Bổ sung Sprint 1: UC-09 Bàn giao (PB-15, T-30)
Đặc tả: [UC-09](../../02-Requirements/Use-Cases/specifications/UC-09-ban-giao-don.md). Test: `CounterHandoverBlackBoxTest`.

| ID | Kiểm chứng | Kết quả |
|---|---|---|
| TC-HAND-01 | Đơn READY, đã thanh toán, đúng mã: bàn giao được, ghi người và thời điểm | Passed |
| TC-HAND-02 | Mã nhập chữ thường hoặc có khoảng trắng hai đầu vẫn khớp | Passed |
| TC-HAND-03 | Sai mã nhận: bị từ chối kèm lý do, đơn vẫn READY | Passed |
| TC-HAND-04 | Đơn chưa thanh toán: bị từ chối kèm lý do, đơn vẫn READY | Passed |
| TC-HAND-05 | Đơn chưa READY (xác nhận, đang chuẩn bị): bị từ chối, báo trạng thái hiện tại | Passed |
| TC-HAND-06 | Đơn đã bàn giao không bàn giao lại được; đơn hủy hoặc vắng mặt cũng không | Passed |
| TC-HAND-07 | Đơn không tồn tại: không tìm thấy; thiếu hoặc trống mã: dữ liệu không hợp lệ | Passed |
| TC-HAND-08 | Chỉ quầy và quản lý bàn giao được; khách, bếp, chưa đăng nhập bị từ chối | Passed |
| TC-HAND-09 | Hai nhân viên cùng bàn giao một đơn: đúng một lần thành công | Passed |
| TC-HAND-10 | Lỗi database khi bàn giao: báo lỗi, đơn vẫn READY, thử lại được | Passed |
| TC-HAND-11 | Vòng đời trọn vẹn: đặt, bếp làm, quầy thu tiền, quầy bàn giao | Passed |

## Phần giao diện
Các màn mới có test thành phần (Vitest, jsdom) trong `03-SourceCodes/frontend/src/features/sprint2.test.tsx` và `CheckoutPage.test.tsx` (khung gợi ý). Chạy tay trên trình duyệt thật ngày 2026-10-28: đăng ký tài khoản mới rồi vào thực đơn; quản lý thêm món "Burger cá thử" và món hiện ở thực đơn công khai; đặt quota riêng khung 18:00 = 3; quầy tạo đơn walk-in 2 × Burger bò lúc 13:57, khung dự kiến 13:45 (khung hiện tại), đơn hiện trong danh sách quầy. Chưa chạy tay: khung gợi ý (UC-18) và các nhánh lỗi trên trình duyệt thật.
