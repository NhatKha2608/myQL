<template>
  <div class="space-y-4">
    <div class="card flex flex-wrap gap-3 p-4">
      <NuxtLink to="/scores" class="btn-secondary">Quay lại</NuxtLink>
      <select v-model="studentId" class="input !w-auto min-w-[12rem]">
        <option :value="null">Tất cả giao dịch lớp</option>
        <option v-for="s in students" :key="s.id" :value="s.id">{{ s.fullName }}</option>
      </select>
    </div>
    <CommonLoadingState v-if="loading" />
    <div v-else class="card divide-y divide-slate-100">
      <div v-for="tx in transactions" :key="tx.id" class="flex flex-wrap justify-between gap-2 px-4 py-3 text-sm">
        <div>
          <p class="font-semibold">
            {{ tx.studentName }} · {{ tx.pointsDelta > 0 ? '+' : '' }}{{ tx.pointsDelta }}
          </p>
          <p class="text-slate-600">{{ tx.reason }}</p>
          <p class="text-xs text-slate-400">
            {{ formatDateVN(tx.transactionDate) }} · {{ tx.createdByName || '—' }}
            <span v-if="tx.reversed" class="text-rose-600"> · Đã hoàn tác</span>
          </p>
        </div>
        <button
          v-if="!tx.reversed"
          type="button"
          class="btn-secondary !py-1 text-xs"
          @click="reverseTx(tx.id)"
        >
          Hoàn tác điểm
        </button>
      </div>
      <CommonEmptyState v-if="!transactions.length" message="Chưa có giao dịch" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ScoreTransaction, Student } from '~/types/api'
import { useClassroomApi, useScoreApi } from '~/services/api'
import { formatDateVN } from '~/utils/date'
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth' })

const { selectedId, loadClassrooms } = useClassroomContext()
const loading = ref(false)
const students = ref<Student[]>([])
const studentId = ref<number | null>(null)
const transactions = ref<ScoreTransaction[]>([])

async function load() {
  if (!selectedId.value) return
  loading.value = true
  try {
    const cid = selectedId.value
    students.value = await useClassroomApi().students(cid)
    if (studentId.value) {
      transactions.value = await useScoreApi().studentHistory(studentId.value, cid)
    } else {
      const page = await useScoreApi().transactions(cid, 0, 50)
      transactions.value = page.content
    }
  } finally {
    loading.value = false
  }
}

const dialog = useAppDialog()

async function reverseTx(id: number) {
  const ok = await dialog.confirm('Hoàn tác điểm này?', {
    title: 'Hoàn tác điểm',
    confirmLabel: 'Hoàn tác điểm',
    danger: true,
  })
  if (!ok) return
  try {
    await useScoreApi().reverse(id)
    await load()
  } catch (e) {
    await dialog.alert(extractErrorMessage(e, 'Hoàn tác điểm thất bại'), { title: 'Hoàn tác điểm thất bại' })
  }
}

onMounted(async () => {
  await loadClassrooms()
  await load()
})
watch(() => selectedId.value, () => {
  studentId.value = null
  load()
})
watch(studentId, load)
</script>
