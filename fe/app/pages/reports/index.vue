<template>
  <div class="space-y-4">
    <div class="card flex flex-wrap items-end gap-3 p-4">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="preset in presets"
          :key="preset.id"
          type="button"
          class="rounded-full px-3 py-1.5 text-sm font-medium transition"
          :class="activePreset === preset.id ? 'bg-primary-100 text-primary-800' : 'text-slate-500 hover:bg-slate-50'"
          @click="applyPreset(preset.id)"
        >
          {{ preset.label }}
        </button>
      </div>
      <div>
        <label class="label" for="report-from">Từ ngày</label>
        <input id="report-from" v-model="from" type="date" class="input !w-auto" />
      </div>
      <div>
        <label class="label" for="report-to">Đến ngày</label>
        <input id="report-to" v-model="to" type="date" class="input !w-auto" />
      </div>
      <div class="ml-auto flex flex-wrap gap-2">
        <button type="button" class="btn-secondary" :disabled="!canExport" @click="exportFile('attendance')">
          {{ exporting === 'attendance' ? 'Đang xuất…' : 'Xuất Excel điểm danh' }}
        </button>
        <button type="button" class="btn-secondary" :disabled="!canExport" @click="exportFile('scores')">
          {{ exporting === 'scores' ? 'Đang xuất…' : 'Xuất Excel thi đua' }}
        </button>
      </div>
    </div>

    <p v-if="rangeError" class="text-sm text-rose-600">{{ rangeError }}</p>

    <CommonLoadingState v-if="loading" />
    <CommonErrorState v-else-if="error" :message="error" @retry="load" />
    <template v-else-if="attendance">
      <p class="text-sm text-slate-500">
        Số liệu từ {{ formatDateVN(String(attendance.from || from)) }} đến {{ formatDateVN(String(attendance.to || to)) }}
      </p>

      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <DashboardStatCard label="Sĩ số" :value="studentCount" hint="Học sinh đang học" tone="blue" />
        <DashboardStatCard
          label="Tỷ lệ có mặt"
          :value="`${formatNumber(attendance.attendanceRate)}%`"
          hint="Có mặt và đi muộn"
          tone="green"
        />
        <DashboardStatCard label="Lượt điểm danh" :value="attendance.totalRecords" tone="sun" />
        <DashboardStatCard
          label="Điểm thi đua trung bình"
          :value="formatNumber(averagePoints)"
          :hint="leaderboard.length ? `${leaderboard.length} học sinh có điểm` : 'Chưa có điểm trong khoảng này'"
          tone="rose"
        />
      </div>

      <div class="grid gap-4 lg:grid-cols-2">
        <div class="card p-5">
          <h3 class="mb-1 font-display text-lg font-bold text-slate-800">Chuyên cần</h3>
          <p class="mb-4 text-sm text-slate-500">Phân bố trạng thái điểm danh trong khoảng đã chọn.</p>
          <DashboardSimpleDoughnutChart
            v-if="chartHasData"
            :labels="chart.labels"
            :values="chart.values"
            :colors="chart.colors"
          />
          <CommonEmptyState v-else message="Chưa có dữ liệu điểm danh trong khoảng này" />
          <ul class="mt-4 divide-y divide-slate-100 text-sm">
            <li v-for="row in statusRows" :key="row.key" class="flex items-center justify-between py-2">
              <span class="flex items-center gap-2 text-slate-700">
                <span class="h-2.5 w-2.5 rounded-full" :style="{ backgroundColor: row.color }" />
                {{ row.label }}
              </span>
              <span class="font-semibold text-slate-800">
                {{ row.count }}
                <span class="font-normal text-slate-400">· {{ formatNumber(row.rate) }}%</span>
              </span>
            </li>
          </ul>
        </div>

        <div class="card overflow-hidden">
          <div class="px-5 pt-5">
            <h3 class="font-display text-lg font-bold text-slate-800">Bảng xếp hạng thi đua</h3>
            <p class="mt-1 text-sm text-slate-500">Xếp theo tổng điểm trong khoảng đã chọn.</p>
          </div>
          <table v-if="leaderboard.length" class="data-table mt-3">
            <thead>
              <tr>
                <th class="w-16 px-4 py-3 text-left">Hạng</th>
                <th class="px-4 py-3 text-left">Học sinh</th>
                <th class="px-4 py-3 text-right">Điểm</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="entry in leaderboard" :key="entry.studentId">
                <td class="px-4 py-3 font-bold text-sun-700">{{ entry.rank }}</td>
                <td class="px-4 py-3">
                  <p class="font-medium text-slate-800">{{ entry.studentName || '—' }}</p>
                  <p v-if="entry.studentCode" class="text-xs text-slate-400">{{ entry.studentCode }}</p>
                </td>
                <td class="px-4 py-3 text-right font-semibold">{{ entry.totalPoints }}</td>
              </tr>
            </tbody>
          </table>
          <CommonEmptyState v-else message="Chưa có điểm thi đua trong khoảng này" />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { LeaderboardEntry } from '~/types/api'
import { useReportApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'
import { daysFromTodayISO, formatDateVN, todayISO } from '~/utils/date'
import { validateDateRange } from '~/utils/dateRange'

definePageMeta({ middleware: 'auth' })

type PresetId = '7' | '30' | 'month'
type ExportType = 'attendance' | 'scores'

interface AttendanceReport {
  from?: string
  to?: string
  byStatus?: Record<string, number>
  totalRecords: number
  attendanceRate: number
}

const STATUS_ROWS = [
  { key: 'PRESENT', label: 'Có mặt', color: '#10B981' },
  { key: 'LATE', label: 'Đi muộn', color: '#0284c7' },
  { key: 'ABSENT_EXCUSED', label: 'Vắng có phép', color: '#fbbf24' },
  { key: 'ABSENT_UNEXCUSED', label: 'Vắng không phép', color: '#f43f5e' },
] as const

const presets: { id: PresetId; label: string }[] = [
  { id: '7', label: '7 ngày' },
  { id: '30', label: '30 ngày' },
  { id: 'month', label: 'Tháng này' },
]

const { selectedId, loadClassrooms } = useClassroomContext()
const dialog = useAppDialog()
const loading = ref(false)
const exporting = ref<ExportType | null>(null)
const error = ref<string | null>(null)
const from = ref(daysFromTodayISO(-29))
const to = ref(todayISO())
const activePreset = ref<PresetId | null>('30')
const attendance = ref<AttendanceReport | null>(null)
const leaderboard = ref<LeaderboardEntry[]>([])
const studentCount = ref(0)

const rangeError = computed(() => {
  const result = validateDateRange(from.value, to.value)
  return result.valid ? null : result.message || 'Khoảng ngày không hợp lệ'
})

const canExport = computed(() => Boolean(selectedId.value) && !rangeError.value && exporting.value == null)

const statusRows = computed(() => {
  const by = attendance.value?.byStatus || {}
  const total = attendance.value?.totalRecords || 0
  return STATUS_ROWS.map((row) => {
    const count = Number(by[row.key] || 0)
    return {
      ...row,
      count,
      rate: total === 0 ? 0 : (count * 100) / total,
    }
  })
})

const chart = computed(() => {
  const rows = statusRows.value.filter((row) => row.count > 0)
  return {
    labels: rows.map((row) => row.label),
    values: rows.map((row) => row.count),
    colors: rows.map((row) => row.color),
  }
})

const chartHasData = computed(() => chart.value.values.some((value) => value > 0))

const averagePoints = computed(() => {
  if (!leaderboard.value.length) return 0
  const sum = leaderboard.value.reduce((total, entry) => total + Number(entry.totalPoints || 0), 0)
  return sum / leaderboard.value.length
})

function formatNumber(value: number) {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(value || 0)
}

function applyPreset(id: PresetId) {
  const today = todayISO()
  to.value = today
  if (id === '7') from.value = daysFromTodayISO(-6)
  else if (id === '30') from.value = daysFromTodayISO(-29)
  else from.value = `${today.slice(0, 7)}-01`
  activePreset.value = id
}

function monthStart(iso: string) {
  return `${iso.slice(0, 7)}-01`
}

watch([from, to], () => {
  const today = todayISO()
  if (to.value === today && from.value === daysFromTodayISO(-6)) activePreset.value = '7'
  else if (to.value === today && from.value === daysFromTodayISO(-29)) activePreset.value = '30'
  else if (to.value === today && from.value === monthStart(today)) activePreset.value = 'month'
  else activePreset.value = null
})

async function load() {
  if (!selectedId.value || rangeError.value) return
  loading.value = true
  error.value = null
  try {
    const [attendanceReport, scoresReport, summary] = await Promise.all([
      useReportApi().attendance(selectedId.value, from.value, to.value),
      useReportApi().scores(selectedId.value, from.value, to.value),
      useReportApi().summary(selectedId.value),
    ])
    attendance.value = attendanceReport as unknown as AttendanceReport
    leaderboard.value = ((scoresReport.leaderboard as LeaderboardEntry[]) || []).slice()
    studentCount.value = Number((summary as { studentCount?: number }).studentCount || 0)
  } catch (e) {
    attendance.value = null
    leaderboard.value = []
    error.value = extractErrorMessage(e, 'Không tải được báo cáo')
  } finally {
    loading.value = false
  }
}

async function exportFile(type: ExportType) {
  if (!selectedId.value || rangeError.value || exporting.value) return
  exporting.value = type
  try {
    const blob = await useReportApi().exportExcel(selectedId.value, type, {
      from: from.value,
      to: to.value,
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = type === 'attendance' ? 'bao-cao-diem-danh.xlsx' : 'bao-cao-thi-dua.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    await dialog.alert(extractErrorMessage(e, 'Xuất file thất bại'), { title: 'Xuất file thất bại' })
  } finally {
    exporting.value = null
  }
}

onMounted(async () => {
  await loadClassrooms()
  await load()
})

watch([() => selectedId.value, from, to], load)
</script>
