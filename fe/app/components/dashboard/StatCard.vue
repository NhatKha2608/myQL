<template>
  <div class="card p-5">
    <div class="flex items-start justify-between gap-3">
      <div>
        <p class="text-sm font-medium text-slate-500">{{ label }}</p>
        <p class="mt-1 font-display text-3xl font-extrabold text-slate-800">{{ value }}</p>
        <p v-if="hint" class="mt-1 text-xs text-slate-400">{{ hint }}</p>
      </div>
      <div
        class="flex h-11 w-11 items-center justify-center rounded-xl text-lg"
        :class="toneClass"
      >
        {{ icon }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    value: string | number
    hint?: string
    icon?: string
    tone?: 'blue' | 'green' | 'sun' | 'rose'
  }>(),
  { icon: '●', tone: 'blue' },
)

const toneClass = computed(() => {
  const map = {
    blue: 'bg-primary-100 text-primary-700',
    green: 'bg-emerald-50 text-accent',
    sun: 'bg-sun-50 text-amber-600',
    rose: 'bg-rose-50 text-rose-600',
  }
  return map[props.tone]
})
</script>
