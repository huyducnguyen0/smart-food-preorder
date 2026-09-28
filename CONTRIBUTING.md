# Working Agreement

## Nguyên tắc

- Mọi thay đổi chính thức phải liên kết với một issue/task hoặc artifact ID.
- Không code feature khi requirement và acceptance criteria tương ứng chưa đủ rõ.
- Không sửa trực tiếp artifact đã `Approved` mà không ghi lịch sử thay đổi.
- Không commit secret, mật khẩu, token hoặc file `.env`.

## Branch

```text
main                 phiên bản ổn định
feature/<id>-<name>  phát triển chức năng
fix/<id>-<name>      sửa lỗi
docs/<id>-<name>     tài liệu
chore/<name>         cấu hình và bảo trì repository
```

Ví dụ: `feature/US-005-slot-capacity`.

## Commit

Định dạng khuyến nghị:

```text
<type>(<scope>): <mô tả ngắn>
```

Các type chính: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`.

## Pull Request

- Phạm vi nhỏ, có mục tiêu duy nhất.
- Mô tả requirement hoặc issue liên quan.
- Nêu rõ cách kiểm thử và ảnh hưởng tới tài liệu.
- Cần ít nhất một người khác review trước khi merge.
- Không merge khi test bắt buộc chưa đạt.

## Definition of Done

- [ ] Requirement và acceptance criteria rõ ràng.
- [ ] Design liên quan đã cập nhật.
- [ ] Code tuân thủ convention và không chứa secret.
- [ ] Test bắt buộc đã chạy và pass.
- [ ] Pull Request đã review.
- [ ] Traceability matrix và tài liệu bị ảnh hưởng đã cập nhật.
- [ ] Feature có thể demo hoặc có bằng chứng kiểm chứng.
