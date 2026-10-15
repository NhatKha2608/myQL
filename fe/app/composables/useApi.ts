import type { ApiResponse } from '~/types/api'
import { ApiError } from '~/utils/apiError'
import { SESSION_MAX_AGE_SECONDS } from '~/utils/session'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface ApiOptions {
  method?: HttpMethod
  body?: BodyInit | Record<string, unknown> | null
  query?: Record<string, string | number | boolean | null | undefined>
  headers?: Record<string, string>
  auth?: boolean
  /** Skip unwrapping ApiResponse envelope (e.g. file download) */
  raw?: boolean
}

function buildQuery(query?: ApiOptions['query']): string {
  if (!query) return ''
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue
    params.set(key, String(value))
  }
  const s = params.toString()
  return s ? `?${s}` : ''
}

export function useApi() {
  const config = useRuntimeConfig()
  const accessToken = useCookie<string | null>('qlhs_access_token', {
    sameSite: 'lax',
    secure: import.meta.env.PROD,
    maxAge: SESSION_MAX_AGE_SECONDS,
  })

  async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
    const {
      method = 'GET',
      body,
      query,
      headers = {},
      auth = true,
      raw = false,
    } = options

    const base = String(config.public.apiBase).replace(/\/$/, '')
    const url = `${base}${path.startsWith('/') ? path : `/${path}`}${buildQuery(query)}`

    const reqHeaders: Record<string, string> = { ...headers }
    if (auth && accessToken.value) {
      reqHeaders.Authorization = `Bearer ${accessToken.value}`
    }
    if (body && !(body instanceof FormData) && !reqHeaders['Content-Type']) {
      reqHeaders['Content-Type'] = 'application/json'
    }

    try {
      if (raw) {
        return await $fetch<T>(url, {
          method,
          body: body as BodyInit | Record<string, unknown> | undefined,
          headers: reqHeaders,
        })
      }

      const res = await $fetch<ApiResponse<T>>(url, {
        method,
        body: body as BodyInit | Record<string, unknown> | undefined,
        headers: reqHeaders,
      })

      if (res && typeof res === 'object' && 'success' in res) {
        if (!res.success) {
          throw new ApiError(res.message || 'Thao tác thất bại', 400, res)
        }
        return res.data as T
      }

      return res as unknown as T
    } catch (err: unknown) {
      const fetchErr = err as {
        statusCode?: number
        status?: number
        data?: { message?: string; success?: boolean }
        message?: string
      }
      const status = fetchErr.statusCode || fetchErr.status || 500
      const message =
        fetchErr.data?.message ||
        fetchErr.message ||
        'Không thể kết nối máy chủ'

      if (status === 401 && auth && !path.includes('/auth/login')) {
        accessToken.value = null
        const refresh = useCookie('qlhs_refresh_token')
        refresh.value = null
        if (import.meta.client) {
          await navigateTo('/login')
        }
      }

      throw new ApiError(message, status, fetchErr.data)
    }
  }

  return { api, accessToken }
}
