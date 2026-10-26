import { ApiError } from './client'

// Câu thông báo tiếng Việt theo mã lỗi của backend (Architecture mục 5.3). Backend chỉ gửi mã; câu chữ nằm ở đây
// để giao diện tự quyết định cách nói với người dùng.
const MESSAGES: Record<string, string> = {
  VALIDATION_ERROR: 'Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại.',
  UNAUTHENTICATED: 'Vui lòng đăng nhập.',
  INVALID_CREDENTIALS: 'Thông tin đăng nhập không đúng.',
  ACCOUNT_LOCKED: 'Tài khoản đã bị khóa, liên hệ quản lý.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  MENU_ITEM_NOT_FOUND: 'Món này không còn trong thực đơn.',
  ORDER_NOT_FOUND: 'Không tìm thấy đơn.',
  OPEN_ORDER_LIMIT_REACHED: 'Bạn đang có quá nhiều đơn chưa hoàn tất, chưa đặt thêm được.',
  SLOT_NOT_BOOKABLE: 'Khung giờ này không còn đặt được, vui lòng chọn lại.',
  ITEMS_CHANGED: 'Một số món đã thay đổi, vui lòng xem lại giỏ hàng.',
  ORDER_TOO_LARGE: 'Đơn quá lớn cho một khung giờ, vui lòng bớt món hoặc liên hệ cửa hàng.',
  SLOT_FULL: 'Khung giờ này vừa hết chỗ, vui lòng chọn khung khác.',
  INVALID_STATUS_TRANSITION: 'Đơn đã thay đổi trạng thái, danh sách sẽ được làm mới.',
  ALREADY_PAID: 'Đơn này đã được ghi nhận thanh toán.',
  ORDER_NOT_PAYABLE: 'Đơn đã kết thúc hoặc bị hủy nên không ghi nhận thanh toán được.',
  PICKUP_CODE_MISMATCH: 'Mã nhận không khớp với đơn, hỏi lại khách và nhập lại.',
  ORDER_NOT_PAID: 'Đơn chưa thanh toán, hãy thu tiền trước khi bàn giao.',
  INTERNAL_ERROR: 'Có lỗi hệ thống, vui lòng thử lại.',
}

/** Câu thông báo cho người dùng từ một lỗi bất kỳ. */
export function messageOf(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return 'Không kết nối được máy chủ, vui lòng thử lại.'
    if (error.code && MESSAGES[error.code]) return MESSAGES[error.code]
    return 'Có lỗi xảy ra, vui lòng thử lại.'
  }
  return 'Có lỗi xảy ra, vui lòng thử lại.'
}
