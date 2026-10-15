<template>
  <div class="space-y-4">
    <div class="flex justify-end">
      <button type="button" class="btn-primary" @click="openCreate = true">Thêm lớp</button>
    </div>
    <CommonLoadingState v-if="loading && !classrooms.length" />
    <CommonErrorState v-else-if="error" :message="error" @retry="loadClassrooms" />
    <CommonEmptyState v-else-if="!classrooms.length" message="Chưa có lớp. Hãy thêm lớp mới để bắt đầu." />
    <div v-else class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <button
        v-for="c in classrooms"
        :key="c.id"
        type="button"
        class="card-interactive p-5 text-left"
        :class="c.id === currentId ? 'border-primary-400 ring-2 ring-primary-200' : ''"
        @click="enterClassroom(c.id)"
      >
        <div class="flex items-center gap-3">
          <span class="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-primary-600 text-sm font-extrabold text-white">
            {{ classroomCode(c.name) }}
          </span>
          <div class="min-w-0">
            <h3 class="truncate font-display text-xl font-bold text-slate-800">{{ classroomTitle(c.name) }}</h3>
            <p class="truncate text-sm text-slate-500">{{ academicYearLabel(c.academicYearName) }}</p>
          </div>
        </div>
        <p class="mt-3 text-sm text-slate-600">
          Khối {{ c.gradeLevel }} · <span class="font-semibold text-slate-800">{{ c.studentCount }}</span> học sinh
        </p>
      </button>
    </div>

    <ClassroomsCreateClassroomModal v-model:open="openCreate" @created="onCreated" />
  </div>
</template>

<script setup lang="ts">
import type { Classroom } from '~/types/api'
import { academicYearLabel, classroomCode, classroomTitle } from '~/utils/classroomLabel'

definePageMeta({ middleware: 'auth' })

const { classrooms, selectedId, loading, error, loadClassrooms, enterClassroom } = useClassroomContext()
const openCreate = ref(false)

const currentId = computed(() => {
  const value = Number(selectedId.value)
  return Number.isFinite(value) && value > 0 ? value : null
})

async function onCreated(room: Classroom) {
  await loadClassrooms()
  await enterClassroom(room.id)
}

onMounted(() => loadClassrooms())
</script>
