<template>
  <CommonAppModal :open="open" close-on-escape @close="open = false">
    <form class="space-y-3" @submit.prevent="create">
      <h3 class="font-display text-lg font-bold text-slate-800">Thêm lớp</h3>
      <p class="text-sm text-slate-500">Lớp mới sẽ được chọn ngay sau khi tạo.</p>
      <div>
        <label class="label" for="classroom-name">Tên lớp</label>
        <input id="classroom-name" v-model="form.name" class="input" placeholder="VD: 3A" required />
      </div>
      <div>
        <label class="label" for="classroom-grade">Khối</label>
        <input
          id="classroom-grade"
          v-model.number="form.gradeLevel"
          type="number"
          min="1"
          max="5"
          class="input"
          required
        />
      </div>
      <div>
        <label class="label" for="classroom-year">Năm học</label>
        <select id="classroom-year" v-model.number="form.academicYearId" class="input" required>
          <option disabled :value="0">Chọn năm học</option>
          <option v-for="y in years" :key="y.id" :value="y.id">{{ y.name }}</option>
        </select>
      </div>
      <p v-if="createError" class="text-sm text-rose-600">{{ createError }}</p>
      <div class="flex gap-2 pt-2">
        <button type="submit" class="btn-primary" :disabled="creating || !years.length">
          {{ creating ? 'Đang lưu…' : 'Lưu lớp' }}
        </button>
        <button type="button" class="btn-secondary" @click="open = false">Huỷ</button>
      </div>
    </form>
  </CommonAppModal>
</template>

<script setup lang="ts">
import type { AcademicYear, Classroom, ClassroomRequest } from '~/types/api'
import { useAcademicApi, useClassroomApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ created: [Classroom] }>()

const years = ref<AcademicYear[]>([])
const creating = ref(false)
const createError = ref<string | null>(null)
const form = reactive<ClassroomRequest>({ name: '', gradeLevel: 3, academicYearId: 0 })

watch(open, async (isOpen) => {
  if (!isOpen) return
  createError.value = null
  form.name = ''
  form.gradeLevel = 3
  try {
    if (!years.value.length) years.value = await useAcademicApi().years()
    if (years.value[0] && !form.academicYearId) form.academicYearId = years.value[0].id
    if (!years.value.length) createError.value = 'Chưa có năm học để tạo lớp'
  } catch (e) {
    createError.value = extractErrorMessage(e, 'Không tải được năm học')
  }
})

async function create() {
  creating.value = true
  createError.value = null
  try {
    const created = await useClassroomApi().create({
      name: form.name.trim(),
      gradeLevel: form.gradeLevel,
      academicYearId: form.academicYearId,
    })
    open.value = false
    emit('created', created)
  } catch (e) {
    createError.value = extractErrorMessage(e, 'Tạo lớp thất bại')
  } finally {
    creating.value = false
  }
}
</script>
