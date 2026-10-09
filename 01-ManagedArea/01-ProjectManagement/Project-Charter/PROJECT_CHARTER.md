# Project Charter

Trạng thái: Bản nháp đã cập nhật bối cảnh, mục tiêu và phạm vi theo nội dung người dùng duyệt ngày 2026-09-29. Chưa có phê duyệt toàn bộ Charter.

## Input và cơ sở lập Charter

- [Quy trình IT3180](../../../00-References/IT3180_PROJECT_PROCESS.md), Bước 0–1: cấu trúc và mục đích Charter.
- [Proposal Nhóm 15](../../../00-References/Proposal_Nhom15.pdf): đề tài, đội ngũ ghi trên proposal và định hướng ban đầu.
- Quyết định người dùng duyệt trong cuộc trao đổi ngày 2026-09-29: mô hình một cửa hàng fast-food có pre-order và walk-in tối giản; quota dùng chung; bảo vệ mọi đơn đã xác nhận/giữ quota; khách walk-in đồng ý thời gian dự kiến trước khi xác nhận; ghi nhận thanh toán nhưng không tích hợp gateway production.

Mô hình dưới đây do người dùng lựa chọn cho dự án học phần, gợi ý từ mô hình fast-food như Texas Chicken. Không khẳng định đây là quy trình nội bộ đã xác minh của thương hiệu hoặc cửa hàng thực tế. Chưa có dữ liệu quan sát định lượng hay kết quả thử nghiệm để chứng minh hiệu quả vận hành.

| Đầu vào tiền khả thi | Kết quả hiện có | Giới hạn còn lại |
|---|---|---|
| Bối cảnh và bài toán | Cửa hàng fast-food, hai nguồn đơn cùng dùng bếp; cần kiểm soát lượng việc đã hứa phục vụ | Bối cảnh mô phỏng được chọn, chưa phải kết quả khảo sát thực tế |
| Phạm vi | Một cửa hàng, đặt/tiếp nhận đơn, quota, bếp, pickup và trạng thái thanh toán | Chi tiết nghiệp vụ để bước yêu cầu/SRS làm rõ |
| Khả thi về phạm vi | Đã loại trừ POS đầy đủ, kho, batch, tối ưu tài nguyên và gateway production | Chưa có estimate WBS hoặc bằng chứng triển khai |
| Nguồn lực và cách làm | Tổ chức Scrum gọn cho 1–2 người theo README; người dùng cho biết tự thực hiện | Không mặc định năng lực của năm người chỉ vì proposal có năm tên |
| Chất lượng và rủi ro | Nhất quán quota, không trùng đơn, phân quyền và giới hạn cam kết thời gian | Workload, ngưỡng đo và test chưa được đặc tả |

Bối cảnh và scope đã đủ làm đầu vào phân rã WBS. Chưa kết luận khả thi về tiến độ, chi phí hoặc hiệu năng production.

## 1. Thông tin chung

- Tên dự án: Phát triển hệ thống đặt món trước và điều phối nhận món theo năng lực phục vụ.
- Tên tiếng Anh trong proposal: Smart Food Pre-order and Pickup Coordination Platform.
- Học phần: Kỹ thuật phần mềm, áp dụng quy trình project IT3180 được cung cấp.
- Đơn vị thực hiện trên proposal: Nhóm 15, Trường Công nghệ Thông tin và Truyền thông, Đại học Bách khoa Hà Nội.
- Nhà tài trợ: chưa có thông tin.
- Mức độ ảnh hưởng dự kiến: thay đổi cách tiếp nhận đơn, phân bổ quota cam kết, theo dõi chuẩn bị và xác nhận bàn giao trong mô hình một cửa hàng. Không tuyên bố tác động đã được đo tại cửa hàng thật.

## 2. Đội ngũ thực hiện

Danh sách dưới đây giữ theo trang 1 của proposal, không phải xác nhận phân công thực tế.

| Thành viên trên proposal | Vai trò thực hiện | Thông tin liên hệ |
|---|---|---|
| Bùi Tăng Nam Khánh | Chưa xác nhận | Chưa cung cấp |
| Lê Đình Lâm | Chưa xác nhận | Chưa cung cấp |
| Phạm Trung Kiên | Chưa xác nhận | Chưa cung cấp |
| Nguyễn Huy Đức | Chưa xác nhận | Chưa cung cấp |
| Trần Quốc Việt | Chưa xác nhận | Chưa cung cấp |

Người dùng cho biết tự thực hiện toàn bộ. Định hướng cộng tác phù hợp 1–2 người; không gán nhiệm vụ hoặc danh tính chưa được xác nhận. Project Manager chưa được điền. Các thông tin hành chính này không chặn việc phân tích nghiệp vụ hiện tại.

## 3. Các bên liên quan

| Bên liên quan / vai trò | Nhu cầu trong mô hình | Đại diện / cơ sở |
|---|---|---|
| Chủ cửa hàng / đại diện khách hàng dự án | Kiểm soát tiếp nhận và phạm vi phục vụ | Vai trò trong mô hình, chưa có khách hàng bên ngoài tham gia |
| Khách pre-order | Chọn món/giờ nhận, biết đơn đã được tiếp nhận, theo dõi và nhận món | Nhóm người dùng của mô hình |
| Khách walk-in | Đặt tại quầy, biết thời gian phục vụ dự kiến để quyết định có đặt hay không | Nhóm người dùng của mô hình; nhân viên nhập đơn thay khách |
| Nhân viên bếp | Biết đơn sắp tới/đến lúc chuẩn bị, cập nhật tiến độ và sẵn sàng | Vai trò nghiệp vụ, chưa chốt cách phân quyền chi tiết |
| Nhân viên quầy | Nhập walk-in tối giản, thông báo thời gian dự kiến, xác minh và bàn giao | Có thể cùng người thực hiện nhập đơn và pickup; không yêu cầu quầy vật lý riêng |
| Quản lý cửa hàng / bên vận hành | Cấu hình menu, tình trạng món, quota và theo dõi tải/đơn trễ | Vai trò trong mô hình |
| Người thực hiện dự án | Xác định mô hình, xây dựng và kiểm chứng sản phẩm | Người dùng chủ động quyết định nghiệp vụ; AI hỗ trợ theo AGENTS.md |
| Giảng viên | Hướng dẫn/đánh giá theo học phần | Nguyễn Quốc Tuấn theo proposal; chưa xác nhận trách nhiệm phê duyệt từng đầu ra |

Không tạo đại diện khảo sát hoặc xác nhận của cửa hàng không có thật.

## 4. Tuyên bố phạm vi

### 4.1. Bối cảnh và động cơ

Dự án phục vụ một cửa hàng fast-food đơn lẻ, một bếp chung và một điểm bàn giao. Các nhóm sản phẩm dự kiến gồm gà rán, suất ăn/combo, món ăn kèm và đồ uống; danh mục cụ thể và cách tính combo sẽ được làm rõ trong yêu cầu.

Cửa hàng tiếp nhận khách đặt trước trên web và khách trực tiếp tại quầy. Nhân viên nhập walk-in bằng luồng tối giản trong cùng hệ thống. Hai nguồn đơn cùng sử dụng quota phục vụ: pre-order giữ quota cho thời gian nhận dự kiến trong tương lai; walk-in được tìm thời gian phục vụ sớm nhất còn phù hợp, khách đồng ý trước khi đơn được xác nhận.

Trong bối cảnh mô phỏng, tiếp nhận nhiều đơn mà không kiểm soát cam kết theo thời gian có thể gây quá tải và trễ đơn. Mục đích của hệ thống là kiểm soát lượng việc được tiếp nhận và phối hợp khách, bếp, quầy. Đây là vấn đề đặt ra cho mô hình, không phải số liệu hoặc kết luận khảo sát thương hiệu.

Quá trình chế biến được giả định kết hợp chuẩn bị trước và hoàn thiện theo đơn. Nhân viên tự quản lý nguyên liệu, batch và thiết bị; phần mềm không quản lý các chi tiết này.

### 4.2. Mục đích / mục tiêu

- Mô hình hóa quy trình pre-order và walk-in tối giản cùng luồng chuẩn bị/bàn giao.
- Tạo ứng dụng web có thể tiếp nhận đơn, kiểm tra quota, theo dõi chuẩn bị, thông báo sẵn sàng và xác nhận nhận món.
- Hạn chế tiếp nhận vượt quota đã cấu hình; bảo vệ phần quota của mọi đơn đã xác nhận, không phân biệt nguồn đơn.
- Đề xuất thời gian nhận thay thế khi pre-order không đáp ứng được; cung cấp thời gian dự kiến cho walk-in trước khi khách chấp nhận đặt.
- Hỗ trợ khả năng đơn sẵn sàng gần thời gian nhận dự kiến. Không cam kết khách luôn lấy ngay hoặc mọi đơn chắc chắn đúng giờ.
- Thiết kế và kiểm chứng tính nhất quán khi đặt đồng thời, tránh trùng đơn khi retry, phân quyền và thời gian phản hồi trong môi trường thử nghiệm được công bố.

Tiêu chí đo, workload và điều kiện chấp nhận cụ thể sẽ được đặc tả trong SRS. Không dùng việc tuân thủ quota làm bằng chứng tự động về năng lực chế biến thực tế.

### 4.3. Sản phẩm bàn giao

- Ứng dụng web, backend, cơ sở dữ liệu và bản chạy phục vụ demo cho phạm vi ở mục 4.4.
- Mã nguồn và lịch sử phát triển, PR/review, release theo hoạt động thực tế.
- Charter, WBS, Risk Management, Product Backlog, Sprint Backlog/Task Board.
- SRS, Use Case, Activity, Analysis Model/Sequence, Architecture/Package, Database/Class Design và UI Prototype.
- Coding Convention, White-box/Black-box Test Cases, Test Results, Release/Feedback và retrospective actions theo nguồn IT3180.

Không bổ sung loại tài liệu riêng ngoài bộ đã thống nhất.

### 4.4. Phạm vi và nguyên tắc tiếp nhận đã duyệt

#### Trong phạm vi

- Một cửa hàng fast-food; một bếp dùng chung và một điểm bàn giao.
- Khách pre-order: đăng nhập, xem món/tình trạng bán, giỏ hàng, chọn giờ nhận, đặt, theo dõi và hủy theo chính sách sẽ đặc tả.
- Walk-in: nhân viên quầy nhập đơn tối giản; hệ thống kiểm tra điều kiện tiếp nhận và thời gian phục vụ dự kiến; nhân viên thông báo và khách đồng ý trước khi xác nhận.
- Menu: quản lý món, trạng thái còn bán/tạm hết và thông tin chuẩn bị cần thiết. Trạng thái còn bán không đồng nghĩa kiểm kê chính xác nguyên liệu.
- Scheduling: quota dùng chung, giữ quota khi xác nhận, tìm thời gian phù hợp và đề xuất thay thế.
- Bếp: xem đơn sắp tới và công việc cần chuẩn bị, cập nhật tiến độ, xác nhận sẵn sàng. Chi tiết cập nhật theo đơn/từng món để SRS xác định.
- Pickup: xem đơn sẵn sàng, xác minh mã nhận và xác nhận bàn giao.
- Thanh toán: ghi nhận trạng thái thanh toán. Phương thức trả tại quầy hay mô phỏng, cùng quan hệ giữa thanh toán và bàn giao, chưa chốt.
- Quản lý: cấu hình menu/quota, theo dõi mức tải theo quota, đơn đang xử lý/đơn trễ và thống kê vận hành cơ bản.

#### Nguyên tắc nghiệp vụ cấp phạm vi

1. Capacity là service quota: lượng công việc cửa hàng cho phép cam kết trong một khoảng nhận món. Chưa chốt đơn vị, công thức hoặc độ dài slot; đây không phải mô hình lập lịch tài nguyên bếp chính xác.
2. Mọi đơn đã xác nhận và giữ quota đều được bảo vệ, gồm cả walk-in và pre-order. Đơn mới từ bất kỳ nguồn nào không được chiếm quota đã cam kết cho đơn trước.
3. Khách walk-in được biết và chấp nhận thời gian dự kiến trước khi xác nhận đơn. Không tự coi khách đồng ý chờ.
4. Pre-order hợp lệ được tự động xác nhận trong luồng bình thường sau khi các điều kiện tiếp nhận được đáp ứng; không yêu cầu nhân viên quầy duyệt từng đơn. Điều kiện chi tiết được làm rõ trong SRS.
5. Chỉ báo xác nhận thành công khi việc tiếp nhận đơn và giữ quota tương ứng cùng thành công. Cơ chế kỹ thuật thực hiện sẽ được thiết kế sau.
6. Xác nhận thể hiện cam kết tiếp nhận, không có nghĩa bắt đầu chế biến ngay. Đơn đặt trước ở nhóm sắp tới cho đến thời điểm phù hợp để chuẩn bị.
7. Hai nguồn đơn dùng chung luồng xử lý sau khi tiếp nhận nhưng vẫn giữ thông tin thời gian phục vụ cần thiết. Không mặc định hàng chờ luôn ưu tiên theo thứ tự tạo đơn.
8. Ghi nhận trạng thái thanh toán nằm trong phạm vi; tích hợp gateway production nằm ngoài phạm vi. Chưa mặc định đơn đã xác nhận là đã trả tiền.
9. Tuân thủ quota không bảo đảm tuyệt đối giờ hoàn thành hoặc thời gian chờ tại quầy.

Các nguyên tắc trên được người dùng duyệt làm cơ sở phạm vi, chưa thay thế đặc tả Use Case, state machine hoặc thuật toán.

#### Ngoài phạm vi

- Giao hàng, tài xế, marketplace và quản lý nhiều cửa hàng.
- POS thương mại đầy đủ, quản lý ca thu ngân, sổ quỹ và đối soát.
- Kho/nguyên liệu đầy đủ, batch chế biến, recipe, thiết bị, phân ca nhân lực và tối ưu lịch bếp chi tiết.
- Cổng thanh toán production, loyalty/promotion phức tạp, IoT và AI nâng cao.
- Chứng minh production-scale, high availability hoặc bảo đảm khách không bao giờ phải chờ.

### 4.5. Kinh phí

Chưa có thông tin dự toán hoặc nguồn tài trợ. Không mặc định chi phí bằng không. Chi tiết môi trường demo/dịch vụ sẽ được xác định khi cần; chưa chọn giải pháp phát sinh chi phí.

### 4.6. Lịch trình và cách tổ chức phát triển

Theo [định hướng đã thống nhất](../../../README.md): chuẩn bị yêu cầu và thiết kế tổng thể tương đối kỹ, sau đó thực hiện Scrum gọn cho 1–2 người. Mỗi Sprint có mục tiêu sản phẩm chạy được, tài liệu liên quan, code, test, Review và Retrospective.

Các hướng chia Sprint trong README là dự kiến; phạm vi/timebox được xác định trong Sprint Planning thực tế. Kế hoạch bốn Sprint theo module trong proposal được giữ nguyên như nguồn ban đầu, không tự coi là lịch cam kết hiện tại. Chưa có ngày bàn giao được xác nhận; không dùng hạn nộp làm điều kiện chặn phân tích nghiệp vụ.

### 4.7. Rủi ro và giả định sơ bộ

Các điểm sau kế thừa rủi ro proposal và bổ sung hệ quả trực tiếp của mô hình được chọn; chưa đánh giá xác suất/ảnh hưởng hoặc giao owner.

| Rủi ro | Hướng xử lý ở mức Charter |
|---|---|
| Walk-in mở rộng thành full POS | Giữ giới hạn nhập đơn tối giản và trạng thái thanh toán |
| Quota bị hiểu thành bảo đảm năng lực bếp chính xác | Ghi rõ mô hình xấp xỉ, công bố giới hạn và kiểm thử đúng tuyên bố |
| Hybrid kéo theo quản lý kho/batch/tài nguyên | Để nhân viên xử lý nội bộ; giữ ngoài phạm vi phần mềm |
| Hai kênh hoặc request đồng thời nhận vượt quota | Yêu cầu giữ quota nhất quán và bảo vệ mọi đơn đã xác nhận |
| Dữ liệu menu/quota hoặc tiến độ bếp không cập nhật kịp | Làm rõ trách nhiệm cập nhật và ngoại lệ ở bước yêu cầu |
| Hủy/no-show làm lãng phí hoặc hoàn sai quota | Đặc tả thời điểm hủy và cách xử lý quota trước khi implement |
| Dồn đơn ở quầy chung, món trễ hoặc hoàn thành quá sớm | Làm rõ flow bếp/pickup; không hứa thời gian chờ bằng không |
| Mô hình giả định khác vận hành thực tế; thiếu dữ liệu production | Ghi nguồn mô hình, kiểm chứng với tình huống mô phỏng và báo cáo giới hạn |

Giả định của mô hình: nhân viên nhập mọi walk-in thuộc phạm vi vào hệ thống; quản lý cấu hình quota và tình trạng món; nhân viên cập nhật trạng thái khi vận hành. Chưa có bằng chứng thực tế về tần suất/độ chính xác cập nhật. Các điểm này là input cho Risk Management và Requirement Engineering.

### 4.8. Ràng buộc

- Bám quy trình và bộ artifact IT3180 đã cung cấp.
- Ghi đúng cách thực hiện 1–2 người, không mặc định năng lực theo danh sách nhóm trên proposal.
- Một cửa hàng, quota trừu tượng, không mô hình hóa bếp chi tiết.
- Không suy rộng kết quả demo/load test thành năng lực production.
- Ngôn ngữ, framework, database và ngưỡng NFR chưa được quyết định.

### 4.9. Các yếu tố bên ngoài

Yêu cầu bàn giao của học phần nếu được cung cấp; tính phù hợp của mô hình quan sát/tham khảo; điều kiện thiết bị và môi trường demo. Không coi phỏng vấn hoặc tích hợp POS/gateway bên ngoài là phụ thuộc bắt buộc.

## 5. Chiến lược truyền thông

| Hoạt động | Cách tổ chức đã thống nhất | Thông tin chưa chốt |
|---|---|---|
| Sprint Planning | Chọn mục tiêu và backlog vừa sức | Lịch/timebox cụ thể và người kiêm nhiệm trách nhiệm |
| Daily Scrum | Kiểm tra tiến độ, blocker, điều chỉnh Sprint Backlog mỗi ngày làm việc; ghi đúng nếu tự thực hiện | Giờ thực hiện khi bắt đầu Sprint |
| Báo cáo tiến độ | Cập nhật Task Board và kết quả công việc; không dựng báo cáo nhóm | Kênh cộng tác khác nếu có |
| Sprint Review | Chạy sản phẩm, đối chiếu yêu cầu và xem xét việc tiếp theo; phân biệt tự đánh giá với phản hồi người khác | Người ngoài tham gia nếu thực tế có |
| Retrospective | Ghi ngắn một cải tiến có ích cho Sprint tiếp theo | Hành động cụ thể sau từng Sprint |
| Kênh giao tiếp | Người dùng và AI trao đổi trong chat; quyết định liên quan được lưu vào tài liệu repository | Kênh với giảng viên/cộng tác viên chưa cung cấp |

## 6. Phê duyệt

- Ngày 2026-09-29: người dùng đã duyệt mô hình nghiệp vụ/phạm vi và bốn điều chỉnh: bảo vệ mọi đơn đã giữ quota; walk-in đồng ý thời gian trước khi xác nhận; thanh toán chỉ ghi trạng thái với phương thức để SRS; đi tiếp WBS → Risk Management trước bước yêu cầu chi tiết.
- Người dùng cho phép cập nhật Charter theo các nội dung đó.
- Chưa có xác nhận phê duyệt toàn bộ bản Charter sau cập nhật, hoặc phê duyệt của giảng viên/cửa hàng.
- Các mục hành chính thiếu thông tin giữ nguyên trạng thái; không tự điền người phê duyệt hoặc ghi Approved cho toàn tài liệu.

## Nội dung để bước yêu cầu / SRS làm rõ

1. Danh mục món/combo, thông tin chuẩn bị và cách biểu diễn tình trạng bán.
2. Độ dài pickup slot, cách biểu diễn giờ nhận, đơn vị/workload và cách cấu hình quota.
3. Thời gian đặt trước tối thiểu, thời gian phục vụ sớm nhất của walk-in, đề xuất slot thay thế.
4. Thời điểm đưa đơn vào công việc bếp, cách ưu tiên và xử lý đơn READY quá sớm/quá muộn.
5. Chính sách hủy/no-show, thời điểm và lượng quota được giải phóng; không mặc định mọi hủy đơn đều hoàn đủ quota.
6. Phương thức thanh toán (trả tại quầy hoặc mô phỏng), các trạng thái và điều kiện liên quan tới xác nhận/bàn giao.
7. State machine, ngoại lệ, quyền thao tác và mức cập nhật đơn/từng món.
8. Tiêu chí đo NFR, workload kiểm thử và điều kiện chấp nhận.

Đây là các điểm mở của yêu cầu, không phải lý do tiếp tục thiết kế thuật toán ngay trong Charter.

## Output chuyển giao

- Charter hiện có bối cảnh, mục tiêu, scope/out-of-scope và nguyên tắc tiếp nhận đã được người dùng duyệt về nội dung.
- Bước kế tiếp: WBS, dùng phạm vi và deliverable để phân rã công việc; sau đó Risk Management dựa trên Charter và WBS.
- Tiếp tục thu thập/phân tích yêu cầu → Product Backlog → Sprint Planning và làm rõ SRS theo nguồn. Không bỏ qua WBS/Risk Management để nhảy thẳng sang thiết kế hoặc code.
