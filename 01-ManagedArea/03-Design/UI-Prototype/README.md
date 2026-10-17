# UI Prototype
Trạng thái: Lập ở task T-11 (Sprint 1) ngày 2026-10-17 cho 7 màn hình Sprint 1. **Chưa đánh giá, chưa được chấp thuận** (mục 6, 7): người dùng là stakeholder duy nhất hiện có và chưa xem prototype.

Bước 14 của [quy trình IT3180](../../../00-References/IT3180_PROJECT_PROCESS.md): kiểm tra giao diện và luồng đi **trước khi** đầu tư code. Thứ tự: bố cục → wireframe đơn sắc → phối màu → navigation → đánh giá → sửa → bản được chấp thuận.

## Input
- Đặc tả [UC-02, UC-03, UC-04, UC-05, UC-12](../../02-Requirements/Use-Cases/README.md) mục 5 (luồng chính, A, E), [Activity Diagrams](../../02-Requirements/Activity-Diagrams/README.md).
- [Architecture](../Architecture/ARCHITECTURE.md) mục 5 (API: màn hình hiển thị được dữ liệu gì) và mục 6 (cấu trúc frontend).
- Quy tắc hiển thị: BR-34 (xem menu không cần đăng nhập), BR-36 (khung chỉ hiện "Đặt được/Không đủ chỗ"), UC-03 Ghi chú (không hiện điểm công việc), UC-12 Ghi chú (bếp không thấy giá).

Giao diện không thêm chức năng hay quy tắc nào ngoài đặc tả. Chỗ đặc tả để UI quyết định được ghi ở mục 2.

## 1. Danh sách màn hình
| # | Màn hình | Route | Vai trò | Use case | Wireframe |
|---|---|---|---|---|---|
| 01 | Đăng nhập | `/login` | Mọi người | UC-02 | [wf-01](wireframes/wf-01_dang-nhap.svg) |
| 02 | Menu | `/` | Mọi người | UC-03, UC-04 bước 1 | [wf-02](wireframes/wf-02_menu.svg) |
| 03 | Giỏ hàng | `/cart` | Khách | UC-04.1, A2, E7, E9 | [wf-03](wireframes/wf-03_gio-hang.svg) |
| 04 | Chọn giờ nhận và xác nhận | `/checkout` | Khách | UC-04.2 bước 5–7, E2–E6, E8, E10 | [wf-04](wireframes/wf-04_chon-khung.svg) |
| 05 | Chi tiết đơn / kết quả đặt | `/orders/:id` | Khách | UC-04 bước 11, UC-05 bước 4 | [wf-05](wireframes/wf-05_chi-tiet-don.svg) |
| 06 | Đơn của tôi | `/orders` | Khách | UC-05 bước 1–2, A1 | [wf-06](wireframes/wf-06_don-cua-toi.svg) |
| 07 | Hàng chờ bếp | `/kitchen` | Nhân viên bếp | UC-12 | [wf-07](wireframes/wf-07_hang-cho-bep.svg) |
| — | Không có quyền | `/forbidden` | — | UC-02 E5 | Chỉ có trong prototype |

Nhân viên quầy và quản lý trong Sprint 1 chỉ dùng được Menu (UC-02 mục phân quyền), nên chưa có màn hình riêng.

## 2. Bố cục và các quyết định giao diện
**Bố cục chung**: thanh điều hướng trên cùng (tên cửa hàng, liên kết theo vai trò, đăng xuất), nội dung một cột ở giữa.
- Màn của khách rộng tối đa 720 px, ưu tiên điện thoại: khách đặt từ thiết bị cá nhân (SRS B.1). Bảng hẹp lại thì ẩn cột phụ (đơn giá, thanh toán).
- Hàng chờ bếp rộng tối đa 1080 px, ba cột cạnh nhau: bếp thường dùng màn hình lớn hoặc máy tính bảng đặt cố định. Trên màn hẹp, ba cột xếp chồng.

| Đặc tả để UI quyết định | Quyết định | Lý do |
|---|---|---|
| Màn hình chính từng vai trò (UC-02 bước 6) | Khách, quầy, quản lý → Menu; bếp → Hàng chờ bếp | Đó là việc đầu tiên mỗi vai trò cần làm trong Sprint 1 |
| Cách làm mới hàng chờ bếp (UC-12 bước 3) | Tự tải lại mỗi 30 giây, kèm nút "Tải lại" và giờ cập nhật | Đơn mới hiện ra mà bếp không phải bấm; 30 giây đủ nhanh so với khung 15 phút và nhẹ cho server. Đẩy từ server (WebSocket) để Sprint sau nếu cần |
| Kết quả đặt thành công (UC-04 bước 11) | Dùng lại màn Chi tiết đơn, thêm khung "Đặt đơn thành công" | Một màn hiển thị đơn thay vì hai; khách lưu được địa chỉ trang |
| Lỗi khi gửi đơn | E7, E9 → về Giỏ kèm khung cảnh báo; E6, E8 → ở lại, tải lại danh sách khung; E5, E10 → ở lại, báo lỗi, giữ giỏ | Theo đúng nơi quay về trong Activity Diagram UC-04 |
| Khung "Không đủ chỗ" | Vẫn hiện, xám, không bấm được | BR-36 |
| Món tạm hết | Hiện nhãn "Tạm hết", không có nút thêm | UC-03 bước 2, BR-04 |
| Khách vãng lai bấm "Thêm vào giỏ" | Chuyển tới Đăng nhập, món không tự thêm | UC-03 A3 |
| Nút thao tác chính của bếp | Đơn "Cần chuẩn bị" có nút Bắt đầu màu chính; đơn "Sắp tới" nút viền | Gợi ý thứ tự làm, nhưng vẫn cho bắt đầu sớm (BR-21) |
| Đơn trễ | Nhãn đỏ "TRỄ" và viền trái đỏ trên thẻ | BR-26: chỉ để nhìn, không chặn thao tác |

## 3. Wireframe đơn sắc
Vẽ bằng PlantUML Salt, chỉ có bố cục và nội dung, không màu. Dòng chữ nghiêng ở cuối mỗi hình mô tả trạng thái khác của cùng màn (trống, lỗi).

| | |
|---|---|
| ![wf-01](wireframes/wf-01_dang-nhap.svg) | ![wf-02](wireframes/wf-02_menu.svg) |
| ![wf-03](wireframes/wf-03_gio-hang.svg) | ![wf-04](wireframes/wf-04_chon-khung.svg) |
| ![wf-05](wireframes/wf-05_chi-tiet-don.svg) | ![wf-06](wireframes/wf-06_don-cua-toi.svg) |

![wf-07](wireframes/wf-07_hang-cho-bep.svg)

## 4. Bản phối màu
Tông cam đất cho nút và điểm nhấn, nền kem nhạt: hợp cửa hàng đồ ăn nhanh, chữ trắng trên nút vẫn đủ tương phản. Màu trạng thái dùng thống nhất ở mọi màn: xanh dương = đã xác nhận, vàng nâu = đang làm/cảnh báo, xanh lá = sẵn sàng/đặt được, đỏ = lỗi/trễ/hủy, xám = đã kết thúc/không chọn được.

| Token (biến CSS) | Màu | Dùng cho |
|---|---|---|
| `--color-bg` | `#FAF7F2` | Nền trang |
| `--color-surface` | `#FFFFFF` | Thẻ, bảng, thanh điều hướng |
| `--color-border` | `#E5E7EB` | Viền |
| `--color-text` | `#1F2937` | Chữ chính |
| `--color-text-muted` | `#5B6472` | Chữ phụ |
| `--color-primary` / `-hover` | `#C2410C` / `#9A3412` | Nút chính, liên kết, khung đang chọn |
| `--color-primary-soft` | `#FFEDD5` | Nền mục đang chọn |
| `--color-info` / `-bg` | `#1D4ED8` / `#DBEAFE` | Trạng thái Đã xác nhận |
| `--color-warning` / `-bg` | `#92400E` / `#FEF3C7` | Đang chuẩn bị, cảnh báo |
| `--color-success` / `-bg` | `#166534` / `#DCFCE7` | Sẵn sàng nhận, "Đặt được", thông báo thành công |
| `--color-danger` / `-bg` | `#B91C1C` / `#FEE2E2` | Lỗi, trễ, đã hủy |
| `--color-neutral` / `-bg` | `#374151` / `#F3F4F6` | Đã nhận, không đến nhận, khung không đủ chỗ |

Đã tính độ tương phản theo WCAG 2.1 (ngày 2026-10-17, script tính độ sáng tương đối): mọi cặp chữ/nền ở bảng trên ≥ 4,5:1 (mức AA cho chữ thường). Thấp nhất là chữ trắng trên nút chính 5,18:1; cao nhất chữ chính trên nền trắng 14,68:1. Nút bị khóa không thuộc yêu cầu tương phản. Không dùng màu làm tín hiệu duy nhất: trạng thái luôn có chữ ("Đặt được", "TRỄ"…).

**Prototype có màu, bấm thử được:** [prototype/index.html](prototype/index.html). Một file HTML, dữ liệu giả, không gọi backend. Cách mở: double-click file, hoặc chạy `python -m http.server` trong thư mục `prototype` rồi mở `http://localhost:8000`. Thanh đen trên cùng là bảng điều khiển của prototype (không thuộc giao diện thật): đổi vai trò, chọn tình huống lỗi E2–E10, hàng chờ trống. Cùng bộ biến màu sẽ được chép vào `frontend/src/index.css` ở T-13.

## 5. Navigation Flow
![Navigation Flow](navigation-flow.svg)

Quy tắc chung (UC-02):
- Trang cần đăng nhập mà chưa đăng nhập hoặc phiên hết hạn → Đăng nhập, xong quay về trang đang mở (E4). Giỏ không mất (UC-04 E11).
- Sai vai trò → "Không có quyền" (E5). Ẩn liên kết trên thanh điều hướng chỉ để gọn; quyền thật kiểm ở backend.
- Đăng xuất → Menu.

## 6. Đánh giá prototype
Theo 5 câu hỏi của nguồn. **Chưa có đánh giá của stakeholder.** Cột "Tự kiểm" là AI đối chiếu prototype với đặc tả, không thay cho đánh giá của người dùng.

| Câu hỏi | Tự kiểm (AI, 2026-10-17) | Đánh giá của người dùng |
|---|---|---|
| Có đáp ứng yêu cầu không? | Mọi luồng chính, A, E của UC-02, 03, 04, 05, 12 có chỗ hiển thị (bảng mục 1, 2). Đã bấm thử trong trình duyệt: đặt thành công, E3, E7, E8, bếp bắt đầu đơn, chặn khách vãng lai vào giỏ, chặn bếp vào Đơn của tôi | Chưa đánh giá |
| Thông tin/chức năng có dễ tìm không? | Mỗi vai trò chỉ thấy liên kết của mình; giỏ có số lượng trên thanh điều hướng; mã nhận là chữ lớn nhất trên chi tiết đơn | Chưa đánh giá |
| Bố cục có hợp lý không? | Màn khách một cột, không cuộn ngang ở bề rộng 375 px (đã kiểm); bếp ba cột trên màn rộng, xếp chồng trên màn hẹp | Chưa đánh giá |
| UI có dễ đọc không? | Tương phản đạt AA (mục 4); chữ 16 px; mã nhận dạng chữ đơn cách | Chưa đánh giá |
| Thao tác có dễ không? | Đặt đơn từ menu: thêm món → Giỏ → Chọn giờ nhận → chọn khung → Xác nhận (5 lần bấm với một món). Nút ±, nút bếp cao ≥ 32 px | Chưa đánh giá |

Cách đánh giá đề xuất cho người dùng: mở prototype trên điện thoại và máy tính, tự đóng vai khách đặt một đơn và vai bếp xử lý đơn đó, thử ít nhất 3 tình huống lỗi; ghi điều chưa hợp lý vào cột cuối. Có góp ý thì sửa wireframe/prototype (vòng Feedback → Redesign của nguồn) rồi đánh giá lại.

## 7. Phiên bản được chấp thuận
Chưa có. Khi người dùng chấp thuận, ghi ngày và phiên bản (commit) tại đây. Bản chấp thuận là căn cứ cho T-13, T-15, T-17, T-19, T-20, T-21.

## Công cụ
Wireframe (`wireframes/*.puml`, cú pháp Salt) và navigation (`navigation-flow.puml`) vẽ bằng PlantUML; cách render như [Analysis Model](../Analysis-Model/README.md) mục Công cụ (làm trong thư mục `UI-Prototype` và `UI-Prototype/wireframes`; wireframe không `!include` file kiểu nên không cần mount thư mục cha).

## Output chuyển giao
- T-13, T-15, T-17, T-19, T-20, T-21: route, nội dung từng màn, câu thông báo lỗi, bảng màu.
- T-23: mỗi trạng thái lỗi trong prototype là một tình huống black-box test.
