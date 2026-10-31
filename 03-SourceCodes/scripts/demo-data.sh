#!/usr/bin/env bash
# Tạo đơn mẫu cho buổi demo: gọi API thật như người dùng bấm trên giao diện, nên mọi quy tắc (quota, giá, quyền) vẫn
# được kiểm tra. Đơn gắn với khung giờ của HÔM NAY, nên chạy ngay trước khi demo, trong giờ mở cửa (10:00–21:00 theo
# cấu hình mẫu) và còn ít nhất khoảng 45 phút trước giờ đóng cửa.
#
# Cần: hệ thống đang chạy CÓ dữ liệu mẫu (tài khoản khach1@example.com, 0901234567, bep1, quay1, quanly; mật khẩu
# Demo@2026), bash và curl (có sẵn trong Git Bash). Cách chạy, trong 03-SourceCodes/:
#   bash scripts/demo-data.sh                          # hệ thống chạy bằng Docker (http://localhost:8000)
#   BASE_URL=http://localhost:5173 bash scripts/demo-data.sh   # chạy dev (Vite)
#
# Mỗi khách mẫu chỉ có tối đa 3 đơn chưa kết thúc (BR-37): chạy lại lần hai trong cùng ngày có thể bị từ chối. Muốn
# làm lại từ đầu với Docker: docker compose --profile app down -v rồi up lại (mất toàn bộ dữ liệu local).
set -u
BASE_URL=${BASE_URL:-http://localhost:8000}
PASSWORD=${DEMO_PASSWORD:-Demo@2026}
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
failures=0

# ---------- HTTP ----------
uuid() {
  printf '%04x%04x-%04x-4%03x-%04x-%04x%04x%04x' $RANDOM $RANDOM $RANDOM $((RANDOM % 4096)) \
    $(((RANDOM % 16384) | 32768)) $RANDOM $RANDOM $RANDOM
}
token() { grep XSRF-TOKEN "$WORK/$1.jar" | awk '{print $7}'; }

# login <tên phiên> <định danh>
login() {
  curl -s -c "$WORK/$1.jar" -b "$WORK/$1.jar" "$BASE_URL/api/auth/me" > /dev/null
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' -c "$WORK/$1.jar" -b "$WORK/$1.jar" \
    -H "X-XSRF-TOKEN: $(token "$1")" -H 'Content-Type: application/json' \
    -d "{\"loginId\":\"$2\",\"password\":\"$PASSWORD\"}" "$BASE_URL/api/auth/login")
  if [ "$code" != 200 ]; then
    echo "Không đăng nhập được $2 (HTTP $code). Hệ thống có đang chạy với dữ liệu mẫu ở $BASE_URL không?" >&2
    exit 1
  fi
}

# post <tên phiên> <đường dẫn> [json]  → in thân trả về, mã HTTP ở dòng cuối
post() {
  local body="$WORK/body.json"
  printf '%s' "${3:-}" > "$body"
  curl -s -c "$WORK/$1.jar" -b "$WORK/$1.jar" -X POST -H "X-XSRF-TOKEN: $(token "$1")" \
    -H 'Content-Type: application/json; charset=utf-8' --data-binary "@$body" \
    -w '\n%{http_code}' "$BASE_URL$2"
}

field() { sed -nE "s/.*\"$1\":\"?([^\",}]*)\"?.*/\1/p" <<< "$2" | head -1; }
status_of() { tail -n 1 <<< "$1"; }

# step <mô tả> <kết quả post> <mã mong đợi>
step() {
  local code; code=$(status_of "$2")
  if [ "$code" = "$3" ]; then
    echo "  OK   $1"
  else
    failures=$((failures + 1))
    echo "  LỖI  $1 (HTTP $code): $(head -n 1 <<< "$2" | sed -nE 's/.*"detail":"([^"]*)".*/\1/p')"
  fi
}

# ---------- giờ cửa hàng (UTC+7) ----------
now_min=$((10#$(date -u -d '+7 hours' +%H) * 60 + 10#$(date -u -d '+7 hours' +%M)))
today=$(date -u -d '+7 hours' +%Y-%m-%d)
# Khung đặt trước sớm nhất: bắt đầu sau ít nhất 30 phút (BR-15), làm tròn lên mốc 15 phút.
first=$(((now_min + 30 + 14) / 15 * 15))
slot() { printf '%sT%02d:%02d' "$today" $(($1 / 60)) $(($1 % 60)); }
SLOT1=$(slot "$first")
SLOT2=$(slot $((first + 15)))

# ---------- giá hiện tại của món (khách gửi giá đã thấy, BR-35) ----------
MENU=$(curl -s "$BASE_URL/api/menu")
price() { sed -nE "s/.*\"id\":$1,[^}]*\"price\":([0-9]+).*/\1/p" <<< "$MENU" | head -1; }
BURGER=$(price 1)
FRIES=$(price 5)
TEA=$(price 6)
if [ -z "$BURGER" ] || [ -z "$FRIES" ] || [ -z "$TEA" ]; then
  echo "Menu không có món mẫu 1, 5, 6 (Burger bò, Khoai tây chiên, Trà đào). Cần chạy với dữ liệu mẫu." >&2
  exit 1
fi
line() { printf '{"menuItemId":%s,"quantity":%s,"unitPrice":%s}' "$1" "$2" "$3"; }
order_json() { printf '{"requestId":"%s","slotStart":"%s","lines":[%s]}' "$(uuid)" "$1" "$2"; }

echo "Tạo dữ liệu demo tại $BASE_URL, giờ cửa hàng $(slot "$now_min" | cut -c12-), khung $SLOT1 và $SLOT2"
login khach khach1@example.com
login khach2 0901234567
login bep bep1@example.com
login quay quay1@example.com
login ql quanly@example.com

# Lưu kết quả: mã nhận và trạng thái mong đợi để in bảng cuối.
summary=()
# Đặt đơn; trả về lỗi (để bỏ qua các bước sau của đơn này) nếu không đặt được.
place() { # place <phiên> <nhãn> <khung> <dòng...>
  local r; r=$(post "$1" /api/orders "$(order_json "$3" "$4")")
  step "$2: đặt đơn" "$r" 201
  [ "$(status_of "$r")" = 201 ] || return 1
  LAST_ID=$(field id "$(head -n 1 <<< "$r")")
  LAST_CODE=$(field pickupCode "$(head -n 1 <<< "$r")")
}

# Đơn đặt trước để ở trạng thái "chờ bếp" và "đã hủy" để demo luồng khách tự hủy; các trạng thái bếp, quầy dùng đơn
# walk-in (ở khung hiện tại). Bếp làm trước đơn đặt trước được (BR-21), bấm "Bắt đầu" trên màn bếp khi demo.
# Điểm theo menu mẫu: Burger bò 5, Khoai tây chiên 2, Trà đào 0.
echo "Khách đặt trước:"
if place khach "Đơn A (chờ bếp)" "$SLOT1" "$(line 1 1 "$BURGER"),$(line 6 2 "$TEA")"; then
  summary+=("$LAST_CODE|Pre-order|Đã xác nhận, chờ bếp|khach1@example.com")
fi

if place khach2 "Đơn B (chờ bếp)" "$SLOT1" "$(line 1 1 "$BURGER"),$(line 5 1 "$FRIES")"; then
  summary+=("$LAST_CODE|Pre-order|Đã xác nhận, chờ bếp|0901234567")
fi

if place khach "Đơn C (khách tự hủy)" "$SLOT2" "$(line 1 1 "$BURGER")"; then
  step "Đơn C: khách hủy" "$(post khach "/api/orders/$LAST_ID/cancel")" 200
  summary+=("$LAST_CODE|Pre-order|Đã hủy (khách)|khach1@example.com")
fi

if place khach2 "Đơn D (cửa hàng hủy)" "$SLOT2" "$(line 1 1 "$BURGER")"; then
  step "Đơn D: quản lý hủy kèm lý do" \
    "$(post ql "/api/counter/orders/$LAST_ID/cancel" '{"reason":"Hết bánh mì burger, xin lỗi quý khách"}')" 200
  summary+=("$LAST_CODE|Pre-order|Đã hủy (cửa hàng, có lý do)|0901234567")
fi

if place khach2 "Đơn E (chờ bếp, khung sau)" "$SLOT2" "$(line 5 1 "$FRIES"),$(line 6 1 "$TEA")"; then
  summary+=("$LAST_CODE|Pre-order|Đã xác nhận, chờ bếp|0901234567")
fi

echo "Khách mua tại quầy (khung hiện tại, đã thu tiền mặt):"
# walk_in <nhãn> <các bước sau khi tạo: start, complete, handover>
walk_in() {
  local label=$1 lines est start r id code
  lines="$(line 5 1 "$FRIES"),$(line 6 1 "$TEA")"
  est=$(post quay /api/counter/walk-in/estimate "{\"lines\":[$lines]}")
  step "$label: ước tính khung" "$est" 200
  start=$(sed -nE 's/.*"start":"([^"]*)".*/\1/p' <<< "$(head -n 1 <<< "$est")")
  r=$(post quay /api/counter/walk-in \
    "{\"requestId\":\"$(uuid)\",\"slotStart\":\"$start\",\"paymentMethod\":\"CASH\",\"lines\":[$lines]}")
  step "$label: xác nhận" "$r" 201
  [ "$(status_of "$r")" = 201 ] || return 1
  id=$(field id "$(head -n 1 <<< "$r")")
  code=$(field pickupCode "$(head -n 1 <<< "$r")")
  shift
  for action in "$@"; do
    case $action in
      start) step "$label: bếp bắt đầu" "$(post bep "/api/kitchen/orders/$id/start")" 200 ;;
      complete) step "$label: bếp hoàn tất" "$(post bep "/api/kitchen/orders/$id/complete")" 200 ;;
      handover) step "$label: quầy bàn giao" \
        "$(post quay "/api/counter/orders/$id/handover" "{\"pickupCode\":\"$code\"}")" 200 ;;
    esac
  done
  LAST_CODE=$code
}
if walk_in "Walk-in F"; then summary+=("$LAST_CODE|Walk-in|Đã xác nhận, chờ bếp|—"); fi
if walk_in "Walk-in G" start; then summary+=("$LAST_CODE|Walk-in|Đang chuẩn bị|—"); fi
if walk_in "Walk-in H" start complete; then summary+=("$LAST_CODE|Walk-in|Sẵn sàng, chờ khách lấy|—"); fi
if walk_in "Walk-in I" start complete handover; then summary+=("$LAST_CODE|Walk-in|Đã nhận|—"); fi

echo
echo "Mã nhận | Nguồn | Trạng thái | Tài khoản khách"
for row in "${summary[@]}"; do echo "$row" | sed 's/|/ | /g'; done
echo
echo "Đơn đặt trước đang chờ bếp: bếp bấm \"Bắt đầu\" (làm trước được), khách sẽ thấy trang đơn tự đổi trạng thái."
echo "Xem: khách ($BASE_URL/orders), bếp ($BASE_URL/kitchen), quầy ($BASE_URL/counter), quản lý ($BASE_URL/admin)."
echo "Mật khẩu mọi tài khoản mẫu: $PASSWORD"
if [ "$failures" -gt 0 ]; then
  echo "Có $failures bước lỗi (xem dòng LỖI ở trên)." >&2
  exit 1
fi
