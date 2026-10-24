import { render } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router'
import type { CurrentUser } from '../api/auth'
import { AuthContext, type AuthContextValue } from '../features/auth/authContext'
import { CartProvider } from '../features/cart/CartProvider'

type AuthOverrides = Partial<Pick<AuthContextValue, 'signIn' | 'signOut'>> & {
  status?: AuthContextValue['status']
  user?: CurrentUser | null
}

/** Dựng giao diện với trạng thái đăng nhập giả lập và một bảng định tuyến tối thiểu để kiểm tra điều hướng. */
export function renderWithAuth(
  element: ReactNode,
  options: {
    path?: string | { pathname: string; state?: unknown }
    auth?: AuthOverrides
    extraRoutes?: ReactNode
  } = {},
) {
  const { path = '/', auth = {}, extraRoutes } = options
  const user = auth.user ?? null
  const status = auth.status ?? (user ? 'authenticated' : 'anonymous')
  const value = {
    status,
    user,
    signIn: auth.signIn ?? (() => Promise.reject(new Error('signIn chưa được giả lập'))),
    signOut: auth.signOut ?? (() => Promise.resolve()),
  } as AuthContextValue

  return render(
    <AuthContext.Provider value={value}>
      <CartProvider>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="*" element={element} />
            {extraRoutes}
          </Routes>
        </MemoryRouter>
      </CartProvider>
    </AuthContext.Provider>,
  )
}
