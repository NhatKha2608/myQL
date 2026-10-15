<template>
  <form class="card space-y-4 p-6" @submit.prevent="onSubmit">
    <div class="grid gap-4 sm:grid-cols-2">
      <div>
        <label class="label">Họ và tên *</label>
        <input v-model="form.fullName" class="input" required />
        <p v-if="errors.fullName" class="mt-1 text-xs text-rose-600">{{ errors.fullName }}</p>
      </div>
      <div>
        <label class="label">Ngày sinh</label>
        <input v-model="form.dateOfBirth" type="date" class="input" />
      </div>
      <div>
        <label class="label">Giới tính</label>
        <select v-model="form.gender" class="input">
          <option value="">—</option>
          <option value="MALE">Nam</option>
          <option value="FEMALE">Nữ</option>
          <option value="OTHER">Khác</option>
        </select>
      </div>
      <div>
        <label class="label">Lớp</label>
        <p v-if="fixedClassroom" class="input bg-slate-50 text-slate-700">
          {{ fixedClassroom.name }}
        </p>
        <select v-else v-model="form.classroomId" class="input">
          <option :value="null">— Chưa xếp lớp —</option>
          <option v-for="c in classroomOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div>
        <label class="label">Trạng thái</label>
        <select v-model="form.status" class="input">
          <option value="ACTIVE">Đang học</option>
          <option value="INACTIVE">Ngừng</option>
          <option value="TRANSFERRED">Chuyển lớp</option>
        </select>
      </div>
      <div class="sm:col-span-2">
        <label class="label">Địa chỉ</label>
        <input v-model="form.address" class="input" />
      </div>
    </div>

    <p v-if="submitError" class="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">
      {{ submitError }}
    </p>

    <div class="flex flex-wrap gap-3 pt-2">
      <button type="submit" class="btn-primary" :disabled="saving">
        {{ saving ? 'Đang lưu…' : submitLabel }}
      </button>
      <NuxtLink to="/students" class="btn-secondary">Huỷ</NuxtLink>
    </div>
  </form>
</template>

<script setup lang="ts">
import { z } from 'zod'
import type { MaybeRef } from 'vue'
import { computed } from 'vue'
import type { Classroom, Student, StudentRequest } from '~/types/api'
import { resolveMaybeRef } from '~/utils/refs'

const props = defineProps<{
  initial?: Student | null
  classrooms: MaybeRef<Classroom[] | undefined>
  fixedClassroom?: Classroom | null
  saving?: boolean
  submitError?: string | null
  submitLabel?: string
}>()

const emit = defineEmits<{ submit: [StudentRequest] }>()

const classroomOptions = computed(() => {
  const list = resolveMaybeRef(props.classrooms) ?? []
  return list.filter((c): c is Classroom => c != null && c.id != null)
})

const schema = z.object({
  fullName: z.string().min(1, 'Vui lòng nhập họ tên'),
})

const form = reactive({
  fullName: props.initial?.fullName || '',
  dateOfBirth: props.initial?.dateOfBirth || '',
  gender: props.initial?.gender || '',
  classroomId: (props.fixedClassroom?.id ?? props.initial?.classroomId ?? null) as number | null,
  status: props.initial?.status || 'ACTIVE',
  address: props.initial?.address || '',
  avatarUrl: props.initial?.avatarUrl || '',
})

const errors = reactive<Record<string, string>>({})

watch(
  () => props.fixedClassroom?.id,
  (id) => {
    if (id) form.classroomId = id
  },
)

function onSubmit() {
  Object.keys(errors).forEach((k) => delete errors[k])
  const parsed = schema.safeParse({
    fullName: form.fullName,
  })
  if (!parsed.success) {
    for (const issue of parsed.error.issues) {
      const key = String(issue.path[0] || '')
      if (key) errors[key] = issue.message
    }
    return
  }
  emit('submit', {
    fullName: form.fullName.trim(),
    dateOfBirth: form.dateOfBirth || null,
    gender: form.gender || undefined,
    classroomId: props.fixedClassroom?.id ?? form.classroomId,
    status: form.status,
    address: form.address || undefined,
    avatarUrl: form.avatarUrl || undefined,
  })
}
</script>
