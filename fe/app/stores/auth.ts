import { defineStore } from 'pinia'
import type { AuthResponse, ChangePasswordRequest, LoginRequest, UserProfile } from '~/types/api'
import { extractErrorMessage } from '~/utils/apiError'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const initialized = ref(false)

  const accessToken = useCookie<string | null>('qlhs_access_token', {
    sameSite: 'lax',
    secure: import.meta.env.PROD,
    maxAge: 60 * 60 * 8,
  })
  const refreshToken = useCookie<string | null>('qlhs_refresh_token', {
    sameSite: 'lax',
    secure: import.meta.env.PROD,
    maxAge: 60 * 60 * 24 * 14,
  })
  const selectedClassroom = useCookie<number | null>('qlhs_selected_classroom', {
    sameSite: 'lax',
  })

  const isAuthenticated = computed(() => !!accessToken.value)
  const displayName = computed(() => user.value?.fullName || user.value?.username || 'Giáo viên')

  async function login(payload: LoginRequest) {
    loading.value = true
    error.value = null
    const { api } = useApi()
    try {
      const data = await api<AuthResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: payload,
        auth: false,
      })
      accessToken.value = data.accessToken
      refreshToken.value = data.refreshToken
      user.value = data.user
      selectedClassroom.value = null
      return data
    } catch (e) {
      error.value = extractErrorMessage(e, 'Đăng nhập thất bại')
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchMe() {
    if (!accessToken.value) {
      user.value = null
      initialized.value = true
      return null
    }
    const { api } = useApi()
    try {
      const profile = await api<UserProfile>('/api/v1/auth/me')
      user.value = profile
      return profile
    } catch {
      accessToken.value = null
      refreshToken.value = null
      user.value = null
      return null
    } finally {
      initialized.value = true
    }
  }

  async function logout() {
    const { api } = useApi()
    try {
      if (accessToken.value) {
        await api<void>('/api/v1/auth/logout', {
          method: 'POST',
          body: { refreshToken: refreshToken.value },
        })
      }
    } catch {
      // still clear local session
    } finally {
      accessToken.value = null
      refreshToken.value = null
      user.value = null
      selectedClassroom.value = null
      await navigateTo('/login')
    }
  }

  async function changePassword(payload: ChangePasswordRequest) {
    const { api } = useApi()
    await api<void>('/api/v1/auth/change-password', {
      method: 'POST',
      body: payload,
    })
  }

  return {
    user,
    loading,
    error,
    initialized,
    accessToken,
    refreshToken,
    isAuthenticated,
    displayName,
    login,
    fetchMe,
    logout,
    changePassword,
  }
})
