<template>
  <div class="space-y-6">
    <CommonLoadingState v-if="loading" />
    <CommonErrorState v-else-if="error" :message="error" @retry="load" />
    <template v-else-if="data">
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <DashboardStatCard label="Lớp đang chọn" :value="selected?.name || '—'" tone="blue" />
        <DashboardStatCard label="Sĩ số" :value="data.totalStudents ?? 0" tone="green" />
        <DashboardStatCard
          label="Có mặt hôm nay"
          :value="presentToday"
          tone="sun"
        />
        <DashboardStatCard
          label="Vắng / muộn hôm nay"
          :value="absentToday"
          tone="rose"
        />
      </div>

      <div class="grid gap-6 lg:grid-cols-2">
        <div class="card p-5">
          <h2 class="page-title mb-4 text-lg">Chuyên cần hôm nay</h2>
          <DashboardSimpleDoughnutChart
            v-if="attendanceChart.labels.length"
            :labels="attendanceChart.labels"
            :values="attendanceChart.values"
          />
          <CommonEmptyState v-else message="Chưa có dữ liệu điểm danh hôm nay" />
        </div>
        <div class="card p-5">
          <h2 class="page-title mb-4 text-lg">Top thi đua</h2>
          <ul v-if="topScorers.length" class="space-y-2">
            <li
              v-for="(s, i) in topScorers"
              :key="s.studentId"
              class="flex items-center justify-between rounded-xl bg-primary-50 px-3 py-2 text-sm"
            >
              <span class="font-medium text-slate-800">{{ i + 1 }}. {{ s.studentName }}</span>
              <span class="badge bg-sun-100 text-sun-800">{{ s.totalPoints }} điểm</span>
            </li>
          </ul>
          <CommonEmptyState v-else message="Chưa có điểm thi đua" />
        </div>
      </div>

      <div class="card p-5">
        <h2 class="page-title mb-4 text-lg">Lịch học hôm nay</h2>
        <ul v-if="todayTimetable.length" class="space-y-2 text-sm">
          <li
            v-for="(row, idx) in todayTimetable"
            :key="idx"
            class="list-row"
          >
            <p class="font-semibold text-slate-800">{{ row.classroomName }}</p>
            <p class="text-slate-600">
              {{ periodLabel(row) }} —
              {{ row.entry?.subjectName ?? 'Môn học' }}
              <span v-if="row.entry?.startTime"> ({{ row.entry.startTime }}–{{ row.entry.endTime }})</span>
            </p>
          </li>
        </ul>
        <CommonEmptyState v-else message="Chưa có tiết học hôm nay" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { LeaderboardEntry } from '~/types/api'
import { useDashboardApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth', layout: 'default' })

const { selectedId, selected, loadClassrooms } = useClassroomContext()
const loading = ref(true)
const error = ref<string | null>(null)
const data = ref<Record<string, unknown> | null>(null)
const ready = ref(false)

const attendanceLabels: Record<string, string> = {
  PRESENT: 'Có mặt',
  LATE: 'Đi muộn',
  ABSENT_EXCUSED: 'Vắng có phép',
  ABSENT_UNEXCUSED: 'Vắng không phép',
}

const presentToday = computed(() => {
  const m = data.value?.attendanceToday as Record<string, number> | undefined
  if (!m) return 0
  return (m.PRESENT ?? 0) + (m.LATE ?? 0)
})

const absentToday = computed(() => {
  const m = data.value?.attendanceToday as Record<string, number> | undefined
  if (!m) return 0
  return (m.ABSENT_EXCUSED ?? 0) + (m.ABSENT_UNEXCUSED ?? 0)
})

const attendanceChart = computed(() => {
  const m = (data.value?.attendanceToday as Record<string, number>) || {}
  const labels: string[] = []
  const values: number[] = []
  for (const [k, v] of Object.entries(m)) {
    if (v > 0) {
      labels.push(attendanceLabels[k] || k)
      values.push(v)
    }
  }
  return { labels, values }
})

const topScorers = computed(() => (data.value?.topScorers as LeaderboardEntry[]) || [])
const todayTimetable = computed(() => (data.value?.todayTimetable as Record<string, unknown>[]) || [])

function periodLabel(row: Record<string, unknown>) {
  const entry = row.entry as { periodNumber?: number } | undefined
  const n = entry?.periodNumber ?? (row.periodNumber as number | undefined)
  if (n == null) return 'Tiết'
  if (n > 5) return `Tiết ${n - 5} (chiều)`
  return `Tiết ${n}`
}

async function load() {
  if (!selectedId.value) {
    data.value = null
    loading.value = false
    return
  }
  loading.value = true
  error.value = null
  try {
    data.value = (await useDashboardApi().classroom(Number(selectedId.value))) as unknown as Record<
      string,
      unknown
    >
  } catch (e) {
    error.value = extractErrorMessage(e, 'Không tải được tổng quan')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadClassrooms()
  ready.value = true
  await load()
})

watch(selectedId, () => {
  if (!ready.value) return
  load()
})
</script>
