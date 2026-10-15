<template>
  <form class="card space-y-4 p-5" @submit.prevent="submit">
    <h3 class="font-display text-lg font-bold text-slate-800">Ghi điểm nhanh</h3>
    <div>
      <label class="label">Học sinh *</label>
      <select v-model="studentId" class="input" required>
        <option disabled :value="null">Chọn học sinh</option>
        <option v-for="s in students" :key="s.id" :value="s.id">
          {{ s.fullName }}
        </option>
      </select>
    </div>
    <div>
      <label class="label">Quy tắc điểm</label>
      <select v-model="scoreRuleId" class="input">
        <option :value="null">— Tuỳ chỉnh —</option>
        <option v-for="r in rules" :key="r.id" :value="r.id">
          {{ r.name }} ({{ r.pointValue > 0 ? '+' : '' }}{{ r.pointValue }})
        </option>
      </select>
    </div>
    <div>
      <label class="label">Lý do *</label>
      <input v-model="reason" class="input" required placeholder="Ví dụ: Giúp bạn hoàn thành bài" />
      <p v-if="reasonError" class="mt-1 text-xs text-rose-600">{{ reasonError }}</p>
    </div>
    <div>
      <label class="label">Ghi chú</label>
      <input v-model="note" class="input" />
    </div>
    <button type="submit" class="btn-primary w-full" :disabled="saving || !studentId">
      {{ saving ? 'Đang lưu…' : 'Lưu điểm' }}
    </button>
  </form>
</template>

<script setup lang="ts">
import { z } from 'zod'
import type { ScoreRule, ScoreTransactionRequest, Student } from '~/types/api'

defineProps<{
  students: Student[]
  rules: ScoreRule[]
  saving?: boolean
}>()

const emit = defineEmits<{ submit: [ScoreTransactionRequest] }>()

const studentId = ref<number | null>(null)
const scoreRuleId = ref<number | null>(null)
const reason = ref('')
const note = ref('')
const reasonError = ref<string | null>(null)

const reasonSchema = z
  .string()
  .trim()
  .min(3, 'Lý do cần ít nhất 3 ký tự')
  .max(500, 'Lý do tối đa 500 ký tự')

function submit() {
  reasonError.value = null
  if (!studentId.value) return
  const parsed = reasonSchema.safeParse(reason.value)
  if (!parsed.success) {
    reasonError.value = parsed.error.issues[0]?.message ?? 'Lý do không hợp lệ'
    return
  }
  emit('submit', {
    studentId: studentId.value,
    scoreRuleId: scoreRuleId.value,
    reason: parsed.data,
    note: note.value.trim() || undefined,
  })
  reason.value = ''
  note.value = ''
}
</script>
