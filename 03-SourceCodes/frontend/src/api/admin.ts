import { apiFetch } from './client'

// Kiểu dữ liệu khớp AdminMenuController và AdminSlotController (Architecture mục 5). Chỉ quản lý dùng.

export type AdminCategory = { id: number; name: string }

export type AdminMenuItem = {
  id: number
  categoryId: number
  name: string
  description: string | null
  imageUrl: string | null
  price: number
  /** Điểm công việc của một phần (BR-01): thông tin vận hành, khách không thấy. */
  workloadPoints: number
  available: boolean
  discontinued: boolean
}

export type AdminMenu = { categories: AdminCategory[]; items: AdminMenuItem[] }

export type MenuItemInput = {
  categoryId: number
  name: string
  description: string
  imageUrl: string
  price: number
  workloadPoints: number
}

/** UC-13: mọi món kể cả ngừng bán, kèm danh sách nhóm. */
export function fetchAdminMenu(signal?: AbortSignal): Promise<AdminMenu> {
  return apiFetch<AdminMenu>('/api/admin/menu', { signal })
}

export function createMenuItem(input: MenuItemInput): Promise<AdminMenuItem> {
  return apiFetch<AdminMenuItem>('/api/admin/menu/items', { method: 'POST', body: input })
}

export function updateMenuItem(id: number, input: MenuItemInput): Promise<AdminMenuItem> {
  return apiFetch<AdminMenuItem>(`/api/admin/menu/items/${id}`, { method: 'PUT', body: input })
}

export function setItemAvailable(id: number, available: boolean): Promise<AdminMenuItem> {
  return apiFetch<AdminMenuItem>(`/api/admin/menu/items/${id}/availability`, {
    method: 'POST',
    body: { available },
  })
}

/** Ngừng bán (ẩn khỏi menu, BR-03) hoặc bán lại món đã ngừng. */
export function setItemDiscontinued(id: number, discontinued: boolean): Promise<AdminMenuItem> {
  return apiFetch<AdminMenuItem>(
    `/api/admin/menu/items/${id}/${discontinued ? 'discontinue' : 'restore'}`,
    { method: 'POST' },
  )
}

/**
 * Một khung 15 phút hôm nay (PB-22): vừa là khoảng làm của bếp (công suất, điểm dự kiến làm) vừa là khung nhận của
 * khách (điểm, số đơn nhận trong khung).
 */
export type AdminSlot = {
  start: string
  end: string
  /** Công suất bếp của khoảng làm này (riêng nếu có, ngược lại mặc định). */
  quota: number
  quotaOverride: number | null
  /** Tổng điểm các đơn nhận trong khung này. */
  heldPoints: number
  openOrders: number
  /** Điểm bếp dự kiến làm trong khoảng này theo lịch bếp. */
  plannedPoints: number
  /** Số đơn (không tính đơn hủy) nhận trong khung, so với giới hạn quầy. */
  pickupOrders: number
}

export type AdminDay = {
  date: string
  openTime: string
  closeTime: string
  defaultQuotaPoints: number
  slotMinutes: number
  /** Làm trước tối đa bao lâu so với giờ nhận (phút). */
  holdMinutes: number
  /** Phần trăm công suất mỗi khoảng làm dành cho đơn đặt trước. */
  preOrderSharePercent: number
  /** Số đơn tối đa mỗi khung nhận. */
  pickupLimitOrders: number
  slots: AdminSlot[]
}

/** UC-14: cấu hình cửa hàng và các khung hôm nay. */
export function fetchAdminDay(signal?: AbortSignal): Promise<AdminDay> {
  return apiFetch<AdminDay>('/api/admin/slots/today', { signal })
}

export function updateSettings(input: {
  openTime: string
  closeTime: string
  defaultQuotaPoints: number
  holdMinutes?: number
  preOrderSharePercent?: number
  pickupLimitOrders?: number
}): Promise<AdminDay> {
  return apiFetch<AdminDay>('/api/admin/settings', { method: 'PUT', body: input })
}

/** Quota riêng của một khung hôm nay; `null` để dùng lại quota mặc định. */
export function setSlotQuota(startTime: string, quota: number | null): Promise<AdminDay> {
  return apiFetch<AdminDay>(`/api/admin/slots/today/${startTime}/quota`, {
    method: 'PUT',
    body: { quota },
  })
}

// ---------- UC-15 tài khoản nhân viên ----------

export type StaffRole = 'COUNTER' | 'KITCHEN'

export type StaffAccount = {
  id: number
  loginId: string
  displayName: string
  role: StaffRole
  locked: boolean
}

export async function fetchStaff(signal?: AbortSignal): Promise<StaffAccount[]> {
  const response = await apiFetch<{ staff: StaffAccount[] }>('/api/admin/staff', { signal })
  return response.staff
}

export function createStaff(input: {
  loginId: string
  displayName: string
  password: string
  role: StaffRole
}): Promise<StaffAccount> {
  return apiFetch<StaffAccount>('/api/admin/staff', { method: 'POST', body: input })
}

/** Khóa hoặc mở khóa; trả lại danh sách mới. */
export async function setStaffLocked(id: number, locked: boolean): Promise<StaffAccount[]> {
  const response = await apiFetch<{ staff: StaffAccount[] }>(
    `/api/admin/staff/${id}/${locked ? 'lock' : 'unlock'}`,
    { method: 'POST' },
  )
  return response.staff
}

// ---------- PB-17 theo dõi vận hành ----------

export type DashboardOrder = {
  id: number
  pickupCode: string
  slotStart: string
  status: 'CONFIRMED' | 'PREPARING' | 'READY'
  source: 'PRE_ORDER' | 'WALK_IN'
  /** Đã qua giờ bắt đầu khung mà chưa sẵn sàng (BR-26). */
  late: boolean
}

export type DayStats = {
  preOrders: number
  walkIns: number
  pickedUp: number
  cancelled: number
  noShow: number
  lateNow: number
  heldPoints: number
  quotaPoints: number
  quotaUsagePercent: number
  /** Số đơn chưa kết thúc của các ngày trước, cần dọn ở màn Quầy. */
  staleOpen: number
}

export type Dashboard = {
  date: string
  slots: AdminSlot[]
  activeOrders: DashboardOrder[]
  stats: DayStats
}

export function fetchDashboard(signal?: AbortSignal): Promise<Dashboard> {
  return apiFetch<Dashboard>('/api/admin/dashboard', { signal })
}
