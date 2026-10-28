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

export type AdminSlot = {
  start: string
  end: string
  /** Quota hiệu lực của khung (riêng nếu có, ngược lại mặc định). */
  quota: number
  quotaOverride: number | null
  heldPoints: number
  openOrders: number
}

export type AdminDay = {
  date: string
  openTime: string
  closeTime: string
  defaultQuotaPoints: number
  slotMinutes: number
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
