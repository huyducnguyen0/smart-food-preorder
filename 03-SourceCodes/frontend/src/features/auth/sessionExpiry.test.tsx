import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RequireRole } from '../../app/RequireRole'
import { apiFetch } from '../../api/client'
import { AuthProvider } from './AuthProvider'

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })
}

function Protected() {
  return (
    <button
      type="button"
      onClick={() => void apiFetch('/api/kitchen/queue').catch(() => undefined)}
    >
      Gọi API
    </button>
  )
}

function renderApp() {
  return render(
    <MemoryRouter initialEntries={['/kitchen']}>
      <AuthProvider>
        <Routes>
          <Route
            path="/kitchen"
            element={
              <RequireRole roles={['KITCHEN']}>
                <Protected />
              </RequireRole>
            }
          />
          <Route path="/login" element={<p>TRANG ĐĂNG NHẬP</p>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('phiên hết hạn giữa chừng (UC-02 E4)', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('API trả 401 UNAUTHENTICATED thì chuyển tới đăng nhập', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(json(200, { id: 4, displayName: 'Bếp Một', role: 'KITCHEN' }))
      .mockResolvedValueOnce(json(401, { code: 'UNAUTHENTICATED', detail: 'Vui lòng đăng nhập' }))
    renderApp()

    await userEvent.click(await screen.findByRole('button', { name: 'Gọi API' }))

    expect(await screen.findByText('TRANG ĐĂNG NHẬP')).toBeInTheDocument()
  })

  it('lỗi 403 hoặc lỗi khác không làm mất phiên', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(json(200, { id: 4, displayName: 'Bếp Một', role: 'KITCHEN' }))
      .mockResolvedValueOnce(json(403, { code: 'FORBIDDEN', detail: 'x' }))
    renderApp()

    await userEvent.click(await screen.findByRole('button', { name: 'Gọi API' }))

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2))
    expect(screen.getByRole('button', { name: 'Gọi API' })).toBeInTheDocument()
    expect(screen.queryByText('TRANG ĐĂNG NHẬP')).not.toBeInTheDocument()
  })
})
