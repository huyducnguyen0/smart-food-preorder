# Risk Register
Trạng thái: Bản đánh giá sơ bộ, chưa được người dùng review toàn bộ. Chưa có số liệu xác suất hoặc bằng chứng các biện pháp đã được thực hiện.

## Input
- [Quy trình IT3180](../../../00-References/IT3180_PROJECT_PROCESS.md), Bước 3: nhận diện → phân tích probability/impact/ưu tiên → phòng ngừa, ứng phó, owner và trạng thái.
- [Project Charter](../Project-Charter/PROJECT_CHARTER.md), mục 4.4 và 4.7–4.9: scope, nguyên tắc, rủi ro sơ bộ, giả định và ràng buộc.
- [WBS](../WBS/WBS.md): các phần việc cần thực hiện và phụ thuộc.
- Bối cảnh thực hiện 1–2 người; chưa có lịch cam kết, estimate chi tiết, lựa chọn công nghệ hoặc test results.

## Cách đọc và đánh giá

- Rủi ro là điều có thể xảy ra trong tương lai. Những business rule hiện chưa chốt là thông tin đầu vào còn thiếu; rủi ro tương ứng là thiết kế/code khi chưa làm rõ, dẫn đến sai hành vi hoặc phải sửa lại.
- Probability, Impact và Risk level dưới đây là nhận định định tính ban đầu bằng chữ Thấp/Trung bình/Cao, không phải xác suất đo được hoặc thang điểm do môn quy định. Không dùng công thức tính điểm. Lý do được ghi ở từng dòng để có thể xem xét lại.
- Probability mô tả khả năng xảy ra trong quá trình dự án; Impact mô tả hậu quả nếu xảy ra; Risk level là mức ưu tiên xử lý dựa trên cả hai và vai trò của chức năng trong mục tiêu dự án.
- Mức Cao ưu tiên xem xét trước khi triển khai phần liên quan; Trung bình theo dõi và xử lý trong công việc liên quan. Đây là đề xuất đánh giá, không phải kết luận đã được phê duyệt.
- Owner chưa được chỉ định theo tên; giữ rõ Chưa xác nhận. Điều này không ngăn việc nhận diện và đề xuất biện pháp, nhưng chưa thể coi việc giao trách nhiệm quản lý rủi ro đã hoàn tất.
- Open nghĩa là đang theo dõi, không có nghĩa sự cố đã xảy ra. Chỉ cập nhật thành đã xử lý khi có bằng chứng; Risk Register được cập nhật xuyên dự án.

## Nhận diện và phân tích

| ID | Risk: nguyên nhân → sự kiện có thể xảy ra → hậu quả | WBS liên quan | Probability và cơ sở | Impact và cơ sở | Risk level | Owner | Status |
|---|---|---|---|---|---|---|---|
| RISK-01 | Các lựa chọn nghiệp vụ còn mở → thiết kế/code dựa vào giả định khác nhau → sai chức năng, phải sửa nhiều đầu ra | 2.1–2.7, 3.1–3.6 | Trung bình (cập nhật 2026-10-08): slot, workload, hủy, no-show, thanh toán và ưu tiên bếp đã có quy tắc trong SRS B.4–B.5; chưa có Use Case Specification và ví dụ kiểm chứng | Cao: ảnh hưởng luồng lõi và dữ liệu | Cao | Chưa xác nhận | Open |
| RISK-02 | Walk-in, payment và hybrid preparation → phạm vi mở rộng thành POS/kho/tối ưu bếp → mất tập trung và vượt khả năng thực hiện | 1.1–1.2, 2.2, 4.5, 4.8, 4.11 | Trung bình: có ranh giới scope nhưng nhiều chức năng lân cận dễ phát sinh | Cao: tăng mạnh khối lượng ngoài mục tiêu | Cao | Chưa xác nhận | Open |
| RISK-03 | Quota chỉ xấp xỉ khả năng phục vụ → mô hình/diễn giải hứa thời gian quá chính xác → demo đúng quota nhưng không chứng minh được mục tiêu đã tuyên bố | 2.1, 2.3–2.4, 4.6, 4.12, 6.1 | Trung bình: đã ghi giới hạn, cách tính/thời gian vẫn còn mở | Cao: ảnh hưởng giá trị và tính đúng đắn của đề tài | Cao | Chưa xác nhận | Open |
| RISK-04 | Hai kênh cùng xác nhận hoặc request gửi lại → vượt quota/tạo đơn trùng → phá cam kết với đơn đã nhận | 3.2, 3.5, 4.4–4.6, 5.3 | Trung bình: yêu cầu có concurrency/retry nhưng chưa có thiết kế và kiểm thử | Cao: vi phạm nguyên tắc tiếp nhận cốt lõi | Cao | Chưa xác nhận | Open |
| RISK-05 | Trạng thái, hủy, thanh toán và bàn giao chưa thống nhất → hoàn sai quota, chuyển trạng thái sai hoặc giao lặp → dữ liệu và vận hành không nhất quán | 2.3, 2.6, 4.7–4.11, 5.2 | Trung bình: nhiều nhánh và điều kiện liên quan còn cần làm rõ | Cao: ảnh hưởng cả đơn và quota dùng chung | Cao | Chưa xác nhận | Open |
| RISK-06 | Menu/quota/trạng thái cập nhật trễ hoặc hàng chờ điều phối chưa phù hợp → nhận đơn không phù hợp, chuẩn bị quá sớm/trễ → thời gian dự kiến kém tin cậy | 2.1, 2.3, 3.7–3.8, 4.3, 4.6, 4.8–4.10 | Trung bình: phụ thuộc thao tác con người và dữ liệu của mô hình chưa được kiểm chứng | Cao: ảnh hưởng luồng vận hành end-to-end | Cao | Chưa xác nhận | Open |
| RISK-07 | Chỉ kiểm soát quyền ở giao diện hoặc thiếu kiểm tra sở hữu đơn → đọc/sửa dữ liệu ngoài quyền → sai yêu cầu bảo vệ dữ liệu và thao tác | 2.4, 3.3, 4.2, 5.3 | Trung bình: có nhiều vai trò, chưa thiết kế kiểm soát truy cập | Cao: ảnh hưởng quyền khách hàng/bếp/quầy/quản lý | Cao | Chưa xác nhận | Open |
| RISK-08 | Một người tự đặc tả, code và đánh giá → lỗi/giả định không bị phát hiện hoặc chọn quá nhiều việc → không tạo increment đạt Done | 1.5, 3.8, 4.13, 5.4, 6.1, 6.3 | Trung bình: hạn chế phản biện độc lập đã biết; chưa có dữ liệu năng suất | Cao: ảnh hưởng hoàn thành và chất lượng | Cao | Chưa xác nhận | Open |
| RISK-09 | Chọn công nghệ quá sức hoặc tích hợp chức năng muộn → các phần không chạy chung → khó hoàn thành luồng demo | 3.3, 4.1, 4.13, 6.2 | Trung bình: công nghệ chưa chốt và chưa có increment chạy | Cao: chặn sản phẩm sử dụng được | Cao | Chưa xác nhận | Open |
| RISK-10 | Yêu cầu/thiết kế thay đổi mà không cập nhật liên kết → tài liệu, code và test lệch nhau → nghiệm thu sai hoặc thiếu bằng chứng | 2.2–2.7, nhóm 3, 4.13, 5.2–5.4 | Trung bình: nhiều artifact phát triển qua các Sprint | Trung bình: gây làm lại và khó giải trình | Trung bình | Chưa xác nhận | Open |
| RISK-11 | Chưa chốt workload/môi trường hoặc chỉ chạy happy path → kết luận chất lượng không đủ căn cứ → bỏ sót lỗi, suy rộng hiệu năng | 2.4, 5.1–5.4, 6.1 | Trung bình: hiện chưa có tiêu chí đo và kết quả test | Cao: không chứng minh được NFR cốt lõi | Cao | Chưa xác nhận | Open |
| RISK-12 | Cấu hình/môi trường demo không tái lập hoặc phụ thuộc chưa kiểm tra → bản release không chạy được → không trình diễn được chức năng đã làm | 1.4, 3.3, 4.13, 6.2 | Trung bình: môi trường triển khai chưa được lựa chọn | Trung bình: có thể chặn buổi demo, cần khắc phục môi trường | Trung bình | Chưa xác nhận | Open |
| RISK-13 | Sprint 1 không giới hạn số lần đăng nhập sai (quyết định 2026-10-10) → có thể đoán mật khẩu tự động → chiếm tài khoản khách hoặc nhân viên | 4.2, 5.3 | Thấp khi chỉ chạy local/demo; tăng khi triển khai công khai | Cao: tài khoản nhân viên đổi được trạng thái đơn | Trung bình | Chưa xác nhận | Open |

## Phòng ngừa và ứng phó

Các hành động dưới đây là công việc dự kiến trong artifact/WBS đã có, chưa phải bằng chứng đã xử lý. Chúng không tự chốt quy tắc mới hoặc công nghệ.

| ID | Preventive action — làm trước để giảm khả năng/ảnh hưởng | Response — khi có dấu hiệu hoặc sự cố |
|---|---|---|
| RISK-01 | Làm rõ phần yêu cầu được chọn trước khi thiết kế; ghi quyết định và ví dụ trong SRS/Use Case | Khi các luồng hiểu khác nhau, xác định phần chưa rõ với người dùng; cập nhật SRS rồi sửa design/code/test bị ảnh hưởng |
| RISK-02 | Đối chiếu backlog và task với Charter; giữ walk-in và payment tối giản | Tách yêu cầu ngoài phạm vi để xem xét; chưa triển khai khi chưa có quyết định thay đổi scope; cập nhật Charter/WBS nếu thay đổi được duyệt |
| RISK-03 | Giữ rõ quota và thời gian dự kiến; đặc tả tình huống/giới hạn trước khi demo | Khi gặp phản ví dụ, điều chỉnh mô hình hoặc tuyên bố theo kết quả kiểm chứng; không tự thêm tối ưu bếp hay hứa bảo đảm giờ |
| RISK-04 | Đưa nguyên tắc xác nhận/giữ quota cùng thành công vào yêu cầu; thiết kế tương tác và kiểm thử tranh quota/retry sớm | Khi test phát hiện vượt quota/trùng đơn, ghi lỗi và dữ liệu tái hiện, sửa cơ chế rồi chạy lại; chưa coi chức năng đạt Done |
| RISK-05 | Đặc tả điều kiện chuyển trạng thái, hủy, hoàn quota, thanh toán và bàn giao trong Use Case trước implement | Khi dữ liệu sai, xác định bước gây sai, làm rõ quy tắc nếu thiếu, sửa code và chạy lại các nhánh liên quan; không tự quyết cách hoàn quota |
| RISK-06 | Làm rõ trách nhiệm cập nhật dữ liệu và luồng đơn sắp tới/đến lúc làm; thử tình huống dồn tải trong prototype/test | Khi mô hình không xử lý được tình huống, bổ sung yêu cầu xử lý ngoại lệ đã thống nhất; giữ giới hạn hiệu quả trong demo và cập nhật backlog |
| RISK-07 | Đặc tả quyền theo actor và sở hữu đơn; thiết kế kiểm tra tại nơi xử lý yêu cầu và test truy cập trái phép | Khi phát hiện truy cập sai, sửa điểm kiểm soát, kiểm tra các thao tác tương tự và chạy lại test; không chỉ ẩn nút trên UI |
| RISK-08 | Chọn mục tiêu Sprint nhỏ, tự đối chiếu AC/DoD bằng bằng chứng; tận dụng phản hồi độc lập khi thực tế có | Khi thiếu thời gian hoặc phát hiện blind spot, điều chỉnh kế hoạch/phạm vi công việc phù hợp Sprint Goal; giữ chuẩn chất lượng, ghi cải tiến trong Retrospective |
| RISK-09 | Chọn kỹ thuật theo yêu cầu và khả năng; tích hợp luồng tối giản sớm trong Sprint, không đợi code xong mọi module | Khi phần đã làm không tích hợp được, xác định điểm không khớp và chỉnh thiết kế/code trong phạm vi; giảm phức tạp kỹ thuật thay vì thêm nền tảng không cần thiết |
| RISK-10 | Cập nhật các mục liên quan trong cùng thay đổi; tham chiếu requirement/Use Case/test ngay trong artifact | Khi phát hiện lệch, xác định nội dung đúng theo quyết định đã duyệt, sửa các đầu ra bị ảnh hưởng và kiểm tra lại |
| RISK-11 | Định nghĩa expected result, workload và môi trường; có test lỗi/biên, white-box và black-box theo nguồn | Khi kết quả thiếu điều kiện hoặc không tái hiện được, giới hạn kết luận, bổ sung test và chạy lại; không báo pass từ suy đoán |
| RISK-12 | Kiểm tra cấu hình và cách chạy với bản increment; giữ version code phù hợp bằng chứng test | Khi demo không chạy, kiểm tra dependency/config của đúng phiên bản, khôi phục khả năng chạy trong môi trường đã chọn và kiểm thử lại trước khi kết luận release |
| RISK-13 | Mật khẩu chỉ lưu dạng băm (NFR-06); báo lỗi đăng nhập chung không lộ tài khoản tồn tại (UC-02 E1); xét lại giới hạn đăng nhập sai trước khi triển khai công khai | Khi có dấu hiệu dò mật khẩu hoặc trước khi đưa lên môi trường công khai: thêm giới hạn số lần thử/tạm khóa, khóa tài khoản bị nghi ngờ (BR-32) |

## Ưu tiên áp dụng vào công việc tiếp theo

- RISK-01–03: ưu tiên làm rõ scope, quy tắc và giới hạn mô hình ở bước yêu cầu; không biến đề xuất quota riêng cho pre-order thành quy tắc đã duyệt.
- RISK-04–07: đưa thành nội dung cần chú ý khi thiết kế và Acceptance Criteria/test tương ứng; chưa lựa chọn transaction, thuật toán hoặc framework tại bước Risk Management.
- RISK-08–10: chọn lượng việc vừa sức, tích hợp sớm và cập nhật tài liệu trong từng Sprint.
- RISK-11–12: xác định điều kiện kiểm chứng và kiểm tra khả năng chạy của increment; không dồn mọi kiểm thử đến cuối dự án.

Xem lại đánh giá khi scope/yêu cầu/thiết kế thay đổi, trong Sprint Planning hoặc khi kết quả kiểm thử/phản hồi cho thấy rủi ro khác dự kiến. Ghi thay đổi trạng thái, owner thực tế và liên kết bằng chứng ngay trong tài liệu này khi có. Không tạo hồ sơ rủi ro riêng.

## Output chuyển giao

Risk Register ban đầu gồm rủi ro, đánh giá định tính, cách phòng ngừa/ứng phó và trạng thái; owner còn cần xác nhận khi tổ chức thực hiện. Bản này cung cấp input cho ưu tiên Sprint, thiết kế kỹ thuật, phân bổ nguồn lực, kế hoạch và Definition of Done.

Theo thứ tự nguồn, bước tiếp theo là Bước 4 — rà lại cấu trúc project/repository và Git workflow đã khởi tạo, xác định phần thiết lập còn thiếu. Sau đó mới tiếp tục thu thập/phân tích yêu cầu để xây Product Backlog. Lập danh sách rủi ro không có nghĩa các rủi ro đã được loại bỏ hoặc dự án đã hoàn thành quản lý rủi ro.
