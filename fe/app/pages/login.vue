<template>
  <div
    class="relative flex min-h-screen items-center justify-center overflow-hidden bg-canvas px-4 py-10"
  >
    <div
      class="pointer-events-none absolute inset-0 bg-gradient-to-br from-primary-100 via-canvas to-primary-50"
    />
    <div
      class="pointer-events-none absolute -left-20 top-10 h-72 w-72 rounded-full bg-accent/20 blur-3xl"
    />
    <div
      class="pointer-events-none absolute -right-16 bottom-10 h-80 w-80 rounded-full bg-primary-800/10 blur-3xl"
    />

    <div class="relative z-10 w-full max-w-md">
      <div class="mb-8 text-center">
        <div
          class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl border-2 border-primary-200 bg-accent font-display text-2xl font-extrabold text-white shadow-card"
        >
          Q
        </div>
        <h1 class="font-display text-3xl font-extrabold text-primary-800">QLHS</h1>
        <p class="mt-2 text-sm text-slate-600">Quản lý lớp học tiểu học — đăng nhập để tiếp tục</p>
      </div>

      <form class="card space-y-4 p-6 sm:p-8" @submit.prevent="onSubmit">
        <div>
          <label class="label" for="username">Email hoặc tên đăng nhập</label>
          <input
            id="username"
            v-model="form.usernameOrEmail"
            class="input"
            autocomplete="username"
            required
            placeholder="admin hoặc gvl01"
          />
          <p v-if="fieldErrors.usernameOrEmail" class="mt-1 text-xs text-rose-600">
            {{ fieldErrors.usernameOrEmail }}
          </p>
        </div>
        <div>
          <label class="label" for="password">Mật khẩu</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            class="input"
            autocomplete="current-password"
            required
          />
          <p v-if="fieldErrors.password" class="mt-1 text-xs text-rose-600">
            {{ fieldErrors.password }}
          </p>
        </div>

        <p v-if="errorMsg" class="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {{ errorMsg }}
        </p>

        <button type="submit" class="btn-primary w-full" :disabled="auth.loading">
          {{ auth.loading ? 'Đang đăng nhập…' : 'Đăng nhập' }}
        </button>

        <p class="text-center text-xs text-slate-400">
          Demo: <span class="font-medium text-slate-600">admin</span> hoặc
          <span class="font-medium text-slate-600">gvl01</span> · mật khẩu
          <span class="font-medium text-slate-600">password</span>
        </p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { z } from 'zod'

definePageMeta({
  layout: 'blank',
  middleware: 'auth',
})

const auth = useAuth()
const route = useRoute()
const { clearSelection, pendingPath } = useClassroomContext()

const form = reactive({
  usernameOrEmail: '',
  password: '',
})
const fieldErrors = reactive<Record<string, string>>({})
const errorMsg = ref<string | null>(null)

const schema = z.object({
  usernameOrEmail: z.string().min(1, 'Vui lòng nhập email hoặc tên đăng nhập'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
})

async function onSubmit() {
  errorMsg.value = null
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k])
  const parsed = schema.safeParse(form)
  if (!parsed.success) {
    for (const issue of parsed.error.issues) {
      fieldErrors[String(issue.path[0])] = issue.message
    }
    return
  }
  try {
    await auth.login(parsed.data)
    clearSelection()
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    pendingPath.value =
      redirect.startsWith('/') && !redirect.startsWith('/login') && !redirect.startsWith('/classrooms')
        ? redirect
        : null
    await navigateTo('/classrooms')
  } catch {
    errorMsg.value = auth.error || 'Đăng nhập thất bại'
  }
}
</script>
