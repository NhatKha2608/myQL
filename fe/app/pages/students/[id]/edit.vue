<template>
  <CommonLoadingState v-if="loading" />
  <CommonErrorState v-else-if="error && !student" :message="error" @retry="load" />
  <CommonErrorState
    v-else-if="blocked"
    title="Không thuộc lớp này"
    message="Học sinh không thuộc lớp đang chọn."
    :retryable="false"
  />
  <StudentsStudentForm
    v-else-if="student"
    :initial="student"
    :classrooms="classrooms"
    :fixed-classroom="selected"
    :saving="saving"
    :submit-error="error"
    submit-label="Cập nhật"
    @submit="onSubmit"
  />
</template>

<script setup lang="ts">
import type { Student, StudentRequest } from '~/types/api'
import { useStudentApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth' })

const route = useRoute()
const id = computed(() => Number(route.params.id))
const { classrooms, selected, selectedId, loadClassrooms } = useClassroomContext()
const loading = ref(true)
const saving = ref(false)
const blocked = ref(false)
const error = ref<string | null>(null)
const student = ref<Student | null>(null)

async function load() {
  loading.value = true
  blocked.value = false
  error.value = null
  student.value = null
  try {
    await loadClassrooms()
    const current = await useStudentApi().get(id.value)
    const classId = Number(selectedId.value)
    if (!classId || Number(current.classroomId) !== classId) {
      blocked.value = true
      return
    }
    student.value = current
  } catch (e) {
    error.value = extractErrorMessage(e, 'Không tải được hồ sơ')
  } finally {
    loading.value = false
  }
}

async function onSubmit(body: StudentRequest) {
  saving.value = true
  error.value = null
  try {
    await useStudentApi().update(id.value, body)
    await navigateTo(`/students/${id.value}`)
  } catch (e) {
    error.value = extractErrorMessage(e, 'Cập nhật thất bại')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
