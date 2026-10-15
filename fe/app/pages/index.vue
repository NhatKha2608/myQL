<script setup lang="ts">
definePageMeta({ middleware: 'auth' })
const selected = useCookie<number | null>('qlhs_selected_classroom')
const classId = Number(selected.value)
await navigateTo(Number.isFinite(classId) && classId > 0 ? '/dashboard' : '/classrooms')
</script>

<template>
  <div />
</template>
