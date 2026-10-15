<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="presentation"
    >
      <div
        class="absolute inset-0 bg-slate-900/40"
        aria-hidden="true"
        @click="onBackdrop"
      />
      <div
        :class="['card relative z-10 w-full p-6', maxWidthClass]"
        role="dialog"
        aria-modal="true"
        tabindex="-1"
        @click.stop
      >
        <slot />
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    open: boolean
    maxWidth?: 'sm' | 'md' | 'lg'
    closeOnBackdrop?: boolean
    closeOnEscape?: boolean
  }>(),
  {
    maxWidth: 'md',
    closeOnBackdrop: false,
    closeOnEscape: false,
  },
)

const emit = defineEmits<{ close: [] }>()

const maxWidthClass = computed(() => {
  if (props.maxWidth === 'sm') return 'max-w-sm'
  if (props.maxWidth === 'lg') return 'max-w-lg'
  return 'max-w-md'
})

function onBackdrop() {
  if (props.closeOnBackdrop) emit('close')
}

function onEscape() {
  if (props.closeOnEscape) emit('close')
}

function onDocumentKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') onEscape()
}

watch(
  () => props.open,
  (isOpen) => {
    if (!import.meta.client) return
    if (isOpen) {
      document.addEventListener('keydown', onDocumentKeydown)
    } else {
      document.removeEventListener('keydown', onDocumentKeydown)
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (import.meta.client) {
    document.removeEventListener('keydown', onDocumentKeydown)
  }
})
</script>
