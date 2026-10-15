<template>
  <header
    class="sticky top-0 z-20 flex h-16 shrink-0 items-center justify-between gap-4 bg-sidebar px-4 text-white shell-divider-y md:px-6"
  >
    <div class="flex items-center gap-3">
      <button
        type="button"
        class="btn-on-dark !px-2.5 !py-2 lg:hidden"
        aria-label="Mở menu"
        @click="$emit('toggle-sidebar')"
      >
        ☰
      </button>
      <div class="border-l border-white/15 pl-3 lg:pl-0 lg:border-l-0">
        <h1 class="font-display text-lg font-bold text-white md:text-xl">{{ title }}</h1>
        <p v-if="subtitle" class="hidden text-xs text-emerald-200/80 sm:block">{{ subtitle }}</p>
      </div>
    </div>
    <div class="flex items-center gap-3">
      <slot />
      <div
        class="hidden items-center gap-2 rounded-xl border border-white/25 bg-white/10 px-3 py-1.5 text-sm text-emerald-50 sm:flex"
      >
        <span class="h-2 w-2 rounded-full border border-white/30 bg-accent" />
        {{ todayLabel }}
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
defineEmits<{ 'toggle-sidebar': [] }>()

const todayLabel = new Date().toLocaleDateString('vi-VN', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})
</script>
