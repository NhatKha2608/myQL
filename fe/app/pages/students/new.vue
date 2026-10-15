<template>
  <CommonLoadingState v-if="!selected" />
  <StudentsStudentForm
    v-else
    :classrooms="[selected]"
    :fixed-classroom="selected"
    :saving="saving"
    :submit-error="error"
    submit-label="Thêm học sinh"
    @submit="onSubmit"
  />
</template>

<script setup lang="ts">
import type { StudentRequest } from '~/types/api'
import { useStudentApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth' })

const { selected, loadClassrooms } = useClassroomContext()
const saving = ref(false)
const error = ref<string | null>(null)

async function onSubmit(body: StudentRequest) {
  saving.value = true
  error.value = null
  try {
    const created = await useStudentApi().create(body)
    await navigateTo(`/students/${created.id}`)
  } catch (e) {
    error.value = extractErrorMessage(e, 'Không thêm được học sinh')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadClassrooms())
</script>
