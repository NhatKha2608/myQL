<template>
  <aside class="flex h-full w-64 flex-col border-r border-slate-200 bg-white text-slate-700">
    <div class="relative p-3" data-class-switcher @mousedown.stop>
      <button
        type="button"
        class="flex w-full items-center gap-3 rounded-2xl border border-slate-200 bg-white px-3 py-2.5 text-left shadow-sm transition hover:border-primary-200 hover:bg-slate-50"
        :aria-expanded="menuOpen"
        @click="menuOpen = !menuOpen"
      >
        <span
          class="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-primary-600 text-xs font-extrabold text-white"
        >
          {{ classroomCode(selected?.name) }}
        </span>
        <span class="min-w-0 flex-1">
          <span class="block truncate text-sm font-bold text-slate-800">
            {{ selected ? classroomTitle(selected.name) : 'Chọn lớp' }}
          </span>
          <span class="block truncate text-xs text-slate-500">
            {{ selected ? academicYearLabel(selected.academicYearName) : 'Thêm hoặc chọn một lớp' }}
          </span>
        </span>
        <svg class="h-4 w-4 shrink-0 text-slate-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
          <path
            fill-rule="evenodd"
            d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.17l3.71-3.94a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z"
            clip-rule="evenodd"
          />
        </svg>
      </button>

      <div
        v-if="menuOpen"
        class="absolute inset-x-3 top-[4.6rem] z-20 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-card"
      >
        <p class="px-3 pt-3 text-[11px] font-semibold uppercase tracking-wider text-slate-400">Các lớp</p>
        <ul class="max-h-64 overflow-y-auto p-2">
          <li v-if="!classrooms.length" class="px-2 py-3 text-sm text-slate-500">Chưa có lớp nào.</li>
          <li v-for="room in classrooms" :key="room.id">
            <button
              type="button"
              class="flex w-full items-center gap-2 rounded-xl px-2 py-2 text-left text-sm transition hover:bg-slate-50"
              :class="room.id === currentId ? 'bg-primary-50 text-primary-800' : 'text-slate-700'"
              @click="choose(room.id)"
            >
              <span
                class="grid h-8 w-8 shrink-0 place-items-center rounded-lg text-[11px] font-bold"
                :class="room.id === currentId ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-600'"
              >
                {{ classroomCode(room.name) }}
              </span>
              <span class="min-w-0 flex-1">
                <span class="block truncate font-semibold">{{ classroomTitle(room.name) }}</span>
                <span class="block truncate text-xs text-slate-500">{{ academicYearLabel(room.academicYearName) }}</span>
              </span>
              <span class="text-xs text-slate-400">{{ room.studentCount }}</span>
            </button>
          </li>
        </ul>
        <div class="space-y-1 border-t border-slate-100 p-2">
          <button
            type="button"
            class="flex w-full items-center gap-2 rounded-xl px-2 py-2 text-sm font-semibold text-primary-700 hover:bg-primary-50"
            @click="openCreateFromMenu"
          >
            + Thêm lớp
          </button>
          <NuxtLink
            to="/classrooms"
            class="block rounded-xl px-2 py-2 text-sm text-slate-600 hover:bg-slate-50"
            @click="menuOpen = false"
          >
            Xem tất cả lớp
          </NuxtLink>
        </div>
      </div>
    </div>

    <p class="px-5 pb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-400">Menu chính</p>

    <nav class="flex-1 space-y-0.5 overflow-y-auto px-3 pb-4">
      <NuxtLink
        v-for="item in items"
        :key="item.to"
        :to="item.to"
        class="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 transition hover:bg-slate-50"
        active-class="!bg-primary-50 !font-semibold !text-primary-700"
      >
        <span class="h-5 w-5 shrink-0" v-html="item.icon" />
        <span class="truncate">{{ item.label }}</span>
      </NuxtLink>
    </nav>

    <div class="border-t border-slate-200 p-4">
      <p class="truncate text-sm font-semibold text-slate-800">{{ auth.displayName }}</p>
      <p class="truncate text-xs text-slate-500">{{ roleLabel }}</p>
      <NuxtLink to="/settings" class="mt-2 block text-xs font-medium text-primary-700 hover:underline">
        Cài đặt
      </NuxtLink>
      <button type="button" class="btn-secondary mt-3 w-full !py-2 text-xs" @click="auth.logout()">
        Đăng xuất
      </button>
    </div>

    <ClassroomsCreateClassroomModal v-model:open="openCreate" @created="onCreated" />
  </aside>
</template>

<script setup lang="ts">
import type { Classroom } from '~/types/api'
import { academicYearLabel, classroomCode, classroomTitle } from '~/utils/classroomLabel'

const auth = useAuth()
const route = useRoute()
const { classrooms, selectedId, selected, loadClassrooms, enterClassroom } = useClassroomContext()

const menuOpen = ref(false)
const openCreate = ref(false)

const currentId = computed(() => {
  const value = Number(selectedId.value)
  return Number.isFinite(value) && value > 0 ? value : null
})

const roleLabel = computed(() => {
  const role = auth.user?.role
  if (role === 'ADMIN') return 'Quản trị'
  if (role === 'TEACHER') return 'Giáo viên'
  if (role === 'PARENT') return 'Phụ huynh'
  return role || '—'
})

const icon =
  (path: string) =>
  `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8" class="h-5 w-5"><path stroke-linecap="round" stroke-linejoin="round" d="${path}"/></svg>`

const items = [
  { to: '/dashboard', label: 'Tổng quan', icon: icon('M3.75 6A2.25 2.25 0 0 1 6 3.75h3.75A2.25 2.25 0 0 1 12 6v3.75A2.25 2.25 0 0 1 9.75 12H6A2.25 2.25 0 0 1 3.75 9.75V6zM3.75 16.5A2.25 2.25 0 0 1 6 14.25h3.75A2.25 2.25 0 0 1 12 16.5V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-1.5zM13.5 6A2.25 2.25 0 0 1 15.75 3.75H18A2.25 2.25 0 0 1 20.25 6v1.5A2.25 2.25 0 0 1 18 9.75h-2.25A2.25 2.25 0 0 1 13.5 7.5V6zM13.5 14.25A2.25 2.25 0 0 1 15.75 12H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-3.75z') },
  { to: '/students', label: 'Học sinh', icon: icon('M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0zM4.5 20.1a7.5 7.5 0 0 1 15 0A17.9 17.9 0 0 1 12 21.75 17.9 17.9 0 0 1 4.5 20.1z') },
  { to: '/attendance', label: 'Điểm danh', icon: icon('M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z') },
  { to: '/scores', label: 'Điểm thi đua', icon: icon('M11.48 3.5a.56.56 0 0 1 1.04 0l2.12 5.11a.56.56 0 0 0 .48.35l5.52.47c.5.04.7.66.32 1l-4.2 3.6a.56.56 0 0 0-.18.56l1.28 5.35a.56.56 0 0 1-.84.61l-4.72-2.88a.56.56 0 0 0-.58 0l-4.72 2.88a.56.56 0 0 1-.84-.61l1.28-5.35a.56.56 0 0 0-.18-.56l-4.2-3.6a.56.56 0 0 1 .32-1l5.52-.47a.56.56 0 0 0 .48-.35L11.48 3.5z') },
  { to: '/timetable', label: 'Lịch học', icon: icon('M6.75 3v2.25M17.25 3v2.25M3.75 8.25h16.5M5.25 5.25h13.5A1.5 1.5 0 0 1 20.25 6.75v11.25a1.5 1.5 0 0 1-1.5 1.5H5.25a1.5 1.5 0 0 1-1.5-1.5V6.75a1.5 1.5 0 0 1 1.5-1.5z') },
  { to: '/reports', label: 'Báo cáo thống kê', icon: icon('M3 13.5 8.25 8.25 12 12l8.25-8.25M14.25 3.75H20.25V9.75') },
]

function choose(id: number) {
  menuOpen.value = false
  enterClassroom(id)
}

function openCreateFromMenu() {
  menuOpen.value = false
  openCreate.value = true
}

async function onCreated(room: Classroom) {
  menuOpen.value = false
  await loadClassrooms()
  await enterClassroom(room.id)
}

watch(
  () => route.path,
  () => {
    menuOpen.value = false
  },
)

function closeMenu(event: Event) {
  const target = event.target as Node | null
  if (!target) {
    menuOpen.value = false
    return
  }
  const roots = document.querySelectorAll('[data-class-switcher]')
  for (const root of roots) {
    if (root.contains(target)) return
  }
  menuOpen.value = false
}

onMounted(() => {
  loadClassrooms()
  document.addEventListener('mousedown', closeMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', closeMenu)
})
</script>
