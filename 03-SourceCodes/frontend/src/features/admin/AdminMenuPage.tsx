import { useCallback, useEffect, useState, type FormEvent } from 'react'
import {
  createMenuItem,
  fetchAdminMenu,
  setItemAvailable,
  setItemDiscontinued,
  updateMenuItem,
  type AdminMenu,
  type AdminMenuItem,
  type MenuItemInput,
} from '../../api/admin'
import { ApiError } from '../../api/client'
import { messageOf } from '../../api/errorMessages'
import { Alert } from '../../components/Alert'
import { formatMoney } from '../../lib/format'

type Draft = {
  categoryId: string
  name: string
  description: string
  imageUrl: string
  price: string
  workloadPoints: string
}

const EMPTY: Draft = {
  categoryId: '',
  name: '',
  description: '',
  imageUrl: '',
  price: '',
  workloadPoints: '',
}

function draftOf(item: AdminMenuItem): Draft {
  return {
    categoryId: String(item.categoryId),
    name: item.name,
    description: item.description ?? '',
    imageUrl: item.imageUrl ?? '',
    price: String(item.price),
    workloadPoints: String(item.workloadPoints),
  }
}

/** Kiểm tra trên giao diện trước khi gửi; backend vẫn kiểm tra lại. */
function validate(draft: Draft): { input?: MenuItemInput; error?: string } {
  if (!draft.categoryId) return { error: 'Chọn nhóm món.' }
  if (!draft.name.trim()) return { error: 'Nhập tên món.' }
  const price = Number(draft.price)
  const points = Number(draft.workloadPoints)
  if (draft.price === '' || !Number.isInteger(price) || price < 0) {
    return { error: 'Giá phải là số nguyên không âm.' }
  }
  if (draft.workloadPoints === '' || !Number.isInteger(points) || points < 0) {
    return { error: 'Điểm công việc phải là số nguyên không âm.' }
  }
  return {
    input: {
      categoryId: Number(draft.categoryId),
      name: draft.name.trim(),
      description: draft.description,
      imageUrl: draft.imageUrl.trim(),
      price,
      workloadPoints: points,
    },
  }
}

/** Câu lỗi kèm lỗi từng trường nếu backend gửi. */
function describe(error: unknown): string {
  if (error instanceof ApiError && Array.isArray(error.extra.errors)) {
    const details = (error.extra.errors as { message?: string }[])
      .map((item) => item.message)
      .filter(Boolean)
    if (details.length > 0) return details.join(' ')
  }
  return messageOf(error)
}

/**
 * UC-13 Quản lý món (quản lý): thêm, sửa, đổi còn bán/tạm hết, ngừng bán thay cho xóa (BR-01–BR-05). Sửa giá và điểm
 * chỉ ảnh hưởng đơn tạo sau đó.
 */
export function AdminMenuPage() {
  const [menu, setMenu] = useState<AdminMenu | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [draft, setDraft] = useState<Draft>(EMPTY)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [message, setMessage] = useState<{ kind: 'success' | 'danger'; text: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetchAdminMenu(controller.signal)
      .then((data) => {
        setMenu(data)
        setLoadError(null)
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setLoadError(messageOf(error))
      })
    return () => controller.abort()
  }, [tick])

  const reload = useCallback(() => setTick((value) => value + 1), [])

  async function run(action: () => Promise<unknown>, success: string) {
    setBusy(true)
    setMessage(null)
    try {
      await action()
      setMessage({ kind: 'success', text: success })
      reload()
      return true
    } catch (error) {
      setMessage({ kind: 'danger', text: describe(error) })
      return false
    } finally {
      setBusy(false)
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const { input, error } = validate(draft)
    if (!input) {
      setMessage({ kind: 'danger', text: error ?? 'Dữ liệu chưa hợp lệ.' })
      return
    }
    const ok =
      editingId === null
        ? await run(() => createMenuItem(input), `Đã thêm món "${input.name}".`)
        : await run(() => updateMenuItem(editingId, input), `Đã lưu món "${input.name}".`)
    if (ok) {
      setDraft(EMPTY)
      setEditingId(null)
    }
  }

  function startEdit(item: AdminMenuItem) {
    setEditingId(item.id)
    setDraft(draftOf(item))
    setMessage(null)
  }

  const set = (name: keyof Draft) => (value: string) =>
    setDraft((current) => ({ ...current, [name]: value }))

  return (
    <div className="content">
      <h1>Quản lý món</h1>

      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      {loadError && <Alert kind="danger">{loadError}</Alert>}

      <form className="admin-form" onSubmit={handleSubmit} noValidate aria-label="Thông tin món">
        <h2 className="section-title">{editingId === null ? 'Thêm món' : 'Sửa món'}</h2>
        <label className="field">
          <span>Nhóm</span>
          <select value={draft.categoryId} onChange={(e) => set('categoryId')(e.target.value)}>
            <option value="">Chọn nhóm</option>
            {menu?.categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Tên món</span>
          <input value={draft.name} onChange={(e) => set('name')(e.target.value)} />
        </label>
        <div className="admin-row">
          <label className="field">
            <span>Giá (đồng)</span>
            <input
              inputMode="numeric"
              value={draft.price}
              onChange={(e) => set('price')(e.target.value)}
            />
          </label>
          <label className="field">
            <span>Điểm công việc</span>
            <input
              inputMode="numeric"
              value={draft.workloadPoints}
              onChange={(e) => set('workloadPoints')(e.target.value)}
            />
          </label>
        </div>
        <label className="field">
          <span>Mô tả (tùy chọn)</span>
          <input value={draft.description} onChange={(e) => set('description')(e.target.value)} />
        </label>
        <label className="field">
          <span>Đường dẫn ảnh (tùy chọn)</span>
          <input value={draft.imageUrl} onChange={(e) => set('imageUrl')(e.target.value)} />
        </label>
        <div className="c-actions">
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {editingId === null ? 'Thêm món' : 'Lưu'}
          </button>
          {editingId !== null && (
            <button
              type="button"
              className="btn"
              onClick={() => {
                setEditingId(null)
                setDraft(EMPTY)
              }}
            >
              Hủy sửa
            </button>
          )}
        </div>
        <p className="muted">Sửa giá hoặc điểm không làm đổi các đơn đã xác nhận.</p>
      </form>

      {menu &&
        menu.categories.map((category) => {
          const items = menu.items.filter((item) => item.categoryId === category.id)
          if (items.length === 0) return null
          return (
            <section key={category.id} aria-label={category.name}>
              <h2 className="section-title">{category.name}</h2>
              <ul className="items">
                {items.map((item) => (
                  <li key={item.id} className={`item${item.discontinued ? ' item-off' : ''}`}>
                    <div className="item-body">
                      <b>{item.name}</b>
                      <span className="price">{formatMoney(item.price)}</span>
                      <span className="tag">{item.workloadPoints} điểm</span>
                      {item.discontinued ? (
                        <span className="tag">Ngừng bán</span>
                      ) : (
                        !item.available && <span className="tag">Tạm hết</span>
                      )}
                      <div className="admin-actions">
                        <button
                          type="button"
                          className="link-btn"
                          aria-label={`Sửa ${item.name}`}
                          onClick={() => startEdit(item)}
                        >
                          Sửa
                        </button>
                        {!item.discontinued && (
                          <button
                            type="button"
                            className="link-btn"
                            disabled={busy}
                            aria-label={`${item.available ? 'Báo tạm hết' : 'Báo còn bán'} ${item.name}`}
                            onClick={() =>
                              run(
                                () => setItemAvailable(item.id, !item.available),
                                `${item.name}: ${item.available ? 'tạm hết' : 'còn bán'}.`,
                              )
                            }
                          >
                            {item.available ? 'Tạm hết' : 'Còn bán'}
                          </button>
                        )}
                        <button
                          type="button"
                          className="link-btn danger-link"
                          disabled={busy}
                          aria-label={`${item.discontinued ? 'Bán lại' : 'Ngừng bán'} ${item.name}`}
                          onClick={() =>
                            run(
                              () => setItemDiscontinued(item.id, !item.discontinued),
                              `${item.name}: ${item.discontinued ? 'bán lại' : 'ngừng bán'}.`,
                            )
                          }
                        >
                          {item.discontinued ? 'Bán lại' : 'Ngừng bán'}
                        </button>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          )
        })}
    </div>
  )
}
