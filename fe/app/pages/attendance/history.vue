<template>
  <div class="space-y-4">
    <div class="card flex flex-wrap items-end gap-3 p-4">
      <NuxtLink to="/attendance" class="btn-secondary">Quay lại</NuxtLink>
      <div>
        <label class="label" for="attendance-month">Tháng</label>
        <input id="attendance-month" v-model="month" type="month" class="input !w-auto" />
      </div>
      <button
        type="button"
        class="btn-secondary"
        :disabled="!selectedId || exporting"
        @click="exportExcel"
      >
        {{ exporting ? 'Đang xuất…' : 'Xuất Excel' }}
      </button>
    </div>

    <div class="flex flex-wrap gap-2 text-xs">
      <span v-for="item in legend" :key="item.mark" class="inline-flex items-center gap-1.5 rounded-lg bg-white px-2 py-1 text-slate-600">
        <span class="grid h-5 w-5 place-items-center rounded font-bold" :class="item.cls">{{ item.mark }}</span>
        {{ item.label }}
      </span>
    </div>

    <CommonLoadingState v-if="loading" />
    <CommonErrorState v-else-if="loadError" :message="loadError" @retry="load" />
    <div v-else class="card overflow-x-auto">
      <table v-if="rows.length" class="data-table min-w-max">
        <thead>
          <tr>
            <th class="sticky left-0 z-20 w-14 min-w-14 bg-slate-100 px-2 py-2 text-center">STT</th>
            <th class="sticky left-14 z-20 min-w-[11rem] bg-slate-100 px-3 py-2 text-left">Học sinh</th>
            <th
              v-for="day in days"
              :key="day.iso"
              class="min-w-[3.25rem] px-1 py-1.5 text-center"
              :class="day.weekend ? 'bg-emerald-50' : 'bg-slate-100'"
            >
              <div>{{ day.date }}</div>
              <div class="text-[11px] font-medium" :class="day.weekend ? 'text-emerald-700' : 'text-slate-500'">
                {{ day.weekday }}
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="row.id">
            <td
              class="sticky left-0 z-10 px-2 py-2 text-center tabular-nums text-slate-500"
              :class="index % 2 ? 'bg-slate-50' : 'bg-white'"
            >
              {{ index + 1 }}
            </td>
            <td
              class="sticky left-14 z-10 px-3 py-2 font-medium text-slate-800"
              :class="index % 2 ? 'bg-slate-50' : 'bg-white'"
            >
              {{ row.name }}
            </td>
            <td
              v-for="day in days"
              :key="`${row.id}-${day.iso}`"
              class="px-1 py-1.5 text-center"
              :class="day.weekend ? 'bg-emerald-50/60' : index % 2 ? 'bg-slate-50' : 'bg-white'"
            >
              <span
                v-if="cellOf(row.id, day.iso)"
                class="inline-grid h-6 min-w-6 place-items-center rounded px-1 text-xs font-bold"
                :class="markOf(cellOf(row.id, day.iso)!.status).cls"
                :title="cellTitle(cellOf(row.id, day.iso)!)"
              >
                {{ markOf(cellOf(row.id, day.iso)!.status).mark }}
              </span>
              <span v-else class="text-slate-300">—</span>
            </td>
          </tr>
        </tbody>
      </table>
      <CommonEmptyState v-else message="Không có học sinh trong lớp" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AttendanceRecord, AttendanceStatus } from '~/types/api'
import { useAttendanceApi, useClassroomApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'
import { todayISO } from '~/utils/date'

definePageMeta({ middleware: 'auth' })

const MARK: Record<AttendanceStatus, { mark: string; label: string; cls: string }> = {
  PRESENT: { mark: 'C', label: 'Có mặt', cls: 'bg-mint-100 text-mint-700' },
  LATE: { mark: 'M', label: 'Muộn', cls: 'bg-primary-100 text-primary-700' },
  ABSENT_EXCUSED: { mark: 'P', label: 'Có phép', cls: 'bg-amber-100 text-amber-800' },
  ABSENT_UNEXCUSED: { mark: 'K', label: 'Không phép', cls: 'bg-rose-100 text-rose-700' },
}

const legend = [
  MARK.PRESENT,
  MARK.LATE,
  MARK.ABSENT_EXCUSED,
  MARK.ABSENT_UNEXCUSED,
]

const { selectedId, loadClassrooms } = useClassroomContext()
const loading = ref(false)
const exporting = ref(false)
const loadError = ref<string | null>(null)
const dialog = useAppDialog()
const month = ref(todayISO().slice(0, 7))
const records = ref<AttendanceRecord[]>([])
const rows = ref<{ id: number; name: string }[]>([])

const days = computed(() => {
  const match = /^(\d{4})-(\d{2})$/.exec(month.value)
  if (!match) return []
  const year = Number(match[1])
  const monthIndex = Number(match[2])
  const count = new Date(year, monthIndex, 0).getDate()
  return Array.from({ length: count }, (_, index) => {
    const date = index + 1
    const jsDay = new Date(year, monthIndex - 1, date).getDay()
    const iso = `${match[1]}-${match[2]}-${String(date).padStart(2, '0')}`
    return {
      iso,
      date,
      weekday: jsDay === 0 ? 'CN' : `T${jsDay + 1}`,
      weekend: jsDay === 0 || jsDay === 6,
    }
  })
})

const byStudentDate = computed(() => {
  const map = new Map<number, Map<string, AttendanceRecord>>()
  for (const record of records.value) {
    const date = (record.attendanceDate || '').slice(0, 10)
    if (!date) continue
    let daysOfStudent = map.get(record.studentId)
    if (!daysOfStudent) {
      daysOfStudent = new Map()
      map.set(record.studentId, daysOfStudent)
    }
    daysOfStudent.set(date, record)
  }
  return map
})

function cellOf(studentId: number, iso: string) {
  return byStudentDate.value.get(studentId)?.get(iso)
}

function markOf(status: AttendanceStatus) {
  return MARK[status]
}

function cellTitle(record: AttendanceRecord) {
  const label = MARK[record.status].label
  return record.note ? `${label} — ${record.note}` : label
}

function monthBounds(value: string) {
  const match = /^(\d{4})-(\d{2})$/.exec(value)
  if (!match) return null
  const year = Number(match[1])
  const monthIndex = Number(match[2])
  const last = new Date(year, monthIndex, 0).getDate()
  return {
    from: `${match[1]}-${match[2]}-01`,
    to: `${match[1]}-${match[2]}-${String(last).padStart(2, '0')}`,
  }
}

async function load() {
  if (!selectedId.value) return
  const bounds = monthBounds(month.value)
  if (!bounds) return
  loading.value = true
  loadError.value = null
  try {
    const [roster, list] = await Promise.all([
      useClassroomApi().students(selectedId.value),
      useAttendanceApi().list(selectedId.value, bounds.from, bounds.to),
    ])
    const seen = new Set<number>()
    const next: { id: number; name: string }[] = []
    for (const student of roster) {
      seen.add(student.id)
      next.push({ id: student.id, name: student.fullName })
    }
    for (const record of list) {
      if (seen.has(record.studentId)) continue
      seen.add(record.studentId)
      next.push({ id: record.studentId, name: record.studentName || '—' })
    }
    rows.value = next
    records.value = list
  } catch (e) {
    loadError.value = extractErrorMessage(e, 'Không tải được lịch sử điểm danh')
    rows.value = []
    records.value = []
  } finally {
    loading.value = false
  }
}

async function exportExcel() {
  if (!selectedId.value || exporting.value) return
  exporting.value = true
  try {
    const blob = await useAttendanceApi().exportMonth(selectedId.value, month.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `lich-su-diem-danh-${month.value}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    await dialog.alert(extractErrorMessage(e, 'Xuất file thất bại'), { title: 'Xuất file thất bại' })
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  await loadClassrooms()
  await load()
})

watch([() => selectedId.value, month], load)
</script>
