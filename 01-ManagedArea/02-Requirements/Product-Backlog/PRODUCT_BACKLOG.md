# Product Backlog
Trạng thái: Bản nháp ngày 2026-10-08, đã rà soát và cập nhật theo quyết định nghiệp vụ cùng ngày (ghi trong [SRS](../SRS/SRS.md) mục B.4–B.5). Người dùng chưa duyệt thứ tự ưu tiên; chưa xác nhận với stakeholder bên ngoài. Backlog được cập nhật liên tục.

## Input và cơ sở
- [Quy trình IT3180](../../../00-References/IT3180_PROJECT_PROCESS.md), Bước 5: story → estimate → NFR → ưu tiên → acceptance criteria → cập nhật và xác nhận lại.
- [Project Charter](../../01-ProjectManagement/Project-Charter/PROJECT_CHARTER.md), mục 4.4: phạm vi và 9 nguyên tắc nghiệp vụ.
- [Proposal Nhóm 15](../../../00-References/Proposal_Nhom15.pdf), mục 5.1, 7, 8.
- [WBS](../../01-ProjectManagement/WBS/WBS.md) nhóm 4, [Risk Register](../../01-ProjectManagement/Risk-Management/RISK_REGISTER.md).
- [SRS](../SRS/SRS.md): quy tắc nghiệp vụ BR-01–BR-33, trạng thái đơn, chức năng FR-01–FR-20, NFR.

Nguồn yêu cầu là mô hình do người dùng chọn cho dự án học phần, không phải kết quả khảo sát cửa hàng thật.

## Cách đọc
- **Thứ tự**: vị trí trong danh sách ưu tiên; mục trên làm trước. Đây là thông tin chính để chọn việc cho Sprint.
- **Priority**: High = thiếu thì không đạt mục tiêu cốt lõi của Charter; Medium = cần cho phạm vi đã duyệt nhưng làm sau luồng cốt lõi; Low = có thể thu gọn nếu thiếu thời gian. Thứ tự còn xét dependency và risk.
- **Estimate**: cỡ tương đối S < M < L, chưa quy đổi giờ vì chưa có dữ liệu năng suất. Điều chỉnh ở Sprint Planning.
- **AC**: điều kiện để story được chấp nhận. Quy tắc chi tiết tham chiếu mã `BR-xx` trong SRS thay vì chép lại, để chỉ có một nơi đặc tả.
- **Nguồn**: `NTn` = nguyên tắc số n trong Charter 4.4; `WBS x.y` = gói công việc.
- **Status**: `Backlog` (chưa chọn) → `Sprint N` (đã chọn vào Sprint) → `Done` (đạt Definition of Done trong README).
- ID là định danh cố định; story mới nhận ID tiếp theo, không đánh số lại.

## 1. Danh sách theo thứ tự ưu tiên

| Thứ tự | ID | Tóm tắt | Priority | Estimate | Phụ thuộc | Status |
|---|---|---|---|---|---|---|
| 1 | PB-01 | Đăng nhập và phân quyền theo vai trò | High | M | — | Sprint 1 |
| 2 | PB-02 | Xem menu | High | S | Dữ liệu món (PB-03 hoặc dữ liệu mẫu) | Sprint 1 |
| 3 | PB-05 | Kiểm tra và giữ quota dùng chung | High | M | Điểm món, quota (PB-03, PB-04 hoặc dữ liệu mẫu) | Sprint 1 |
| 4 | PB-06 | Giỏ hàng | High | S | PB-02 | Sprint 1 |
| 5 | PB-07 | Đặt pre-order theo khung giờ, xác nhận tự động | High | M | PB-05, PB-06 | Sprint 1 |
| 6 | PB-08 | Khách xem đơn, trạng thái và mã nhận | High | S | PB-07 | Sprint 1 |
| 7 | PB-12 | Hàng chờ bếp | High | M | PB-07 | Sprint 1 |
| 8 | PB-13 | Bếp cập nhật tiến độ theo đơn | High | S | PB-12 | Sprint 1 |
| 9 | PB-16 | Ghi nhận thanh toán tại quầy | High | S | PB-07 | Backlog (làm thêm Sprint 1) |
| 10 | PB-15 | Quầy xác minh mã và bàn giao | High | M | PB-08, PB-13, PB-16 | Backlog (làm thêm Sprint 1) |
| 11 | PB-03 | Quản lý menu và tình trạng bán | High | M | PB-01 | Backlog |
| 12 | PB-04 | Cấu hình giờ mở cửa và quota theo khung | High | M | PB-01 | Backlog |
| 13 | PB-19 | Khách tự đăng ký tài khoản | High | S | PB-01 | Backlog |
| 14 | PB-11 | Tạo đơn walk-in sau khi khách đồng ý thời gian | High | M | PB-05, PB-16 | Backlog |
| 15 | PB-09 | Đề xuất khung giờ thay thế | High | M | PB-07 | Backlog |
| 16 | PB-10 | Khách hủy đơn | Medium | S | PB-07 | Backlog |
| 17 | PB-21 | Cửa hàng hủy đơn kèm lý do | Medium | M | PB-07, PB-11 | Backlog |
| 18 | PB-14 | Trang đơn tự cập nhật khi READY | Medium | S | PB-08, PB-13 | Backlog |
| 19 | PB-20 | Quản lý tài khoản nhân viên | Medium | S | PB-01 | Backlog |
| 20 | PB-17 | Theo dõi tải, đơn trễ và thống kê | Medium | M | PB-05, PB-13 | Backlog |
| 21 | PB-18 | Đánh dấu no-show | Low | S | PB-15 | Backlog |

Lý do sắp xếp:
- **1–10** tạo luồng tối giản đi hết vòng đời một đơn: đặt theo khung → giữ quota → bếp → thanh toán → bàn giao (hướng Sprint 1 trong README). PB-16 đứng trước PB-15 vì bàn giao yêu cầu đơn đã `PAID` (BR-29). Tài khoản, menu và quota dùng dữ liệu mẫu nên đăng ký và các màn quản lý chưa chặn luồng này.
- **11–12** màn quản lý menu và quota: vẫn High vì cửa hàng cần tự cấu hình, nhưng xếp sau luồng cốt lõi khi lập Sprint 1 (2026-10-08), do dữ liệu mẫu thay thế tạm được.
- **13–15** đăng ký, walk-in và đề xuất khung: hoàn thiện điều phối khi có hai kênh tranh quota; NFR-01 cần kiểm chứng cùng lúc (hướng Sprint 2).
- **16–21** hủy, ngoại lệ, quản trị và theo dõi (hướng Sprint 3).
- Sprint 1 chọn thứ tự 1–8, làm thêm 9–10 nếu dư thời gian: xem [Sprint 1 Backlog](../../01-ProjectManagement/SPRINT_1_BACKLOG.md).

## 2. Chi tiết User Story và Acceptance Criteria

### A. Tài khoản và menu
| ID | User Story | Acceptance Criteria | Nguồn |
|---|---|---|---|
| PB-01 | Là **người dùng hệ thống** (khách, nhân viên quầy, nhân viên bếp, quản lý), tôi muốn đăng nhập và chỉ thực hiện được thao tác của vai trò mình, để dữ liệu và thao tác được bảo vệ. | 1. Đúng thông tin thì vào chức năng của vai trò; sai thông tin hoặc tài khoản bị khóa thì báo lỗi và không cho vào.<br>2. Gọi thẳng thao tác ngoài quyền hoặc đơn của khách khác đều bị từ chối (BR-33, NFR-03).<br>3. Có đăng xuất.<br>4. Có sẵn tài khoản quản lý đầu tiên (BR-32). | Charter 4.4; proposal 7; WBS 4.2 |
| PB-19 | Là **khách**, tôi muốn tự đăng ký tài khoản, để đặt món trước được. | 1. Đăng ký bằng email hoặc số điện thoại chưa dùng, cùng mật khẩu (BR-31).<br>2. Email/SĐT đã tồn tại thì từ chối kèm lý do.<br>3. Mật khẩu không lưu dạng đọc được (NFR-06). | Charter 4.4; BR-31; WBS 4.2 |
| PB-20 | Là **quản lý**, tôi muốn tạo, khóa và mở khóa tài khoản nhân viên quầy/bếp, để kiểm soát ai được thao tác trong cửa hàng. | 1. Tạo tài khoản với vai trò quầy hoặc bếp.<br>2. Khóa thì tài khoản không đăng nhập được; mở khóa thì dùng lại được (BR-32).<br>3. Chỉ quản lý làm được. | BR-32; WBS 4.2 |
| PB-03 | Là **quản lý**, tôi muốn quản lý món, tình trạng còn bán/tạm hết và điểm công việc, để menu và quota phản ánh khả năng phục vụ. | 1. Tạo, sửa món với các thông tin ở BR-01; combo là một món (BR-02).<br>2. Chuyển còn bán ↔ tạm hết; ngừng bán thay cho xóa (BR-03).<br>3. Thay đổi chỉ ảnh hưởng đơn tạo sau đó; đơn đã xác nhận giữ nguyên (BR-05, BR-08).<br>4. Không quản lý tồn kho nguyên liệu. | Charter 4.4; WBS 4.3 |
| PB-02 | Là **khách pre-order** hoặc **nhân viên quầy**, tôi muốn xem menu và tình trạng còn bán/tạm hết, để chọn món đặt được. | 1. Hiển thị món đang bán theo nhóm, kèm giá và tình trạng.<br>2. Món tạm hết được đánh dấu, không chọn được (BR-04).<br>3. Món ngừng bán không hiển thị. | Charter 4.4; proposal 7; WBS 4.3 |

### B. Quota và điều phối
| ID | User Story | Acceptance Criteria | Nguồn |
|---|---|---|---|
| PB-04 | Là **quản lý**, tôi muốn cấu hình giờ mở cửa và quota điểm công việc cho từng khung 15 phút, để giới hạn lượng việc cửa hàng cam kết. | 1. Đặt giờ mở cửa; hệ thống chia thành các khung 15 phút (BR-06).<br>2. Đặt quota mặc định và sửa quota từng khung của ngày hiện tại (BR-07).<br>3. Xem được quota và điểm đã giữ của mỗi khung.<br>4. Không đặt được quota thấp hơn điểm đã giữ; không thu hẹp giờ mở cửa làm mất khung đang có đơn (BR-11). | Charter 4.4, NT1, NT2; WBS 4.6 |
| PB-05 | Là **chủ cửa hàng**, tôi muốn mọi đơn chỉ được xác nhận khi giữ được quota dùng chung, để không nhận vượt khả năng và không phá cam kết với đơn trước. | Nơi duy nhất trong backlog cho quy tắc quota; PB-07, PB-09, PB-11 dùng lại.<br>1. Điểm đơn tính và lưu theo BR-08.<br>2. Pre-order và walk-in trừ cùng một quota; đơn chỉ xác nhận khi điểm đơn ≤ quota còn lại (BR-09).<br>3. Tạo đơn và giữ quota thành công cùng nhau hoặc không có gì thay đổi (BR-10).<br>4. Đơn vượt quota của cả khung bị từ chối (BR-12).<br>5. Quota giữ và hoàn theo BR-14.<br>6. Đúng khi có request đồng thời được kiểm chứng ở NFR-01. | Charter 4.4, NT1, NT2, NT5; proposal 7; WBS 4.6; RISK-04 |

### C. Pre-order
| ID | User Story | Acceptance Criteria | Nguồn |
|---|---|---|---|
| PB-06 | Là **khách pre-order**, tôi muốn thêm, đổi số lượng và bỏ món trong giỏ, để chuẩn bị đơn trước khi đặt. | 1. Thêm, đổi số lượng, bỏ món; xem lại nội dung và tổng tiền.<br>2. Không thêm được món tạm hết hoặc ngừng bán (BR-04). | Charter 4.4; WBS 4.3 |
| PB-07 | Là **khách pre-order**, tôi muốn chọn khung giờ nhận trong ngày và gửi đơn, để được xác nhận ngay khi cửa hàng còn khả năng phục vụ. | 1. Chỉ hiển thị và cho chọn khung hợp lệ theo BR-15, kèm tình trạng còn chỗ.<br>2. Đơn hợp lệ và giữ được quota (PB-05) thì **tự động xác nhận**, cấp mã nhận, trạng thái `CONFIRMED`, thanh toán `UNPAID` (NT4, BR-28).<br>3. Đơn có món không còn bán hoặc khung không hợp lệ thì bị từ chối kèm lý do.<br>4. Gửi lại cùng yêu cầu không tạo thêm đơn (BR-17, NFR-02).<br>5. Khung hết quota mà chưa có PB-09 thì báo hết chỗ để khách chọn khung khác. | Charter 4.4, NT4–NT6; proposal 7; WBS 4.4 |
| PB-08 | Là **khách pre-order**, tôi muốn xem đơn, trạng thái và mã nhận của mình, để biết tiến độ và có mã khi tới quầy. | 1. Xem danh sách và chi tiết đơn của chính mình: món, khung nhận, trạng thái, mã nhận, trạng thái thanh toán.<br>2. Trạng thái theo đúng mục B.5 của SRS; đơn bị cửa hàng hủy hiển thị lý do (BR-24). | Charter 4.4; proposal 5.1; WBS 4.7 |
| PB-09 | Là **khách pre-order**, tôi muốn được đề xuất khung giờ khác khi khung tôi chọn đã hết quota, để vẫn đặt được mà không phải thử lần lượt. | 1. Khung chọn không đủ quota thì không xác nhận; đề xuất tối đa 3 khung theo BR-16.<br>2. Khách chọn khung đề xuất thì đơn được kiểm tra lại qua PB-05.<br>3. Không còn khung phù hợp trong ngày thì báo hết chỗ. | Charter 4.2, 4.4; proposal 7; WBS 4.6 |
| PB-10 | Là **khách pre-order**, tôi muốn hủy đơn khi bếp chưa cần chuẩn bị, để không giữ chỗ khi không cần nữa. | 1. Hủy được khi đơn `CONFIRMED` và chưa vào cửa sổ chuẩn bị; quota được hoàn đủ (BR-23).<br>2. Các trường hợp khác bị từ chối kèm lý do. | Charter 4.4, 4.7; proposal 7; WBS 4.7; RISK-05 |

### D. Walk-in
| ID | User Story | Acceptance Criteria | Nguồn |
|---|---|---|---|
| PB-11 | Là **nhân viên quầy**, tôi muốn nhập nhanh đơn của khách trực tiếp, xem thời gian dự kiến sớm nhất và chỉ xác nhận khi khách đồng ý, để walk-in dùng chung quota mà khách vẫn tự quyết định. | 1. Nhập món, không cần thông tin khách (BR-20); không có chức năng POS đầy đủ.<br>2. Hiển thị khung dự kiến sớm nhất theo BR-18 trước khi xác nhận.<br>3. Chỉ xác nhận sau khi khách đồng ý; lúc xác nhận ghi nhận thanh toán và kiểm tra lại quota (BR-19, BR-27). Khung đã hết thì hiển thị khung mới để báo lại khách.<br>4. Khách không đồng ý thì không tạo đơn, không giữ quota (NT3).<br>5. Đơn xác nhận có mã nhận để nhân viên đưa cho khách và vào cùng hàng chờ bếp (NT7).<br>6. Bấm xác nhận lặp không tạo thêm đơn (NFR-02). | Charter 4.4, NT2, NT3, NT7; WBS 4.5; RISK-02 |

### E. Bếp và bàn giao
| ID | User Story | Acceptance Criteria | Nguồn |
|---|---|---|---|
| PB-12 | Là **nhân viên bếp**, tôi muốn xem đơn sắp tới, đơn cần chuẩn bị và đơn đang làm, để biết cần làm gì tiếp theo. | 1. Hiển thị đơn từ cả hai nguồn theo ba nhóm; nhóm sắp tới / cần chuẩn bị theo BR-21.<br>2. Sắp theo BR-22, không theo thời điểm tạo đơn (NT7).<br>3. Đơn trễ được đánh dấu (BR-26). | Charter 4.4, NT6, NT7; WBS 4.8 |
| PB-13 | Là **nhân viên bếp**, tôi muốn bấm bắt đầu và hoàn tất cho từng đơn, để quầy và khách biết khi nào nhận được món. | 1. `CONFIRMED` → `PREPARING` → `READY`, cập nhật theo cả đơn (BR-22).<br>2. Không có chuyển trạng thái nào khác ngoài mục B.5 SRS.<br>3. Chỉ nhân viên bếp làm được. | Charter 4.4; proposal 7; WBS 4.8 |
| PB-14 | Là **khách pre-order**, tôi muốn trang đơn tự cập nhật khi đơn sẵn sàng, để đến nhận đúng lúc mà không phải tải lại liên tục. | 1. Khi đơn chuyển `READY`, trang đơn của khách hiển thị trạng thái mới mà khách không cần thao tác.<br>2. Không dùng email/SMS. Khách walk-in được quầy gọi mã ngoài hệ thống. | Charter 4.2; proposal 3; WBS 4.9 |
| PB-16 | Là **nhân viên quầy**, tôi muốn ghi nhận đơn đã thanh toán và phương thức, để chỉ giao món đã trả tiền. | 1. Chuyển `UNPAID` → `PAID`, chọn tiền mặt hoặc chuyển khoản tại quầy (BR-27).<br>2. Pre-order mặc định `UNPAID` (BR-28).<br>3. Không tích hợp cổng thanh toán; không xử lý hoàn tiền trong hệ thống (BR-30). | Charter 4.4, NT8; WBS 4.11 |
| PB-15 | Là **nhân viên quầy**, tôi muốn xem đơn sẵn sàng, xác minh mã nhận và xác nhận bàn giao, để giao đúng đơn cho đúng người. | 1. Xem danh sách đơn `READY`.<br>2. Chỉ bàn giao khi đơn `READY`, mã đúng và `PAID`; ngược lại từ chối kèm lý do (BR-29).<br>3. Đơn đã bàn giao không bàn giao lại được. | Charter 4.4; WBS 4.10; RISK-05 |

### F. Hủy, ngoại lệ và vận hành
| ID | User Story | Acceptance Criteria | Nguồn |
|---|---|---|---|
| PB-21 | Là **quản lý** hoặc **nhân viên quầy**, tôi muốn hủy đơn đã xác nhận kèm lý do khi cửa hàng không phục vụ được hoặc khách walk-in đổi ý, để xử lý ngoại lệ trong hệ thống. | 1. Quyền và trạng thái được hủy theo BR-24; bắt buộc nhập lý do.<br>2. Quota hoàn đủ nếu hủy từ `CONFIRMED`, không hoàn nếu từ `PREPARING`.<br>3. Khách pre-order thấy lý do trên trang đơn. | Charter 4.4, 4.7; BR-24; WBS 4.7 |
| PB-17 | Là **quản lý**, tôi muốn xem mức tải theo quota, đơn đang xử lý, đơn trễ và thống kê ngày, để điều hành và phát hiện nguy cơ quá tải. | 1. Theo từng khung trong ngày: quota, điểm đã giữ, số đơn.<br>2. Danh sách đơn đang xử lý và đơn trễ (BR-26).<br>3. Thống kê ngày theo FR-20.<br>4. Không trình bày số liệu như năng lực bếp thực tế (NT9). | Charter 4.4, NT9; proposal 5.1; WBS 4.12; RISK-03 |
| PB-18 | Là **nhân viên quầy** hoặc **quản lý**, tôi muốn đánh dấu đơn không đến nhận, để danh sách đơn sẵn sàng không bị tồn đọng. | 1. Liệt kê đơn `READY` đã quá hạn theo BR-25.<br>2. Đánh dấu `NO_SHOW`; quota không hoàn.<br>3. Đơn chưa quá hạn thì không đánh dấu được. | Charter 4.7; WBS 4.7; RISK-05 |

## 3. Yêu cầu phi chức năng (NFR)
NFR áp lên các story liên quan và có công việc kiểm chứng riêng. Tiêu chí nháp ở [SRS mục D](../SRS/SRS.md).

| ID | Yêu cầu | Áp lên | Priority | Estimate | Thời điểm kiểm chứng |
|---|---|---|---|---|---|
| NFR-01 | Nhất quán quota khi đồng thời | PB-05, PB-07, PB-09, PB-11 | High | L | Thiết kế từ Sprint 1; kiểm chứng đầy đủ khi có hai kênh (thứ tự 14) |
| NFR-02 | Không trùng đơn khi gửi lại | PB-07, PB-11 | High | M | Cùng PB-07 |
| NFR-03 | Phân quyền tại nơi xử lý | Mọi story theo vai trò | High | M | Từ Sprint đầu, theo từng story |
| NFR-04 | Hiệu năng theo workload công bố | PB-07, PB-12, PB-15 | Medium | M | Sau khi luồng chính ổn định |
| NFR-05 | Tách trách nhiệm đặt đơn / quota / bếp | Thiết kế chung | Medium | — | Khi thiết kế kiến trúc |
| NFR-06 | Mật khẩu không lưu dạng đọc được | PB-01, PB-19, PB-20 | High | S | Cùng PB-01 |

## 4. Đối chiếu độ phủ
| Phạm vi Charter 4.4 | Story |
|---|---|
| Đăng nhập, tài khoản, quyền | PB-01, PB-19, PB-20, NFR-03, NFR-06 |
| Menu, tình trạng bán | PB-02, PB-03 |
| Giỏ, chọn giờ, đặt, theo dõi, hủy | PB-06, PB-07, PB-08, PB-10 |
| Walk-in tối giản, đồng ý thời gian | PB-11 |
| Quota dùng chung, giữ quota, tìm/đề xuất thời gian | PB-04, PB-05, PB-09, PB-11, NFR-01, NFR-02 |
| Bếp | PB-12, PB-13 |
| Thông báo | PB-14 |
| Pickup | PB-15 |
| Thanh toán | PB-16 |
| Quản lý cấu hình và theo dõi | PB-03, PB-04, PB-17 |
| Ngoại lệ hủy/no-show (Charter 4.7) | PB-10, PB-18, PB-21 |

Mọi gói WBS 4.2–4.12 đều có story tương ứng. Không có story cho mục ngoài phạm vi.

Không đưa vào backlog:
- **Quota riêng cho pre-order**: đề xuất sau Charter, chưa duyệt.
- **Sửa đơn sau khi xác nhận**: Charter không đưa vào phạm vi; khách hủy và đặt lại.
- **Hoàn tiền trong hệ thống**: ngoài phạm vi (BR-30).

## Xác nhận và cập nhật
Backlog này chưa được xác nhận với khách hàng bên ngoài; mô hình do người dùng chọn. Phản hồi từ Sprint Review và thay đổi quy tắc trong SRS được cập nhật vào đây.

## Output chuyển giao
Danh sách có thứ tự, Priority, Estimate, AC và Status làm input cho Sprint Planning (Bước 6) và Use Case (Bước 8). Liên kết Use Case và test được thêm vào story khi có.
