# Software Requirements Specification
Trạng thái: Bản nháp ngày 2026-10-08. Các quy tắc nghiệp vụ ở mục B.3–B.5 đã được người dùng chọn hoặc chấp nhận trong trao đổi cùng ngày. Ngưỡng NFR ở mục D là nháp, chốt khi chọn môi trường kiểm thử. Bổ sung BR-34 ngày 2026-10-10 khi lập Use Case Model và BR-35–BR-38 cùng ngày khi đặc tả Use Case Sprint 1 ([Use Cases](../Use-Cases/README.md)). Use Case Specification mới có cho phạm vi Sprint 1; các mục sẽ được làm rõ thêm theo từng Sprint.

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
| BR-07 | Công suất bếp tính bằng điểm công việc cho mỗi **khoảng làm 15 phút**. Quản lý đặt công suất mặc định và sửa riêng từng khoảng của ngày hiện tại (trước Sprint 4 gọi là "quota của khung"). | NT1; quyết định 2026-10-08; PB-22, quyết định 2026-10-31 |
| BR-08 | Điểm của đơn = Σ (điểm món × số lượng), lưu lại tại thời điểm xác nhận; sửa điểm món sau đó không làm đổi điểm đơn đã xác nhận. | NT2 |
| BR-09 | Đơn chỉ được xác nhận khi bếp còn **xếp được lịch làm**: mọi đơn đã nhận cùng đơn mới được chia vào các khoảng làm trong cửa sổ của từng đơn mà không khoảng nào vượt công suất (BR-39), và khung nhận còn chỗ ở quầy (BR-41). Áp dụng như nhau cho pre-order và walk-in, dùng chung một bếp. | NT1, NT2; PB-22, quyết định 2026-10-31 |
| BR-10 | Tạo đơn và giữ quota thành công cùng nhau, hoặc không có gì thay đổi. Chỉ báo thành công khi cả hai thành công. | NT5 |
| BR-11 | Không được giảm công suất hay đổi cấu hình tới mức bếp không còn xếp kịp các đơn đã nhận. Giờ mở cửa không được thu hẹp làm mất khung nhận đang có đơn. | NT2; PB-22, quyết định 2026-10-31 |
| BR-12 | Một đơn nhận trong một khung. Đơn mà bếp trống hoàn toàn cũng không làm kịp trong cửa sổ của nó (đơn quá lớn) thì từ chối và báo khách liên hệ cửa hàng. | Quyết định 2026-10-08; PB-22, quyết định 2026-10-31 |
| BR-13 | Đơn có tổng điểm 0 (ví dụ chỉ đồ uống điểm 0) không tốn công bếp, nhưng vẫn tính vào số đơn của khung nhận (BR-41). Quản lý muốn giới hạn loại đơn này thì đặt điểm > 0 cho món. | Hệ quả của BR-08 |
| BR-14 | Đơn ở trạng thái `CONFIRMED`, `PREPARING`, `READY`, `PICKED_UP`, `NO_SHOW` vẫn được tính là đã nhận trong khung nhận. Lịch bếp chỉ xếp việc của đơn còn phải làm (`CONFIRMED`, `PREPARING`). Chỉ hủy theo BR-23, BR-24 mới trả lại chỗ. | Quyết định 2026-10-08; PB-22, quyết định 2026-10-31 |

**Pre-order**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-15 | Chỉ đặt cho khung trong ngày hiện tại, có giờ bắt đầu cách thời điểm đặt ít nhất 30 phút (cấu hình được). | Quyết định 2026-10-08 |
| BR-16 | Khung khách chọn không đủ quota thì đề xuất tối đa 3 khung hợp lệ (theo BR-15) còn đủ quota, gần khung đã chọn nhất, cả trước và sau. Cùng khoảng cách thì khung sau đứng trước. Không có khung nào thì báo hết chỗ trong ngày. | Proposal mục 7; quyết định 2026-10-08 |
| BR-17 | Gửi lại cùng một yêu cầu đặt (do retry, bấm lặp) chỉ tạo tối đa một đơn và trả lại kết quả của đơn đó. | Proposal mục 8 |
| BR-35 | Khi gửi đơn, hệ thống kiểm tra lại giá và tình trạng từng món. Nếu khác nội dung khách đang xem (giá đổi, món tạm hết hoặc ngừng bán) thì không tạo đơn, báo các món thay đổi để khách xem lại rồi gửi lại. Đơn lưu giá từng món tại thời điểm xác nhận; đổi giá sau đó không ảnh hưởng đơn đã xác nhận. | Quyết định 2026-10-10 |
| BR-36 | Danh sách khung cho khách chỉ hiển thị "đặt được" hoặc "không đủ chỗ", tính theo điểm của giỏ hiện tại so với quota còn lại; không hiển thị con số quota hay điểm đã giữ. Khung không đủ chỗ vẫn hiện nhưng không chọn được. Kết quả hiển thị chỉ mang tính tham khảo; việc xác nhận cuối cùng theo BR-09 lúc gửi đơn. | Quyết định 2026-10-10 |
| BR-37 | Mỗi tài khoản khách có tối đa N đơn chưa kết thúc (`CONFIRMED`, `PREPARING`, `READY`) **có khung từ hôm nay trở đi** cùng lúc; mặc định N = 3, cấu hình được. Đặt thêm khi đã đủ N thì bị từ chối. Đơn walk-in không thuộc tài khoản khách nên không tính. Đơn chưa kết thúc của ngày đã qua (đơn tồn) không chặn khách; cửa hàng dọn ở màn quầy. | Quyết định 2026-10-10; N = 3 xác nhận 2026-10-11; chỉ tính từ hôm nay: 2026-10-31 |

**Walk-in**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-18 | Thời gian dự kiến của walk-in là khung nhận sớm nhất, tính từ khung hiện tại, mà bếp còn xếp kịp (làm từ bây giờ) và quầy còn chỗ. Không áp dụng mốc 30 phút của BR-15. | NT3; quyết định 2026-10-08; PB-22, quyết định 2026-10-31 |
| BR-19 | Thời gian dự kiến chỉ để báo khách, chưa giữ quota. Khi nhân viên xác nhận, hệ thống kiểm tra lại theo BR-09; nếu khung đã hết thì hiển thị khung mới để báo lại khách. | NT3, NT5 |
| BR-20 | Walk-in không cần thông tin khách. Thanh toán ghi nhận tại thời điểm xác nhận. | Quyết định 2026-10-08 |

**Bếp và trạng thái đơn**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-21 | Đơn `CONFIRMED` thuộc nhóm "cần chuẩn bị" khi thời điểm hiện tại ≥ giờ bắt đầu khung − N phút (N = 15, cấu hình được); trước đó thuộc nhóm "sắp tới". Bếp được bắt đầu cả đơn sắp tới (làm trước lúc vắng để giảm quá tải giờ đông). | NT6; ngày 2026-10-31 có lúc đổi thành "chỉ bắt đầu từ nhóm cần chuẩn bị", người dùng quyết định khôi phục cùng ngày |
| BR-22 | Hàng chờ bếp sắp theo giờ bắt đầu khung tăng dần; cùng khung thì theo thời điểm xác nhận. Cập nhật tiến độ theo cả đơn. | NT7; quyết định 2026-10-08 |

**Hủy, no-show, trễ**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-23 | Khách hủy được đơn của mình khi đơn còn `CONFIRMED` (cửa hàng **chưa bắt đầu làm**), **chưa thanh toán** và chưa vào cửa sổ chuẩn bị (BR-21). Hoàn toàn bộ điểm quota. Trang đơn ghi rõ hạn và điều kiện này. Đơn đã thanh toán hoặc đã bắt đầu làm thì khách liên hệ cửa hàng; cửa hàng hủy kèm lý do (BR-24), hoàn tiền nếu đã thu (BR-30). | Quyết định 2026-10-08; điều kiện "chưa thanh toán", hiển thị điều kiện: 2026-10-31 |
| BR-24 | Quản lý hủy được đơn `CONFIRMED` hoặc `PREPARING`; nhân viên quầy hủy được đơn walk-in `CONFIRMED`. Bắt buộc nhập lý do; khách pre-order thấy lý do. Hủy từ `CONFIRMED` thì hoàn toàn bộ điểm; hủy từ `PREPARING` thì không hoàn. | Quyết định 2026-10-08 |
| BR-25 | Đơn `READY` quá giờ kết thúc khung + 30 phút (cấu hình được) thì quầy hoặc quản lý được đánh dấu `NO_SHOW` bằng tay. Không hoàn quota. | Quyết định 2026-10-08 |
| BR-26 | Đơn trễ: đơn `CONFIRMED` hoặc `PREPARING` khi đã qua giờ bắt đầu khung nhận; riêng đơn walk-in (được xếp vào khung đang diễn ra) tính từ giờ **kết thúc** khung. Hệ thống chỉ hiển thị, không chặn thao tác. | NT9; quyết định 2026-10-08; ngoại lệ walk-in: quyết định 2026-10-28 |

**Thanh toán và bàn giao**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-27 | Thanh toán tại quầy: pre-order trả khi nhận, walk-in trả khi đặt. Trạng thái `UNPAID` / `PAID`, kèm phương thức (tiền mặt hoặc chuyển khoản tại quầy). Hệ thống chỉ ghi nhận, không xử lý tiền. | NT8; quyết định 2026-10-08 |
| BR-28 | Đơn đã xác nhận không mặc định là đã thanh toán; pre-order bắt đầu ở `UNPAID`. | NT8 |
| BR-29 | Chỉ bàn giao đơn `READY`, mã nhận đúng và `PAID`. Đơn đã bàn giao không bàn giao lại được. | Quyết định 2026-10-08 |
| BR-39 | **Cửa sổ làm**: đơn đặt trước nhận lúc S được bếp làm trong [S − H, S], H là thời gian làm trước tối đa để món còn ngon (mặc định 45 phút, quản lý đổi được, tối thiểu 15 phút). Đơn walk-in làm từ lúc tạo tới hết khung nhận. Bếp xếp lịch theo nguyên tắc hạn sớm làm trước, làm sớm nhất có thể (làm trước lúc vắng). | PB-22, quyết định 2026-10-31 |
| BR-40 | **Dành chỗ cho walk-in**: tổng đơn đặt trước trong mỗi khoảng làm không vượt X% công suất (mặc định 70%, quản lý đổi được); phần còn lại cho khách tại quầy. Quản lý giảm X% thì chỉ chặn đơn đặt trước mới. | PB-22, quyết định 2026-10-31 |
| BR-41 | **Giới hạn quầy**: mỗi khung nhận tối đa N đơn (mặc định 8, quản lý đổi được), tính mọi đơn không bị hủy, kể cả đơn 0 điểm. | PB-22, quyết định 2026-10-31 |
| BR-38 | Mã nhận gồm 6 ký tự lấy ngẫu nhiên từ chữ in hoa và chữ số, bỏ các ký tự dễ nhầm `0`, `O`, `1`, `I`, `L`; không trùng với mã của đơn khác trong cùng ngày. Áp dụng cho cả pre-order và walk-in. | Quyết định 2026-10-10 |
| BR-30 | Hoàn tiền cho đơn đã `PAID` bị hủy thực hiện ngoài hệ thống; lý do hủy ghi nhận việc này. | Giới hạn phạm vi (không phải POS) |

**Tài khoản**
| ID | Quy tắc | Nguồn |
|---|---|---|
| BR-31 | Khách tự đăng ký bằng email hoặc số điện thoại (duy nhất) và mật khẩu. | Quyết định 2026-10-08 |
| BR-32 | Quản lý tạo, khóa và mở khóa tài khoản nhân viên quầy/bếp; tài khoản nhân viên đăng nhập bằng email hoặc số điện thoại như khách (xác nhận 2026-10-11). Tài khoản bị khóa không đăng nhập được, và phiên đang mở của tài khoản đó mất hiệu lực ngay ở thao tác kế tiếp. Tài khoản quản lý đầu tiên có sẵn khi cài đặt. | Quyết định 2026-10-08; hiệu lực với phiên đang mở: 2026-10-31 |
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
| FR-06 | Cấu hình giờ mở cửa và lịch bếp | Quản lý | Đặt giờ mở cửa, công suất bếp mặc định, công suất từng khoảng làm ngày hiện tại, thời gian làm trước tối đa, phần cho đặt trước, giới hạn quầy; xem điểm bếp dự kiến làm và số đơn mỗi khung | BR-06, BR-07, BR-11, BR-39–BR-41 | PB-04, PB-22 |
| FR-07 | Kiểm tra và giữ chỗ (lịch bếp) | Hệ thống | Tính điểm đơn; kiểm tra bếp còn xếp kịp và quầy còn chỗ; ghi nhận cùng lúc tạo đơn | BR-08–BR-14, BR-39–BR-41 | PB-05, PB-22 |
| FR-08 | Giỏ hàng | Khách | Thêm, đổi số lượng, bỏ món; hiển thị tổng tiền | BR-04 | PB-06 |
| FR-09 | Đặt pre-order | Khách | Liệt kê khung hợp lệ kèm tình trạng còn chỗ; tạo đơn theo FR-07; cấp mã nhận; chống trùng | BR-12, BR-15, BR-17, BR-35–BR-38 | PB-07 |
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
| NFR-01 | Nhất quán lịch bếp khi đồng thời | Nhiều request đặt đồng thời (pre-order và walk-in): với các đơn được xác nhận, bếp luôn xếp được lịch không khoảng làm nào vượt công suất; tổng đơn đặt trước mỗi khoảng không vượt phần dành cho đặt trước khi nhận; số đơn mỗi khung nhận không vượt giới hạn quầy; mọi request bị từ chối nhận lỗi hết chỗ. Kiểm bằng test đồng thời (TC-CON-08, TC-PLAN-08) | PB-05, PB-11, PB-22 |
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
