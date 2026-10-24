# Black-box Test Cases — Sprint 1
Trạng thái: Thiết kế ở task T-23 (Sprint 1) ngày 2026-10-24; **chưa chạy test nào** (cột Kết quả đều là "Chưa chạy"). Người dùng chưa review.

Theo Bước 16 của [quy trình IT3180](../../../00-References/IT3180_PROJECT_PROCESS.md): thiết kế test từ yêu cầu, không nhìn code. Mỗi test gắn với tiêu chí chấp nhận (AC), luồng của Use Case và quy tắc nghiệp vụ mà nó kiểm chứng. Kết quả chạy (Expected so với Actual, Passed hoặc Failed) ghi ở [Test Results](../Test-Results/README.md) khi test được chạy; không ghi Passed/Failed ở đây.

## 1. Phạm vi và đầu vào
| Phạm vi | Nguồn |
|---|---|
| UC-02 Đăng nhập, đăng xuất (PB-01) | [UC-02](../../02-Requirements/Use-Cases/specifications/UC-02-dang-nhap-dang-xuat.md) |
| UC-03 Xem menu (PB-02) | [UC-03](../../02-Requirements/Use-Cases/specifications/UC-03-xem-menu.md) |
| UC-04.1 Giỏ hàng (PB-06) và UC-04.2 Chọn khung và gửi đơn (PB-07) | [UC-04](../../02-Requirements/Use-Cases/specifications/UC-04-dat-pre-order.md) |
| UC-17 Kiểm tra và giữ quota (PB-05) | [UC-17](../../02-Requirements/Use-Cases/specifications/UC-17-kiem-tra-va-giu-quota.md) |
| UC-05 Xem đơn của tôi (PB-08) | [UC-05](../../02-Requirements/Use-Cases/specifications/UC-05-xem-don-cua-toi.md) |
| UC-12 Xử lý đơn tại bếp (PB-12, PB-13) | [UC-12](../../02-Requirements/Use-Cases/specifications/UC-12-xu-ly-don-tai-bep.md) |
| Quy tắc, trạng thái, NFR | [SRS](../../02-Requirements/SRS/SRS.md) B.4, B.5, D |

Kỹ thuật chọn test: phân vùng tương đương (hợp lệ, không hợp lệ), giá trị biên (đúng bằng quota, đúng 30 phút, đúng 15 phút), đoán lỗi ở các chỗ nhiều yêu cầu cùng lúc, và mỗi luồng thay thế (A) hoặc luồng lỗi (E) của Use Case có ít nhất một test.

## 2. Dữ liệu và điều kiện chung
**Tài khoản** (mật khẩu chung và danh sách ở [03-SourceCodes/README.md](../../../03-SourceCodes/README.md)):
| Ký hiệu | Tài khoản | Vai trò |
|---|---|---|
| KH1, KH2 | `khach1@example.com`, `0901234567` | Khách |
| KH-KHÓA | `khoa@example.com` | Khách, bị khóa |
| BẾP | `bep1@example.com` | Nhân viên bếp |
| QUẦY | `quay1@example.com` | Nhân viên quầy |
| QL | `quanly@example.com` | Quản lý |

**Cấu hình**: giờ mở cửa 10:00–21:00, khung 15 phút, quota mặc định **20 điểm**, đặt trước tối thiểu 30 phút, cửa sổ chuẩn bị 15 phút, tối đa 3 đơn chưa kết thúc (khớp dữ liệu mẫu và `application.yaml`).

**Menu kiểm thử** (đề xuất; dữ liệu mẫu của T-15 phải khớp bảng này để các test chạy được):
| Món | Nhóm | Giá | Điểm | Tình trạng |
|---|---|---|---|---|
| Burger bò (M1) | Burger | 45.000 | 5 | Còn bán |
| Burger phô mai đôi (M2) | Burger | 59.000 | 7 | Còn bán |
| Burger gà giòn (M3) | Burger | 39.000 | 5 | Tạm hết |
| Gà rán 2 miếng (M4) | Gà rán | 55.000 | 6 | Còn bán |
| Khoai tây chiên (M5) | Gà rán | 25.000 | 2 | Còn bán |
| Trà đào (M6) | Đồ uống | 20.000 | 0 | Còn bán |
| Nước suối (M7) | Đồ uống | 10.000 | 0 | Còn bán |
| Combo cũ (M8) | Burger | 99.000 | 10 | Ngừng bán |

**Thời gian**: các test phụ thuộc giờ cần cố định "bây giờ" (đồng hồ của backend, bean `Clock`). Ký hiệu **T** là thời điểm hiện tại cố định, ví dụ T = 11:00. "Khung 11:30" là khung bắt đầu 11:30.

**Cách chạy**: test API gọi trực tiếp backend (kèm cookie phiên và header chống CSRF); test giao diện thao tác trên frontend; test đồng thời chạy bằng chương trình gửi nhiều yêu cầu cùng lúc. Cột "Cách" ghi **API**, **UI** hoặc **ĐT** (đồng thời).

## 3. UC-02 Đăng nhập, đăng xuất (PB-01, NFR-03, NFR-06)
| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-AUTH-01 | AC1, bước 3–6 | Chưa đăng nhập | Đăng nhập KH1 bằng email và mật khẩu đúng | Thành công; nhận thông tin người dùng (tên, vai trò Khách), không kèm mật khẩu; vào màn hình chính của Khách (Menu) | API, UI | Chưa chạy |
| TC-AUTH-02 | AC1, bước 6 | Chưa đăng nhập | Đăng nhập BẾP | Vào màn hình chính của bếp (Hàng chờ bếp) | UI | Chưa chạy |
| TC-AUTH-03 | AC1, bước 3 (chuẩn hóa) | | Đăng nhập bằng `  KHACH1@Example.COM ` và mật khẩu đúng | Thành công như TC-AUTH-01 | API | Chưa chạy |
| TC-AUTH-04 | AC1, bước 3 (số điện thoại) | | Đăng nhập bằng ` 0901 234 567 ` và mật khẩu đúng | Thành công, đúng tài khoản KH2 | API | Chưa chạy |
| TC-AUTH-05 | AC1, E1 | | Email đúng, mật khẩu sai | Từ chối, lỗi chung "Thông tin đăng nhập không đúng"; không tạo phiên; ô mật khẩu được xóa, định danh giữ nguyên | API, UI | Chưa chạy |
| TC-AUTH-06 | E1 | | Định danh không tồn tại, mật khẩu bất kỳ | Phản hồi **giống hệt** TC-AUTH-05 (cùng mã, cùng nội dung), không lộ tài khoản có tồn tại hay không | API | Chưa chạy |
| TC-AUTH-07 | AC1, E2, BR-32 | | KH-KHÓA, mật khẩu **đúng** | Từ chối kèm thông báo tài khoản bị khóa; không tạo phiên | API | Chưa chạy |
| TC-AUTH-08 | E2 | | KH-KHÓA, mật khẩu **sai** | Báo lỗi chung như TC-AUTH-05, không cho biết tài khoản bị khóa | API | Chưa chạy |
| TC-AUTH-09 | E3 | | Bỏ trống định danh; bỏ trống mật khẩu; bỏ trống cả hai | Báo thiếu từng trường cụ thể, không gọi xác thực; mỗi trường thiếu có thông báo riêng | API, UI | Chưa chạy |
| TC-AUTH-10 | Biên, NFR-06 | | Mật khẩu dài 73 byte trở lên | Từ chối như sai mật khẩu, không gây lỗi hệ thống | API | Chưa chạy |
| TC-AUTH-11 | AC3, bước 7–8 | Đã đăng nhập KH1 | Bấm Đăng xuất; rồi dùng lại phiên cũ gọi "tôi là ai" | Về trang menu; phiên cũ bị từ chối (chưa đăng nhập) | API, UI | Chưa chạy |
| TC-AUTH-12 | E4, BR-34 | Chưa đăng nhập | Truy cập giỏ, đơn của tôi, hàng chờ bếp | Chuyển tới đăng nhập (UI) hoặc trả "chưa đăng nhập" (API); sau khi đăng nhập quay lại trang trước | API, UI | Chưa chạy |
| TC-AUTH-13 | E4 | Đăng nhập KH1, để phiên hết hạn | Thực hiện một thao tác cần đăng nhập | Bị chuyển tới đăng nhập; giỏ hàng vẫn còn sau khi đăng nhập lại | UI | Chưa chạy |
| TC-AUTH-14 | AC2, E5, NFR-03 | Đăng nhập KH1 | Gọi trực tiếp API hàng chờ bếp, bắt đầu và hoàn tất đơn | Mọi lời gọi bị từ chối "không có quyền"; không đổi dữ liệu | API | Chưa chạy |
| TC-AUTH-15 | AC2, E5, NFR-03 | Đăng nhập BẾP | Gọi trực tiếp API đặt đơn, danh sách khung, đơn của tôi | Bị từ chối "không có quyền" | API | Chưa chạy |
| TC-AUTH-16 | AC2, NFR-03 | Đăng nhập QUẦY, rồi QL | Gọi API của khách và API của bếp | Bị từ chối trong Sprint 1 (chưa có chức năng của quầy, quản lý) | API | Chưa chạy |
| TC-AUTH-17 | AC2 (mặc định từ chối) | Đã đăng nhập bất kỳ vai trò | Gọi một đường dẫn `/api/...` không tồn tại | Bị từ chối (chưa đăng nhập: "chưa đăng nhập"; đã đăng nhập: "không có quyền") | API | Chưa chạy |
| TC-AUTH-18 | Bảo mật CSRF | | Gửi đăng nhập, đặt đơn, đăng xuất **không kèm** header chống CSRF | Bị từ chối, không đổi dữ liệu | API | Chưa chạy |
| TC-AUTH-19 | AC4, BR-32 | Cài mới (dữ liệu mẫu) | Đăng nhập QL | Có sẵn tài khoản quản lý, đăng nhập thành công | API | Chưa chạy |
| TC-AUTH-20 | NFR-06 | Có tài khoản mẫu | Đọc bảng tài khoản trong database | Cột mật khẩu chỉ là chuỗi băm (bắt đầu `$2`), không có mật khẩu gốc | Truy vấn DB | Chưa chạy |

## 4. UC-03 Xem menu (PB-02)
| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-MENU-01 | AC1, bước 2 | Chưa đăng nhập | Mở trang menu | Hiện M1, M2, M3, M4, M5, M6, M7 theo nhóm Burger, Gà rán, Đồ uống; mỗi món có tên và giá; sắp theo nhóm | UI, API | Chưa chạy |
| TC-MENU-02 | AC2, BR-04, A2 | | Xem M3 | M3 có nhãn "Tạm hết", không có nút thêm vào giỏ | UI | Chưa chạy |
| TC-MENU-03 | AC3, BR-03 | | Xem toàn bộ menu | M8 (ngừng bán) không hiển thị ở bất kỳ đâu; không có trong dữ liệu API trả về | UI, API | Chưa chạy |
| TC-MENU-04 | AC4, BR-34 | Chưa đăng nhập | Bấm "Thêm vào giỏ" ở M1 | Chuyển tới đăng nhập; sau khi đăng nhập quay lại menu; M1 **chưa** được thêm vào giỏ (A3) | UI | Chưa chạy |
| TC-MENU-05 | Bước 3 | | Lọc theo nhóm Đồ uống | Chỉ còn M6, M7 | UI | Chưa chạy |
| TC-MENU-06 | UC-03 Ghi chú | | Xem dữ liệu menu (API, màn hình) | Không có điểm công việc của món trong dữ liệu trả về cho khách | API | Chưa chạy |
| TC-MENU-07 | A1 | Mọi món ngừng bán hoặc chưa có món | Mở menu | Thông báo "Hiện chưa có món", không lỗi | UI | Chưa chạy |
| TC-MENU-08 | E1 | Backend tắt hoặc lỗi | Mở menu | Thông báo lỗi và nút thử lại; không hiển thị dữ liệu cũ như dữ liệu hiện tại | UI | Chưa chạy |
| TC-MENU-09 | Bước 2 | Đăng nhập KH1 và chưa đăng nhập | So sánh menu hai trạng thái | Nội dung giống nhau | UI | Chưa chạy |
| TC-MENU-10 | A2, E1 UC-04 | Trang menu mở sẵn; sau đó M1 chuyển sang tạm hết | Bấm thêm M1 từ trang cũ | Không thêm; báo món không còn bán và làm mới menu | UI | Chưa chạy |

## 5. UC-04.1 Giỏ hàng (PB-06)
| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-CART-01 | AC1, bước 1–2 | Đăng nhập KH1, giỏ trống | Thêm M1 | Giỏ có M1 × 1, đơn giá 45.000, tổng 45.000 | UI | Chưa chạy |
| TC-CART-02 | Bước 2 | Giỏ có M1 × 1 | Thêm M1 lần nữa | Giỏ vẫn một dòng M1 × 2, tổng 90.000 | UI | Chưa chạy |
| TC-CART-03 | AC1, bước 3–4 | Giỏ có M1 × 2 | Đổi số lượng thành 5; sau đó thành 1 | Thành tiền và tổng tiền cập nhật ngay: 225.000 rồi 45.000 | UI | Chưa chạy |
| TC-CART-04 | Biên, bước 3 | Giỏ có M1 × 1 | Đổi số lượng thành 0, âm hoặc chữ | Không chấp nhận; số lượng giữ nguyên (nhỏ nhất là 1) | UI | Chưa chạy |
| TC-CART-05 | AC1 | Giỏ có M1, M4, M6 | Bỏ M4 | M4 biến mất; tổng tiền = M1 + M6 | UI | Chưa chạy |
| TC-CART-06 | AC1 | Giỏ có M1 × 2, M5 × 1 | Xem giỏ | Từng dòng: tên, số lượng, đơn giá, thành tiền; tổng = 90.000 + 25.000 = 115.000 | UI | Chưa chạy |
| TC-CART-07 | A2 | Giỏ trống | Xem nút "Chọn giờ nhận" | Nút không khả dụng; có đường quay về menu | UI | Chưa chạy |
| TC-CART-08 | AC2, BR-04, E1 | Đăng nhập KH1 | Thêm M3 (tạm hết) và M8 (ngừng bán) bằng cách gọi trực tiếp | Cả hai bị từ chối; giỏ không đổi | API, UI | Chưa chạy |
| TC-CART-09 | Quyết định 2026-10-10 | Giỏ có M1 | Tải lại trang; đóng mở trình duyệt | Giỏ vẫn còn (lưu trên trình duyệt) | UI | Chưa chạy |
| TC-CART-10 | Quyết định 2026-10-10 | Giỏ có M1 của KH1 | Đăng xuất, đăng nhập KH2 trên cùng trình duyệt | KH2 thấy giỏ trống, không thấy giỏ của KH1 | UI | Chưa chạy |

## 6. UC-04.2 Chọn khung giờ (PB-07 AC1, AC5; BR-06, BR-15, BR-36)
Giả định: giỏ có đơn 20 điểm trừ khi ghi khác. T = 11:00.

| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-SLOT-01 | AC1, bước 6, BR-06, BR-15 | KH1, giỏ có M1 × 1 (5 điểm), T = 11:00, chưa có đơn nào | Xem danh sách khung | Khung đầu tiên là **11:30**; mỗi khung dài 15 phút; khung cuối bắt đầu sớm nhất là 20:45; tất cả đều "Đặt được" | API, UI | Chưa chạy |
| TC-SLOT-02 | Biên BR-15 | T = 11:00 | Xem danh sách khung | Khung **11:30 có** (đúng 30 phút); khung **11:15 và 11:00 không có** | API | Chưa chạy |
| TC-SLOT-03 | Biên BR-15 | T = 11:01 | Xem danh sách khung | Khung 11:30 **không có** (còn 29 phút); khung đầu tiên là 11:45 | API | Chưa chạy |
| TC-SLOT-04 | BR-06 | T = 09:00 (trước giờ mở cửa) | Xem danh sách khung | Khung đầu tiên là 10:00 | API | Chưa chạy |
| TC-SLOT-05 | E2 | T = 20:20 (khung cuối 20:45 còn 25 phút) | Xem danh sách khung | Không có khung; báo "Hôm nay không còn khung nhận"; giỏ được giữ | API, UI | Chưa chạy |
| TC-SLOT-06 | BR-15 | Bất kỳ | Xem danh sách khung | Chỉ có khung của hôm nay; không có khung ngày mai | API | Chưa chạy |
| TC-SLOT-07 | AC1, BR-36 | Khung 12:00 đã giữ 18 điểm; giỏ 5 điểm | Xem danh sách khung | Khung 12:00 "Không đủ chỗ" và không chọn được; khung khác "Đặt được" | UI | Chưa chạy |
| TC-SLOT-08 | BR-36 | | Xem toàn bộ dữ liệu danh sách khung | Không có con số quota hay điểm đã giữ; chỉ có trạng thái | API | Chưa chạy |
| TC-SLOT-09 | Biên BR-09 | Khung 12:00 đã giữ 15 điểm; giỏ 5 điểm | Xem khung 12:00 | "Đặt được" (còn đúng 5 điểm, ≤) | API | Chưa chạy |
| TC-SLOT-10 | Biên BR-09 | Khung 12:00 đã giữ 16 điểm; giỏ 5 điểm | Xem khung 12:00 | "Không đủ chỗ" (còn 4 điểm) | API | Chưa chạy |
| TC-SLOT-11 | E3, AC5 | Mọi khung trong ngày còn lại đã giữ 18 điểm trở lên; giỏ 5 điểm | Xem danh sách khung | Báo hết chỗ trong ngày; giỏ được giữ; gợi ý bớt món | API, UI | Chưa chạy |
| TC-SLOT-12 | E4, BR-12 | Giỏ 21 điểm (ví dụ M2 × 3) | Xem danh sách khung | Báo đơn quá lớn cho một khung, liên hệ cửa hàng; không có khung nào chọn được | API, UI | Chưa chạy |
| TC-SLOT-13 | BR-13 | Giỏ chỉ M6, M7 (0 điểm); mọi khung đã đầy | Xem danh sách khung | Mọi khung "Đặt được" (đơn 0 điểm không chiếm quota) | API | Chưa chạy |
| TC-SLOT-14 | Quota riêng, BR-07 | Quản lý đã sửa quota khung 13:00 xuống 4 điểm; giỏ 5 điểm | Xem danh sách khung | Khung 13:00 "Không đủ chỗ"; các khung khác "Đặt được" | API | Chưa chạy |
| TC-SLOT-15 | Phân quyền | Chưa đăng nhập; đăng nhập BẾP | Gọi API danh sách khung | Bị từ chối | API | Chưa chạy |

## 7. UC-04.2 và UC-17 Gửi đơn và giữ quota (PB-07, PB-05)
Giả định: KH1 đã chọn khung 12:00, T = 11:00, quota khung 20 điểm, chưa có đơn nào, trừ khi ghi khác. "Mã yêu cầu" là mã tạo khi khách vào bước xác nhận.

### 7.1 Đặt đơn thành công
| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-ORD-01 | AC2, bước 8–11, BR-28 | Giỏ M1 × 2, M6 × 1 (10 điểm) | Xác nhận đặt | Tạo đúng một đơn: trạng thái Đã xác nhận (CONFIRMED), chưa thanh toán (UNPAID), nguồn pre-order; khung 12:00; tổng tiền 110.000; điểm đơn 10; có mã nhận; giỏ được xóa | API, UI | Chưa chạy |
| TC-ORD-02 | BR-38 | Như TC-ORD-01 | Xem mã nhận | Đúng 6 ký tự, chỉ gồm chữ in hoa và chữ số, không có `0`, `O`, `1`, `I`, `L` | API | Chưa chạy |
| TC-ORD-03 | BR-38 | Tạo 100 đơn trong cùng ngày (nhiều khách, nhiều khung) | So sánh mã nhận | Không có hai đơn trùng mã | API | Chưa chạy |
| TC-ORD-04 | AC1 PB-05, BR-08, BR-14 | Như TC-ORD-01 | Đặt tiếp đơn 10 điểm vào cùng khung; rồi đơn 1 điểm | Đơn 10 điểm thứ hai được nhận (đủ 20); đơn 1 điểm sau đó bị từ chối hết chỗ | API | Chưa chạy |
| TC-ORD-05 | Bước 11, UC-05 | Như TC-ORD-01 | Mở "Đơn của tôi" ngay sau khi đặt | Đơn mới xuất hiện với đúng mã, khung, trạng thái | UI | Chưa chạy |
| TC-ORD-06 | UC-12, NT7 | Như TC-ORD-01 | Đăng nhập BẾP, mở hàng chờ | Đơn mới có trong hàng chờ | UI | Chưa chạy |
| TC-ORD-07 | BR-35 | Đã đặt đơn TC-ORD-01; sau đó quản lý đổi giá M1 | Xem lại đơn cũ | Đơn cũ giữ nguyên giá và tổng tiền lúc xác nhận | API | Chưa chạy |
| TC-ORD-08 | BR-13 | Giỏ chỉ M6 × 2, M7 × 1 (0 điểm); khung 12:00 đã giữ 20 điểm | Xác nhận đặt | Đơn được nhận (CONFIRMED); điểm đang giữ của khung vẫn 20 | API | Chưa chạy |
| TC-ORD-09 | Biên BR-09 | Giỏ M1 × 4 (20 điểm) | Xác nhận đặt | Đơn được nhận; khung giữ đúng 20 điểm (≤ quota) | API | Chưa chạy |
| TC-ORD-10 | UC-17 bước 1, BR-08 | Giỏ M1 × 2 (10) + M4 × 1 (6) + M5 × 2 (4) | Xác nhận đặt | Điểm đơn lưu = 20; khung tăng đúng 20 | API | Chưa chạy |

### 7.2 Luồng thay thế và lỗi
| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-ORD-11 | A1, AC4, BR-17 | Giỏ 10 điểm | Gửi **5 lần liên tiếp** cùng mã yêu cầu | Đúng 1 đơn; cả 5 lần trả về cùng đơn, cùng mã nhận; khung chỉ giữ 10 điểm | API | Chưa chạy |
| TC-ORD-12 | A1, NFR-02 | Giỏ 10 điểm | Gửi **5 lần đồng thời** cùng mã yêu cầu | Đúng 1 đơn; khung giữ 10 điểm; các lần còn lại nhận đúng đơn đó | ĐT | Chưa chạy |
| TC-ORD-13 | A1 | Đơn đã tạo; khách đã có 3 đơn chưa kết thúc (kể cả đơn này); khung đã đầy | Gửi lại cùng mã yêu cầu | Vẫn trả về đơn cũ, không báo lỗi đủ 3 đơn hay hết chỗ | API | Chưa chạy |
| TC-ORD-14 | A1 | | Gửi hai yêu cầu **mã khác nhau** với cùng giỏ | Tạo hai đơn khác nhau, mỗi đơn giữ quota riêng | API | Chưa chạy |
| TC-ORD-15 | E5, AC6, BR-37 | KH1 có 3 đơn Đã xác nhận | Đặt đơn thứ 4 | Bị từ chối, báo đang có 3 đơn chưa hoàn tất và dẫn tới "Đơn của tôi"; giỏ được giữ; không tạo đơn; quota không đổi | API, UI | Chưa chạy |
| TC-ORD-16 | Biên BR-37 | KH1 có 2 đơn chưa kết thúc | Đặt đơn thứ 3 | Được nhận | API | Chưa chạy |
| TC-ORD-17 | BR-37 | KH1 có 3 đơn: 1 Đã xác nhận, 1 Đang chuẩn bị, 1 Sẵn sàng nhận | Đặt thêm | Bị từ chối (cả ba trạng thái đều tính) | API | Chưa chạy |
| TC-ORD-18 | BR-37 | KH1 có 2 đơn chưa kết thúc và 5 đơn đã nhận, hủy, không đến | Đặt thêm | Được nhận (đơn đã kết thúc không tính) | API | Chưa chạy |
| TC-ORD-19 | BR-37 | KH1 đã đủ 3 đơn | KH2 đặt đơn | KH2 được nhận bình thường (giới hạn tính theo từng khách) | API | Chưa chạy |
| TC-ORD-20 | E6, AC3, BR-15 | KH1 chọn khung 11:30 lúc T = 11:00; để T chuyển sang 11:05 | Xác nhận đặt | Bị từ chối, báo khung không còn đặt được; giỏ giữ; quay lại danh sách khung mới | API, UI | Chưa chạy |
| TC-ORD-21 | E6 | Gửi khung không thuộc hôm nay; khung 11:07 (sai mốc); khung 09:00 (ngoài giờ mở); khung 21:00 | Xác nhận đặt | Mỗi trường hợp đều bị từ chối "khung không còn đặt được" | API | Chưa chạy |
| TC-ORD-22 | E7, AC6, BR-35 | Giỏ M1 giá 45.000; quản lý đổi giá M1 thành 47.000 | Xác nhận đặt | Không tạo đơn; báo M1 đổi giá, kèm giá mới; giỏ cập nhật 47.000; quota không đổi; khách xem lại rồi gửi lại thì đặt được | API, UI | Chưa chạy |
| TC-ORD-23 | E7, AC3, BR-04 | Giỏ có M1, M4; M4 chuyển sang tạm hết | Xác nhận đặt | Không tạo đơn; báo M4 không còn bán; M4 bị bỏ khỏi giỏ | API | Chưa chạy |
| TC-ORD-24 | E7 | Giỏ có món bị ngừng bán | Xác nhận đặt | Không tạo đơn; báo món không còn bán | API | Chưa chạy |
| TC-ORD-25 | E7, BR-35 | Giỏ gồm nhiều món đổi giá hoặc hết hàng cùng lúc | Xác nhận đặt | Báo **từng món** đã thay đổi (không chỉ món đầu) | API | Chưa chạy |
| TC-ORD-26 | E8, UC-17 E2, AC5 | KH1 thấy khung 12:00 "Đặt được" (còn 10 điểm); KH2 vừa đặt 15 điểm vào khung 12:00; giỏ KH1 10 điểm | KH1 xác nhận đặt | KH1 bị từ chối "khung vừa hết chỗ"; không tạo đơn; quay lại danh sách khung với trạng thái mới; khung vẫn giữ 15 điểm | API, UI | Chưa chạy |
| TC-ORD-27 | E9, UC-17 E1, BR-12 | Giỏ 21 điểm, khung 12:00 còn trống hoàn toàn | Xác nhận đặt | Bị từ chối "đơn quá lớn cho một khung", khác với "hết chỗ"; quay lại giỏ; quota không đổi | API | Chưa chạy |
| TC-ORD-28 | E9, quota riêng | Quản lý đặt quota riêng khung 12:00 = 6; giỏ 8 điểm | Xác nhận đặt | Bị từ chối "đơn quá lớn" (so với quota riêng, không phải quota mặc định 20) | API | Chưa chạy |
| TC-ORD-29 | E10, UC-17 E3, BR-10, AC3 PB-05 | Giả lập lỗi database ngay sau khi giữ quota (trước khi lưu đơn) | Xác nhận đặt | Báo lỗi hệ thống; **không có đơn**; điểm đang giữ của khung **trở về như trước**; giỏ được giữ; gửi lại cùng mã yêu cầu sau khi hết lỗi thì tạo được đúng một đơn | API (tiêm lỗi) | Chưa chạy |
| TC-ORD-30 | E11 | KH1 đang ở bước xác nhận; phiên hết hạn | Xác nhận đặt | Chuyển tới đăng nhập, không tạo đơn; sau khi đăng nhập giỏ còn nguyên | UI | Chưa chạy |
| TC-ORD-31 | A2 | Giỏ trống | Gửi đơn bằng API với danh sách món rỗng | Bị từ chối dữ liệu không hợp lệ; không tạo đơn | API | Chưa chạy |
| TC-ORD-32 | Dữ liệu sai | | Gửi số lượng 0, âm; món không tồn tại; thiếu mã yêu cầu | Bị từ chối dữ liệu không hợp lệ hoặc món không tồn tại; không tạo đơn | API | Chưa chạy |
| TC-ORD-33 | Phân quyền | Đăng nhập BẾP, QUẦY; chưa đăng nhập | Gọi API đặt đơn | Bị từ chối | API | Chưa chạy |

### 7.3 Nhất quán khi đồng thời (NFR-01, PB-05 AC6; bản rút gọn của Sprint 1)
| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-CON-01 | NFR-01 | 50 khách khác nhau; khung 12:00 trống, quota 20 | Mỗi khách đặt đơn 3 điểm vào khung 12:00, gửi cùng lúc | Đúng **6** đơn được nhận (18 điểm); 44 đơn còn lại bị từ chối hết chỗ; điểm đang giữ = 18; không bao giờ vượt 20 | ĐT | Chưa chạy |
| TC-CON-02 | NFR-01 | Khung 12:00 còn đúng 10 điểm; KH1 và KH2 cùng đặt đơn 8 điểm | Gửi hai yêu cầu cùng lúc | Đúng 1 đơn được nhận, 1 bị từ chối hết chỗ; khung giữ thêm 8 điểm | ĐT | Chưa chạy |
| TC-CON-03 | NFR-01 | 60 yêu cầu với số điểm 1–6 ngẫu nhiên vào khung có quota 15 | Gửi cùng lúc | Điểm đang giữ = tổng điểm các đơn được nhận ≤ 15; mọi đơn bị từ chối đều lớn hơn phần còn lại | ĐT | Chưa chạy |
| TC-CON-04 | NFR-01, BR-17 | KH1 đang có 2 đơn chưa kết thúc | KH1 gửi cùng lúc hai yêu cầu mã khác nhau | Chỉ một đơn được nhận (tổng 3); đơn kia bị từ chối vì đủ 3 đơn; không bao giờ có 4 đơn | ĐT | Chưa chạy |
| TC-CON-05 | NFR-01 | Khung 12:00 và khung 12:15 cùng trống | Hai nhóm khách đặt đồng thời vào hai khung | Mỗi khung giữ đúng theo quota riêng, không ảnh hưởng nhau | ĐT | Chưa chạy |
| TC-CON-06 | NFR-02 | | Gửi lại cùng mã yêu cầu 5 lần, mix tuần tự và đồng thời | Đúng 1 đơn, quota giữ 1 lần | ĐT | Chưa chạy |
| TC-CON-07 | Bất biến BR-09 | Sau mọi test ở mục 7.3 | Đối chiếu database | Với mọi khung: điểm đang giữ = tổng điểm các đơn đang giữ quota của khung ≤ quota | Truy vấn DB | Chưa chạy |

## 8. UC-05 Xem đơn của tôi (PB-08)
| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-MY-01 | AC1, bước 2 | KH1 có 2 đơn; KH2 có 1 đơn | KH1 mở "Đơn của tôi" | Chỉ thấy 2 đơn của mình; mỗi đơn có mã, ngày, khung, trạng thái, thanh toán, tổng tiền | API, UI | Chưa chạy |
| TC-MY-02 | Bước 2 (sắp xếp) | KH1 có đơn mở khung 14:00, đơn mở khung 12:00, đơn đã nhận hôm qua và đơn đã hủy hôm kia | Mở danh sách | Đơn chưa kết thúc ở trên, khung 12:00 trước 14:00; đơn đã kết thúc ở dưới, mới nhất trước | API | Chưa chạy |
| TC-MY-03 | AC1, bước 4 | KH1 có đơn M1 × 2, M6 × 1 | Mở chi tiết đơn | Từng món với số lượng và giá lưu lúc xác nhận; tổng tiền; khung nhận; trạng thái; mã nhận; thanh toán "chưa" kèm nhắc trả tại quầy; thời điểm đặt | API, UI | Chưa chạy |
| TC-MY-04 | AC2, bảng tên trạng thái | Đơn lần lượt ở 6 trạng thái | Xem tên hiển thị | Đã xác nhận, Đang chuẩn bị, Sẵn sàng nhận, Đã nhận, Đã hủy, Không đến nhận; khớp SRS B.5 | UI | Chưa chạy |
| TC-MY-05 | AC2, BR-24 | Đơn bị cửa hàng hủy kèm lý do (Sprint sau) | Xem chi tiết | Hiện lý do hủy | UI | Chưa chạy |
| TC-MY-06 | Bước 5 | Đơn Đang chuẩn bị; bếp hoàn tất đơn | Khách tải lại | Thấy "Sẵn sàng nhận" | UI | Chưa chạy |
| TC-MY-07 | A1 | KH mới, chưa có đơn | Mở "Đơn của tôi" | "Bạn chưa có đơn" kèm liên kết tới menu | UI | Chưa chạy |
| TC-MY-08 | E1, AC2 PB-01, NFR-03 | Đơn của KH2 có mã số X | KH1 mở chi tiết đơn X (sửa địa chỉ hoặc gọi API) | Trả "không tồn tại", **giống hệt** phản hồi khi mở một mã đơn không có thật; không lộ đơn của KH2 | API | Chưa chạy |
| TC-MY-09 | NFR-03 | | Chưa đăng nhập; đăng nhập BẾP | Gọi API danh sách và chi tiết đơn | Bị từ chối | API | Chưa chạy |
| TC-MY-10 | E3 | Backend lỗi | Mở danh sách | Báo lỗi và cho thử lại; không hiện trạng thái cũ như hiện tại | UI | Chưa chạy |
| TC-MY-11 | Ghi chú UC-05 | Đơn walk-in (Sprint sau) | KH mở danh sách | Không thấy đơn walk-in | API | Chưa chạy |

## 9. UC-12 Xử lý đơn tại bếp (PB-12, PB-13)
### 9.1 Hàng chờ
Giả định: BẾP đã đăng nhập; hôm nay là ngày kiểm thử.

| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-KIT-01 | AC1 PB-12, bước 2, BR-21 | T = 11:20. Có 4 đơn: A khung 11:30 Đang chuẩn bị; B khung 11:30 Đã xác nhận; C khung 11:45 Đã xác nhận; D khung 13:00 Đã xác nhận | Mở hàng chờ | Ba nhóm đúng: **Đang làm** = A; **Cần chuẩn bị** = B (11:30 − 15 phút = 11:15 ≤ 11:20); **Sắp tới** = C (11:45 − 15 = 11:30 > 11:20) và D | API, UI | Chưa chạy |
| TC-KIT-02 | Biên BR-21 | Đơn Đã xác nhận khung 12:00; T = 11:45 | Mở hàng chờ | Đơn thuộc nhóm "Cần chuẩn bị" (đúng 15 phút trước) | API | Chưa chạy |
| TC-KIT-03 | Biên BR-21 | Như trên; T = 11:44 | Mở hàng chờ | Đơn thuộc nhóm "Sắp tới" (còn 16 phút) | API | Chưa chạy |
| TC-KIT-04 | AC2, BR-22 | Đơn A khung 12:15 xác nhận lúc 10:00; đơn B khung 12:00 xác nhận lúc 11:00; đơn C khung 12:00 xác nhận lúc 10:30; cùng nhóm | Mở hàng chờ | Thứ tự: C, B, A (theo giờ khung, cùng khung theo giờ xác nhận), không theo thời điểm tạo đơn | API | Chưa chạy |
| TC-KIT-05 | AC3, BR-26 | Đơn Đã xác nhận khung 11:15; T = 11:20 | Mở hàng chờ | Đơn được đánh dấu **trễ** | API, UI | Chưa chạy |
| TC-KIT-06 | Biên BR-26 | Đơn khung 11:30; T = 11:30; rồi T = 11:29 | Mở hàng chờ | Đúng 11:30 thì trễ; 11:29 thì chưa trễ | API | Chưa chạy |
| TC-KIT-07 | BR-26 | Đơn Đang chuẩn bị, khung 11:15, T = 11:20 | Mở hàng chờ | Cũng đánh dấu trễ; vẫn bấm Hoàn tất được (chỉ hiển thị, không chặn) | UI | Chưa chạy |
| TC-KIT-08 | Bước 2 | Đơn Sẵn sàng nhận, Đã nhận, Đã hủy, đơn của hôm qua | Mở hàng chờ | Không có các đơn đó | API | Chưa chạy |
| TC-KIT-09 | Bước 2 | Một đơn | Xem thẻ đơn | Có mã nhận, khung nhận, nguồn pre-order, món và số lượng, thời điểm xác nhận; **không** có giá hay trạng thái thanh toán | API, UI | Chưa chạy |
| TC-KIT-10 | A1 | Không có đơn nào hôm nay | Mở hàng chờ | "Chưa có đơn cần làm" | UI | Chưa chạy |
| TC-KIT-11 | Bước 3 | Hàng chờ đang mở; khách đặt thêm một đơn | Chờ chu kỳ tự làm mới hoặc bấm Tải lại | Đơn mới xuất hiện không cần tải lại cả trang | UI | Chưa chạy |
| TC-KIT-12 | E3 | Backend lỗi | Mở hàng chờ | Báo lỗi và cho thử lại | UI | Chưa chạy |

### 9.2 Cập nhật tiến độ
| ID | Kiểm chứng | Điều kiện trước | Bước và dữ liệu | Kết quả mong đợi | Cách | Kết quả |
|---|---|---|---|---|---|---|
| TC-KIT-13 | AC1 PB-13, bước 4–5 | Đơn Đã xác nhận (nhóm sắp tới) | BẾP bấm Bắt đầu | Đơn thành Đang chuẩn bị; ghi thời điểm và người thực hiện (BẾP); chuyển sang nhóm "Đang làm" | API, UI | Chưa chạy |
| TC-KIT-14 | Bước 6–7, AC1 | Đơn Đang chuẩn bị | BẾP bấm Hoàn tất | Đơn thành Sẵn sàng nhận; ghi thời điểm và người; rời hàng chờ; quota **vẫn giữ** (BR-14) | API, UI | Chưa chạy |
| TC-KIT-15 | UC-05 | Đơn của KH1 vừa Hoàn tất | KH1 mở chi tiết đơn | Thấy "Sẵn sàng nhận" | UI | Chưa chạy |
| TC-KIT-16 | E1, AC2 PB-13 | Đơn đã ở Đang chuẩn bị (do bếp khác bấm) | Bếp thứ hai bấm Bắt đầu trên màn hình cũ | Không đổi đơn; báo trạng thái hiện tại; hàng chờ làm mới | API, UI | Chưa chạy |
| TC-KIT-17 | E1, đồng thời | Đơn Đã xác nhận | Hai bếp bấm Bắt đầu cùng lúc | Đúng một lần thành công; lần kia nhận "trạng thái không hợp lệ"; đơn chỉ có một người bắt đầu | ĐT | Chưa chạy |
| TC-KIT-18 | AC2, SRS B.5 | Đơn Đã xác nhận | Bấm Hoàn tất (bỏ qua bước chuẩn bị) | Bị từ chối; đơn vẫn Đã xác nhận | API | Chưa chạy |
| TC-KIT-19 | AC2, SRS B.5 | Đơn Sẵn sàng nhận | Bấm Bắt đầu hoặc Hoàn tất | Bị từ chối; đơn giữ nguyên | API | Chưa chạy |
| TC-KIT-20 | Ghi chú UC-12 | Đơn Đang chuẩn bị | Tìm cách quay lại Đã xác nhận | Không có chức năng hoàn tác; không có API nào cho phép | API, UI | Chưa chạy |
| TC-KIT-21 | AC3, E2, NFR-03 | KH1; QUẦY; QL | Gọi API bắt đầu, hoàn tất đơn | Bị từ chối "không có quyền"; đơn không đổi | API | Chưa chạy |
| TC-KIT-22 | Mã đơn | BẾP | Bắt đầu một đơn không tồn tại | Trả "không tìm thấy", không lỗi hệ thống | API | Chưa chạy |
| TC-KIT-23 | E3 | Giả lập lỗi database khi cập nhật | Bấm Bắt đầu | Báo lỗi, cho thử lại; trạng thái đơn **không đổi** | API (tiêm lỗi) | Chưa chạy |
| TC-KIT-24 | E4 | Phiên bếp hết hạn | Bấm Bắt đầu | Chuyển tới đăng nhập; đơn không đổi | UI | Chưa chạy |
| TC-KIT-25 | Bước 4 (BR-21) | Đơn Đã xác nhận ở nhóm "Sắp tới" (còn hơn 15 phút) | BẾP bấm Bắt đầu | Được phép (bếp được bắt đầu sớm) | API | Chưa chạy |

## 10. Độ phủ so với Acceptance Criteria
| Story | AC | Test |
|---|---|---|
| PB-01 | AC1 | TC-AUTH-01 → 09 |
| | AC2 | TC-AUTH-14 → 17, TC-MY-08, TC-MY-09, TC-KIT-21 |
| | AC3 | TC-AUTH-11 |
| | AC4 | TC-AUTH-19 |
| PB-02 | AC1 → AC4 | TC-MENU-01 → 04 (AC1 → AC4), TC-MENU-05 → 10 |
| PB-05 | AC1, AC2 | TC-ORD-04, 10, TC-SLOT-07, 09, 10 |
| | AC3 | TC-ORD-29 |
| | AC4 | TC-ORD-27, 28, TC-SLOT-12 |
| | AC5 | TC-ORD-04, 26 (giữ), TC-KIT-14 (vẫn giữ khi READY); hoàn quota thuộc Sprint sau |
| | AC6 | TC-CON-01 → 07 |
| PB-06 | AC1, AC2 | TC-CART-01 → 10 |
| PB-07 | AC1 | TC-SLOT-01 → 07, 15 |
| | AC2 | TC-ORD-01, 02 |
| | AC3 | TC-ORD-20 → 25 |
| | AC4 | TC-ORD-11 → 14, TC-CON-06 |
| | AC5 | TC-SLOT-11, TC-ORD-26 |
| | AC6 | TC-ORD-15 → 19, 22 |
| PB-08 | AC1 | TC-MY-01 → 03 |
| | AC2 | TC-MY-04, 05 |
| PB-12 | AC1 → AC3 | TC-KIT-01 → 09 |
| PB-13 | AC1 | TC-KIT-13 → 15, 25 |
| | AC2 | TC-KIT-16 → 20 |
| | AC3 | TC-KIT-21 |

Luồng thay thế và lỗi của Use Case: UC-02 E1–E5 (TC-AUTH-05 → 17); UC-03 A1–A3, E1 (TC-MENU-04, 07, 08, 10); UC-04 A1, A2, E1–E11 (TC-ORD-11 → 32, TC-CART-07, 08, TC-SLOT-05, 11, 12); UC-05 A1, E1–E3 (TC-MY-07, 08, 10); UC-12 A1, E1–E4 (TC-KIT-10, 16, 17, 21, 23, 24); UC-17 E1–E3 (TC-ORD-27 → 29).

## 11. Việc cần làm trước khi chạy
- Dữ liệu mẫu menu (T-15) khớp bảng ở mục 2; có cách cố định giờ hiện tại và tiêm lỗi database cho TC-ORD-29, TC-KIT-23.
- Test chạy ở T-25; kết quả ghi vào Test Results, mỗi dòng gắn ID test ở trên.
- Test white-box cho logic giữ quota (T-24) bổ sung, không thay thế các test này.
