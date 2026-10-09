# Source Codes
Input: Sprint Backlog, architecture, database/class design, UI prototype và coding convention.

Quy trình: task → tạo/checkout branch, pull mới nhất → implement → local test → commit/push → Pull Request → review/merge.

Output: source code, commit history, Pull Request và reviewed/merged code.

Trạng thái: khung dự án dựng ở task T-03 (Sprint 1). Đã có backend, frontend, database chạy local và endpoint `/api/health`; chưa có chức năng nghiệp vụ. Convention và formatter/linter chốt ở T-02 ([CODING_CONVENTION.md](CODING_CONVENTION.md)); kiến trúc package chốt ở T-08; schema database thiết kế ở T-09.

## Cấu trúc
```text
03-SourceCodes/
├── compose.yaml          # PostgreSQL cho môi trường local (Docker Compose)
├── .env.example          # Mẫu cấu hình; copy thành .env (không commit .env)
├── backend/              # Spring Boot (Maven Wrapper)
│   ├── mvnw, mvnw.cmd    # Maven Wrapper: tự tải Maven, không cần cài Maven
│   ├── pom.xml
│   └── src/
│       ├── main/java/vn/nhom15/preorder/
│       │   ├── PreorderBackendApplication.java
│       │   ├── config/TimeConfig.java   # Clock theo múi giờ cửa hàng
│       │   └── health/                  # GET /api/health
│       ├── main/resources/
│       │   ├── application.yaml
│       │   └── db/migration/            # Flyway: V1__init.sql, V2__..., ...
│       └── test/java/...                # JUnit test
└── frontend/             # React + TypeScript (Vite)
    ├── vite.config.ts    # Proxy /api → backend
    └── src/
        ├── api/health.ts # Hàm gọi API
        ├── App.tsx       # Màn kiểm tra kết nối
        └── main.tsx
```

## Công nghệ và phiên bản
| Thành phần | Phiên bản | Ghi chú |
|---|---|---|
| Java | 25 (LTS) | |
| Spring Boot | 4.1.1 | Web MVC, JDBC, Flyway |
| Maven | 3.10.0 qua Maven Wrapper | Không cần cài Maven |
| PostgreSQL | image `postgres:18-alpine` | Chạy bằng Docker Compose |
| Node.js | 24 LTS (tối thiểu 22.12) | Yêu cầu của Vite 8 |
| React / Vite / TypeScript | 19 / 8 / 6 | Chưa dùng thư viện component UI |

## Cài đặt trên máy mới
Cần cài trước:
1. **Git**.
2. **JDK 25** (ví dụ Eclipse Temurin hoặc Oracle JDK). Kiểm tra: `java -version`.
3. **Node.js 24 LTS** (kèm npm). Kiểm tra: `node -v` ≥ 22.12.
4. **Docker Desktop**, và phải đang chạy. Kiểm tra: `docker compose version` và `docker info` không báo lỗi.

Không cần cài Maven hoặc PostgreSQL.

## Chạy từ đầu
Lệnh dưới đây dùng Git Bash. Với PowerShell, chỗ khác nhau được ghi chú.

**1. Lấy mã nguồn và tạo file cấu hình**
```bash
git clone https://github.com/huyducnguyen0/smart-food-preorder.git
cd smart-food-preorder/03-SourceCodes
cp .env.example .env
```
PowerShell: `Copy-Item .env.example .env`. Có thể đổi `DB_PASSWORD` trong `.env`; giá trị mẫu chỉ dùng cho máy local.

**2. Chạy PostgreSQL** (trong `03-SourceCodes/`)
```bash
docker compose up -d --wait
```
Lệnh chờ tới khi database `healthy`. Kiểm tra: `docker compose ps`.

**3. Chạy backend** (terminal mới, trong `03-SourceCodes/backend/`)
```bash
./mvnw spring-boot:run
```
PowerShell: `.\mvnw.cmd spring-boot:run`. Lần đầu sẽ tải Maven và thư viện, mất vài phút. Khi chạy, Flyway tự áp các migration chưa chạy; log có dòng `Successfully applied ... migration` và `Started PreorderBackendApplication`.

Kiểm tra: mở http://localhost:8080/api/health, kết quả dạng:
```json
{"status":"UP","database":"UP","schemaVersion":"1","serverTime":"2026-10-09T20:15:59+07:00"}
```

**4. Chạy frontend** (terminal mới, trong `03-SourceCodes/frontend/`)
```bash
npm install
npm run dev
```
Mở http://localhost:5173. Trang hiển thị Trạng thái chung `UP`, Database `UP`, phiên bản schema và giờ máy chủ (+07:00).

**Dừng**: `Ctrl+C` ở terminal backend và frontend; `docker compose stop` để dừng database (giữ dữ liệu).

## Kiểm thử và build
```bash
# trong backend/
./mvnw test
# trong frontend/
npm run lint
npm run build
```
Test backend hiện tại không cần database (giả lập bằng Mockito).

## Cấu hình (.env)
| Biến | Mặc định mẫu | Dùng bởi |
|---|---|---|
| `DB_HOST` | `localhost` | Backend |
| `DB_PORT` | `5433` | Compose (cổng máy host), backend |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `preorder`, `preorder`, `change-me-local-only` | Compose (tạo database lần đầu), backend |
| `BACKEND_PORT` | `8080` | Backend, proxy của Vite |

Compose tự đọc `.env` cạnh `compose.yaml`; backend đọc qua `spring.config.import` trong `application.yaml`; Vite đọc qua `loadEnv` trong `vite.config.ts`. Biến môi trường hệ điều hành có cùng tên sẽ ghi đè giá trị trong `.env`.

Thông tin `DB_NAME`, `DB_USER`, `DB_PASSWORD` chỉ được dùng khi container tạo database **lần đầu**. Đổi sau đó thì phải xóa dữ liệu (xem dưới) hoặc đổi trực tiếp trong PostgreSQL.

## Migration database
- Flyway chạy khi backend khởi động, áp các file `backend/src/main/resources/db/migration/V<số>__<mô_tả>.sql` theo thứ tự số; lịch sử nằm trong bảng `flyway_schema_history`.
- Thay đổi schema bằng file mới (`V2__...`, `V3__...`). Không sửa file đã chạy: Flyway kiểm tra checksum và backend sẽ báo lỗi khi khởi động.
- `V1__init.sql` chỉ xác nhận Flyway hoạt động; bảng nghiệp vụ và dữ liệu mẫu được thêm từ T-09 trở đi.

## Xử lý sự cố
| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `docker compose` báo không kết nối được Docker | Docker Desktop chưa chạy | Mở Docker Desktop, đợi chạy xong rồi chạy lại |
| `Thiếu DB_PASSWORD - copy .env.example thành .env` | Chưa tạo `.env` | Làm bước 1 |
| `port is already allocated` ở cổng 5433 | Cổng đã có chương trình khác dùng | Đổi `DB_PORT` trong `.env` |
| Backend báo `password authentication failed` | Đang kết nối nhầm PostgreSQL cài trên máy (thường cổng 5432), hoặc đổi mật khẩu sau khi database đã tạo | Kiểm tra `DB_PORT`; nếu dữ liệu local không cần giữ thì xóa và tạo lại database |
| Backend báo `Port 8080 was already in use` | Backend cũ chưa tắt hoặc chương trình khác dùng 8080 | Tắt tiến trình cũ hoặc đổi `BACKEND_PORT` |
| Trang báo `HTTP 502` | Backend chưa chạy | Chạy bước 3 |
| `/api/health` trả 503, database `DOWN` (chờ khoảng 30 giây) | Container database đang dừng | `docker compose up -d --wait`; backend tự kết nối lại |

Xóa toàn bộ dữ liệu database local và tạo lại từ migration (**mất hết dữ liệu local**):
```bash
docker compose down -v
docker compose up -d --wait
```
