import type {
  AcademicRecord,
  AcademicRecordRequest,
  AcademicYear,
  Announcement,
  AnnouncementRequest,
  AttendanceRecord,
  Classroom,
  ClassroomRequest,
  DailyAttendanceSaveRequest,
  DashboardSummary,
  LeaderboardEntry,
  PageResponse,
  ReportSummary,
  ScoreRule,
  ScoreRuleRequest,
  ScoreTransaction,
  ScoreTransactionRequest,
  Student,
  StudentScoreSummary,
  StudentRequest,
  Subject,
  TimetableEntry,
  TimetableSlotInput,
} from '~/types/api'

export function useClassroomApi() {
  const { api } = useApi()
  return {
    list: (page = 0, size = 50) =>
      api<PageResponse<Classroom>>('/api/v1/classrooms', { query: { page, size } }),
    get: (id: number) => api<Classroom>(`/api/v1/classrooms/${id}`),
    create: (body: ClassroomRequest) =>
      api<Classroom>('/api/v1/classrooms', { method: 'POST', body }),
    update: (id: number, body: ClassroomRequest) =>
      api<Classroom>(`/api/v1/classrooms/${id}`, { method: 'PUT', body }),
    remove: (id: number) =>
      api<void>(`/api/v1/classrooms/${id}`, { method: 'DELETE' }),
    students: (id: number) =>
      api<Student[]>(`/api/v1/classrooms/${id}/students`),
  }
}

export function useStudentApi() {
  const { api } = useApi()
  return {
    list: (params: {
      keyword?: string
      classroomId?: number | null
      status?: string
      page?: number
      size?: number
    }) =>
      api<PageResponse<Student>>('/api/v1/students', {
        query: {
          keyword: params.keyword,
          classroomId: params.classroomId ?? undefined,
          status: params.status,
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
    get: (id: number) => api<Student>(`/api/v1/students/${id}`),
    create: (body: StudentRequest) =>
      api<Student>('/api/v1/students', { method: 'POST', body }),
    update: (id: number, body: StudentRequest) =>
      api<Student>(`/api/v1/students/${id}`, { method: 'PUT', body }),
    remove: (id: number) =>
      api<void>(`/api/v1/students/${id}`, { method: 'DELETE' }),
    importExcel: (classroomId: number, file: File) => {
      const form = new FormData()
      form.append('file', file)
      form.append('classroomId', String(classroomId))
      return api<{ imported: number }>('/api/v1/students/import', {
        method: 'POST',
        body: form,
        query: { classroomId },
      })
    },
    exportExcel: (classroomId: number) =>
      fetchStudentBlob(`/api/v1/students/export?classroomId=${classroomId}`),
    downloadImportTemplate: () => fetchStudentBlob('/api/v1/students/import-template'),
  }
}

async function fetchStudentBlob(path: string): Promise<Blob> {
  const config = useRuntimeConfig()
  const token = useCookie<string | null>('qlhs_access_token')
  const base = String(config.public.apiBase).replace(/\/$/, '')
  const res = await fetch(`${base}${path}`, {
    headers: { Authorization: `Bearer ${token.value || ''}` },
  })
  if (!res.ok) throw new Error('Tải file thất bại')
  return res.blob()
}

export function useAttendanceApi() {
  const { api } = useApi()
  return {
    list: (classroomId: number, from?: string, to?: string) =>
      api<AttendanceRecord[]>(`/api/v1/classrooms/${classroomId}/attendance`, {
        query: { from, to },
      }),
    daily: (classroomId: number, date?: string) =>
      api<AttendanceRecord[]>(`/api/v1/classrooms/${classroomId}/attendance/daily`, {
        query: { date },
      }),
    saveDaily: (classroomId: number, body: DailyAttendanceSaveRequest) =>
      api<AttendanceRecord[]>(`/api/v1/classrooms/${classroomId}/attendance/daily`, {
        method: 'PUT',
        body,
      }),
    studentHistory: (studentId: number, from?: string, to?: string) =>
      api<AttendanceRecord[]>(`/api/v1/students/${studentId}/attendance-history`, {
        query: { from, to },
      }),
    statistics: (classroomId: number, from?: string, to?: string) =>
      api<Record<string, unknown>>(`/api/v1/classrooms/${classroomId}/attendance/statistics`, {
        query: { from, to },
      }),
    exportMonth: async (classroomId: number, month: string) => {
      const config = useRuntimeConfig()
      const token = useCookie<string | null>('qlhs_access_token')
      const base = String(config.public.apiBase).replace(/\/$/, '')
      const res = await fetch(
        `${base}/api/v1/classrooms/${classroomId}/attendance/export?month=${encodeURIComponent(month)}`,
        { headers: { Authorization: `Bearer ${token.value || ''}` } },
      )
      if (!res.ok) throw new Error('Xuất file thất bại')
      return res.blob()
    },
  }
}

export function useScoreApi() {
  const { api } = useApi()
  return {
    rules: (classroomId: number) =>
      api<ScoreRule[]>(`/api/v1/classrooms/${classroomId}/score-rules`),
    createRule: (classroomId: number, body: ScoreRuleRequest) =>
      api<ScoreRule>(`/api/v1/classrooms/${classroomId}/score-rules`, {
        method: 'POST',
        body,
      }),
    updateRule: (id: number, body: ScoreRuleRequest) =>
      api<ScoreRule>(`/api/v1/score-rules/${id}`, { method: 'PUT', body }),
    deleteRule: (id: number) =>
      api<void>(`/api/v1/score-rules/${id}`, { method: 'DELETE' }),
    transactions: (classroomId: number, page = 0, size = 20) =>
      api<PageResponse<ScoreTransaction>>(
        `/api/v1/classrooms/${classroomId}/score-transactions`,
        { query: { page, size } },
      ),
    createTransaction: (classroomId: number, body: ScoreTransactionRequest) =>
      api<ScoreTransaction>(`/api/v1/classrooms/${classroomId}/score-transactions`, {
        method: 'POST',
        body,
      }),
    reverse: (id: number) =>
      api<ScoreTransaction>(`/api/v1/score-transactions/${id}/reverse`, {
        method: 'POST',
      }),
    studentHistory: (studentId: number, classroomId: number) =>
      api<ScoreTransaction[]>(`/api/v1/students/${studentId}/score-history`, {
        query: { classroomId },
      }),
    leaderboard: (classroomId: number, range?: { from?: string; to?: string }) =>
      api<LeaderboardEntry[]>(`/api/v1/classrooms/${classroomId}/leaderboard`, {
        query: range,
      }),
    scores: (classroomId: number) =>
      api<StudentScoreSummary[]>(`/api/v1/classrooms/${classroomId}/scores`),
  }
}

export function useAcademicApi() {
  const { api } = useApi()
  return {
    subjects: () => api<Subject[]>('/api/v1/subjects'),
    createSubject: (body: { name: string }) =>
      api<Subject>('/api/v1/subjects', { method: 'POST', body }),
    years: () => api<AcademicYear[]>('/api/v1/academic-years'),
    list: (classroomId: number) =>
      api<AcademicRecord[]>(`/api/v1/classrooms/${classroomId}/academic-records`),
    create: (body: AcademicRecordRequest) =>
      api<AcademicRecord>('/api/v1/academic-records', { method: 'POST', body }),
    update: (id: number, body: AcademicRecordRequest) =>
      api<AcademicRecord>(`/api/v1/academic-records/${id}`, { method: 'PUT', body }),
    remove: (id: number) =>
      api<void>(`/api/v1/academic-records/${id}`, { method: 'DELETE' }),
  }
}

export function useTimetableApi() {
  const { api } = useApi()
  return {
    get: (classroomId: number) =>
      api<TimetableEntry[]>(`/api/v1/classrooms/${classroomId}/timetable`),
    save: (classroomId: number, entries: TimetableSlotInput[]) =>
      api<TimetableEntry[]>(`/api/v1/classrooms/${classroomId}/timetable`, {
        method: 'PUT',
        body: { entries },
      }),
  }
}

export function useAnnouncementApi() {
  const { api } = useApi()
  return {
    list: (classroomId: number) =>
      api<Announcement[]>(`/api/v1/classrooms/${classroomId}/announcements`),
    create: (classroomId: number, body: AnnouncementRequest) =>
      api<Announcement>(`/api/v1/classrooms/${classroomId}/announcements`, {
        method: 'POST',
        body,
      }),
    update: (id: number, body: AnnouncementRequest) =>
      api<Announcement>(`/api/v1/announcements/${id}`, { method: 'PUT', body }),
    remove: (id: number) =>
      api<void>(`/api/v1/announcements/${id}`, { method: 'DELETE' }),
  }
}

export function useDashboardApi() {
  const { api } = useApi()
  return {
    summary: () => api<DashboardSummary>('/api/v1/dashboard/summary'),
    classroom: (classroomId: number) =>
      api<DashboardSummary>(`/api/v1/classrooms/${classroomId}/dashboard`),
  }
}

export function useReportApi() {
  const { api } = useApi()
  return {
    summary: (classroomId?: number) =>
      api<ReportSummary>('/api/v1/reports/summary', {
        query: { classroomId },
      }),
    attendance: (classroomId: number, from?: string, to?: string) =>
      api<Record<string, unknown>>('/api/v1/reports/attendance', {
        query: { classroomId, from, to },
      }),
    scores: (classroomId: number, from?: string, to?: string) =>
      api<Record<string, unknown>>('/api/v1/reports/scores', {
        query: { classroomId, from, to },
      }),
    academic: (classroomId: number) =>
      api<Record<string, unknown>>('/api/v1/reports/academic', {
        query: { classroomId },
      }),
    exportExcel: async (
      classroomId: number,
      type: 'attendance' | 'scores',
      range?: { from?: string; to?: string },
    ) => {
      const config = useRuntimeConfig()
      const token = useCookie<string | null>('qlhs_access_token')
      const base = String(config.public.apiBase).replace(/\/$/, '')
      const params = new URLSearchParams({ classroomId: String(classroomId), type })
      if (range?.from) params.set('from', range.from)
      if (range?.to) params.set('to', range.to)
      const res = await fetch(`${base}/api/v1/reports/export?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token.value || ''}` },
      })
      if (!res.ok) throw new Error('Xuất báo cáo thất bại')
      return res.blob()
    },
  }
}
