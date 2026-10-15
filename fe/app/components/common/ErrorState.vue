<template>
  <div class="card mx-auto max-w-lg border-rose-100 bg-rose-50/50 p-6 text-center">
    <p class="font-display text-lg font-bold text-rose-700">{{ title }}</p>
    <p class="mt-2 text-sm text-rose-600/90">{{ message }}</p>
    <button v-if="retryable" type="button" class="btn-secondary mt-4" @click="$emit('retry')">
      Thử lại
    </button>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    message: string
    retryable?: boolean
  }>(),
  {
    title: 'Không tải được dữ liệu',
    retryable: true,
  },
)
defineEmits<{ retry: [] }>()
</script>
