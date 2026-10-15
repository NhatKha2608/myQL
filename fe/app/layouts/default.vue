<template>
  <div class="flex min-h-screen bg-canvas">
    <!-- Desktop sidebar -->
    <div class="hidden lg:block">
      <LayoutAppSidebar class="fixed inset-y-0 left-0 z-30" />
    </div>

    <!-- Mobile drawer -->
    <Teleport to="body">
      <div v-if="mobileOpen" class="fixed inset-0 z-40 lg:hidden">
        <div class="absolute inset-0 bg-slate-900/40" @click="mobileOpen = false" />
        <LayoutAppSidebar class="absolute inset-y-0 left-0 z-50 shadow-card" />
      </div>
    </Teleport>

    <div class="flex min-h-screen flex-1 flex-col lg:pl-64">
      <LayoutAppHeader
        :title="pageTitle"
        :subtitle="pageSubtitle"
        @toggle-sidebar="mobileOpen = !mobileOpen"
      />
      <main class="flex-1 bg-canvas p-4 md:p-6">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const auth = useAuth()
const mobileOpen = ref(false)
const { selectedId, pendingPath, loadClassrooms } = useClassroomContext()

watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
  },
)

function hasSelectedClass() {
  const value = Number(selectedId.value)
  return Number.isFinite(value) && value > 0
}

function allowsMissingClass(path: string) {
  return path === '/classrooms' || path.startsWith('/classrooms/') || path === '/settings'
}

watch(
  [selectedId, () => route.path],
  () => {
    if (!auth.accessToken) return
    if (hasSelectedClass()) return
    if (allowsMissingClass(route.path)) return
    if (!pendingPath.value) pendingPath.value = route.fullPath
    navigateTo('/classrooms')
  },
  { immediate: true },
)

onMounted(() => {
  loadClassrooms()
})

const titles: Record<string, { title: string; subtitle?: string }> = {
  '/dashboard': { title: 'Tổng quan', subtitle: 'Bức tranh của lớp đang chọn' },
  '/classrooms': { title: 'Chọn lớp', subtitle: 'Chọn một lớp để xem dữ liệu, hoặc thêm lớp mới' },
  '/students': { title: 'Học sinh', subtitle: 'Học sinh của lớp đang chọn' },
  '/attendance': { title: 'Điểm danh', subtitle: 'Điểm danh theo ngày' },
  '/attendance/history': { title: 'Lịch sử điểm danh' },
  '/scores': { title: 'Điểm thi đua', subtitle: 'Ghi nhận những nỗ lực nhỏ mỗi ngày của các em.' },
  '/scores/history': { title: 'Lịch sử điểm' },
  '/scores/leaderboard': { title: 'Bảng xếp hạng' },
  '/timetable': { title: 'Lịch học', subtitle: '5 tiết mỗi buổi, sáng và chiều' },
  '/reports': { title: 'Báo cáo thống kê', subtitle: 'Chuyên cần và điểm thi đua theo khoảng thời gian' },
  '/settings': { title: 'Cài đặt tài khoản' },
}

const pageTitle = computed(() => {
  const path = route.path
  if (titles[path]) return titles[path].title
  if (path.startsWith('/students')) return 'Học sinh'
  if (path.startsWith('/classrooms')) return 'Chi tiết lớp'
  return 'QLHS'
})

const pageSubtitle = computed(() => titles[route.path]?.subtitle)
</script>
