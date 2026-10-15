export interface ApiResponse<T = unknown> {
  success: boolean
  message: string
  data: T
  timestamp?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type UserRole = 'ADMIN' | 'TEACHER' | 'PARENT' | string
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | string

export interface UserProfile {
  id: number
  fullName: string
  email: string
  username: string
  role: UserRole
  status: UserStatus
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: UserProfile
}

export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface Classroom {
  id: number
  name: string
  gradeLevel: number
  academicYearId: number
  academicYearName?: string
  homeroomTeacherId?: number
  homeroomTeacherName?: string
  description?: string
  status?: string
  studentCount: number
}

export interface ClassroomRequest {
  name: string
  gradeLevel: number
  academicYearId: number
  description?: string
  status?: string
}

export interface Student {
  id: number
  studentCode: string
  fullName: string
  dateOfBirth?: string
  gender?: string
  avatarUrl?: string
  address?: string
  status?: string
  classroomId?: number
  classroomName?: string
}

export interface StudentRequest {
  studentCode?: string
  fullName: string
  dateOfBirth?: string | null
  gender?: string
  avatarUrl?: string
  address?: string
  status?: string
  classroomId?: number | null
}

export type AttendanceStatus =
  | 'PRESENT'
  | 'ABSENT_EXCUSED'
  | 'ABSENT_UNEXCUSED'
  | 'LATE'

export interface AttendanceRecord {
  id?: number
  studentId: number
  studentName?: string
  studentCode?: string
  dateOfBirth?: string
  attendanceDate?: string
  status: AttendanceStatus
  note?: string
}

export interface DailyAttendanceItem {
  studentId: number
  status: AttendanceStatus
  note?: string
}

export interface DailyAttendanceSaveRequest {
  date: string
  items: DailyAttendanceItem[]
}

export interface ScoreRule {
  id: number
  classroomId: number
  name: string
  description?: string
  pointValue: number
  category?: string
  active: boolean
}

export interface ScoreRuleRequest {
  name: string
  description?: string
  pointValue: number
  category?: string
  active?: boolean
}

export interface ScoreTransaction {
  id: number
  studentId: number
  studentName?: string
  pointsDelta: number
  reason: string
  note?: string
  transactionDate?: string
  scoreRuleId?: number
  scoreRuleName?: string
  createdBy?: number
  createdByName?: string
  createdAt?: string
  reversedTransactionId?: number
  reversed: boolean
}

export interface ScoreTransactionRequest {
  studentId: number
  scoreRuleId?: number | null
  pointsDelta?: number | null
  reason: string
  note?: string
  transactionDate?: string
}

export interface LeaderboardEntry {
  studentId: number
  studentName: string
  studentCode?: string
  totalPoints: number
  rank?: number
}

export interface StudentScoreSummary {
  studentId: number
  studentName: string
  studentCode?: string
  totalPoints: number
}

export interface Subject {
  id: number
  code: string
  name: string
  description?: string
  active: boolean
}

export interface AcademicRecord {
  id: number
  classroomId: number
  studentId: number
  studentName?: string
  subjectId: number
  subjectName?: string
  evaluationPeriod: string
  evaluationType: string
  scoreValue?: number | null
  gradeLevelText?: string
  commentText?: string
  evaluatedAt: string
  createdBy?: number
}

export interface AcademicRecordRequest {
  classroomId: number
  studentId: number
  subjectId: number
  evaluationPeriod: string
  evaluationType: string
  scoreValue?: number | null
  gradeLevelText?: string
  commentText?: string
  evaluatedAt: string
}

export interface TimetableEntry {
  id: number
  classroomId: number
  subjectId: number
  subjectName?: string
  dayOfWeek: number
  periodNumber: number
  startTime: string
  endTime: string
  room?: string
  note?: string
}

export interface TimetableSlotInput {
  subjectId?: number | null
  subjectName?: string
  dayOfWeek: number
  periodNumber: number
  startTime: string
  endTime: string
}

export interface Announcement {
  id: number
  classroomId: number
  title: string
  content: string
  publishedAt?: string
  status: string
  createdBy?: number
  createdByName?: string
  createdAt?: string
}

export interface AnnouncementRequest {
  title: string
  content: string
  status?: string
}

export interface AcademicYear {
  id: number
  name: string
  startDate?: string
  endDate?: string
  status?: string
}

export interface DashboardSummary {
  classroomCount?: number
  studentCount?: number
  presentToday?: number
  absentToday?: number
  announcementsToday?: number
  recentAnnouncements?: Announcement[]
  attendanceTrend?: { date: string; present: number; absent: number }[]
  topStudents?: LeaderboardEntry[]
}

export interface ReportSummary {
  attendanceByStatus?: Record<string, number>
  scoreDistribution?: { label: string; value: number }[]
  monthlyAttendance?: { month: string; rate: number }[]
  classroomStats?: { classroomName: string; studentCount: number; avgScore: number }[]
}
