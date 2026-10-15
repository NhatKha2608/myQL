const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

export interface DateRangeValidation {
  valid: boolean
  message?: string
  from: string
  to: string
}

/** Cộng/trừ số ngày trên chuỗi yyyy-MM-dd (theo lịch local). */
export function shiftISODate(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  date.setDate(date.getDate() + days)
  const yy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yy}-${mm}-${dd}`
}

function daysInclusive(from: string, to: string): number {
  const [y1, m1, d1] = from.split('-').map(Number)
  const [y2, m2, d2] = to.split('-').map(Number)
  const a = new Date(y1, m1 - 1, d1).getTime()
  const b = new Date(y2, m2 - 1, d2).getTime()
  return Math.floor((b - a) / 86400000) + 1
}

export function validateDateRange(
  from: string,
  to: string,
  options?: { maxDays?: number },
): DateRangeValidation {
  if (!ISO_DATE.test(from) || !ISO_DATE.test(to)) {
    return { valid: false, message: 'Ngày không hợp lệ', from, to }
  }
  if (from > to) {
    return {
      valid: false,
      message: 'Ngày bắt đầu không được sau ngày kết thúc',
      from,
      to,
    }
  }
  const maxDays = options?.maxDays ?? 366
  if (daysInclusive(from, to) > maxDays) {
    return {
      valid: false,
      message: `Khoảng thời gian tối đa ${maxDays} ngày`,
      from,
      to,
    }
  }
  return { valid: true, from, to }
}
