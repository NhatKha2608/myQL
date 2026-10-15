<template>
  <div class="space-y-4">
    <div class="card flex flex-wrap items-end gap-4 p-4">
      <div>
        <label class="label">Ngày điểm danh</label>
        <input v-model="date" type="date" class="input !w-auto" />
      </div>
      <NuxtLink to="/attendance/history" class="btn-secondary">Lịch sử</NuxtLink>
      <button type="button" class="btn-mint" :disabled="!rows.length" @click="markAllPresent">
        Tất cả có mặt
      </button>
      <button
        type="button"
        class="btn-primary"
        :disabled="!canSave || saving"
        @click="save"
      >
        {{ saving ? 'Đang lưu…' : 'Lưu điểm danh' }}
      </button>
    </div>

    <p v-if="dirty" class="text-sm font-medium text-sun-700">
      Bạn có thay đổi chưa lưu. Vui lòng nhấn «Lưu điểm danh» trước khi rời trang.
    </p>

    <CommonLoadingState v-if="loading" />
    <CommonErrorState v-else-if="error" :message="error" @retry="loadDaily" />
    <template v-else>
      <AttendanceCounts :counts="statusCounts" />
      <AttendanceTable
        v-if="rows.length"
        :rows="rows"
        @update:status="(studentId, status) => updateRow(studentId, { status })"
        @update:note="(studentId, note) => updateRow(studentId, { note })"
        @apply-status="applyStatus"
      />
      <CommonEmptyState v-else message="Không có học sinh trong lớp" />
    </template>

    <p v-if="saveError" class="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ saveError }}</p>
    <p v-if="saveOk" class="rounded-xl bg-mint-50 px-3 py-2 text-sm text-mint-800">Đã lưu điểm danh thành công.</p>
  </div>
</template>

<script setup lang="ts">
import type { AttendanceRecord, AttendanceStatus } from '~/types/api'
import { useAttendanceApi, useClassroomApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'
import { todayISO } from '~/utils/date'

definePageMeta({ middleware: 'auth' })

const { selectedId, loadClassrooms } = useClassroomContext()
const date = ref(todayISO())
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const saveError = ref<string | null>(null)
const saveOk = ref(false)
const rows = ref<AttendanceRecord[]>([])
const savedSnapshot = ref('')

const dirty = computed(() => JSON.stringify(rows.value) !== savedSnapshot.value)
const canSave = computed(() => !!selectedId.value && rows.value.length > 0 && dirty.value)

const statusCounts = computed(() => {
  const c: Record<AttendanceStatus, number> = {
    PRESENT: 0,
    LATE: 0,
    ABSENT_EXCUSED: 0,
    ABSENT_UNEXCUSED: 0,
  }
  for (const r of rows.value) c[r.status]++
  return c
})

function updateRow(studentId: number, patch: Partial<AttendanceRecord>) {
  rows.value = rows.value.map((r) =>
    r.studentId === studentId ? { ...r, ...patch } : r,
  )
  saveOk.value = false
}

function markAllPresent() {
  rows.value = rows.value.map((r) => ({ ...r, status: 'PRESENT' as AttendanceStatus, note: '' }))
}

async function withBirthDates(classroomId: number, list: AttendanceRecord[]) {
  if (list.every((r) => r.dateOfBirth)) return list
  try {
    const students = await useClassroomApi().students(classroomId)
    const byId = new Map(students.map((s) => [s.id, s.dateOfBirth]))
    return list.map((r) => ({ ...r, dateOfBirth: r.dateOfBirth || byId.get(r.studentId) }))
  } catch {
    return list
  }
}

function applyStatus(payload: { status: AttendanceStatus; studentIds: number[] }) {
  const ids = new Set(payload.studentIds)
  rows.value = rows.value.map((r) => (ids.has(r.studentId) ? { ...r, status: payload.status } : r))
  saveOk.value = false
}

async function loadDaily() {
  if (!selectedId.value) return
  loading.value = true
  error.value = null
  saveOk.value = false
  try {
    const list = await useAttendanceApi().daily(selectedId.value, date.value)
    rows.value = await withBirthDates(selectedId.value, list.map((r) => ({ ...r })))
    savedSnapshot.value = JSON.stringify(rows.value)
  } catch (e) {
    error.value = extractErrorMessage(e, 'Không tải được điểm danh')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!selectedId.value || saving.value) return
  saving.value = true
  saveError.value = null
  saveOk.value = false
  try {
    const saved = await useAttendanceApi().saveDaily(selectedId.value, {
      date: date.value,
      items: rows.value.map((r) => ({
        studentId: r.studentId,
        status: r.status,
        note: r.note,
      })),
    })
    rows.value = await withBirthDates(selectedId.value, saved)
    savedSnapshot.value = JSON.stringify(rows.value)
    saveOk.value = true
  } catch (e) {
    saveError.value = extractErrorMessage(e, 'Lưu điểm danh thất bại')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadClassrooms()
  await loadDaily()
})

watch([() => selectedId.value, date], loadDaily)

const dialog = useAppDialog()

onBeforeRouteLeave((_to, _from, next) => {
  if (!dirty.value || !import.meta.client) {
    next()
    return
  }
  dialog
    .confirm('Bạn có thay đổi điểm danh chưa lưu. Rời trang?', {
      title: 'Rời trang?',
      confirmLabel: 'Rời trang',
      cancelLabel: 'Ở lại',
    })
    .then((ok) => (ok ? next() : next(false)))
})
</script>
