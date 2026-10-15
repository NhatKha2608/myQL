<template>
  <CommonAppModal
    :open="state.open"
    max-width="sm"
    :close-on-backdrop="state.mode === 'alert'"
    :close-on-escape="true"
    @close="dismiss"
  >
    <h3 class="font-display text-lg font-bold text-slate-800">{{ state.title }}</h3>
    <p class="text-sm leading-relaxed text-slate-600">{{ state.message }}</p>
    <div class="flex flex-wrap gap-2 pt-2">
      <button
        type="button"
        :class="state.danger ? 'btn-danger' : 'btn-primary'"
        @click="accept"
      >
        {{ state.confirmLabel }}
      </button>
      <button
        v-if="state.mode === 'confirm'"
        type="button"
        class="btn-secondary"
        @click="dismiss"
      >
        {{ state.cancelLabel }}
      </button>
    </div>
  </CommonAppModal>
</template>

<script setup lang="ts">
const { state, accept, dismiss } = useAppDialog()
</script>
