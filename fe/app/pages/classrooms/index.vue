<template>
  <div class="space-y-4">
    <div class="flex justify-end">
      <button type="button" class="btn-primary" @click="openCreate = true">Thêm lớp</button>
    </div>
    <CommonLoadingState v-if="loading && !classrooms.length" />
    <CommonErrorState v-else-if="error" :message="error" @retry="loadClassrooms" />
    <CommonEmptyState v-else-if="!classrooms.length" message="Chưa có lớp. Hãy thêm lớp mới để bắt đầu." />
    <div v-else class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <article
        v-for="c in classrooms"
        :key="c.id"
        class="card-interactive flex flex-col p-5"
        :class="c.id === currentId ? 'border-primary-400 ring-2 ring-primary-200' : ''"
      >
        <button type="button" class="text-left" @click="enterClassroom(c.id)">
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
        <div class="mt-4 flex justify-end border-t border-slate-100 pt-3">
          <button
            type="button"
            class="text-sm font-semibold text-rose-600 hover:underline disabled:opacity-50"
            :disabled="deletingId === c.id"
            @click="remove(c)"
          >
            {{ deletingId === c.id ? 'Đang xóa…' : 'Xóa lớp' }}
          </button>
        </div>
      </article>
    </div>

    <ClassroomsCreateClassroomModal v-model:open="openCreate" @created="onCreated" />
  </div>
</template>

<script setup lang="ts">
import type { Classroom } from '~/types/api'
import { useClassroomApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'
import { academicYearLabel, classroomCode, classroomTitle } from '~/utils/classroomLabel'

definePageMeta({ middleware: 'auth' })

const { classrooms, selectedId, loading, error, loadClassrooms, enterClassroom } = useClassroomContext()
const { confirm, alert } = useAppDialog()
const openCreate = ref(false)
const deletingId = ref<number | null>(null)

const currentId = computed(() => {
  const value = Number(selectedId.value)
  return Number.isFinite(value) && value > 0 ? value : null
})

async function onCreated(room: Classroom) {
  await loadClassrooms()
  await enterClassroom(room.id)
}

async function remove(room: Classroom) {
  if (deletingId.value != null) return
  const title = classroomTitle(room.name)
  const ok = await confirm(
    `Xóa ${title}? Lớp và danh sách học sinh của lớp sẽ không còn hiện trong hệ thống.`,
    { title: 'Xóa lớp', confirmLabel: 'Xóa', danger: true },
  )
  if (!ok) return
  deletingId.value = room.id
  try {
    await useClassroomApi().remove(room.id)
    await loadClassrooms()
  } catch (e) {
    await alert(extractErrorMessage(e, 'Không xóa được lớp'), { title: 'Không xóa được' })
  } finally {
    deletingId.value = null
  }
}

onMounted(() => loadClassrooms())
</script>
