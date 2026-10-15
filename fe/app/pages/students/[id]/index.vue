<template>
  <div class="mx-auto max-w-3xl space-y-4">
    <CommonLoadingState v-if="loading" />
    <CommonErrorState v-else-if="error" :message="error" @retry="load" />
    <CommonErrorState
      v-else-if="blocked"
      title="Không thuộc lớp này"
      message="Học sinh không thuộc lớp đang chọn."
      :retryable="false"
    />
    <template v-else-if="student">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <NuxtLink to="/students" class="btn-secondary">Quay lại</NuxtLink>
        <div class="flex flex-wrap gap-2">
          <NuxtLink :to="`/students/${student.id}/edit`" class="btn-primary">Sửa</NuxtLink>
          <button type="button" class="btn-danger" :disabled="deleting" @click="remove">
            {{ deleting ? 'Đang xóa…' : 'Xóa' }}
          </button>
        </div>
      </div>

      <section class="card overflow-hidden">
        <header class="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 px-6 py-5">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">Hồ sơ học sinh</p>
            <h2 class="page-title mt-1">{{ student.fullName }}</h2>
            <p class="mt-1 text-slate-600">{{ student.classroomName || 'Chưa xếp lớp' }}</p>
          </div>
          <CommonStatusBadge :status="student.status" />
        </header>
        <dl>
          <div
            v-for="row in rows"
            :key="row.label"
            class="grid gap-1 border-b border-slate-100 px-6 py-4 last:border-b-0 sm:grid-cols-[10rem_1fr] sm:items-center"
          >
            <dt class="text-sm text-slate-500">{{ row.label }}</dt>
            <dd class="text-sm font-medium text-slate-800">{{ row.value }}</dd>
          </div>
        </dl>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { Student } from '~/types/api'
import { useStudentApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'
import { formatDateVN } from '~/utils/date'

definePageMeta({ middleware: 'auth' })

const genderMap: Record<string, string> = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
}

const route = useRoute()
const id = computed(() => Number(route.params.id))
const { selectedId, loadClassrooms } = useClassroomContext()
const loading = ref(true)
const error = ref<string | null>(null)
const blocked = ref(false)
const deleting = ref(false)
const student = ref<Student | null>(null)
const { confirm, alert } = useAppDialog()

const rows = computed(() => {
  const current = student.value
  if (!current) return []
  return [
    { label: 'Ngày sinh', value: formatDateVN(current.dateOfBirth) },
    { label: 'Giới tính', value: genderMap[current.gender || ''] || '—' },
    { label: 'Địa chỉ', value: current.address || '—' },
  ]
})

async function load() {
  loading.value = true
  error.value = null
  blocked.value = false
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

async function remove() {
  const current = student.value
  if (!current || deleting.value) return
  const ok = await confirm(`Xóa học sinh ${current.fullName}? Học sinh sẽ không còn hiện trong danh sách.`, {
    title: 'Xóa học sinh',
    confirmLabel: 'Xóa',
    danger: true,
  })
  if (!ok) return
  deleting.value = true
  try {
    await useStudentApi().remove(current.id)
    await navigateTo('/students')
  } catch (e) {
    await alert(extractErrorMessage(e, 'Không xóa được học sinh'), { title: 'Không xóa được' })
  } finally {
    deleting.value = false
  }
}

onMounted(load)
</script>
