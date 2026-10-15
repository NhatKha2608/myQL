package vn.edu.qlhs.report.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.academicrecord.repository.AcademicRecordRepository;
import vn.edu.qlhs.attendance.repository.AttendanceRecordRepository;
import vn.edu.qlhs.classroom.entity.ClassroomEntity;
import vn.edu.qlhs.classroom.repository.ClassroomRepository;
import vn.edu.qlhs.common.enums.AttendanceStatus;
import vn.edu.qlhs.common.enums.UserRole;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.score.dto.LeaderboardEntryResponse;
import vn.edu.qlhs.score.service.ScoreService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.student.entity.StudentEntity;
import vn.edu.qlhs.student.repository.ClassroomStudentRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomAccessService classroomAccessService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ScoreService scoreService;
    private final AcademicRecordRepository academicRecordRepository;
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClassroomStudentRepository classroomStudentRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long classroomId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (classroomId != null) {
            classroomAccessService.requireAccessibleClassroom(classroomId, principal);
            return buildClassroomSummary(classroomId);
        }
        List<ClassroomEntity> classrooms = accessibleClassrooms(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classroomCount", classrooms.size());
        List<Map<String, Object>> stats = new ArrayList<>();
        for (ClassroomEntity classroom : classrooms) {
            stats.add(buildClassroomSummary(classroom.getId()));
        }
        result.put("classroomStats", stats);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> attendanceReport(Long classroomId, LocalDate from, LocalDate to) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (AttendanceStatus status : AttendanceStatus.values()) {
            byStatus.put(status.name(), 0L);
        }
        for (Object[] row : attendanceRecordRepository.countByStatus(classroomId, start, end)) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }
        long totalRecords = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long present = byStatus.getOrDefault(AttendanceStatus.PRESENT.name(), 0L)
                + byStatus.getOrDefault(AttendanceStatus.LATE.name(), 0L);
        double rate = totalRecords == 0 ? 0 : (present * 100.0 / totalRecords);
        return Map.of(
                "classroomId", classroomId,
                "from", start,
                "to", end,
                "byStatus", byStatus,
                "totalRecords", totalRecords,
                "attendanceRate", Math.round(rate * 100.0) / 100.0
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> scoresReport(Long classroomId, LocalDate from, LocalDate to) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        List<LeaderboardEntryResponse> board = scoreService.leaderboard(classroomId, from, to);
        return Map.of(
                "classroomId", classroomId,
                "from", from,
                "to", to,
                "leaderboard", board
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> academicReport(Long classroomId) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        long count = academicRecordRepository.findByClassroomId(classroomId).size();
        return Map.of("classroomId", classroomId, "totalRecords", count);
    }

    @Transactional(readOnly = true)
    public byte[] export(Long classroomId, String type, LocalDate from, LocalDate to) throws IOException {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        String reportType = type != null ? type.toLowerCase(Locale.ROOT) : "attendance";
        LocalDate start = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate end = to != null ? to : LocalDate.now();
        if (start.isAfter(end)) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            switch (reportType) {
                case "scores" -> writeScoresSheet(workbook, classroomId, start, end);
                case "attendance" -> writeAttendanceSheet(workbook, classroomId, start, end);
                default -> throw new BusinessException("Loại báo cáo không hợp lệ");
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private Map<String, Object> buildClassroomSummary(Long classroomId) {
        ClassroomEntity classroom = classroomRepository.findByIdAndDeletedFalse(classroomId).orElseThrow();
        List<StudentEntity> students = classroomStudentRepository.findActiveStudentsByClassroom(classroomId);
        List<LeaderboardEntryResponse> top = scoreService.leaderboard(classroomId, null, null);
        double avgScore = top.isEmpty()
                ? 0
                : top.stream().mapToInt(LeaderboardEntryResponse::getTotalPoints).average().orElse(0);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("classroomId", classroomId);
        row.put("classroomName", classroom.getName());
        row.put("studentCount", students.size());
        row.put("avgScore", Math.round(avgScore * 100.0) / 100.0);
        return row;
    }

    private void writeAttendanceSheet(Workbook workbook, Long classroomId, LocalDate from, LocalDate to) {
        Sheet sheet = workbook.createSheet("Điểm danh");
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("Báo cáo điểm danh");
        Row period = sheet.createRow(1);
        period.createCell(0).setCellValue("Từ ngày " + from.format(VN_DATE) + " đến ngày " + to.format(VN_DATE));
        Row header = sheet.createRow(3);
        header.createCell(0).setCellValue("Ngày");
        header.createCell(1).setCellValue("Mã học sinh");
        header.createCell(2).setCellValue("Họ tên");
        header.createCell(3).setCellValue("Trạng thái");
        header.createCell(4).setCellValue("Ghi chú");
        int r = 4;
        var records = attendanceRecordRepository
                .findByClassroomIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(classroomId, from, to);
        Map<Long, StudentEntity> students = new HashMap<>();
        for (StudentEntity s : classroomStudentRepository.findActiveStudentsByClassroom(classroomId)) {
            students.put(s.getId(), s);
        }
        for (var rec : records) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(rec.getAttendanceDate().format(VN_DATE));
            StudentEntity st = students.get(rec.getStudentId());
            row.createCell(1).setCellValue(st != null && st.getStudentCode() != null ? st.getStudentCode() : "");
            row.createCell(2).setCellValue(st != null ? st.getFullName() : "");
            row.createCell(3).setCellValue(attendanceStatusLabel(rec.getStatus()));
            row.createCell(4).setCellValue(rec.getNote() != null ? rec.getNote() : "");
        }
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeScoresSheet(Workbook workbook, Long classroomId, LocalDate from, LocalDate to) {
        Sheet sheet = workbook.createSheet("Thi đua");
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("Báo cáo điểm thi đua");
        Row period = sheet.createRow(1);
        period.createCell(0).setCellValue("Từ ngày " + from.format(VN_DATE) + " đến ngày " + to.format(VN_DATE));
        Row header = sheet.createRow(3);
        header.createCell(0).setCellValue("Hạng");
        header.createCell(1).setCellValue("Mã học sinh");
        header.createCell(2).setCellValue("Họ tên");
        header.createCell(3).setCellValue("Tổng điểm");
        int r = 4;
        for (LeaderboardEntryResponse entry : scoreService.leaderboard(classroomId, from, to)) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(entry.getRank());
            row.createCell(1).setCellValue(entry.getStudentCode() != null ? entry.getStudentCode() : "");
            row.createCell(2).setCellValue(entry.getStudentName() != null ? entry.getStudentName() : "");
            row.createCell(3).setCellValue(entry.getTotalPoints() != null ? entry.getTotalPoints() : 0);
        }
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String attendanceStatusLabel(AttendanceStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PRESENT -> "Có mặt";
            case LATE -> "Đi muộn";
            case ABSENT_EXCUSED -> "Vắng có phép";
            case ABSENT_UNEXCUSED -> "Vắng không phép";
        };
    }

    private List<ClassroomEntity> accessibleClassrooms(UserPrincipal principal) {
        if (principal.getRole() == UserRole.ADMIN) {
            return classroomRepository.findByDeletedFalse(
                    org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
        return classroomRepository.findByHomeroomTeacherIdAndDeletedFalse(principal.getId());
    }
}
