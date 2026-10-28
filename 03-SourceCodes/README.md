# Source Codes
Input: Sprint Backlog, architecture, database/class design, UI prototype và coding convention.

Quy trình: task → tạo/checkout branch, pull mới nhất → implement → local test → commit/push → Pull Request → review/merge.

Output: source code, commit history, Pull Request và reviewed/merged code.

Trạng thái: khung dự án dựng ở task T-03 (Sprint 1); chức năng nghiệp vụ làm qua Sprint 1–3. Từ bước chuẩn bị phát hành (2026-10-28) chạy được toàn bộ hệ thống trong container bằng một lệnh (mục Chạy toàn bộ hệ thống bằng Docker). Coding convention và formatter/linter chốt ở T-02: [CODING_CONVENTION.md](CODING_CONVENTION.md). Kiến trúc package chốt ở T-08; schema database thiết kế ở T-09.

## Cấu trúc
```text
03-SourceCodes/
├── CODING_CONVENTION.md  # Quy ước code, formatter, linter (T-02)
├── compose.yaml          # PostgreSQL (mặc định); thêm backend + frontend với --profile app
├── .env.example          # Mẫu cấu hình; copy thành .env (không commit .env)
├── backend/              # Spring Boot (Maven Wrapper)
│   ├── Dockerfile        # Image chạy backend (build jar rồi chạy bằng JRE)
│   ├── mvnw, mvnw.cmd    # Maven Wrapper: tự tải Maven, không cần cài Maven
│   ├── pom.xml           # Thư viện, plugin Spotless (format) và Checkstyle (lint)
│   ├── config/checkstyle/checkstyle.xml
│   └── src/
│       ├── main/java/vn/nhom15/preorder/
│       │   ├── PreorderBackendApplication.java
│       │   ├── auth/                    # Đăng nhập, đăng xuất, tài khoản (UC-02)
│       │   ├── common/                  # Mã lỗi, ApiException, xử lý lỗi chung
│       │   ├── config/                  # TimeConfig (Clock), SecurityConfig (phiên, CSRF, phân quyền)
│       │   └── health/                  # GET /api/health
│       ├── main/resources/
│       │   ├── application.yaml
│       │   ├── db/migration/            # Flyway: cấu trúc bảng V1__init.sql, V2__..., ...
│       │   └── db/seed/                 # Dữ liệu mẫu R__...sql (chỉ local/demo)
│       └── test/java/...                # JUnit test; support/TestDatabase = PostgreSQL thật chạy trong test
└── frontend/             # React + TypeScript (Vite)
    ├── Dockerfile        # Image frontend: build React, nginx phục vụ file tĩnh
    ├── nginx.conf        # nginx chuyển tiếp /api tới backend, mọi đường dẫn khác trả index.html
    ├── vite.config.ts    # Proxy /api → backend
    ├── .prettierrc.json  # Cấu hình Prettier (format)
    ├── .oxlintrc.json    # Cấu hình oxlint (lint)
    └── src/
        ├── main.tsx      # Điểm vào: bộ định tuyến, AuthProvider, font
        ├── app/          # App.tsx (bảng định tuyến), RequireRole (chặn trang theo vai trò)
        ├── api/          # client.ts (nơi duy nhất gọi fetch, CSRF, lỗi), auth.ts, errorMessages.ts
        ├── features/     # Mỗi nhóm màn hình một thư mục: auth, menu, kitchen (các nhóm khác thêm dần)
        ├── components/   # Dùng chung: AppShell (khung + thanh tab), Alert, Icon
        └── test/         # Hỗ trợ test (setup, renderWithAuth)
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
| React Router | 8 | Điều hướng giữa các màn hình |
| Vitest + Testing Library | 4 / 16 | Test frontend (`npm test`) |
| Be Vietnam Pro | `@fontsource` | Font thiết kế cho tiếng Việt, cài qua npm nên không cần mạng ngoài |

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
{"status":"UP","database":"UP","schemaVersion":"2","serverTime":"2026-10-09T20:15:59+07:00"}
```

**4. Chạy frontend** (terminal mới, trong `03-SourceCodes/frontend/`)
```bash
npm install
npm run dev
```
Mở http://localhost:5173. Trang hiển thị Trạng thái chung `UP`, Database `UP`, phiên bản schema và giờ máy chủ (+07:00).

**Dừng**: `Ctrl+C` ở terminal backend và frontend; `docker compose stop` để dừng database (giữ dữ liệu).

## Chạy toàn bộ hệ thống bằng Docker (demo, chuẩn bị phát hành)
Chỉ cần **Git** và **Docker Desktop** đang chạy; không cần JDK hay Node trên máy. Trong `03-SourceCodes/`:
```bash
cp .env.example .env
docker compose --profile app up -d --build --wait
```
Lần đầu build mất vài phút (tải image nền, thư viện Maven và npm). Lệnh chờ tới khi cả ba container `healthy`/`running`. Mở http://localhost:8000 (đổi bằng `APP_PORT`) và đăng nhập bằng tài khoản mẫu.

| Container | Nội dung | Cổng ra máy host |
|---|---|---|
| `db` | PostgreSQL 18, dữ liệu trong volume `pgdata` | `DB_PORT` (5433) |
| `backend` | Spring Boot; Flyway tự áp migration khi khởi động | Không mở ra ngoài; chỉ frontend gọi tới |
| `frontend` | nginx: trang React và chuyển tiếp `/api` tới backend | `APP_PORT` (8000) |

- Xem log: `docker compose --profile app logs -f backend`.
- Dừng, giữ dữ liệu: `docker compose --profile app stop`. Xóa container và **cả dữ liệu**: `docker compose --profile app down -v`.
- Sửa code thì chạy lại lệnh `up ... --build` để build lại image.
- Image không chạy test; chạy test bằng `./mvnw verify` và `npm test` trước khi merge (mục Kiểm thử).
- Triển khai thật: đặt `FLYWAY_LOCATIONS=classpath:db/migration` để không nạp tài khoản và menu mẫu, đổi `DB_PASSWORD`, và đặt HTTPS phía trước (chưa làm trong dự án).

## Chạy nhanh không cần Docker (dùng để phát triển và thử tay)
Nếu Docker Desktop chưa chạy được, backend vẫn chạy được trên một PostgreSQL 18 thật khởi động ngay trong tiến trình (cùng thư viện với test). Mỗi lần chạy tạo database mới có sẵn dữ liệu mẫu; tắt là mất dữ liệu.
```bash
# terminal 1, trong backend/ (lần đầu mất vài phút để tải PostgreSQL nhúng)
./mvnw spring-boot:test-run -Dspring-boot.run.main-class=vn.nhom15.preorder.DevApplication
# terminal 2, trong frontend/
npm install
npm run dev
```
Mở http://localhost:5173 và đăng nhập bằng một tài khoản mẫu (mục Tài khoản mẫu). Không cần tạo `.env` cho cách này. Cách chạy chính thức vẫn là Docker Compose ở trên.

## Kiểm thử, kiểm tra convention và build
Chạy trước mỗi commit/PR (chưa có CI):
```bash
# trong backend/: test + kiểm tra định dạng (Spotless) + lint (Checkstyle)
./mvnw verify
# trong frontend/: định dạng (Prettier) + lint (oxlint) + kiểm tra kiểu và build
npm run format:check && npm run lint && npm run build && npm test
```
Nếu báo lỗi định dạng, chạy `./mvnw spotless:apply` (backend) hoặc `npm run format` (frontend) để tự sửa. Lỗi Checkstyle/oxlint phải sửa tay theo [CODING_CONVENTION.md](CODING_CONVENTION.md). Chỉ chạy test backend: `./mvnw test`. Test chạy trên **PostgreSQL 18 thật được khởi động ngay trong test** (thư viện `embedded-postgres`, tự tải bản nhị phân lần đầu), nên **không cần Docker** để chạy test. Lần đầu khởi động PostgreSQL nhúng mất khoảng 15–20 giây. Nếu máy báo lỗi khi khởi động (ví dụ chạy bằng quyền Administrator), xem mục Xử lý sự cố.

## Cấu hình (.env)
| Biến | Mặc định mẫu | Dùng bởi |
|---|---|---|
| `DB_HOST` | `localhost` | Backend |
| `DB_PORT` | `5433` | Compose (cổng máy host), backend |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `preorder`, `preorder`, `change-me-local-only` | Compose (tạo database lần đầu), backend |
| `BACKEND_PORT` | `8080` | Backend, proxy của Vite |
| `APP_PORT` | `8000` | Compose `--profile app`: cổng mở trang web |
| `FLYWAY_LOCATIONS` | (không đặt: có dữ liệu mẫu) | Compose `--profile app`: thư mục migration backend nạp |

Compose tự đọc `.env` cạnh `compose.yaml`; backend đọc qua `spring.config.import` trong `application.yaml`; Vite đọc qua `loadEnv` trong `vite.config.ts`. Biến môi trường hệ điều hành có cùng tên sẽ ghi đè giá trị trong `.env`.

Thông tin `DB_NAME`, `DB_USER`, `DB_PASSWORD` chỉ được dùng khi container tạo database **lần đầu**. Đổi sau đó thì phải xóa dữ liệu (xem dưới) hoặc đổi trực tiếp trong PostgreSQL.

## Migration database
- Flyway chạy khi backend khởi động, áp các file `backend/src/main/resources/db/migration/V<số>__<mô_tả>.sql` theo thứ tự số; lịch sử nằm trong bảng `flyway_schema_history`.
- Thay đổi schema bằng file mới (`V2__...`, `V3__...`). Không sửa file đã chạy: Flyway kiểm tra checksum và backend sẽ báo lỗi khi khởi động.
- `V1__init.sql` chỉ xác nhận Flyway hoạt động; mỗi bảng nghiệp vụ được tạo ở task đầu tiên cần nó (`V2` tài khoản ở T-12, các bảng khác theo [Database Design](../01-ManagedArea/03-Design/Database/DATABASE_DESIGN.md) mục 5).
- **Dữ liệu mẫu** (tài khoản, menu, giờ mở cửa, quota) nằm riêng ở `backend/src/main/resources/db/seed/` dưới dạng migration lặp `R__<mô_tả>.sql` (chạy lại khi file đổi, dùng `ON CONFLICT DO NOTHING`). `application.yaml` bật cả hai thư mục (`db/migration` và `db/seed`); khi triển khai công khai phải bỏ `db/seed` khỏi `spring.flyway.locations`.

## Tài khoản mẫu (chỉ dùng ở local)
Đăng nhập bằng email hoặc số điện thoại; mọi tài khoản mẫu dùng chung mật khẩu **`Demo@2026`**.
| Định danh | Vai trò | Ghi chú |
|---|---|---|
| `khach1@example.com` | Khách | |
| `0901234567` | Khách | Đăng nhập bằng số điện thoại |
| `khoa@example.com` | Khách | Tài khoản bị khóa, dùng để thử lỗi UC-02 E2 |
| `bep1@example.com` | Nhân viên bếp | |
| `quay1@example.com` | Nhân viên quầy | |
| `quanly@example.com` | Quản lý | |

Thử nhanh bằng dòng lệnh (cần cookie chống CSRF: lấy token rồi gửi lại ở header `X-XSRF-TOKEN`). Với trình duyệt, frontend làm việc này tự động ở T-13.
```bash
curl -s -c jar.txt http://localhost:8080/api/auth/me > /dev/null            # nhận cookie XSRF-TOKEN
TOKEN=$(grep XSRF-TOKEN jar.txt | awk '{print $7}')
curl -s -b jar.txt -c jar.txt -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json"   -d '{"loginId":"khach1@example.com","password":"Demo@2026"}' http://localhost:8080/api/auth/login
curl -s -b jar.txt http://localhost:8080/api/auth/me
```

## Xử lý sự cố
| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `docker compose` báo không kết nối được Docker | Docker Desktop chưa chạy | Mở Docker Desktop, đợi chạy xong rồi chạy lại |
| `Thiếu DB_PASSWORD - copy .env.example thành .env` | Chưa tạo `.env` | Làm bước 1 |
| `port is already allocated` ở cổng 5433 | Cổng đã có chương trình khác dùng | Đổi `DB_PORT` trong `.env` |
| Backend báo `password authentication failed` | Đang kết nối nhầm PostgreSQL cài trên máy (thường cổng 5432), hoặc đổi mật khẩu sau khi database đã tạo | Kiểm tra `DB_PORT`; nếu dữ liệu local không cần giữ thì xóa và tạo lại database |
| Backend báo `Port 8080 was already in use` | Backend cũ chưa tắt hoặc chương trình khác dùng 8080 | Tắt tiến trình cũ hoặc đổi `BACKEND_PORT` |
| Trang báo `HTTP 502` | Backend chưa chạy | Chạy bước 3 |
| `verify` in cảnh báo `Could not transfer metadata ... jvnet-nexus-releases ... certificate_expired` | Một thư viện cũ mà plugin Checkstyle dùng gián tiếp khai báo kho Maven đã ngừng hoạt động | Vô hại, bỏ qua; thư viện vẫn tải từ Maven Central |
| `./mvnw test` báo không khởi động được PostgreSQL nhúng | PostgreSQL không chạy được bằng quyền Administrator, hoặc phần mềm diệt virus chặn tiến trình `postgres.exe` | Mở terminal bằng tài khoản thường; thêm thư mục tạm của Windows (`%TEMP%\embedded-pg`) vào danh sách loại trừ của phần mềm diệt virus |
| `/api/health` trả 503, database `DOWN` (chờ khoảng 30 giây) | Container database đang dừng | `docker compose up -d --wait`; backend tự kết nối lại |

Xóa toàn bộ dữ liệu database local và tạo lại từ migration (**mất hết dữ liệu local**):
```bash
docker compose down -v
docker compose up -d --wait
```
