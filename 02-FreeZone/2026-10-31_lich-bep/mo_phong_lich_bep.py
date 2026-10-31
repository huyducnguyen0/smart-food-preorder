"""Mô phỏng một ngày bán 10:00–14:00 (16 khung 15 phút), so sánh hai cách nhận đơn.

A (hiện tại): mỗi khung NHẬN có quota 20 điểm; đơn đặt trước và walk-in cùng trừ vào khung nhận.
B (đề xuất): bếp có công suất 20 điểm cho mỗi 15 phút LÀM; đơn nhận lúc S được làm trong [S − H, S] (H = thời gian
   giữ món còn ngon). Nhận đơn khi còn xếp được lịch làm (bếp làm sớm nhất có thể). Thêm giới hạn số đơn mỗi khung nhận
   (quầy) để khách không dồn cục.
Số liệu là GIẢ ĐỊNH để so sánh hai mô hình, không phải dữ liệu quán thật.
"""
import random
import statistics

BINS = 16            # 10:00 .. 13:45
CAP = 20             # điểm bếp làm được mỗi 15 phút
SLOT_QUOTA = 20      # mô hình A
HOLD = 3             # mô hình B: làm trước tối đa 3 khung (45 phút)
PICKUP_LIMIT = 8     # mô hình B: tối đa 8 đơn mỗi khung nhận
MAX_WAIT_SLOTS = 2   # walk-in bỏ đi nếu phải chờ quá 2 khung (khoảng 30–45 phút)
PEAK = [1, 1, 2, 3, 5, 7, 9, 10, 10, 9, 7, 5, 3, 2, 1, 1]  # trọng số theo khung, đỉnh 11:45–12:15


def sample_size(rng):
    return rng.choice([2, 5, 5, 7, 7, 10])


def feasible(orders, start_bin, cap_used, cap=CAP):
    """Xếp lịch làm sớm nhất (EDF) cho mọi đơn, từ start_bin; trả True nếu không vượt công suất.
    orders: list (release, deadline, work) — được làm trong các khung [release, deadline)."""
    free = [cap - cap_used[b] for b in range(BINS)]
    for release, deadline, work in sorted(orders, key=lambda o: o[1]):
        need = work
        for b in range(max(release, start_bin), min(deadline, BINS)):
            take = min(free[b], need)
            free[b] -= take
            need -= take
            if need == 0:
                break
        if need > 0:
            return False
    return True


def run(seed, n_pre=55, n_walk=55, reserve=0.0):
    rng = random.Random(seed)
    pre_req = [(rng.choices(range(2, BINS), PEAK[2:])[0], sample_size(rng)) for _ in range(n_pre)]
    walk_req = sorted((rng.choices(range(BINS - 1), PEAK[:-1])[0], sample_size(rng)) for _ in range(n_walk))

    out = {}
    # ---------- A ----------
    held = [0] * BINS
    pre_ok = 0
    for s, w in pre_req:  # khách thử khung muốn, rồi khung gợi ý ±1
        for cand in (s, s + 1, s - 1):
            if 2 <= cand < BINS and held[cand] + w <= SLOT_QUOTA * (1 - reserve):
                held[cand] += w
                pre_ok += 1
                break
    served, waits = 0, []
    for b, w in walk_req:
        for s in range(b, min(b + MAX_WAIT_SLOTS + 1, BINS)):
            if held[s] + w <= SLOT_QUOTA:
                held[s] += w
                served += 1
                waits.append((s - b + 1) * 15)
                break
    out['A'] = (pre_ok, served, waits)

    # ---------- B ----------
    plan = []  # (release, deadline, work)
    count = [0] * BINS
    pre_ok = 0
    for s, w in pre_req:
        for cand in (s, s + 1, s - 1):
            if not (2 <= cand < BINS) or count[cand] >= PICKUP_LIMIT:
                continue
            order = (cand - HOLD, cand, w)  # làm xong trước giờ bắt đầu khung nhận
            if feasible(plan + [order], 0, [0] * BINS, cap=int(CAP * (1 - reserve))):
                plan.append(order)
                count[cand] += 1
                pre_ok += 1
                break
    # Trong ngày: khung nào đã qua thì việc đã làm trong khung đó là cố định.
    used = [0] * BINS
    remaining = list(plan)
    served, waits = 0, []
    walk_by_bin = {}
    for b, w in walk_req:
        walk_by_bin.setdefault(b, []).append(w)
    for b in range(BINS):
        for w in walk_by_bin.get(b, []):
            for s in range(b, min(b + MAX_WAIT_SLOTS + 1, BINS)):
                if count[s] >= PICKUP_LIMIT:
                    continue
                order = (b, s + 1, w)  # walk-in: làm từ bây giờ, xong trong khung nhận s
                if feasible(remaining + [order], b, used):
                    remaining.append(order)
                    count[s] += 1
                    served += 1
                    waits.append((s - b + 1) * 15)
                    break
        # Bếp làm khung b theo lịch sớm nhất; ghi nhận phần đã làm.
        free = [CAP - used[x] for x in range(BINS)]
        nxt = []
        for release, deadline, work in sorted(remaining, key=lambda o: o[1]):
            need = work
            for x in range(max(release, b), min(deadline, BINS)):
                take = min(free[x], need)
                free[x] -= take
                need -= take
                if x == b:
                    used[b] += take
                    work -= take
                if need == 0:
                    break
            if work > 0:
                nxt.append((release, deadline, work))
        remaining = nxt
    out['B'] = (pre_ok, served, waits)
    out['B_peak_load'] = max(used)
    return out


def report(n_pre, n_walk, runs=300):
    print(f'Nhu cầu: {n_pre} đơn đặt trước + {n_walk} khách walk-in, điểm trung bình 6/đơn; công suất bếp 320 điểm/ngày')
    variants = (('A  hiện tại                    ', 'A', 0.0), ('A+ hiện tại + dành 30% cho walk-in', 'A', 0.3),
                ('B  làm trước (lịch bếp)         ', 'B', 0.0), ('B+ làm trước + dành 30% cho walk-in', 'B', 0.3))
    for name, model, reserve in variants:
        pre, walk, wait = [], [], []
        for seed in range(runs):
            pre_ok, served, waits = run(seed, n_pre, n_walk, reserve)[model]
            pre.append(pre_ok); walk.append(served); wait.append(statistics.mean(waits) if waits else 0)
        print(f'  {name}: đặt trước {statistics.mean(pre):4.1f}/{n_pre} | walk-in {statistics.mean(walk):4.1f}/{n_walk}'
              f' | tổng {statistics.mean(pre)+statistics.mean(walk):4.1f} | walk-in chờ {statistics.mean(wait):4.1f} phút')


for n_pre, n_walk in ((20, 20), (25, 25), (30, 30)):
    report(n_pre, n_walk)
