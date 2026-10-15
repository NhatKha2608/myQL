<template>
  <div class="space-y-4">
    <div class="flex justify-end">
      <NuxtLink to="/scores" class="btn-secondary">Quay lại bảng điểm</NuxtLink>
    </div>
    <CommonLoadingState v-if="loading" />
    <div v-else class="card overflow-hidden">
      <table class="data-table">
        <thead>
          <tr>
            <th class="px-4 py-3 text-left">Hạng</th>
            <th class="px-4 py-3 text-left">Học sinh</th>
            <th class="px-4 py-3 text-right">Điểm</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in board" :key="e.studentId">
            <td class="px-4 py-3 font-bold text-sun-700">{{ e.rank }}</td>
            <td class="px-4 py-3">{{ e.studentName }}</td>
            <td class="px-4 py-3 text-right font-semibold">{{ e.totalPoints }}</td>
          </tr>
        </tbody>
      </table>
      <CommonEmptyState v-if="!board.length" message="Chưa có dữ liệu xếp hạng" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LeaderboardEntry } from '~/types/api'
import { useScoreApi } from '~/services/api'

definePageMeta({ middleware: 'auth' })

const { selectedId, loadClassrooms } = useClassroomContext()
const loading = ref(false)
const board = ref<LeaderboardEntry[]>([])

async function load() {
  if (!selectedId.value) return
  loading.value = true
  try {
    board.value = await useScoreApi().leaderboard(selectedId.value)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadClassrooms()
  await load()
})
watch(() => selectedId.value, load)
</script>
