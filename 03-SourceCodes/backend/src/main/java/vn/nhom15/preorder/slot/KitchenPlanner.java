package vn.nhom15.preorder.slot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Xếp lịch bếp (PB-22): chia điểm công việc của các đơn vào các khoảng làm 15 phút, mỗi khoảng có công suất riêng.
 *
 * <p>Cách xếp: đơn có hạn sớm hơn được xếp trước; mỗi đơn lấp vào khoảng làm <b>sớm nhất</b> còn trống trong cửa sổ
 * của nó. Nhờ vậy bếp tranh thủ làm trước lúc vắng, giờ đông còn trống cho khách tại quầy. Đơn đặt trước chỉ được dùng
 * một phần công suất mỗi khoảng (phần còn lại dành cho walk-in).
 *
 * <p>Bảo đảm: lịch trả về "xếp được" thì không khoảng nào vượt công suất, nên không bao giờ nhận đơn quá sức bếp. Cách
 * xếp đơn giản này không phải tối ưu tuyệt đối trong mọi trường hợp: hiếm khi nó từ chối một đơn mà một cách xếp khéo
 * hơn vẫn nhận được (an toàn cho bếp, chỉ thiệt một ít doanh thu).
 *
 * <p>Lớp thuần, không đọc database và không biết giờ hệ thống: mọi thứ truyền vào, nên test được bằng số cụ thể.
 */
public final class KitchenPlanner {

    /** Loại việc: đơn đặt trước bị giới hạn phần công suất dành cho đặt trước; walk-in dùng được toàn bộ. */
    public enum Kind {
        PRE_ORDER,
        WALK_IN
    }

    /**
     * Một khoảng làm.
     *
     * @param start giờ bắt đầu khoảng
     * @param capacity điểm bếp làm được trong khoảng (đã trừ phần thời gian đã trôi qua nếu là khoảng hiện tại)
     */
    public record Interval(LocalDateTime start, int capacity) {}

    /**
     * Một việc cần xếp.
     *
     * @param key định danh để đọc lại kết quả (mã đơn, hoặc null với đơn đang xét)
     * @param kind loại đơn
     * @param release thời điểm sớm nhất được làm (khoảng bắt đầu từ mốc này trở đi)
     * @param deadline phải làm xong trước mốc này (khoảng kết thúc không muộn hơn)
     * @param work điểm công việc
     * @param late đơn đã quá hạn: vẫn được ưu tiên xếp sớm nhất nhưng không làm lịch "không xếp được"
     */
    public record Job(Long key, Kind kind, LocalDateTime release, LocalDateTime deadline, int work, boolean late) {}

    /**
     * Kết quả xếp.
     *
     * @param feasible mọi việc chưa quá hạn đều xếp đủ trong cửa sổ của nó
     * @param plannedByInterval điểm đã xếp vào từng khoảng, theo giờ bắt đầu khoảng
     * @param firstIntervalByKey khoảng đầu tiên mỗi việc được xếp vào (giờ bếp nên bắt đầu), theo {@code key}
     */
    public record Result(
            boolean feasible,
            Map<LocalDateTime, Integer> plannedByInterval,
            Map<Long, LocalDateTime> firstIntervalByKey) {}

    private KitchenPlanner() {}

    /**
     * Xếp lịch.
     *
     * @param intervals các khoảng làm, theo thứ tự thời gian, độ dài {@code intervalMinutes}
     * @param intervalMinutes độ dài một khoảng (15)
     * @param jobs các việc
     * @param preOrderSharePercent phần trăm công suất mỗi khoảng mà đơn đặt trước được dùng (0–100)
     */
    public static Result plan(List<Interval> intervals, int intervalMinutes, List<Job> jobs, int preOrderSharePercent) {
        int n = intervals.size();
        int[] free = new int[n];
        int[] preFree = new int[n];
        for (int i = 0; i < n; i++) {
            free[i] = intervals.get(i).capacity();
            preFree[i] = intervals.get(i).capacity() * preOrderSharePercent / 100;
        }
        Map<LocalDateTime, Integer> planned = new LinkedHashMap<>();
        intervals.forEach(interval -> planned.put(interval.start(), 0));
        Map<Long, LocalDateTime> firstByKey = new LinkedHashMap<>();

        // Đơn quá hạn xếp trước tiên (cần làm gấp), rồi theo hạn, rồi theo mốc được làm.
        List<Job> ordered = new ArrayList<>(jobs);
        ordered.sort(Comparator.comparing((Job job) -> !job.late())
                .thenComparing(Job::deadline)
                .thenComparing(Job::release));

        boolean feasible = true;
        for (Job job : ordered) {
            int need = job.work();
            for (int i = 0; i < n && need > 0; i++) {
                LocalDateTime start = intervals.get(i).start();
                LocalDateTime end = start.plusMinutes(intervalMinutes);
                if (start.isBefore(job.release()) || (!job.late() && end.isAfter(job.deadline()))) {
                    continue;
                }
                int room = job.kind() == Kind.PRE_ORDER ? Math.min(free[i], preFree[i]) : free[i];
                int take = Math.min(room, need);
                if (take <= 0) {
                    continue;
                }
                free[i] -= take;
                if (job.kind() == Kind.PRE_ORDER) {
                    preFree[i] -= take;
                }
                need -= take;
                planned.merge(start, take, Integer::sum);
                if (job.key() != null) {
                    firstByKey.putIfAbsent(job.key(), start);
                }
            }
            if (need > 0 && !job.late()) {
                feasible = false;
            }
        }
        return new Result(feasible, planned, firstByKey);
    }
}
