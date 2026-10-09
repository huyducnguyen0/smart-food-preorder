// Kiểu dữ liệu khớp với HealthResponse của backend (GET /api/health).
export type HealthResponse = {
  status: 'UP' | 'DOWN'
  database: 'UP' | 'DOWN'
  schemaVersion: string | null
  serverTime: string
}

// Gọi đường dẫn tương đối: khi phát triển, Vite proxy chuyển /api sang backend.
// Backend trả 503 kèm JSON khi database lỗi, nên vẫn đọc body thay vì coi mọi mã khác 2xx là lỗi mạng.
export async function fetchHealth(signal?: AbortSignal): Promise<HealthResponse> {
  const response = await fetch('/api/health', { signal })
  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) {
    throw new Error(`Backend không phản hồi đúng (HTTP ${response.status})`)
  }
  return (await response.json()) as HealthResponse
}
