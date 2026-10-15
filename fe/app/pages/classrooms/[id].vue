<template>
  <div class="space-y-4">
    <CommonLoadingState v-if="loading" />
    <CommonErrorState v-else-if="error" :message="error" @retry="load" />
    <template v-else-if="classroom">
      <div class="card p-6">
        <h2 class="page-title">{{ classroom.name }}</h2>
        <p class="text-slate-600">Khối {{ classroom.gradeLevel }} · GVCN: {{ classroom.homeroomTeacherName }}</p>
        <p class="mt-2 text-sm">{{ classroom.description || '—' }}</p>
      </div>
      <div class="card p-4">
        <h3 class="mb-3 font-semibold">Học sinh ({{ students.length }})</h3>
        <ul class="divide-y divide-slate-100 text-sm">
          <li v-for="(s, index) in students" :key="s.id" class="flex gap-3 py-2">
            <span class="w-8 tabular-nums text-slate-400">{{ index + 1 }}</span>
            <span>{{ s.fullName }}</span>
          </li>
        </ul>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { Classroom, Student } from '~/types/api'
import { useClassroomApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth' })

const route = useRoute()
const id = computed(() => Number(route.params.id))
const { selectClassroom } = useClassroomContext()
const loading = ref(true)
const error = ref<string | null>(null)
const classroom = ref<Classroom | null>(null)
const students = ref<Student[]>([])

async function load() {
  loading.value = true
  error.value = null
  try {
    classroom.value = await useClassroomApi().get(id.value)
    students.value = await useClassroomApi().students(id.value)
    selectClassroom(classroom.value.id)
  } catch (e) {
    error.value = extractErrorMessage(e, 'Không tải được lớp học')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
