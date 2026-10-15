<template>
  <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
    <div
      v-for="item in items"
      :key="item.key"
      class="rounded-2xl border border-card bg-white px-4 py-3 shadow-card"
    >
      <p class="text-xs font-medium text-slate-500">{{ item.label }}</p>
      <p class="font-display text-2xl font-extrabold" :class="item.color">{{ item.count }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AttendanceStatus } from '~/types/api'

const props = defineProps<{
  counts: Record<AttendanceStatus, number>
}>()

const items = computed(() => [
  { key: 'p', label: 'Có mặt', count: props.counts.PRESENT, color: 'text-mint-600' },
  { key: 'l', label: 'Muộn', count: props.counts.LATE, color: 'text-primary-600' },
  { key: 'e', label: 'Có phép', count: props.counts.ABSENT_EXCUSED, color: 'text-amber-600' },
  { key: 'u', label: 'Không phép', count: props.counts.ABSENT_UNEXCUSED, color: 'text-rose-600' },
])
</script>
