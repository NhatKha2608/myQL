import type { AcademicYear } from '~/types/api'
import { todayISO } from '~/utils/date'

function day(value?: string | null): string | null {
  if (!value) return null
  const iso = value.slice(0, 10)
  return /^\d{4}-\d{2}-\d{2}$/.test(iso) ? iso : null
}

/** Năm học đang diễn ra theo ngày hiện tại. Ngoài thời gian học thì lấy năm sắp bắt đầu. */
export function currentAcademicYear(years: AcademicYear[], today = todayISO()): AcademicYear | null {
  if (!years.length) return null

  const ongoing = years.find((year) => {
    const start = day(year.startDate)
    const end = day(year.endDate)
    return start != null && end != null && start <= today && today <= end
  })
  if (ongoing) return ongoing

  const upcoming = years
    .filter((year) => {
      const start = day(year.startDate)
      return start != null && start > today
    })
    .sort((a, b) => day(a.startDate)!.localeCompare(day(b.startDate)!))
  if (upcoming[0]) return upcoming[0]

  const finished = years
    .filter((year) => {
      const end = day(year.endDate)
      return end != null && end < today
    })
    .sort((a, b) => day(b.endDate)!.localeCompare(day(a.endDate)!))
  return finished[0] ?? years[0]
}
