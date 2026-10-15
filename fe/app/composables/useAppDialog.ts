export type AppDialogMode = 'confirm' | 'alert'

export interface AppDialogOptions {
  title?: string
  confirmLabel?: string
  cancelLabel?: string
  /** Nút xác nhận dùng màu cảnh báo (hoàn tác, xóa…) */
  danger?: boolean
}

interface AppDialogState {
  open: boolean
  mode: AppDialogMode
  title: string
  message: string
  confirmLabel: string
  cancelLabel: string
  danger: boolean
  _resolve: ((value: boolean) => void) | null
}

function createClosedState(): AppDialogState {
  return {
    open: false,
    mode: 'alert',
    title: '',
    message: '',
    confirmLabel: 'Xác nhận',
    cancelLabel: 'Huỷ',
    danger: false,
    _resolve: null,
  }
}

export function useAppDialog() {
  const state = useState<AppDialogState>('qlhs-app-dialog', createClosedState)

  function settle(result: boolean) {
    const resolve = state.value._resolve
    state.value = createClosedState()
    resolve?.(result)
  }

  function confirm(message: string, options?: AppDialogOptions): Promise<boolean> {
    return new Promise((resolve) => {
      state.value = {
        open: true,
        mode: 'confirm',
        title: options?.title ?? 'Xác nhận',
        message,
        confirmLabel: options?.confirmLabel ?? 'Xác nhận',
        cancelLabel: options?.cancelLabel ?? 'Huỷ',
        danger: options?.danger ?? false,
        _resolve: resolve,
      }
    })
  }

  function alert(message: string, options?: Pick<AppDialogOptions, 'title'>) {
    return new Promise<void>((resolve) => {
      state.value = {
        open: true,
        mode: 'alert',
        title: options?.title ?? 'Thông báo',
        message,
        confirmLabel: 'Đóng',
        cancelLabel: '',
        danger: false,
        _resolve: () => resolve(),
      }
    })
  }

  return {
    state,
    confirm,
    alert,
    accept: () => settle(true),
    dismiss: () => settle(false),
  }
}
