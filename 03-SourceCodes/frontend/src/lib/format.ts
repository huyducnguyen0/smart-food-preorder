/** Giá tiền kiểu Việt Nam: 45000 → "45.000đ". Tiền luôn là số nguyên đồng (Architecture mục 4). */
export function formatMoney(amount: number): string {
  return `${amount.toLocaleString('vi-VN')}đ`
}
