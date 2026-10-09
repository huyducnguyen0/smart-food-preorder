# Coding Convention
Trạng thái: Chốt ở task T-02 (Sprint 1) ngày 2026-10-09 theo lựa chọn của người dùng. Bổ sung khi có quy ước mới; quy ước tổ chức package/module chốt ở Architecture (T-08), quy ước bảng/cột ở Database Design (T-09).

Theo Bước 15 của [quy trình IT3180](../00-References/IT3180_PROJECT_PROCESS.md): thống nhất indentation, whitespace, line length, naming, declaration, expression, comment, documentation. Project mẫu của nguồn tham khảo convention của Sun; dự án giữ cách thụt 4 space của Sun cho Java, còn định dạng chi tiết giao cho formatter tự động.

## Nguồn tham khảo
| Phạm vi | Chuẩn | Ghi chú |
|---|---|---|
| Java – định dạng | [Palantir Java Format](https://github.com/palantir/palantir-java-format) | Biến thể của Google Java Format: thụt 4 space, dòng 120 ký tự |
| Java – đặt tên, khai báo | [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), [Code Conventions for Java (Sun/Oracle)](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html) | Khi hai nguồn khác nhau, theo tài liệu này |
| TypeScript/React – định dạng | [Prettier](https://prettier.io/docs/options) | Cấu hình trong `frontend/.prettierrc.json` |
| TypeScript/React – lint | [oxlint](https://oxc.rs/docs/guide/usage/linter) | Cấu hình trong `frontend/.oxlintrc.json` (từ template Vite) |

## Công cụ và lệnh
| Việc | Backend (trong `backend/`) | Frontend (trong `frontend/`) |
|---|---|---|
| Tự định dạng | `./mvnw spotless:apply` | `npm run format` |
| Kiểm tra định dạng | `./mvnw spotless:check` | `npm run format:check` |
| Lint | `./mvnw checkstyle:check` | `npm run lint` |
| Kiểm tra toàn bộ trước khi mở PR | `./mvnw verify` (test + Spotless + Checkstyle) | `npm run format:check && npm run lint && npm run build` |

PowerShell dùng `.\mvnw.cmd` thay cho `./mvnw`. `verify` báo `BUILD FAILURE` khi code chưa định dạng hoặc vi phạm luật Checkstyle; sửa rồi chạy lại. Dự án chưa có CI (quyết định 2026-10-09), nên chạy các lệnh này trước mỗi commit/PR là việc của người thực hiện, và PR ghi rõ đã chạy.

Cấu hình đi kèm:
- `.editorconfig` ở gốc repository: UTF-8, LF, thụt 4 space cho Java/XML và 2 space cho phần còn lại. Hầu hết editor (IntelliJ, VS Code có extension EditorConfig) tự áp dụng.
- `.gitattributes`: mọi file text lưu và checkout bằng LF, trừ `*.cmd`/`*.bat`. Nhờ vậy formatter không báo sai do CRLF trên Windows.
- `backend/config/checkstyle/checkstyle.xml`: bộ luật Checkstyle.

## Quy ước chung
- **Ngôn ngữ**: tên trong code (class, biến, hàm, bảng, cột, endpoint) bằng tiếng Anh; comment, Javadoc, thông báo log và thông báo hiển thị cho người dùng bằng tiếng Việt.
- **Thuật ngữ**: dịch nhất quán theo mục Thuật ngữ của [SRS](../01-ManagedArea/02-Requirements/SRS/SRS.md); một khái niệm chỉ có một tên trong toàn bộ code. Tên lớp miền (entity) chốt ở Class Design (T-10).
- **Truy vết**: khi code hiện thực một quy tắc nghiệp vụ, ghi mã quy tắc trong comment, ví dụ `// BR-09: chỉ xác nhận khi điểm đơn ≤ quota còn lại`.
- **Không commit** code bị comment bỏ, `System.out.println`, `console.log` dùng để debug, hoặc bí mật (xem Git workflow trong README).

## 1. Indentation
| | Java | TypeScript/React, CSS, JSON |
|---|---|---|
| Thụt lề | 4 space; dòng tiếp nối 8 space | 2 space |
| Tab | Không dùng (Checkstyle `FileTabCharacter`) | Không dùng |
| Kiểm tra | Spotless + Palantir | Prettier |

## 2. Whitespace
- Một khoảng trắng quanh toán tử nhị phân, sau dấu phẩy, sau từ khóa (`if (`, `for (`), trước `{`.
- Một dòng trống giữa các method và giữa các nhóm logic trong method dài; không để nhiều dòng trống liên tiếp.
- Import Java: một khối `import static`, một dòng trống, rồi một khối import thường, sắp theo thứ tự chữ cái (formatter tự sắp).
- Cuối file có một dòng trống; không có khoảng trắng thừa cuối dòng.
- Toàn bộ do formatter tự sửa; không chỉnh tay khác với kết quả formatter.

## 3. Line length
| | Giới hạn | Kiểm tra |
|---|---|---|
| Java | 120 ký tự; trừ dòng `package`, `import` và dòng có URL | Palantir tự xuống dòng; Checkstyle `LineLength` báo khi vượt (ví dụ chuỗi quá dài) |
| TypeScript/React | 100 ký tự | Prettier tự xuống dòng |

Chuỗi dài (SQL, thông báo) tách bằng text block Java `"""` hoặc nối chuỗi thay vì để một dòng rất dài.

## 4. Naming
**Java** (Checkstyle kiểm tra các dòng có ✓)
| Thành phần | Quy ước | Ví dụ trong dự án | |
|---|---|---|---|
| Package | chữ thường, không gạch dưới, gốc `vn.nhom15.preorder` | `vn.nhom15.preorder.health` | ✓ |
| Class, record, interface, enum | PascalCase, danh từ | `HealthService`, `HealthResponse` | ✓ |
| Method | camelCase, bắt đầu bằng động từ | `check()`, `health()` | ✓ |
| Field, tham số, biến cục bộ | camelCase | `jdbcTemplate`, `schemaVersion` | ✓ |
| Hằng (`static final` bất biến) | UPPER_SNAKE_CASE | `LATEST_SCHEMA_VERSION_SQL`, `UP` | ✓ |
| Logger | `private static final Logger log` (ngoại lệ của luật hằng, vì logger có trạng thái) | `HealthService.log` | ✓ |
| Viết tắt | Viết như một từ thường | `HttpStatus`, `isHttpOk` (không phải `isHTTPOk`) | ✓ |
| Giá trị enum | UPPER_SNAKE_CASE, trùng tên trạng thái trong SRS | `CONFIRMED`, `NO_SHOW` | |
| Lớp theo vai trò | Hậu tố cho biết trách nhiệm | `...Controller`, `...Service`, `...Repository`, `...Request`, `...Response` | |
| Class test | Tên class được test + `Test` | `HealthServiceTest` | |
| Method test | camelCase mô tả hành vi mong đợi | `returns503WhenDatabaseIsDown` | ✓ |

**TypeScript/React**
| Thành phần | Quy ước | Ví dụ trong dự án |
|---|---|---|
| Component và file component | PascalCase, file `.tsx` | `App.tsx`, `function App()` |
| File không phải component | camelCase `.ts`, nhóm theo thư mục | `api/health.ts` |
| Hàm, biến | camelCase; hàm xử lý sự kiện bắt đầu bằng `handle` | `fetchHealth`, `handleRetry` |
| Hook | `use` + PascalCase | `useState`, hook tự viết `useOrders` |
| Type | PascalCase | `HealthResponse`, `CheckState` |
| Hằng cấp module | camelCase nếu là cấu hình thường; UPPER_SNAKE_CASE nếu là giá trị cố định dùng nhiều nơi | `envDir` |

**API và database**
| Thành phần | Quy ước | Ví dụ |
|---|---|---|
| Đường dẫn REST | bắt đầu `/api/`, chữ thường, nối bằng `-` | `/api/health` |
| Field JSON | camelCase, khớp record/type hai phía | `schemaVersion`, `serverTime` |
| Bảng, cột SQL | snake_case (số ít/số nhiều chốt cùng ERD ở T-09) | `flyway_schema_history` |
| File migration | `V<số>__<mo_ta_snake_case>.sql`, số tăng dần, không sửa file đã chạy | `V1__init.sql` |

## 5. Declaration
**Java**
- Mỗi dòng khai báo một biến (Checkstyle `MultipleVariableDeclarations`), mỗi dòng một câu lệnh (`OneStatementPerLine`).
- Không dùng `import *` (`AvoidStarImport`); không để import thừa (`UnusedImports`).
- Thứ tự modifier theo chuẩn Java: `public protected private abstract static final ...` (`ModifierOrder`).
- Dependency inject qua constructor và lưu vào field `private final`; không dùng `@Autowired` trên field trong code chính (test được phép).
- Dữ liệu vào/ra API dùng `record`.
- Thứ tự trong class: hằng → field → constructor → method public → method private.
- Biến khai báo gần chỗ dùng đầu tiên.

**TypeScript**
- Dùng `const`; chỉ dùng `let` khi biến thật sự được gán lại; không dùng `var`.
- Dùng `type` cho dữ liệu; hạn chế `any`, dùng `unknown` rồi kiểm tra kiểu (ví dụ `catch (error: unknown)`).
- Import kiểu bằng `import { type X }` (TypeScript đang bật `verbatimModuleSyntax`).

## 6. Expression and statement
**Java**
- `if`, `for`, `while` luôn có `{}` kể cả một dòng (Checkstyle `NeedBraces`).
- So sánh chuỗi bằng `equals`, đặt hằng ở bên trái khi giá trị có thể null: `HealthResponse.UP.equals(status)` (`StringLiteralEquality`).
- Không để `catch` rỗng (`EmptyCatchBlock`); bắt exception cụ thể nhất có thể và ghi log hoặc chuyển thành lỗi có nghĩa.
- Override `equals` thì override cả `hashCode` (`EqualsHashCode`).
- Toán tử ba ngôi chỉ dùng cho biểu thức ngắn; logic có nhiều nhánh dùng `if`/`switch`.
- Lấy thời gian hiện tại qua `Clock` được inject, không gọi `LocalDateTime.now()` không tham số (để test cố định thời điểm và đúng múi giờ cửa hàng, SRS mục D).
- Số có ý nghĩa nghiệp vụ (30 phút, 15 phút...) đặt thành hằng hoặc cấu hình có tên, không viết số trực tiếp trong logic.

**TypeScript/React**
- So sánh bằng `===` / `!==`.
- Promise phải được xử lý (`await`, `.then/.catch`); gọi bỏ qua kết quả có chủ ý thì ghi `void`.
- Không gọi `setState` đồng bộ trong `useEffect` (oxlint `react/set-state-in-effect`); xem cách làm trong `App.tsx`.
- Gọi API qua hàm trong `src/api/`, không gọi `fetch` trực tiếp trong component.

## 7. Comment
- Comment giải thích **vì sao** (lý do, quy tắc nghiệp vụ, ràng buộc), không lặp lại code làm gì.
- Comment ghi mã quy tắc khi hiện thực SRS: `// BR-23: chỉ hủy khi chưa vào cửa sổ chuẩn bị`.
- `TODO` phải kèm mã task hoặc story: `// TODO(T-21): đánh dấu đơn trễ`. Không merge `TODO` không có mã.
- Cập nhật hoặc xóa comment khi code thay đổi; comment sai còn tệ hơn không có comment.

## 8. Documentation
- **Java**: mỗi class/record public có Javadoc ngắn nêu trách nhiệm. Method public có Javadoc khi tên và kiểu chưa đủ nói rõ hành vi, điều kiện hoặc lỗi trả về. Record mô tả field bằng `@param` (xem `HealthResponse`).
- **TypeScript**: comment `//` phía trên hàm/type export khi cần giải thích hợp đồng với backend hoặc hành vi không hiển nhiên (xem `api/health.ts`).
- **Hướng dẫn chạy, cấu hình, migration**: [03-SourceCodes/README.md](README.md). Quyết định thiết kế: tài liệu trong `01-ManagedArea/03-Design`, không viết dài trong comment.

## Áp dụng khi tự review PR
- [ ] `./mvnw verify` và `npm run format:check && npm run lint && npm run build` đều đạt.
- [ ] Tên theo bảng Naming, dùng đúng thuật ngữ SRS.
- [ ] Logic nghiệp vụ có comment mã BR; không còn code debug hoặc `TODO` không mã.
- [ ] Class/record public mới có Javadoc trách nhiệm.
