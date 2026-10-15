package vn.edu.qlhs.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.announcement.dto.AnnouncementResponse;
import vn.edu.qlhs.announcement.entity.AnnouncementEntity;
import vn.edu.qlhs.announcement.repository.AnnouncementRepository;
import vn.edu.qlhs.attendance.repository.AttendanceRecordRepository;
import vn.edu.qlhs.classroom.entity.ClassroomEntity;
import vn.edu.qlhs.classroom.repository.ClassroomRepository;
import vn.edu.qlhs.common.enums.AttendanceStatus;
import vn.edu.qlhs.common.enums.UserRole;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.score.dto.LeaderboardEntryResponse;
import vn.edu.qlhs.score.service.ScoreService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.timetable.dto.TimetableEntryResponse;
import vn.edu.qlhs.timetable.entity.TimetableEntryEntity;
import vn.edu.qlhs.timetable.repository.TimetableEntryRepository;
import vn.edu.qlhs.timetable.service.TimetableService;
import vn.edu.qlhs.user.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClassroomRepository classroomRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AnnouncementRepository announcementRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final ClassroomAccessService classroomAccessService;
    private final ScoreService scoreService;
    private final TimetableService timetableService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> teacherSummary() {
        UserPrincipal principal = SecurityUtils.currentUser();
        List<ClassroomEntity> classrooms = accessibleClassrooms(principal);
        List<Long> classroomIds = classrooms.stream().map(ClassroomEntity::getId).toList();
        return buildSummary(classroomIds, classrooms, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> classroomSummary(Long classroomId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        ClassroomEntity classroom = classroomAccessService.requireAccessibleClassroom(classroomId, principal);
        return buildSummary(List.of(classroomId), List.of(classroom), classroomId);
    }

    private Map<String, Object> buildSummary(List<Long> classroomIds,
                                            List<ClassroomEntity> classrooms,
                                            Long scopedClassroomId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new LinkedHashMap<>();

        long totalStudents = classroomIds.isEmpty()
                ? 0L
                : classroomRepository.countActiveStudentsInClassrooms(classroomIds);
        result.put("totalStudents", totalStudents);
        result.put("classroomCount", classroomIds.size());

        Map<String, Long> attendanceToday = new LinkedHashMap<>();
        for (AttendanceStatus status : AttendanceStatus.values()) {
            long count = classroomIds.isEmpty()
                    ? 0L
                    : attendanceRecordRepository.countByClassroomIdInAndAttendanceDateAndStatus(
                    classroomIds, today, status);
            attendanceToday.put(status.name(), count);
        }
        result.put("attendanceToday", attendanceToday);

        List<AnnouncementResponse> recentAnnouncements = classroomIds.isEmpty()
                ? List.of()
                : announcementRepository.findByClassroomIdInOrderByCreatedAtDesc(classroomIds).stream()
                .limit(10)
                .map(this::toAnnouncementResponse)
                .toList();
        result.put("recentAnnouncements", recentAnnouncements);

        List<LeaderboardEntryResponse> topScorers = new ArrayList<>();
        if (scopedClassroomId != null) {
            topScorers = scoreService.leaderboard(scopedClassroomId, null, null).stream().limit(5).toList();
        } else {
            for (Long id : classroomIds) {
                topScorers.addAll(scoreService.leaderboard(id, null, null).stream().limit(3).toList());
            }
            topScorers = topScorers.stream()
                    .sorted((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()))
                    .limit(5)
                    .toList();
        }
        result.put("topScorers", topScorers);

        int dayOfWeek = mapDayOfWeek(today.getDayOfWeek());
        List<Map<String, Object>> todayTimetable = new ArrayList<>();
        Map<Long, String> classroomNames = classrooms.stream()
                .collect(Collectors.toMap(ClassroomEntity::getId, ClassroomEntity::getName));
        for (Long classroomId : classroomIds) {
            List<TimetableEntryEntity> entries = timetableEntryRepository
                    .findByClassroomIdAndDayOfWeekOrderByPeriodNumberAsc(classroomId, dayOfWeek);
            for (TimetableEntryEntity entry : entries) {
                TimetableEntryResponse mapped = timetableService.listByClassroom(classroomId).stream()
                        .filter(t -> t.getId().equals(entry.getId()))
                        .findFirst()
                        .orElse(null);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("classroomId", classroomId);
                row.put("classroomName", classroomNames.get(classroomId));
                if (mapped != null) {
                    row.put("entry", mapped);
                } else {
                    row.put("entryId", entry.getId());
                    row.put("subjectId", entry.getSubjectId());
                    row.put("periodNumber", entry.getPeriodNumber());
                    row.put("startTime", entry.getStartTime());
                    row.put("endTime", entry.getEndTime());
                }
                todayTimetable.add(row);
            }
        }
        result.put("todayTimetable", todayTimetable);
        result.put("date", today);
        return result;
    }

    private List<ClassroomEntity> accessibleClassrooms(UserPrincipal principal) {
        if (principal.getRole() == UserRole.ADMIN) {
            return classroomRepository.findByDeletedFalse(
                    org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
        return classroomRepository.findByHomeroomTeacherIdAndDeletedFalse(principal.getId());
    }

    private int mapDayOfWeek(DayOfWeek dayOfWeek) {
        // ISO: Monday=1 ... Sunday=7 — matches typical VN school timetable
        return dayOfWeek.getValue();
    }

    private AnnouncementResponse toAnnouncementResponse(AnnouncementEntity entity) {
        String createdByName = userRepository.findByIdAndDeletedFalse(entity.getCreatedBy())
                .map(u -> u.getFullName())
                .orElse(null);
        return AnnouncementResponse.builder()
                .id(entity.getId())
                .classroomId(entity.getClassroomId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .publishedAt(entity.getPublishedAt())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
