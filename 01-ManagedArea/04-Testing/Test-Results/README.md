# Test Results

Lưu kết quả theo build/release, không ghi đè bằng chứng cũ.

Ghi kết quả white-box và black-box theo test tương ứng:

- Test Case ID và liên kết test;
- Input và Precondition;
- Expected Outcome;
- Actual Outcome;
- Result: Passed / Failed sau khi chạy;
- Note và bằng chứng liên quan.

Kết quả dùng để đối chiếu Acceptance Criteria, xử lý lỗi blocking và Sprint Review. Kết quả theo lượt chạy, mới nhất ở cuối.

## Lượt 2026-10-24: white-box logic giữ quota (T-24)
Kiểm chứng test thiết kế ở [WHITE_BOX_QUOTA.md](../Test-Cases/WHITE_BOX_QUOTA.md). Test là test tự động trong `03-SourceCodes/backend/src/test/java/vn/nhom15/preorder/slot/`, chạy cùng toàn bộ test của backend.

| Mục | Kết quả |
|---|---|
| Lệnh | `./mvnw clean verify` trên nhánh `docs/T-24-white-box-quota`, môi trường: Windows 11, Java 25, PostgreSQL 18.6 nhúng trong test |
| Tổng số test của backend | 82, **Passed 82**, Failed 0, Skipped 0 |
| Test thuộc white-box quota | `QuotaServiceTest` 11, `QuotaDatabaseTest` 12, `SlotRepositoryTest` 15 (kể cả 10 dòng giá trị biên của điều kiện SQL): tất cả **Passed** |
| Đường độc lập của `QuotaService.reserve` | P1, P2, P3, P4, P5: tất cả có test và **Passed** |
| Độ phủ (JaCoCo) | `QuotaService` 24/24 dòng, 8/8 nhánh; `QuotaLine` 4/4 nhánh; `TimeSlot` 2/2 nhánh; `StoreSettingsRepository` 2/2 nhánh |
| Giới hạn | Chỉ logic quota; đồng thời ở bản rút gọn tại tầng dịch vụ; chưa có kiểm chứng NFR-01 đầy đủ qua API (Sprint 2) |

Đây chưa phải kết quả của các test black-box ở [BLACK_BOX_TEST_CASES.md](../Test-Cases/BLACK_BOX_TEST_CASES.md) (chạy ở T-25).
