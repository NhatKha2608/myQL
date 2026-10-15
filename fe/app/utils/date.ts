export function todayISO(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Ngày yyyy-MM-dd cách hôm nay `days` ngày (local). */
export function daysFromTodayISO(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() + days)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Ngày sinh đủ ngày, hoặc chỉ năm nếu file Excel chỉ có năm sinh. */
export function formatBirth(iso?: string | null): string {
  if (!iso) return '—'
  const [y, m, d] = iso.slice(0, 10).split('-')
  if (!y || !m || !d) return '—'
  if (m === '01' && d === '01') return y
  return `${d}/${m}/${y}`
}

export function formatDateVN(iso?: string | null): string {
  if (!iso) return '—'
  try {
    const d = new Date(iso.includes('T') ? iso : `${iso}T00:00:00`)
    return d.toLocaleDateString('vi-VN')
  } catch {
    return iso
  }
}

export function formatDateTimeVN(iso?: string | null): string {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('vi-VN')
  } catch {
    return iso
  }
}

export const DAY_LABELS = [
  '',
  'Thứ 2',
  'Thứ 3',
  'Thứ 4',
  'Thứ 5',
  'Thứ 6',
  'Thứ 7',
  'Chủ nhật',
]
