<template>
  <div class="flex flex-wrap items-center gap-2">
    <label class="text-sm font-medium text-slate-600">Lớp:</label>
    <select
      class="input !w-auto min-w-[10rem]"
      :value="currentValue ?? ''"
      :disabled="isLoading || !roomList.length"
      @change="onChange"
    >
      <option v-if="!roomList.length" value="">Chưa có lớp</option>
      <option v-for="c in roomList" :key="c.id" :value="c.id">
        {{ c.name }} (Khối {{ c.gradeLevel }})
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
import type { Classroom } from '~/types/api'
import type { MaybeRef } from 'vue'
import { computed } from 'vue'
import { resolveMaybeRef } from '~/utils/refs'

const props = defineProps<{
  modelValue: MaybeRef<number | null | undefined>
  classrooms: MaybeRef<Classroom[] | undefined>
  loading?: MaybeRef<boolean | undefined>
}>()

const emit = defineEmits<{ 'update:modelValue': [number | null] }>()

const roomList = computed(() => {
  const list = resolveMaybeRef(props.classrooms) ?? []
  return list.filter((c): c is Classroom => c != null && c.id != null)
})

const isLoading = computed(() => resolveMaybeRef(props.loading) ?? false)

const currentValue = computed(() => resolveMaybeRef(props.modelValue) ?? null)

function onChange(e: Event) {
  const v = (e.target as HTMLSelectElement).value
  emit('update:modelValue', v ? Number(v) : null)
}
</script>
