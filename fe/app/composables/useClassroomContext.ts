import type { Classroom } from '~/types/api'
import { useClassroomApi } from '~/services/api'
import { extractErrorMessage } from '~/utils/apiError'

function normalizeClassroomId(value: unknown): number | null {
  if (value == null || value === '') return null
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : null
}

/** Lớp đang chọn dùng chung cho toàn bộ màn hình sau đăng nhập. */
export function useClassroomContext() {
  const route = useRoute()
  const classrooms = useState<Classroom[]>('qlhs_classrooms', () => [])
  const selectedId = useCookie<number | null>('qlhs_selected_classroom', {
    sameSite: 'lax',
    default: () => null,
  })
  const pendingPath = useState<string | null>('qlhs_after_class', () => null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const selected = computed(
    () => classrooms.value.find((c) => c.id === normalizeClassroomId(selectedId.value)) || null,
  )

  async function loadClassrooms() {
    loading.value = true
    error.value = null
    try {
      const api = useClassroomApi()
      const page = await api.list(0, 100)
      classrooms.value = (page.content || []).filter((c) => c != null && c.id != null)
      const current = normalizeClassroomId(selectedId.value)
      if (current && !classrooms.value.some((c) => c.id === current)) {
        selectedId.value = null
      } else if (selectedId.value != null && selectedId.value !== current) {
        selectedId.value = current
      }
    } catch (e) {
      error.value = extractErrorMessage(e, 'Không tải được danh sách lớp')
    } finally {
      loading.value = false
    }
  }

  function selectClassroom(id: number) {
    selectedId.value = id
  }

  function clearSelection() {
    selectedId.value = null
  }

  function enterClassroom(id: number) {
    selectClassroom(id)
    const path = route.path
    const onPicker = path === '/classrooms' || path.startsWith('/classrooms/')
    if (!onPicker) return
    const next = pendingPath.value
    pendingPath.value = null
    const target =
      next &&
      next.startsWith('/') &&
      !next.startsWith('/login') &&
      !next.startsWith('/classrooms')
        ? next
        : '/dashboard'
    return navigateTo(target)
  }

  return {
    classrooms,
    selectedId,
    selected,
    pendingPath,
    loading,
    error,
    loadClassrooms,
    selectClassroom,
    clearSelection,
    enterClassroom,
  }
}
