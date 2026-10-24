import { apiFetch } from './client'

// Kiểu dữ liệu khớp MenuController của backend (Architecture mục 5.2). Không có điểm công việc: đó là thông tin vận hành.
export type MenuItem = {
  id: number
  name: string
  description: string | null
  imageUrl: string | null
  /** Giá hiện tại, đồng. */
  price: number
  /** true: còn bán; false: tạm hết (BR-04). */
  available: boolean
}

export type MenuCategory = {
  id: number
  name: string
  items: MenuItem[]
}

/** UC-03: món đang bán theo nhóm (món ngừng bán không có trong kết quả). */
export async function fetchMenu(signal?: AbortSignal): Promise<MenuCategory[]> {
  const response = await apiFetch<{ categories: MenuCategory[] }>('/api/menu', { signal })
  return response.categories
}

/** UC-04 E1: tình trạng và giá hiện tại của một món, kiểm tra trước khi thêm vào giỏ. */
export function fetchMenuItem(id: number): Promise<MenuItem> {
  return apiFetch<MenuItem>(`/api/menu/items/${id}`)
}
