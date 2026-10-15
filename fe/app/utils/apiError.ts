export class ApiError extends Error {
  status: number
  data?: unknown

  constructor(message: string, status = 500, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

export function extractErrorMessage(error: unknown, fallback = 'Đã xảy ra lỗi'): string {
  if (error instanceof ApiError) return error.message
  if (error && typeof error === 'object') {
    const e = error as Record<string, unknown>
    if (typeof e.message === 'string' && e.message) return e.message
    const data = e.data as Record<string, unknown> | undefined
    if (data && typeof data.message === 'string') return data.message
  }
  if (typeof error === 'string') return error
  return fallback
}
