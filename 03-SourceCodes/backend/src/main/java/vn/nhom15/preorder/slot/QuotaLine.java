package vn.nhom15.preorder.slot;

/**
 * Một dòng của đơn nhìn từ phía quota: điểm công việc của món và số lượng.
 *
 * @param workloadPoints điểm công việc của một phần món (BR-01), không âm
 * @param quantity số lượng, ít nhất 1
 */
public record QuotaLine(int workloadPoints, int quantity) {

    public QuotaLine {
        if (workloadPoints < 0) {
            throw new IllegalArgumentException("Điểm công việc không được âm: " + workloadPoints);
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng phải từ 1: " + quantity);
        }
    }
}
