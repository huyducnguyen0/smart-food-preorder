// Nơi duy nhất gọi fetch (Architecture mục 6). Lo ba việc chung: gửi cookie phiên, gửi token chống CSRF cho yêu cầu
// ghi, và đọc lỗi dạng Problem Details của backend thành ApiError có mã (xem Architecture mục 5.3).

/** Lỗi do backend báo (có mã nghiệp vụ) hoặc do không kết nối được (status = 0). */
export class ApiError extends Error {
  readonly status: number
  /** Mã nghiệp vụ như INVALID_CREDENTIALS; null nếu phản hồi không có mã. */
  readonly code: string | null
  /** Thông tin thêm backend gửi kèm (ví dụ danh sách món đã đổi giá, lỗi từng trường). */
  readonly extra: Record<string, unknown>

  constructor(
    status: number,
    code: string | null,
    detail: string,
    extra: Record<string, unknown> = {},
  ) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.extra = extra
  }
}

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

function readCsrfToken(): string | null {
  const entry = document.cookie.split('; ').find((item) => item.startsWith(`${CSRF_COOKIE}=`))
  return entry ? decodeURIComponent(entry.slice(CSRF_COOKIE.length + 1)) : null
}

/** Yêu cầu ghi cần token; nếu chưa có cookie (lần đầu mở trang) thì gọi một yêu cầu đọc để backend cấp. */
async function ensureCsrfToken(): Promise<string | null> {
  const existing = readCsrfToken()
  if (existing) return existing
  await fetch('/api/auth/me', { credentials: 'same-origin' }).catch(() => undefined)
  return readCsrfToken()
}

async function toApiError(response: Response): Promise<ApiError> {
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('json')) {
    const body: unknown = await response.json().catch(() => null)
    if (body && typeof body === 'object') {
      const { code, detail, ...extra } = body as Record<string, unknown>
      return new ApiError(
        response.status,
        typeof code === 'string' ? code : null,
        typeof detail === 'string' ? detail : `HTTP ${response.status}`,
        extra,
      )
    }
  }
  return new ApiError(response.status, null, `HTTP ${response.status}`)
}

export type ApiOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  signal?: AbortSignal
}

/** Gọi API và trả dữ liệu JSON (undefined nếu phản hồi 204). Ném ApiError khi không thành công. */
export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (method !== 'GET') {
    const token = await ensureCsrfToken()
    if (token) headers[CSRF_HEADER] = token
  }

  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'same-origin',
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError(0, null, 'Không kết nối được máy chủ')
  }

  if (!response.ok) throw await toApiError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
