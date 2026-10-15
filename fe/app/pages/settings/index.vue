<template>
  <form class="card mx-auto max-w-md space-y-4 p-6" @submit.prevent="submit">
    <h2 class="page-title text-lg">Đổi mật khẩu</h2>
    <div>
      <label class="label">Mật khẩu hiện tại</label>
      <input v-model="form.currentPassword" type="password" class="input" required autocomplete="current-password" />
    </div>
    <div>
      <label class="label">Mật khẩu mới</label>
      <input v-model="form.newPassword" type="password" class="input" required minlength="8" autocomplete="new-password" />
    </div>
    <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>
    <p v-if="ok" class="text-sm text-mint-700">Đã đổi mật khẩu thành công.</p>
    <button type="submit" class="btn-primary" :disabled="saving">{{ saving ? 'Đang lưu…' : 'Lưu' }}</button>
  </form>
</template>

<script setup lang="ts">
import { extractErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: 'auth' })

const auth = useAuth()
const saving = ref(false)
const error = ref<string | null>(null)
const ok = ref(false)
const form = reactive({ currentPassword: '', newPassword: '' })

async function submit() {
  saving.value = true
  error.value = null
  ok.value = false
  try {
    await auth.changePassword(form)
    ok.value = true
    form.currentPassword = ''
    form.newPassword = ''
  } catch (e) {
    error.value = extractErrorMessage(e, 'Đổi mật khẩu thất bại')
  } finally {
    saving.value = false
  }
}
</script>
