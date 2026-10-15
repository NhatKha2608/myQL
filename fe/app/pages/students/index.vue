<template>
  <div class="space-y-4">
    <div class="card flex flex-wrap gap-3 p-4">
      <input v-model="keyword" class="input max-w-xs" placeholder="Tìm theo tên" />
      <div class="ml-auto flex flex-wrap gap-2">
        <button type="button" class="btn-secondary" @click="openImport = true">
          Nhập từ Excel
        </button>
        <NuxtLink to="/students/new" class="btn-primary">Thêm học sinh</NuxtLink>
      </div>
    </div>

    <StudentsStudentImportDialog
      v-model:open="openImport"
      :classrooms="selected ? [selected] : []"
      :default-classroom-id="selectedId"
      @imported="onImported"
    />

    <CommonLoadingState v-if="loading" />
    <CommonErrorState v-else-if="error" :message="error" @retry="load" />
    <div v-else class="card overflow-hidden">
      <table class="data-table text-left">
        <thead>
          <tr>
            <th class="w-16 px-4 py-3">STT</th>
            <th class="px-4 py-3">Họ tên</th>
            <th class="px-4 py-3">Lớp</th>
            <th class="px-4 py-3">Trạng thái</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(s, index) in students" :key="s.id">
            <td class="px-4 py-3 tabular-nums text-slate-500">{{ page.page * 20 + index + 1 }}</td>
            <td class="px-4 py-3">{{ s.fullName }}</td>
            <td class="px-4 py-3">{{ s.classroomName || '—' }}</td>
            <td class="px-4 py-3"><CommonStatusBadge :status="s.status" /></td>
            <td class="px-4 py-3 text-right">
              <NuxtLink :to="`/students/${s.id}`" class="text-primary-600 hover:underline">Chi tiết</NuxtLink>
              <button
                type="button"
                class="ml-3 text-rose-600 hover:underline disabled:opacity-50"
                :disabled="deletingId === s.id"
                @click="remove(s)"
              >
                {{ deletingId === s.id ? 'Đang xóa…' : 'Xóa' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <CommonEmptyState v-if="!students.length" message="Không tìm thấy học sinh" />
      <CommonPaginationBar
        v-if="page.totalPages > 1"
        :page="page.page"
        :total-pages="page.totalPages"
        :total-elements="page.totalElements"
        @update:page="onPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Student } from '~/types/api'
import { useStudentApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth' })

const { selectedId, selected, loadClassrooms } = useClassroomContext()
const keyword = ref('')
const loading = ref(false)
const error = ref<string | null>(null)
const students = ref<Student[]>([])
const page = reactive({ page: 0, totalPages: 0, totalElements: 0 })
const openImport = ref(false)
const deletingId = ref<number | null>(null)
const { confirm, alert } = useAppDialog()

async function remove(student: Student) {
  const ok = await confirm(`Xóa học sinh ${student.fullName}? Học sinh sẽ không còn hiện trong danh sách.`, {
    title: 'Xóa học sinh',
    confirmLabel: 'Xóa',
    danger: true,
  })
  if (!ok) return
  deletingId.value = student.id
  try {
    await useStudentApi().remove(student.id)
    if (students.value.length === 1 && page.page > 0) {
      page.page -= 1
    }
    await load()
  } catch (e) {
    await alert(extractErrorMessage(e, 'Không xóa được học sinh'), { title: 'Không xóa được' })
  } finally {
    deletingId.value = null
  }
}

function onImported() {
  page.page = 0
  load()
}

async function load() {
  if (!selectedId.value) {
    students.value = []
    page.totalPages = 0
    page.totalElements = 0
    return
  }
  loading.value = true
  error.value = null
  try {
    const res = await useStudentApi().list({
      keyword: keyword.value,
      classroomId: Number(selectedId.value),
      page: page.page,
      size: 20,
    })
    students.value = res.content
    page.totalPages = res.totalPages
    page.totalElements = res.totalElements
  } catch (e) {
    error.value = extractErrorMessage(e, 'Không tải được danh sách học sinh')
  } finally {
    loading.value = false
  }
}

function onPage(p: number) {
  page.page = p
  load()
}

let debounce: ReturnType<typeof setTimeout>
watch(keyword, () => {
  clearTimeout(debounce)
  debounce = setTimeout(() => {
    page.page = 0
    load()
  }, 300)
})

const ready = ref(false)

watch(selectedId, () => {
  if (!ready.value) return
  page.page = 0
  load()
})

onMounted(async () => {
  await loadClassrooms()
  ready.value = true
  await load()
})
</script>
