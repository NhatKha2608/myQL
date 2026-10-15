<template>
  <div class="card overflow-hidden">
    <div class="overflow-x-auto">
      <table class="data-table min-w-[1020px]">
        <thead>
          <tr>
            <th class="w-14 whitespace-nowrap px-2 py-3 text-left font-semibold">STT</th>
            <th class="min-w-[200px] px-3 py-3 text-left font-semibold">Họ tên</th>
            <th class="w-36 whitespace-nowrap px-3 py-3 text-left font-semibold">Ngày sinh</th>
            <th
              v-for="col in statusColumns"
              :key="col.value"
              class="whitespace-nowrap px-3 py-3 text-center font-semibold"
            >
              <span class="inline-flex items-center gap-1">
                {{ col.label }}
                <button
                  type="button"
                  class="rounded p-0.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                  :aria-label="`Điểm danh hàng loạt: ${col.label}`"
                  @click="openMenu(col.value, $event)"
                >
                  <svg class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path
                      fill-rule="evenodd"
                      d="M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z"
                      clip-rule="evenodd"
                    />
                  </svg>
                </button>
              </span>
            </th>
            <th class="min-w-[180px] px-3 py-3 text-left font-semibold">Ghi chú</th>
          </tr>
          <tr>
            <th class="px-2 py-2" />
            <th class="px-3 py-2">
              <div class="relative">
                <svg
                  class="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                  aria-hidden="true"
                >
                  <path
                    d="M2.5 3.5A1.5 1.5 0 014 2h12a1.5 1.5 0 011.2 2.4L13 10.1V15a1 1 0 01-.45.83l-3 2A1 1 0 018 17v-6.9L2.8 4.4A1.5 1.5 0 012.5 3.5z"
                  />
                </svg>
                <input
                  v-model="nameQuery"
                  type="search"
                  class="w-full rounded-md border border-slate-200 bg-white py-1.5 pl-7 pr-2 text-xs font-normal text-slate-700 placeholder:text-slate-400 focus:border-cyan-400 focus:outline-none focus:ring-2 focus:ring-cyan-100"
                  placeholder="Lọc họ tên"
                  aria-label="Lọc theo họ tên"
                />
              </div>
            </th>
            <th class="px-3 py-2">
              <div class="relative">
                <svg
                  class="pointer-events-none absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                  aria-hidden="true"
                >
                  <path
                    d="M2.5 3.5A1.5 1.5 0 014 2h12a1.5 1.5 0 011.2 2.4L13 10.1V15a1 1 0 01-.45.83l-3 2A1 1 0 018 17v-6.9L2.8 4.4A1.5 1.5 0 012.5 3.5z"
                  />
                </svg>
                <input
                  v-model="dobQuery"
                  type="search"
                  class="w-full rounded-md border border-slate-200 bg-white py-1.5 pl-7 pr-2 text-xs font-normal text-slate-700 placeholder:text-slate-400 focus:border-cyan-400 focus:outline-none focus:ring-2 focus:ring-cyan-100"
                  placeholder="dd/mm/yyyy"
                  aria-label="Lọc theo ngày sinh"
                />
              </div>
            </th>
            <th v-for="col in statusColumns" :key="`${col.value}-filter`" class="px-2 py-2" />
            <th class="px-3 py-2" />
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(row, index) in filtered"
            :key="row.studentId"
            :class="index % 2 === 1 ? 'bg-slate-50' : 'bg-white'"
          >
            <td class="px-2 py-2.5 tabular-nums text-slate-500">{{ row.order }}</td>
            <td class="px-3 py-2.5">
              <p class="font-medium text-slate-800">{{ row.studentName || '—' }}</p>
            </td>
            <td class="px-3 py-2.5 tabular-nums text-slate-700">{{ formatDob(row.dateOfBirth) }}</td>
            <td v-for="col in statusColumns" :key="`${row.studentId}-${col.value}`" class="px-2 py-2.5 text-center">
              <button
                type="button"
                class="mx-auto grid h-6 w-6 place-items-center rounded-full border-2 transition"
                :class="row.status === col.value ? col.active : col.idle"
                :aria-pressed="row.status === col.value"
                :aria-label="`${col.label}: ${row.studentName}`"
                @click="$emit('update:status', row.studentId, col.value)"
              >
                <svg
                  v-if="row.status === col.value"
                  class="h-3.5 w-3.5"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                  aria-hidden="true"
                >
                  <path
                    fill-rule="evenodd"
                    d="M16.7 5.3a1 1 0 010 1.4l-7.2 7.2a1 1 0 01-1.4 0L3.3 9.1a1 1 0 111.4-1.4l4.1 4.1 6.5-6.5a1 1 0 011.4 0z"
                    clip-rule="evenodd"
                  />
                </svg>
              </button>
            </td>
            <td class="px-3 py-2.5">
              <input
                class="w-full rounded-md border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-700 placeholder:text-slate-400 focus:border-cyan-400 focus:outline-none focus:ring-2 focus:ring-cyan-100"
                :value="row.note || ''"
                placeholder="Ghi chú"
                :aria-label="`Ghi chú ${row.studentName}`"
                @input="$emit('update:note', row.studentId, ($event.target as HTMLInputElement).value)"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-if="!filtered.length" class="px-4 py-8 text-center text-sm text-slate-500">
      Không có học sinh khớp bộ lọc
    </p>
  </div>

  <Teleport to="body">
    <div v-if="openStatus" class="fixed inset-0 z-40" @click="openStatus = null" />
    <div
      v-if="openStatus"
      class="fixed z-50 w-56 -translate-x-1/2 overflow-hidden rounded-xl border border-slate-200 bg-white py-1 text-sm shadow-lg"
      :style="{ top: `${menuPos.top}px`, left: `${menuPos.left}px` }"
    >
      <button
        type="button"
        class="block w-full px-3 py-2 text-left text-slate-700 hover:bg-slate-50"
        @click="apply(openStatus)"
      >
        Điểm danh tất cả
      </button>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import type { AttendanceRecord, AttendanceStatus } from '~/types/api'

const props = defineProps<{
  rows: AttendanceRecord[]
}>()

const emit = defineEmits<{
  'update:status': [studentId: number, status: AttendanceStatus]
  'update:note': [studentId: number, note: string]
  'apply-status': [payload: { status: AttendanceStatus; studentIds: number[] }]
}>()

const statusColumns: {
  value: AttendanceStatus
  label: string
  active: string
  idle: string
}[] = [
  {
    value: 'PRESENT',
    label: 'Có mặt',
    active: 'border-cyan-400 bg-cyan-400 text-white',
    idle: 'border-cyan-300 bg-white text-transparent hover:bg-cyan-50',
  },
  {
    value: 'LATE',
    label: 'Muộn',
    active: 'border-orange-400 bg-orange-400 text-white',
    idle: 'border-orange-300 bg-white text-transparent hover:bg-orange-50',
  },
  {
    value: 'ABSENT_EXCUSED',
    label: 'Nghỉ có phép',
    active: 'border-amber-400 bg-amber-400 text-white',
    idle: 'border-amber-300 bg-white text-transparent hover:bg-amber-50',
  },
  {
    value: 'ABSENT_UNEXCUSED',
    label: 'Nghỉ không phép',
    active: 'border-rose-300 bg-rose-400 text-white',
    idle: 'border-rose-200 bg-white text-transparent hover:bg-rose-50',
  },
]

const nameQuery = ref('')
const dobQuery = ref('')
const openStatus = ref<AttendanceStatus | null>(null)
const menuPos = ref({ top: 0, left: 0 })

const orderById = computed(() => {
  const map = new Map<number, number>()
  props.rows.forEach((row, index) => map.set(row.studentId, index + 1))
  return map
})

const filtered = computed(() => {
  const name = fold(nameQuery.value)
  const dob = dobQuery.value.trim().toLowerCase()
  return props.rows
    .map((row) => ({ ...row, order: orderById.value.get(row.studentId) ?? 0 }))
    .filter((row) => {
      if (name) {
        const hay = fold(row.studentName || '')
        if (!hay.includes(name)) return false
      }
      if (dob) {
        const shown = formatDob(row.dateOfBirth).toLowerCase()
        const iso = (row.dateOfBirth || '').toLowerCase()
        if (!shown.includes(dob) && !iso.includes(dob)) return false
      }
      return true
    })
})

const rosterKey = computed(() => props.rows.map((row) => row.studentId).join(','))
watch(rosterKey, () => {
  openStatus.value = null
})

function fold(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/gi, 'd')
    .toLowerCase()
    .trim()
}

function formatDob(iso?: string) {
  if (!iso) return '—'
  const [y, m, d] = iso.slice(0, 10).split('-')
  if (!y || !m || !d) return '—'
  return `${d}/${m}/${y}`
}

function openMenu(status: AttendanceStatus, event: MouseEvent) {
  const el = event.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  menuPos.value = { top: rect.bottom + 6, left: rect.left + rect.width / 2 }
  openStatus.value = openStatus.value === status ? null : status
}

function apply(status: AttendanceStatus) {
  const studentIds = props.rows.map((row) => row.studentId)
  if (!studentIds.length) return
  emit('apply-status', { status, studentIds })
  openStatus.value = null
}
</script>
