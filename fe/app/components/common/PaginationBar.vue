<template>
  <div
    v-if="totalPages > 1"
    class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-4 py-3"
  >
    <p class="text-sm text-slate-500">
      Trang {{ page + 1 }} / {{ totalPages }}
      <span class="text-slate-400">· {{ totalElements }} mục</span>
    </p>
    <div class="flex gap-2">
      <button
        type="button"
        class="btn-secondary !py-1.5"
        :disabled="page <= 0"
        @click="$emit('update:page', page - 1)"
      >
        Trước
      </button>
      <button
        type="button"
        class="btn-secondary !py-1.5"
        :disabled="page >= totalPages - 1"
        @click="$emit('update:page', page + 1)"
      >
        Sau
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  page: number
  totalPages: number
  totalElements: number
}>()
defineEmits<{ 'update:page': [number] }>()
</script>
