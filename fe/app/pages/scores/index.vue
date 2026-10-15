<template>
  <div class="space-y-4">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div class="flex flex-wrap gap-2">
        <NuxtLink to="/scores/history" class="btn-secondary">Lịch sử điểm</NuxtLink>
        <NuxtLink to="/scores/leaderboard" class="btn-secondary">Bảng xếp hạng</NuxtLink>
      </div>
      <button type="button" class="btn-primary" :disabled="!selectedId" @click="openCreate = true">
        + Tạo mới
      </button>
    </div>

    <CommonLoadingState v-if="loading" />
    <div v-else class="grid items-start gap-4 lg:grid-cols-[minmax(0,1fr)_20rem]">
      <section class="card overflow-hidden">
        <div class="flex gap-1 px-4 pt-4">
          <button
            v-for="tab in periodTabs"
            :key="tab.id"
            type="button"
            class="rounded-full px-3 py-1.5 text-sm font-medium transition"
            :class="period === tab.id ? 'bg-primary-100 text-primary-800' : 'text-slate-500 hover:bg-slate-50'"
            @click="period = tab.id"
          >
            {{ tab.label }}
          </button>
        </div>

        <ul v-if="rows.length" class="mt-2 divide-y divide-slate-100">
          <li v-for="(row, index) in rows" :key="row.studentId">
            <button
              type="button"
              class="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-slate-50"
              :class="selectedStudentId === row.studentId ? 'bg-primary-50' : ''"
              @click="selectStudent(row.studentId)"
            >
              <span class="w-5 shrink-0 text-sm font-semibold text-slate-400">{{ index + 1 }}</span>
              <span
                class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-xs font-bold"
                :class="avatarTone(row.studentName)"
              >
                {{ initials(row.studentName) }}
              </span>
              <span class="min-w-0 flex-1">
                <span class="block truncate font-semibold text-slate-800">{{ row.studentName }}</span>
                <span class="block text-xs text-primary-600">{{ periodCaption }}</span>
              </span>
              <span class="shrink-0 text-right">
                <span class="block text-xl font-bold leading-none text-slate-800">{{ row.totalPoints }}</span>
                <span class="text-[11px] text-slate-400">điểm</span>
              </span>
            </button>
          </li>
        </ul>
        <p v-else class="px-4 py-10 text-center text-sm text-slate-500">Lớp chưa có học sinh.</p>
      </section>

      <section class="card space-y-3 p-5">
        <div>
          <h2 class="font-display text-lg font-bold text-slate-800">Cộng hoặc trừ điểm</h2>
          <p class="mt-1 text-sm text-slate-500">
            {{
              selectedStudent
                ? `Đang chọn ${selectedStudent.studentName}. Bấm một ô điểm để ghi nhận.`
                : 'Bấm tên học sinh, rồi chọn một ô điểm.'
            }}
          </p>
        </div>

        <p v-if="notice" class="rounded-xl bg-primary-50 px-3 py-2 text-sm text-primary-800">{{ notice }}</p>
        <p v-if="noticeError" class="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ noticeError }}</p>

        <div v-if="pointBoxes.length" class="space-y-2">
          <button
            v-for="rule in pointBoxes"
            :key="rule.id"
            type="button"
            class="flex w-full items-center gap-2 rounded-xl border px-3 py-2.5 text-left text-sm font-medium transition disabled:opacity-60"
            :class="boxClass(rule)"
            :disabled="applyingRuleId != null"
            @click="applyRule(rule)"
          >
            <span class="font-bold">{{ formatPoints(rule.pointValue) }}</span>
            <span>{{ rule.name }}</span>
          </button>
        </div>
        <p v-else class="rounded-xl bg-slate-50 px-3 py-4 text-sm text-slate-500">
          Chưa có mức điểm. Bấm «Tạo mới» để thêm ô cộng hoặc trừ điểm.
        </p>
      </section>
    </div>

    <CommonAppModal :open="openCreate" max-width="sm" close-on-backdrop close-on-escape @close="openCreate = false">
      <form class="space-y-3" @submit.prevent="createRule">
        <h3 class="font-display text-lg font-bold text-slate-800">Tạo mức điểm</h3>
        <p class="text-sm text-slate-500">Mức này sẽ hiện thành một ô điểm bên cạnh bảng thi đua.</p>
        <div>
          <label class="label">Tên mức điểm *</label>
          <input v-model="ruleName" class="input" required maxlength="120" placeholder="Ví dụ: Hoàn thành bài tập tốt" />
        </div>
        <div>
          <label class="label">Điểm *</label>
          <input v-model.number="rulePoints" class="input" type="number" step="1" required placeholder="Ví dụ: 2 hoặc -1" />
          <p class="mt-1 text-xs text-slate-400">Số dương để cộng, số âm để trừ.</p>
        </div>
        <p v-if="ruleError" class="text-sm text-rose-600">{{ ruleError }}</p>
        <div class="flex justify-end gap-2 pt-1">
          <button type="button" class="btn-secondary" @click="openCreate = false">Huỷ</button>
          <button type="submit" class="btn-primary" :disabled="creating">
            {{ creating ? 'Đang lưu…' : 'Tạo ô điểm' }}
          </button>
        </div>
      </form>
    </CommonAppModal>
  </div>
</template>

<script setup lang="ts">
import type { AcademicYear, ScoreRule, Student } from '~/types/api'
import { useAcademicApi, useClassroomApi, useScoreApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth' })

type Period = 'week' | 'month' | 'term'

interface RankedStudent {
  studentId: number
  studentName: string
  totalPoints: number
}

const periodTabs: { id: Period; label: string }[] = [
  { id: 'week', label: 'Tuần này' },
  { id: 'month', label: 'Tháng này' },
  { id: 'term', label: 'Học kỳ' },
]

const avatarTones = [
  'bg-rose-100 text-rose-700',
  'bg-slate-200 text-slate-600',
  'bg-emerald-100 text-emerald-700',
  'bg-green-100 text-green-700',
  'bg-amber-100 text-amber-800',
  'bg-sky-100 text-sky-700',
  'bg-violet-100 text-violet-700',
]

const { selectedId, selected, loadClassrooms } = useClassroomContext()

const loading = ref(true)
const period = ref<Period>('week')
const students = ref<Student[]>([])
const rules = ref<ScoreRule[]>([])
const years = ref<AcademicYear[]>([])
const rows = ref<RankedStudent[]>([])
const selectedStudentId = ref<number | null>(null)
const applyingRuleId = ref<number | null>(null)
const notice = ref<string | null>(null)
const noticeError = ref<string | null>(null)

const openCreate = ref(false)
const creating = ref(false)
const ruleName = ref('')
const rulePoints = ref<number | null>(2)
const ruleError = ref<string | null>(null)

const selectedStudent = computed(
  () => rows.value.find((row) => row.studentId === selectedStudentId.value) ?? null,
)

const pointBoxes = computed(() =>
  rules.value
    .filter((rule) => rule.active)
    .slice()
    .sort((a, b) => b.pointValue - a.pointValue || a.name.localeCompare(b.name, 'vi')),
)

const academicYear = computed(
  () => years.value.find((year) => year.id === selected.value?.academicYearId) ?? null,
)

const periodCaption = computed(() => {
  if (period.value === 'week') return 'Điểm trong tuần này'
  if (period.value === 'month') return 'Điểm trong tháng này'
  return 'Điểm trong học kỳ'
})

function isoDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function periodRange(kind: Period): { from: string; to: string } {
  const now = new Date()
  if (kind === 'week') {
    const start = new Date(now)
    const weekday = start.getDay() || 7
    start.setDate(start.getDate() - weekday + 1)
    const end = new Date(start)
    end.setDate(start.getDate() + 6)
    return { from: isoDate(start), to: isoDate(end) }
  }
  if (kind === 'month') {
    const start = new Date(now.getFullYear(), now.getMonth(), 1)
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
    return { from: isoDate(start), to: isoDate(end) }
  }
  const year = academicYear.value
  if (year?.startDate && year.endDate) {
    return { from: year.startDate.slice(0, 10), to: year.endDate.slice(0, 10) }
  }
  const start = new Date(now.getFullYear(), 0, 1)
  const end = new Date(now.getFullYear(), 11, 31)
  return { from: isoDate(start), to: isoDate(end) }
}

function initials(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toLocaleUpperCase('vi')
  return (parts[parts.length - 2][0] + parts[parts.length - 1][0]).toLocaleUpperCase('vi')
}

function avatarTone(name: string): string {
  let hash = 0
  for (const ch of name) hash = (hash + ch.charCodeAt(0)) % avatarTones.length
  return avatarTones[hash]
}

function formatPoints(value: number): string {
  return value > 0 ? `+${value}` : String(value)
}

function boxClass(rule: ScoreRule): string {
  const positive = rule.pointValue >= 0
  const base = positive
    ? 'border-emerald-200 bg-emerald-50 text-emerald-800 hover:border-emerald-500'
    : 'border-rose-200 bg-rose-50 text-rose-800 hover:border-rose-400'
  return applyingRuleId.value === rule.id ? `${base} ring-2 ring-slate-800` : base
}

function selectStudent(studentId: number) {
  selectedStudentId.value = studentId
  noticeError.value = null
}

async function loadBoard() {
  if (!selectedId.value) {
    rows.value = []
    return
  }
  const range = periodRange(period.value)
  const board = await useScoreApi().leaderboard(selectedId.value, range)
  const points = new Map(board.map((entry) => [entry.studentId, entry.totalPoints]))
  rows.value = students.value
    .map((student) => ({
      studentId: student.id,
      studentName: student.fullName,
      totalPoints: points.get(student.id) ?? 0,
    }))
    .sort(
      (a, b) =>
        b.totalPoints - a.totalPoints || a.studentName.localeCompare(b.studentName, 'vi'),
    )
  if (
    selectedStudentId.value != null &&
    !rows.value.some((row) => row.studentId === selectedStudentId.value)
  ) {
    selectedStudentId.value = null
  }
}

async function reload() {
  if (!selectedId.value) {
    students.value = []
    rules.value = []
    rows.value = []
    loading.value = false
    return
  }
  loading.value = true
  notice.value = null
  noticeError.value = null
  try {
    const cid = selectedId.value
    const [roster, ruleList, yearList] = await Promise.all([
      useClassroomApi().students(cid),
      useScoreApi().rules(cid),
      years.value.length ? Promise.resolve(years.value) : useAcademicApi().years(),
    ])
    students.value = roster
    rules.value = ruleList
    years.value = yearList
    await loadBoard()
  } catch (e) {
    noticeError.value = extractErrorMessage(e, 'Không tải được bảng điểm')
  } finally {
    loading.value = false
  }
}

async function applyRule(rule: ScoreRule) {
  if (!selectedId.value || applyingRuleId.value != null) return
  const student = selectedStudent.value
  if (!student) {
    notice.value = null
    noticeError.value = 'Hãy bấm tên học sinh trước, rồi chọn ô điểm.'
    return
  }
  applyingRuleId.value = rule.id
  notice.value = null
  noticeError.value = null
  try {
    await useScoreApi().createTransaction(selectedId.value, {
      studentId: student.studentId,
      scoreRuleId: rule.id,
      reason: rule.name,
    })
    await loadBoard()
    notice.value = `Đã ghi ${formatPoints(rule.pointValue)} cho ${student.studentName}.`
  } catch (e) {
    noticeError.value = extractErrorMessage(e, 'Ghi điểm thất bại')
  } finally {
    applyingRuleId.value = null
  }
}

async function createRule() {
  if (!selectedId.value) return
  ruleError.value = null
  const name = ruleName.value.trim()
  const points = rulePoints.value
  if (name.length < 2) {
    ruleError.value = 'Tên mức điểm cần ít nhất 2 ký tự.'
    return
  }
  if (points == null || !Number.isInteger(points) || points === 0) {
    ruleError.value = 'Nhập số điểm nguyên khác 0. Ví dụ: 2 hoặc -1.'
    return
  }
  creating.value = true
  try {
    await useScoreApi().createRule(selectedId.value, {
      name,
      pointValue: points,
      active: true,
    })
    rules.value = await useScoreApi().rules(selectedId.value)
    ruleName.value = ''
    rulePoints.value = 2
    openCreate.value = false
    noticeError.value = null
    notice.value = `Đã thêm ô điểm ${formatPoints(points)} ${name}.`
  } catch (e) {
    ruleError.value = extractErrorMessage(e, 'Không tạo được mức điểm')
  } finally {
    creating.value = false
  }
}

const ready = ref(false)

onMounted(async () => {
  await loadClassrooms()
  ready.value = true
  await reload()
})

watch(() => selectedId.value, () => {
  if (!ready.value) return
  selectedStudentId.value = null
  reload()
})

watch(period, () => {
  notice.value = null
  loadBoard().catch((e) => {
    noticeError.value = extractErrorMessage(e, 'Không tải được bảng điểm')
  })
})
</script>
