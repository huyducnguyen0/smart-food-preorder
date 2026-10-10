# Software Requirements Specification
Trạng thái: Bản nháp ngày 2026-10-08. Các quy tắc nghiệp vụ ở mục B.3–B.5 đã được người dùng chọn hoặc chấp nhận trong trao đổi cùng ngày. Ngưỡng NFR ở mục D là nháp, chốt khi chọn môi trường kiểm thử. Bổ sung BR-34 ngày 2026-10-10 khi lập Use Case Model ([Use Cases](../Use-Cases/README.md)). Chưa có Use Case Specification; các mục sẽ được làm rõ thêm theo từng Sprint.

## Input
- [Product Backlog](../Product-Backlog/PRODUCT_BACKLOG.md): User Story và Acceptance Criteria.
- [Project Charter](../../01-ProjectManagement/Project-Charter/PROJECT_CHARTER.md) mục 4.4: phạm vi và 9 nguyên tắc nghiệp vụ (ký hiệu NT1–NT9).
- Quyết định của người dùng ngày 2026-10-08 về các điểm mở trong Charter và backlog.

Nghiệp vụ là mô hình do người dùng chọn cho dự án học phần, gợi ý từ mô hình cửa hàng fast-food; không phải quy trình đã khảo sát tại cửa hàng thật. Các con số mặc định (15 phút, 30 phút...) là lựa chọn của mô hình, cấu hình được, không phải số liệu đo.

## A. Giới thiệu chung

### Mục đích
Đặc tả yêu cầu cho hệ thống web đặt món trước và điều phối nhận món theo năng lực phục vụ của một cửa hàng fast-food, làm cơ sở cho Use Case, thiết kế, lập trình và kiểm thử.

### Phạm vi
Theo Charter mục 4.4: một cửa hàng, một bếp, một điểm bàn giao; pre-order trên web và walk-in nhập tại quầy dùng chung quota; menu, quota, bếp, bàn giao, ghi nhận thanh toán và theo dõi vận hành. Ngoài phạm vi: giao hàng, POS đầy đủ, kho/batch/recipe, cổng thanh toán production, nhiều cửa hàng.

### Thuật ngữ
| Thuật ngữ | Nghĩa trong dự án |
|---|---|
| Pre-order | Đơn khách tự đặt trên web cho một giờ nhận trong tương lai |
| Walk-in | Đơn của khách tại cửa hàng, do nhân viên quầy nhập |
| Khung giờ (slot) | Khoảng 15 phút trong giờ mở cửa, là đơn vị để chọn giờ nhận và tính quota |
| Điểm công việc (workload) | Số nguyên ≥ 0 gán cho mỗi món, biểu thị lượng công việc tương đối để chuẩn bị món đó |
| Quota | Tổng điểm công việc tối đa cửa hàng cam kết cho một khung giờ |
| Giữ quota | Phần quota của một khung bị chiếm bởi đơn đã xác nhận |
| Xác nhận đơn | Hệ thống tiếp nhận đơn và giữ quota thành công; là cam kết tiếp nhận, chưa phải bắt đầu chế biến (NT6) |
| Mã nhận | Mã ngắn cấp khi xác nhận, dùng để xác minh khi bàn giao |
| Cửa sổ chuẩn bị | Khoảng N phút trước giờ bắt đầu khung nhận (mặc định N = 15); đơn trong khoảng này thuộc nhóm "cần chuẩn bị" |

### Tài liệu tham khảo
[Proposal](../../../00-References/Proposal_Nhom15.pdf), [quy trình IT3180](../../../00-References/IT3180_PROJECT_PROCESS.md), [Charter](../../01-ProjectManagement/Project-Charter/PROJECT_CHARTER.md), [Product Backlog](../Product-Backlog/PRODUCT_BACKLOG.md).

## B. Mô tả tổng quan hệ thống

### B.1. Context
Hệ thống là ứng dụng web dùng trong một cửa hàng. Khách pre-order truy cập từ thiết bị cá nhân; nhân viên quầy, nhân viên bếp và quản lý dùng giao diện riêng theo vai trò. Không kết nối hệ thống bên ngoài (POS, cổng thanh toán, SMS/email).

### B.2. Đối tượng sử dụng
| Actor | Mô tả | Cách có tài khoản |
|---|---|---|
| Khách vãng lai | Người chưa đăng nhập: xem menu, đăng ký tài khoản (BR-34) | Không có tài khoản |
| Khách pre-order | Đặt, theo dõi, hủy đơn của mình | Tự đăng ký |
| Nhân viên quầy | Nhập walk-in, ghi nhận thanh toán, bàn giao, hủy walk-in, đánh dấu no-show | Quản lý tạo |
| Nhân viên bếp | Xem hàng chờ, cập nhật tiến độ | Quản lý tạo |
| Quản lý | Cấu hình menu/quota/giờ mở cửa, quản lý tài khoản nhân viên, hủy đơn kèm lý do, theo dõi vận hành; có thể làm thao tác của quầy | Tài khoản đầu tiên có sẵn khi cài đặt |
| Khách walk-in | Không dùng hệ thống trực tiếp; nhận mã từ nhân viên | Không có tài khoản |

### B.3. Business process

**Pre-order**
1. Khách chọn món vào giỏ, chọn khung giờ nhận trong ngày.
2. Hệ thống kiểm tra món còn bán, khung giờ hợp lệ và quota còn lại.
3. Đủ điều kiện → tạo đơn, giữ quota, cấp mã nhận, trạng thái `CONFIRMED`. Không có nhân viên duyệt (NT4).
4. Hết quota → không tạo đơn; đề xuất tối đa 3 khung khác. Khách chọn thì quay lại bước 2.

**Walk-in**
1. Nhân viên quầy nhập món khách chọn.
2. Hệ thống tìm khung sớm nhất còn đủ quota và hiển thị cho nhân viên.
3. Nhân viên báo khách. Khách đồng ý → nhân viên ghi nhận thanh toán và bấm xác nhận; hệ thống kiểm tra lại quota rồi tạo đơn, cấp mã. Khách không đồng ý → không tạo đơn (NT3).
4. Nhân viên đọc hoặc ghi mã cho khách.

**Chuẩn bị và bàn giao (chung cho hai nguồn, NT7)**
1. Bếp thấy đơn ở nhóm "sắp tới", chuyển sang nhóm "cần chuẩn bị" khi vào cửa sổ chuẩn bị.
2. Bếp bấm bắt đầu (`PREPARING`) rồi hoàn tất (`READY`).
3. Khách pre-order thấy trạng thái READY trên trang đơn; khách walk-in được quầy gọi mã.
4. Quầy kiểm tra mã, bảo đảm đã thanh toán, xác nhận bàn giao (`PICKED_UP`).

### B.4. Quy tắc nghiệp vụ

**Menu**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-01 | Mỗi món có: tên, nhóm, giá, điểm công việc (số nguyên ≥ 0), trạng thái còn bán/tạm hết, trạng thái đang bán/ngừng bán. Mô tả và hình ảnh là tùy chọn. | PB-03 |
| BR-02 | Combo là một món bình thường có giá và điểm riêng; không tùy chọn thành phần. | Quyết định 2026-10-08 |
| BR-03 | Món không bị xóa; quản lý chuyển sang ngừng bán để ẩn khỏi menu, giữ dữ liệu đơn cũ. | Quyết định 2026-10-08 |
| BR-04 | Món tạm hết hoặc ngừng bán không thêm được vào giỏ và không dùng được để tạo đơn mới. | Proposal mục 7 |
| BR-05 | Món chuyển tạm hết không ảnh hưởng đơn đã xác nhận. Nếu cửa hàng thật sự không làm được, xử lý bằng hủy đơn theo BR-24. | NT2; quyết định 2026-10-08 |

**Khung giờ và quota**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-06 | Khung giờ dài 15 phút, bắt đầu tại các mốc :00, :15, :30, :45, nằm trong giờ mở cửa do quản lý cấu hình. Độ dài khung là cấu hình hệ thống, không đổi trong vận hành. | Quyết định 2026-10-08 |
| BR-07 | Quota tính bằng điểm công việc. Quản lý đặt quota mặc định cho mọi khung và có thể sửa riêng từng khung của ngày hiện tại. | NT1; quyết định 2026-10-08 |
| BR-08 | Điểm của đơn = Σ (điểm món × số lượng), lưu lại tại thời điểm xác nhận; sửa điểm món sau đó không làm đổi điểm đơn đã xác nhận. | NT2 |
| BR-09 | Đơn chỉ được xác nhận khi điểm đơn ≤ quota còn lại của khung (quota − tổng điểm các đơn đang giữ). Áp dụng như nhau cho pre-order và walk-in. | NT1, NT2 |
| BR-10 | Tạo đơn và giữ quota thành công cùng nhau, hoặc không có gì thay đổi. Chỉ báo thành công khi cả hai thành công. | NT5 |
| BR-11 | Không được đặt quota một khung thấp hơn tổng điểm đang giữ. Giờ mở cửa không được thu hẹp làm mất khung đang có đơn giữ quota. | NT2 |
| BR-12 | Một đơn phải nằm gọn trong một khung. Điểm đơn lớn hơn quota của khung thì từ chối và báo khách liên hệ cửa hàng. | Quyết định 2026-10-08 |
| BR-13 | Đơn có tổng điểm 0 (ví dụ chỉ đồ uống điểm 0) không chiếm quota. Quản lý muốn giới hạn loại đơn này thì đặt điểm > 0 cho món. | Hệ quả của BR-08 |
| BR-14 | Đơn ở trạng thái `CONFIRMED`, `PREPARING`, `READY`, `PICKED_UP`, `NO_SHOW` vẫn giữ quota. Chỉ hủy theo BR-23, BR-24 mới hoàn quota. | Quyết định 2026-10-08 |

**Pre-order**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-15 | Chỉ đặt cho khung trong ngày hiện tại, có giờ bắt đầu cách thời điểm đặt ít nhất 30 phút (cấu hình được). | Quyết định 2026-10-08 |
| BR-16 | Khung khách chọn không đủ quota thì đề xuất tối đa 3 khung hợp lệ (theo BR-15) còn đủ quota, gần khung đã chọn nhất, cả trước và sau. Cùng khoảng cách thì khung sau đứng trước. Không có khung nào thì báo hết chỗ trong ngày. | Proposal mục 7; quyết định 2026-10-08 |
| BR-17 | Gửi lại cùng một yêu cầu đặt (do retry, bấm lặp) chỉ tạo tối đa một đơn và trả lại kết quả của đơn đó. | Proposal mục 8 |

**Walk-in**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-18 | Thời gian dự kiến của walk-in là khung sớm nhất, tính từ khung hiện tại, còn đủ quota trong ngày. Không áp dụng mốc 30 phút của BR-15. | NT3; quyết định 2026-10-08 |
| BR-19 | Thời gian dự kiến chỉ để báo khách, chưa giữ quota. Khi nhân viên xác nhận, hệ thống kiểm tra lại theo BR-09; nếu khung đã hết thì hiển thị khung mới để báo lại khách. | NT3, NT5 |
| BR-20 | Walk-in không cần thông tin khách. Thanh toán ghi nhận tại thời điểm xác nhận. | Quyết định 2026-10-08 |

**Bếp và trạng thái đơn**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-21 | Đơn `CONFIRMED` thuộc nhóm "cần chuẩn bị" khi thời điểm hiện tại ≥ giờ bắt đầu khung − N phút (N = 15, cấu hình được); trước đó thuộc nhóm "sắp tới". Bếp vẫn được bắt đầu đơn sắp tới. | NT6 |
| BR-22 | Hàng chờ bếp sắp theo giờ bắt đầu khung tăng dần; cùng khung thì theo thời điểm xác nhận. Cập nhật tiến độ theo cả đơn. | NT7; quyết định 2026-10-08 |

**Hủy, no-show, trễ**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-23 | Khách hủy được đơn của mình khi đơn còn `CONFIRMED` và chưa vào cửa sổ chuẩn bị (BR-21). Hoàn toàn bộ điểm quota. | Quyết định 2026-10-08 |
| BR-24 | Quản lý hủy được đơn `CONFIRMED` hoặc `PREPARING`; nhân viên quầy hủy được đơn walk-in `CONFIRMED`. Bắt buộc nhập lý do; khách pre-order thấy lý do. Hủy từ `CONFIRMED` thì hoàn toàn bộ điểm; hủy từ `PREPARING` thì không hoàn. | Quyết định 2026-10-08 |
| BR-25 | Đơn `READY` quá giờ kết thúc khung + 30 phút (cấu hình được) thì quầy hoặc quản lý được đánh dấu `NO_SHOW` bằng tay. Không hoàn quota. | Quyết định 2026-10-08 |
| BR-26 | Đơn trễ: đơn `CONFIRMED` hoặc `PREPARING` khi đã qua giờ bắt đầu khung nhận. Hệ thống chỉ hiển thị, không chặn thao tác. | NT9; quyết định 2026-10-08 |

**Thanh toán và bàn giao**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-27 | Thanh toán tại quầy: pre-order trả khi nhận, walk-in trả khi đặt. Trạng thái `UNPAID` / `PAID`, kèm phương thức (tiền mặt hoặc chuyển khoản tại quầy). Hệ thống chỉ ghi nhận, không xử lý tiền. | NT8; quyết định 2026-10-08 |
| BR-28 | Đơn đã xác nhận không mặc định là đã thanh toán; pre-order bắt đầu ở `UNPAID`. | NT8 |
| BR-29 | Chỉ bàn giao đơn `READY`, mã nhận đúng và `PAID`. Đơn đã bàn giao không bàn giao lại được. | Quyết định 2026-10-08 |
| BR-30 | Hoàn tiền cho đơn đã `PAID` bị hủy thực hiện ngoài hệ thống; lý do hủy ghi nhận việc này. | Giới hạn phạm vi (không phải POS) |

**Tài khoản**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-31 | Khách tự đăng ký bằng email hoặc số điện thoại (duy nhất) và mật khẩu. | Quyết định 2026-10-08 |
| BR-32 | Quản lý tạo, khóa và mở khóa tài khoản nhân viên quầy/bếp. Tài khoản bị khóa không đăng nhập được. Tài khoản quản lý đầu tiên có sẵn khi cài đặt. | Quyết định 2026-10-08 |
| BR-33 | Mọi thao tác kiểm tra quyền theo vai trò và quyền sở hữu đơn tại nơi xử lý yêu cầu, không chỉ trên giao diện. | Proposal mục 8; RISK-07 |
| BR-34 | Xem menu không cần đăng nhập. Thêm vào giỏ, đặt đơn và mọi thao tác khác (trừ đăng ký, đăng nhập) cần đăng nhập. | Quyết định 2026-10-10 |

### B.5. Trạng thái đơn

```text
         (tạo + giữ quota thành công)
                    │
                    ▼
              ┌───────────┐  khách (BR-23) / quầy-walk-in, quản lý (BR-24)
              │ CONFIRMED │────────────────────────────────┐
              └─────┬─────┘                                ▼
              bếp   │ bắt đầu                        ┌───────────┐
                    ▼                                │ CANCELLED │
              ┌───────────┐   quản lý (BR-24)        └───────────┘
              │ PREPARING │──────────────────────────────▲
              └─────┬─────┘
              bếp   │ hoàn tất
                    ▼
              ┌───────────┐   quầy/quản lý, quá hạn (BR-25)   ┌─────────┐
              │   READY   │──────────────────────────────────▶│ NO_SHOW │
              └─────┬─────┘                                   └─────────┘
              quầy  │ mã đúng + PAID (BR-29)
                    ▼
              ┌───────────┐
              │ PICKED_UP │
              └───────────┘
```

| Từ | Sang | Ai | Điều kiện | Quota |
|---|---|---|---|---|
| — | CONFIRMED | Hệ thống (pre-order) / quầy (walk-in) | BR-09, BR-10, BR-15/BR-19 | Giữ |
| CONFIRMED | PREPARING | Bếp | — | Giữ |
| PREPARING | READY | Bếp | Đã làm xong cả đơn | Giữ |
| READY | PICKED_UP | Quầy | BR-29 | Giữ |
| CONFIRMED | CANCELLED | Khách / quầy (walk-in) / quản lý | BR-23, BR-24 | Hoàn đủ |
| PREPARING | CANCELLED | Quản lý | BR-24 | Không hoàn |
| READY | NO_SHOW | Quầy / quản lý | BR-25 | Không hoàn |

Không có chuyển trạng thái nào khác. `PICKED_UP`, `CANCELLED`, `NO_SHOW` là trạng thái kết thúc. Không có chức năng sửa đơn sau khi xác nhận (ngoài phạm vi Charter); muốn đổi thì hủy và đặt lại.

## C. Đặc tả chức năng
Mỗi chức năng liên kết story trong Product Backlog. Luồng chính/thay thế chi tiết sẽ nằm trong Use Case Specification.

| ID | Chức năng | Actor | Hành vi hệ thống | Quy tắc | Story |
|---|---|---|---|---|---|
| FR-01 | Đăng ký tài khoản khách | Khách vãng lai | Tạo tài khoản với email/SĐT chưa dùng và mật khẩu | BR-31 | PB-19 |
| FR-02 | Đăng nhập, đăng xuất | Tất cả | Xác thực; chuyển vào chức năng theo vai trò; từ chối tài khoản bị khóa | BR-32, BR-33 | PB-01 |
| FR-03 | Quản lý tài khoản nhân viên | Quản lý | Tạo, khóa, mở khóa tài khoản quầy/bếp | BR-32 | PB-20 |
| FR-04 | Quản lý món | Quản lý | Tạo, sửa món; đặt còn bán/tạm hết; ngừng bán | BR-01–BR-03, BR-05 | PB-03 |
| FR-05 | Xem menu | Khách vãng lai, khách, quầy | Hiển thị món đang bán theo nhóm và tình trạng | BR-04, BR-34 | PB-02 |
| FR-06 | Cấu hình giờ mở cửa và quota | Quản lý | Đặt giờ mở cửa, quota mặc định, quota từng khung ngày hiện tại; xem điểm đã giữ | BR-06, BR-07, BR-11 | PB-04 |
| FR-07 | Kiểm tra và giữ quota | Hệ thống | Tính điểm đơn, kiểm tra quota còn lại, giữ quota cùng lúc tạo đơn | BR-08–BR-14 | PB-05 |
| FR-08 | Giỏ hàng | Khách | Thêm, đổi số lượng, bỏ món; hiển thị tổng tiền | BR-04 | PB-06 |
| FR-09 | Đặt pre-order | Khách | Liệt kê khung hợp lệ kèm tình trạng còn chỗ; tạo đơn theo FR-07; cấp mã nhận; chống trùng | BR-12, BR-15, BR-17 | PB-07 |
| FR-10 | Đề xuất khung thay thế | Hệ thống | Khi khung chọn không đủ quota, trả về tối đa 3 khung | BR-16 | PB-09 |
| FR-11 | Xem đơn của khách | Khách | Danh sách, chi tiết, trạng thái, mã nhận, trạng thái thanh toán, lý do hủy; trang tự cập nhật trạng thái | BR-33 | PB-08, PB-14 |
| FR-12 | Khách hủy đơn | Khách | Hủy và hoàn quota khi thỏa điều kiện; ngược lại từ chối kèm lý do | BR-23 | PB-10 |
| FR-13 | Tạo đơn walk-in | Quầy | Nhập món; hiển thị khung dự kiến; ghi nhận thanh toán; xác nhận sau khi khách đồng ý; cấp mã | BR-18–BR-20, BR-27 | PB-11 |
| FR-14 | Hàng chờ bếp | Bếp | Hiển thị đơn theo nhóm sắp tới / cần chuẩn bị / đang làm, sắp theo BR-22, đánh dấu đơn trễ | BR-21, BR-22, BR-26 | PB-12 |
| FR-15 | Cập nhật tiến độ | Bếp | CONFIRMED → PREPARING → READY | B.5 | PB-13 |
| FR-16 | Ghi nhận thanh toán | Quầy | Chuyển UNPAID → PAID kèm phương thức | BR-27, BR-28 | PB-16 |
| FR-17 | Bàn giao | Quầy | Xem đơn READY; kiểm tra mã; xác nhận bàn giao | BR-29 | PB-15 |
| FR-18 | Cửa hàng hủy đơn | Quản lý, quầy | Hủy kèm lý do theo quyền; xử lý quota | BR-24, BR-30 | PB-21 |
| FR-19 | Đánh dấu no-show | Quầy, quản lý | Liệt kê đơn READY quá hạn; đánh dấu NO_SHOW | BR-25 | PB-18 |
| FR-20 | Theo dõi vận hành | Quản lý | Theo khung trong ngày: quota, điểm đã giữ, số đơn; danh sách đơn đang xử lý và đơn trễ; thống kê ngày: số đơn theo nguồn, tỉ lệ dùng quota, số đơn trễ, hủy, no-show | BR-26 | PB-17 |

## D. Non-functional Requirements / Constraints
Ngưỡng dưới đây là **nháp**, sẽ chốt cùng cấu hình môi trường khi lập kế hoạch kiểm thử. Kết quả chỉ có giá trị cho đúng workload và môi trường đã công bố; không suy rộng thành năng lực production.

| ID | Yêu cầu | Tiêu chí kiểm chứng (nháp) | Story |
|---|---|---|---|
| NFR-01 | Nhất quán quota khi đồng thời | 50 request đặt đồng thời vào một khung chỉ còn đủ quota cho vài đơn: tổng điểm đơn xác nhận ≤ quota, số đơn xác nhận đúng bằng số đơn vừa đủ, các request còn lại bị từ chối hoặc nhận đề xuất | PB-05, PB-07, PB-11 |
| NFR-02 | Không trùng đơn khi gửi lại | Cùng một yêu cầu đặt gửi 5 lần (tuần tự và đồng thời): đúng 1 đơn, quota giữ một lần, các lần sau trả về cùng đơn | PB-07, PB-11 |
| NFR-03 | Phân quyền | Với mỗi vai trò, gọi trực tiếp thao tác của vai trò khác và đơn của khách khác đều bị từ chối | Tất cả |
| NFR-04 | Hiệu năng | Với 50 người dùng đồng thời trên môi trường được công bố: p95 thời gian phản hồi ≤ 1 giây cho xem menu, đặt đơn, xem hàng chờ bếp, bàn giao | PB-07, PB-12, PB-15 |
| NFR-05 | Bảo trì | Đặt đơn, điều phối quota và luồng bếp/bàn giao tách trách nhiệm; kiểm tra qua review kiến trúc và regression test | Thiết kế |
| NFR-06 | Bảo mật mật khẩu | Mật khẩu không lưu dạng đọc được | PB-01, PB-19 |

**Ràng buộc**
- Ứng dụng web; một cửa hàng; không tích hợp hệ thống ngoài.
- Thời gian theo múi giờ cửa hàng (Việt Nam, UTC+7).
- Công nghệ người dùng chọn ngày 2026-10-08: Java Spring Boot, PostgreSQL, React (phiên bản ở [03-SourceCodes](../../../03-SourceCodes/README.md)). Tổ chức kiến trúc quyết định ở bước Architecture.

## Output chuyển giao
SRS dùng để xác định Actor/Use Case (Bước 8), đặc tả Use Case (Bước 9), thiết kế và kiểm thử. Cập nhật phần được làm rõ qua từng Sprint; quy tắc mới hoặc thay đổi phải ghi nguồn quyết định.
