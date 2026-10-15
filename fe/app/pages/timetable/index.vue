<template>
  <div class="space-y-4">
    <div class="flex flex-wrap items-center justify-end gap-3">
      <div class="flex flex-wrap gap-2">
        <button v-if="!editing" type="button" class="btn-primary" :disabled="!selectedId || loading" @click="startEdit">
          Sửa thời khóa biểu
        </button>
        <template v-else>
          <button type="button" class="btn-primary" :disabled="saving" @click="save">
            {{ saving ? 'Đang lưu…' : 'Lưu' }}
          </button>
          <button type="button" class="btn-secondary" :disabled="saving" @click="cancelEdit">Huỷ</button>
        </template>
      </div>
    </div>

    <div class="card space-y-3 p-4">
      <form class="flex flex-wrap items-end gap-2" @submit.prevent="addSubject">
        <div class="min-w-[14rem] flex-1">
          <label class="label" for="new-subject">Thêm môn học</label>
          <input
            id="new-subject"
            v-model="newSubjectName"
            class="input"
            maxlength="255"
            placeholder="Ví dụ: Toán, Tiếng Việt, Âm nhạc"
          />
        </div>
        <button type="submit" class="btn-primary" :disabled="addingSubject">
          {{ addingSubject ? 'Đang thêm…' : 'Thêm môn' }}
        </button>
      </form>
      <p v-if="subjectError" class="text-sm text-rose-600">{{ subjectError }}</p>
      <div v-if="subjects.length" class="flex flex-wrap gap-2">
        <span
          v-for="subject in subjects"
          :key="subject.id"
          class="badge border-amber-200 bg-sun-100 text-amber-900"
        >
          {{ subject.name }}
        </span>
      </div>
      <p v-else class="text-sm text-slate-500">Chưa có môn học. Thêm môn rồi chọn vào từng tiết.</p>
    </div>

    <p v-if="message" class="text-sm font-medium text-emerald-700">{{ message }}</p>
    <p v-if="formError" class="text-sm text-rose-600">{{ formError }}</p>

    <CommonLoadingState v-if="loading" />
    <CommonEmptyState v-else-if="!selectedId" message="Chưa có lớp để lập thời khóa biểu." />

    <div v-else class="tkb-sheet">
      <div class="tkb-inner">
        <header class="tkb-head">
          <svg class="tkb-books" viewBox="0 0 92 78" aria-hidden="true">
            <rect x="8" y="34" width="62" height="14" rx="2" fill="#5ec8f0" transform="rotate(-8 39 41)" />
            <rect x="10" y="24" width="64" height="14" rx="2" fill="#3b82f6" transform="rotate(-4 42 31)" />
            <rect x="12" y="16" width="64" height="14" rx="2" fill="#86efac" />
            <rect x="14" y="6" width="62" height="14" rx="2" fill="#fde047" />
            <rect x="40" y="6" width="6" height="16" rx="1" fill="#f43f5e" />
            <rect x="18" y="50" width="58" height="12" rx="2" fill="#38bdf8" />
          </svg>
          <div class="tkb-heading">
            <h2 class="tkb-title">Thời khóa biểu</h2>
            <p class="tkb-meta">
              Lớp: <strong>{{ selected?.name || '—' }}</strong>
            </p>
          </div>
        </header>

        <div class="tkb-scroll">
          <table class="tkb-table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th v-for="day in DAYS" :key="day.value">{{ day.label }}</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="session in SESSIONS" :key="session.id">
                <tr :class="session.id === 'morning' ? 'tkb-session-morning' : 'tkb-session-afternoon'">
                  <td colspan="6">{{ session.label }}</td>
                </tr>
                <tr v-for="row in rowsFor(session.id)" :key="row.periodNumber">
                  <th scope="row" class="tkb-time">
                    <span class="tkb-tiet">Tiết {{ row.tiet }}</span>
                    <div v-if="editing" class="tkb-time-edit">
                      <input v-model="row.start" class="tkb-input tkb-clock" type="time" :aria-label="`Giờ bắt đầu tiết ${row.tiet} ${session.label}`" />
                      <span>–</span>
                      <input v-model="row.end" class="tkb-input tkb-clock" type="time" :aria-label="`Giờ kết thúc tiết ${row.tiet} ${session.label}`" />
                    </div>
                    <span v-else class="tkb-clock-text">{{ row.start }}–{{ row.end }}</span>
                  </th>
                  <td v-for="day in DAYS" :key="day.value">
                    <select
                      v-if="editing"
                      v-model="row.subjects[day.value]"
                      class="tkb-input"
                      :aria-label="`${session.label} tiết ${row.tiet} ${day.label}`"
                    >
                      <option value="">—</option>
                      <option
                        v-if="row.subjects[day.value] && !knownSubject(row.subjects[day.value])"
                        :value="row.subjects[day.value]"
                      >
                        {{ row.subjects[day.value] }}
                      </option>
                      <option v-for="subject in subjects" :key="subject.id" :value="subject.name">
                        {{ subject.name }}
                      </option>
                    </select>
                    <span v-else class="tkb-subject">{{ row.subjects[day.value] }}</span>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        <p class="tkb-hint">
          Mỗi buổi 5 tiết. Chọn môn đã thêm. Ô để trống sẽ được bỏ khi lưu.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Subject, TimetableEntry, TimetableSlotInput } from '~/types/api'
import { useAcademicApi, useTimetableApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth' })

const DAYS = [
  { value: 1, label: 'Thứ 2' },
  { value: 2, label: 'Thứ 3' },
  { value: 3, label: 'Thứ 4' },
  { value: 4, label: 'Thứ 5' },
  { value: 5, label: 'Thứ 6' },
] as const

const SESSIONS = [
  { id: 'morning', label: 'Buổi sáng' },
  { id: 'afternoon', label: 'Buổi chiều' },
] as const

const DEFAULT_TIMES: Record<number, { start: string; end: string }> = {
  1: { start: '07:30', end: '08:05' },
  2: { start: '08:15', end: '08:50' },
  3: { start: '09:00', end: '09:35' },
  4: { start: '09:45', end: '10:20' },
  5: { start: '10:30', end: '11:05' },
  6: { start: '13:30', end: '14:05' },
  7: { start: '14:15', end: '14:50' },
  8: { start: '15:00', end: '15:35' },
  9: { start: '15:45', end: '16:20' },
  10: { start: '16:30', end: '17:05' },
}

interface GridRow {
  periodNumber: number
  tiet: number
  session: 'morning' | 'afternoon'
  start: string
  end: string
  subjects: Record<number, string>
}

function blankGrid(): GridRow[] {
  return Array.from({ length: 10 }, (_, index) => {
    const periodNumber = index + 1
    return {
      periodNumber,
      tiet: periodNumber <= 5 ? periodNumber : periodNumber - 5,
      session: periodNumber <= 5 ? 'morning' : 'afternoon',
      start: DEFAULT_TIMES[periodNumber].start,
      end: DEFAULT_TIMES[periodNumber].end,
      subjects: { 1: '', 2: '', 3: '', 4: '', 5: '' },
    }
  })
}

function hm(value?: string | null) {
  if (!value) return ''
  return value.slice(0, 5)
}

function toApiTime(value: string) {
  return value.length === 5 ? `${value}:00` : value
}

const { selectedId, selected, loadClassrooms } = useClassroomContext()
const { confirm } = useAppDialog()

const loading = ref(false)
const saving = ref(false)
const addingSubject = ref(false)
const editing = ref(false)
const message = ref<string | null>(null)
const formError = ref<string | null>(null)
const subjectError = ref<string | null>(null)
const newSubjectName = ref('')
const subjects = ref<Subject[]>([])
const stored = ref<TimetableEntry[]>([])
const rows = ref<GridRow[]>(blankGrid())
let snapshot = ''

function rowsFor(session: 'morning' | 'afternoon') {
  return rows.value.filter((row) => row.session === session)
}

function knownSubject(name: string) {
  return subjects.value.some((subject) => subject.name === name)
}

async function loadSubjects() {
  subjects.value = await useAcademicApi().subjects()
}

async function addSubject() {
  const name = newSubjectName.value.trim()
  subjectError.value = null
  if (!name) {
    subjectError.value = 'Vui lòng nhập tên môn học'
    return
  }
  addingSubject.value = true
  try {
    const created = await useAcademicApi().createSubject({ name })
    if (!subjects.value.some((subject) => subject.id === created.id)) {
      subjects.value = [...subjects.value, created].sort((a, b) =>
        a.name.localeCompare(b.name, 'vi'),
      )
    }
    newSubjectName.value = ''
    message.value = `Đã thêm môn ${created.name}`
  } catch (e) {
    subjectError.value = extractErrorMessage(e, 'Không thêm được môn học')
  } finally {
    addingSubject.value = false
  }
}

function applyEntries(list: TimetableEntry[]) {
  const grid = blankGrid()
  const timed = new Set<number>()
  for (const entry of list) {
    if (entry.dayOfWeek < 1 || entry.dayOfWeek > 5) continue
    if (entry.periodNumber < 1 || entry.periodNumber > 10) continue
    const row = grid[entry.periodNumber - 1]
    row.subjects[entry.dayOfWeek] = entry.subjectName || ''
    if (!timed.has(entry.periodNumber) && entry.startTime && entry.endTime) {
      row.start = hm(entry.startTime)
      row.end = hm(entry.endTime)
      timed.add(entry.periodNumber)
    }
  }
  rows.value = grid
}

function startEdit() {
  snapshot = JSON.stringify(rows.value)
  editing.value = true
  message.value = null
  formError.value = null
}

function cancelEdit() {
  rows.value = JSON.parse(snapshot) as GridRow[]
  editing.value = false
  formError.value = null
}

function sessionLabel(row: GridRow) {
  return row.session === 'morning' ? 'sáng' : 'chiều'
}

async function save() {
  if (!selectedId.value) return
  formError.value = null
  message.value = null

  const entries: TimetableSlotInput[] = []
  for (const row of rows.value) {
    const names = DAYS.map((day) => row.subjects[day.value].trim())
    if (!names.some(Boolean)) continue
    if (!row.start || !row.end) {
      formError.value = `Tiết ${row.tiet} buổi ${sessionLabel(row)} thiếu giờ học`
      return
    }
    if (row.end <= row.start) {
      formError.value = `Giờ kết thúc phải sau giờ bắt đầu (tiết ${row.tiet} buổi ${sessionLabel(row)})`
      return
    }
    DAYS.forEach((day, index) => {
      const name = names[index]
      if (!name) return
      const matched = subjects.value.find(
        (subject) => subject.name.localeCompare(name, 'vi', { sensitivity: 'accent' }) === 0,
      )
      entries.push({
        subjectId: matched?.id,
        subjectName: matched ? undefined : name,
        dayOfWeek: day.value,
        periodNumber: row.periodNumber,
        startTime: toApiTime(row.start),
        endTime: toApiTime(row.end),
      })
    })
  }

  if (!entries.length && stored.value.length) {
    const ok = await confirm('Thời khóa biểu đang trống. Lưu sẽ xóa toàn bộ tiết từ thứ 2 đến thứ 6.', {
      title: 'Xóa thời khóa biểu',
      confirmLabel: 'Xóa và lưu',
      danger: true,
    })
    if (!ok) return
  }

  saving.value = true
  try {
    const saved = await useTimetableApi().save(selectedId.value, entries)
    stored.value = saved
    applyEntries(saved)
    subjects.value = await useAcademicApi().subjects()
    editing.value = false
    message.value = entries.length
      ? `Đã lưu ${entries.length} tiết học`
      : 'Đã xóa thời khóa biểu'
  } catch (e) {
    formError.value = extractErrorMessage(e, 'Lưu thời khóa biểu thất bại')
  } finally {
    saving.value = false
  }
}

async function load() {
  editing.value = false
  message.value = null
  formError.value = null
  if (!selectedId.value) {
    stored.value = []
    rows.value = blankGrid()
    return
  }
  loading.value = true
  try {
    const [list, subjectList] = await Promise.all([
      useTimetableApi().get(selectedId.value),
      useAcademicApi().subjects(),
    ])
    stored.value = list
    subjects.value = subjectList
    applyEntries(list)
  } catch (e) {
    formError.value = extractErrorMessage(e, 'Không tải được thời khóa biểu')
    rows.value = blankGrid()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadClassrooms()
  try {
    await loadSubjects()
  } catch (e) {
    subjectError.value = extractErrorMessage(e, 'Không tải được danh sách môn học')
  }
  await load()
})

watch(() => selectedId.value, load)
</script>

<style scoped>
.tkb-sheet {
  max-width: 52rem;
  margin-inline: auto;
  border-radius: 1rem;
  background: #c5eef8;
  padding: 0.5rem;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
}

.tkb-inner {
  border-radius: 0.75rem;
  background: #fff;
  padding: 0.7rem 0.6rem 0.55rem;
}

.tkb-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  margin-bottom: 0.45rem;
}

.tkb-books {
  width: 2.4rem;
  height: 2rem;
  flex: none;
}

.tkb-heading {
  text-align: center;
}

.tkb-title {
  font-family: Nunito, "Be Vietnam Pro", sans-serif;
  font-size: 1.25rem;
  font-weight: 800;
  line-height: 1.1;
  color: #67c84a;
  -webkit-text-stroke: 0.6px #2d6a22;
  paint-order: stroke fill;
}

.tkb-meta {
  margin-top: 0.05rem;
  font-size: 0.78rem;
  font-weight: 700;
  color: #1e293b;
}

.tkb-meta strong {
  font-weight: 800;
}

.tkb-scroll {
  overflow-x: auto;
}

.tkb-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  text-align: center;
  font-size: 0.78rem;
}

.tkb-table th,
.tkb-table td {
  border: 1px solid #e6c34a;
  padding: 0.2rem 0.25rem;
  vertical-align: middle;
}

.tkb-table thead th {
  background: #ffe14d;
  font-weight: 800;
  color: #1e293b;
  padding: 0.35rem 0.25rem;
}

.tkb-session-morning td,
.tkb-session-afternoon td {
  font-weight: 800;
  letter-spacing: 0.02em;
  padding: 0.22rem;
}

.tkb-session-morning td {
  background: #fff4c2;
  color: #854d0e;
}

.tkb-session-afternoon td {
  background: #d7f4fb;
  color: #0e7490;
}

.tkb-time {
  width: 7.25rem;
  background: #fffdf3;
  font-weight: 600;
}

.tkb-tiet {
  display: block;
  font-size: 0.68rem;
  font-weight: 800;
  color: #a16207;
}

.tkb-clock-text {
  display: block;
  margin-top: 0.1rem;
  font-variant-numeric: tabular-nums;
  color: #334155;
}

.tkb-time-edit {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
  margin-top: 0.15rem;
}

.tkb-subject {
  display: flex;
  min-height: 1.45rem;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  line-height: 1.2;
  color: #0f172a;
}

.tkb-input {
  width: 100%;
  border: 1px solid #f3d36b;
  border-radius: 0.4rem;
  background: #fffef8;
  padding: 0.15rem 0.2rem;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: #0f172a;
}

.tkb-input:focus {
  border-color: #34d399;
  outline: 2px solid #bbf7d0;
}

.tkb-clock {
  width: 4.7rem;
  padding-inline: 0.05rem;
  font-size: 0.7rem;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.tkb-hint {
  margin-top: 0.4rem;
  text-align: center;
  font-size: 0.72rem;
  color: #64748b;
}
</style>
