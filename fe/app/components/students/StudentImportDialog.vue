<template>
  <CommonAppModal :open="open" max-width="lg" close-on-escape @close="close">
    <form class="space-y-4" @submit.prevent="submit">
      <h3 class="font-display text-lg font-bold text-slate-800">Nhập học sinh từ Excel</h3>
      <p class="text-sm text-slate-600">
        File <strong>.xlsx</strong>, tối đa 5MB. Dòng đầu là tiêu đề; từ dòng 2 chỉ cần cột
        <strong>HoTen</strong>. Mã học sinh được tạo tự động.
      </p>
      <div v-if="classrooms.length === 1" class="rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700">
        Học sinh sẽ được thêm vào <strong>{{ classrooms[0].name }}</strong>
      </div>
      <div v-else>
        <label class="label">Lớp ghi danh *</label>
        <select v-model.number="classroomId" class="input" required>
          <option disabled :value="0">— Chọn lớp —</option>
          <option v-for="c in classrooms" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div>
        <label class="label">File Excel *</label>
        <input
          ref="fileInput"
          type="file"
          accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          class="input !py-2 file:mr-3 file:rounded-lg file:border-0 file:bg-primary-50 file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-primary-700"
          required
          @change="onFileChange"
        />
        <p v-if="fileName" class="mt-1 text-xs text-slate-500">Đã chọn: {{ fileName }}</p>
      </div>
      <button
        type="button"
        class="text-sm font-medium text-primary-600 hover:underline"
        :disabled="downloadingTemplate"
        @click="downloadTemplate"
      >
        {{ downloadingTemplate ? 'Đang tải…' : 'Tải file mẫu (.xlsx)' }}
      </button>
      <p v-if="error" class="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ error }}</p>
      <p v-if="success" class="rounded-xl bg-mint-50 px-3 py-2 text-sm text-mint-800">{{ success }}</p>
      <div class="flex gap-2 pt-2">
        <button type="submit" class="btn-primary" :disabled="importing || !canSubmit">
          {{ importing ? 'Đang nhập…' : 'Nhập danh sách' }}
        </button>
        <button type="button" class="btn-secondary" :disabled="importing" @click="close">Huỷ</button>
      </div>
    </form>
  </CommonAppModal>
</template>

<script setup lang="ts">
import type { Classroom } from '~/types/api'
import { useStudentApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'
import { downloadBlob } from '~/utils/download'

const props = defineProps<{
  open: boolean
  classrooms: Classroom[]
  defaultClassroomId?: number | null
}>()

const emit = defineEmits<{
  'update:open': [boolean]
  imported: []
}>()

const classroomId = ref(0)
const file = ref<File | null>(null)
const fileName = ref('')
const importing = ref(false)
const downloadingTemplate = ref(false)
const error = ref<string | null>(null)
const success = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const canSubmit = computed(() => classroomId.value > 0 && !!file.value)

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return
    error.value = null
    success.value = null
    file.value = null
    fileName.value = ''
    classroomId.value = props.defaultClassroomId || props.classrooms[0]?.id || 0
    if (fileInput.value) fileInput.value.value = ''
  },
)

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const f = input.files?.[0] ?? null
  file.value = f
  fileName.value = f?.name ?? ''
  success.value = null
}

function close() {
  emit('update:open', false)
}

async function downloadTemplate() {
  downloadingTemplate.value = true
  error.value = null
  try {
    const blob = await useStudentApi().downloadImportTemplate()
    downloadBlob(blob, 'mau-hoc-sinh.xlsx')
  } catch (e) {
    error.value = extractErrorMessage(e, 'Không tải được file mẫu')
  } finally {
    downloadingTemplate.value = false
  }
}

async function submit() {
  if (!file.value || !classroomId.value || importing.value) return
  const name = file.value.name.toLowerCase()
  if (!name.endsWith('.xlsx')) {
    error.value = 'Chỉ chấp nhận file Excel định dạng .xlsx'
    return
  }
  if (file.value.size > 5 * 1024 * 1024) {
    error.value = 'File vượt quá 5MB'
    return
  }
  importing.value = true
  error.value = null
  success.value = null
  try {
    const { imported } = await useStudentApi().importExcel(classroomId.value, file.value)
    success.value =
      imported > 0
        ? `Đã thêm ${imported} học sinh vào lớp.`
        : 'Không có dòng hợp lệ (kiểm tra cột họ tên).'
    emit('imported')
  } catch (e) {
    error.value = extractErrorMessage(e, 'Nhập file thất bại')
  } finally {
    importing.value = false
  }
}
</script>
