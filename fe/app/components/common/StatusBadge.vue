<template>
  <span class="badge" :class="cls">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status?: string | null }>()

const map: Record<string, { label: string; cls: string }> = {
  ACTIVE: { label: 'Đang học', cls: 'bg-mint-100 text-mint-600' },
  INACTIVE: { label: 'Ngừng', cls: 'bg-slate-100 text-slate-600' },
  TRANSFERRED: { label: 'Chuyển lớp', cls: 'bg-sun-100 text-sun-600' },
  PRESENT: { label: 'Có mặt', cls: 'bg-mint-100 text-mint-600' },
  ABSENT_EXCUSED: { label: 'Có phép', cls: 'bg-sun-100 text-amber-700' },
  ABSENT_UNEXCUSED: { label: 'Không phép', cls: 'bg-rose-100 text-rose-700' },
  LATE: { label: 'Muộn', cls: 'bg-primary-100 text-primary-700' },
  PUBLISHED: { label: 'Đã đăng', cls: 'bg-mint-100 text-mint-600' },
  DRAFT: { label: 'Nháp', cls: 'bg-slate-100 text-slate-600' },
}

const label = computed(() => map[props.status || '']?.label || props.status || '—')
const cls = computed(() => map[props.status || '']?.cls || 'bg-slate-100 text-slate-600')
</script>
