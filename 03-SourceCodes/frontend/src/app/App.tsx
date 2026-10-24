import { Link, Navigate, Route, Routes } from 'react-router'
import { AppShell } from '../components/AppShell'
import { PlaceholderPage } from '../components/PlaceholderPage'
import { LoginPage } from '../features/auth/LoginPage'
import { useAuth } from '../features/auth/useAuth'
import { KitchenPage } from '../features/kitchen/KitchenPage'
import { CartPage } from '../features/cart/CartPage'
import { CheckoutPage } from '../features/checkout/CheckoutPage'
import { MenuPage } from '../features/menu/MenuPage'
import { OrderDetailPage } from '../features/orders/OrderDetailPage'
import { RequireRole } from './RequireRole'

/** Trang chủ: nhân viên bếp vào thẳng hàng chờ bếp, người khác vào thực đơn (UI Prototype mục 2). */
function Home() {
  const { user } = useAuth()
  if (user?.role === 'KITCHEN') return <Navigate to="/kitchen" replace />
  return <MenuPage />
}

function ForbiddenPage() {
  return (
    <div className="empty">
      <h1>Không có quyền</h1>
      <p className="muted">Tài khoản của bạn không dùng được chức năng này.</p>
      <Link className="btn btn-primary" to="/">
        Về trang chính
      </Link>
    </div>
  )
}

function NotFoundPage() {
  return (
    <div className="empty">
      <h1>Không tìm thấy trang</h1>
      <Link className="btn btn-primary" to="/">
        Về trang chính
      </Link>
    </div>
  )
}

/** Bảng định tuyến theo Navigation Flow của UI Prototype. Trang của khách và bếp được bảo vệ theo vai trò. */
export function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Home />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="forbidden" element={<ForbiddenPage />} />
        <Route
          path="cart"
          element={
            <RequireRole roles={['CUSTOMER']}>
              <CartPage />
            </RequireRole>
          }
        />
        <Route
          path="checkout"
          element={
            <RequireRole roles={['CUSTOMER']}>
              <CheckoutPage />
            </RequireRole>
          }
        />
        <Route
          path="orders"
          element={
            <RequireRole roles={['CUSTOMER']}>
              <PlaceholderPage title="Đơn của tôi" task="T-20" />
            </RequireRole>
          }
        />
        <Route
          path="orders/:id"
          element={
            <RequireRole roles={['CUSTOMER']}>
              <OrderDetailPage />
            </RequireRole>
          }
        />
        <Route
          path="kitchen"
          element={
            <RequireRole roles={['KITCHEN']}>
              <KitchenPage />
            </RequireRole>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
