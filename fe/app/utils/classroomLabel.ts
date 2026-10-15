export function classroomTitle(name?: string | null) {
  if (!name) return 'Chọn lớp'
  return /^lớp\s/i.test(name) ? name : `Lớp ${name}`
}

export function classroomCode(name?: string | null) {
  if (!name) return '—'
  return name.replace(/^lớp\s+/i, '').trim().slice(0, 4) || 'Lớp'
}

export function academicYearLabel(name?: string | null) {
  if (!name) return 'Chưa có năm học'
  const pretty = name.replace(/\s*[-–]\s*/, ' – ')
  return /năm học/i.test(pretty) ? pretty : `Năm học ${pretty}`
}
