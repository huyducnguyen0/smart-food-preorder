import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch } from './client'
import { messageOf } from './errorMessages'

function jsonResponse(status: number, body: unknown, contentType = 'application/problem+json') {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': contentType } })
}

describe('apiFetch', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    fetchMock.mockReset()
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('gửi yêu cầu đọc kèm cookie phiên, không kèm token CSRF', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { ok: true }, 'application/json'))

    const result = await apiFetch<{ ok: boolean }>('/api/menu')

    expect(result).toEqual({ ok: true })
    const [, init] = fetchMock.mock.calls[0]
    expect(init.credentials).toBe('same-origin')
    expect(init.headers['X-XSRF-TOKEN']).toBeUndefined()
  })

  it('gửi yêu cầu ghi kèm token CSRF lấy từ cookie', async () => {
    document.cookie = 'XSRF-TOKEN=abc-123'
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 1 }, 'application/json'))

    await apiFetch('/api/auth/login', { method: 'POST', body: { loginId: 'a', password: 'b' } })

    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers['X-XSRF-TOKEN']).toBe('abc-123')
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(init.body).toBe(JSON.stringify({ loginId: 'a', password: 'b' }))
  })

  it('chưa có cookie CSRF thì gọi một yêu cầu đọc để backend cấp trước khi ghi', async () => {
    fetchMock.mockImplementation(async (path: string) => {
      if (path === '/api/auth/me') {
        document.cookie = 'XSRF-TOKEN=moi-cap'
        return jsonResponse(401, { code: 'UNAUTHENTICATED', detail: 'Vui lòng đăng nhập' })
      }
      return jsonResponse(200, {}, 'application/json')
    })

    await apiFetch('/api/auth/logout', { method: 'POST' })

    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      '/api/auth/me',
      '/api/auth/logout',
    ])
    expect(fetchMock.mock.calls[1][1].headers['X-XSRF-TOKEN']).toBe('moi-cap')
  })

  it('đọc lỗi Problem Details thành ApiError có mã và thông tin kèm theo', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(409, {
        code: 'ITEMS_CHANGED',
        detail: 'Món đổi giá',
        changes: [{ menuItemId: 12 }],
      }),
    )

    const error = await apiFetch('/api/orders').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(409)
    expect((error as ApiError).code).toBe('ITEMS_CHANGED')
    expect((error as ApiError).extra.changes).toEqual([{ menuItemId: 12 }])
  })

  it('trả undefined khi phản hồi 204', async () => {
    document.cookie = 'XSRF-TOKEN=t'
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(apiFetch('/api/auth/logout', { method: 'POST' })).resolves.toBeUndefined()
  })

  it('lỗi không có JSON vẫn thành ApiError, không làm vỡ giao diện', async () => {
    fetchMock.mockResolvedValue(new Response('<html>502</html>', { status: 502 }))

    await expect(apiFetch('/api/menu')).rejects.toMatchObject({ status: 502, code: null })
  })

  it('không kết nối được máy chủ thì status = 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(apiFetch('/api/menu')).rejects.toMatchObject({ status: 0 })
  })
})

describe('messageOf', () => {
  it('chọn câu tiếng Việt theo mã lỗi', () => {
    expect(messageOf(new ApiError(401, 'INVALID_CREDENTIALS', 'x'))).toBe(
      'Thông tin đăng nhập không đúng.',
    )
    expect(messageOf(new ApiError(403, 'ACCOUNT_LOCKED', 'x'))).toContain('khóa')
    expect(messageOf(new ApiError(0, null, 'x'))).toContain('kết nối')
  })

  it('mã lạ hoặc lỗi không phải ApiError thì dùng câu chung, không lộ chi tiết kỹ thuật', () => {
    expect(messageOf(new ApiError(500, 'MA_LA', 'stack trace'))).toBe(
      'Có lỗi xảy ra, vui lòng thử lại.',
    )
    expect(messageOf(new Error('boom'))).toBe('Có lỗi xảy ra, vui lòng thử lại.')
  })
})
