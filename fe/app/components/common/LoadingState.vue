<template>
  <div class="flex flex-col items-center justify-center gap-3 py-16 text-slate-500">
    <div
      class="h-10 w-10 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600"
    />
    <p class="text-sm font-medium">{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ message?: string }>(), {
  message: 'Đang tải dữ liệu…',
})
</script>
