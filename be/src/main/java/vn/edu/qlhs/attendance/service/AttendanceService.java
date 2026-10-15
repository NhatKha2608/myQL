package vn.edu.qlhs.attendance.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.attendance.dto.AttendanceRecordResponse;
import vn.edu.qlhs.attendance.dto.DailyAttendanceItemRequest;
import vn.edu.qlhs.attendance.dto.DailyAttendanceSaveRequest;
import vn.edu.qlhs.attendance.entity.AttendanceRecordEntity;
import vn.edu.qlhs.attendance.repository.AttendanceRecordRepository;
import vn.edu.qlhs.audit.service.AuditLogService;
import vn.edu.qlhs.common.enums.AttendanceStatus;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.common.service.ClassroomAccessService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.student.entity.StudentEntity;
import vn.edu.qlhs.student.repository.ClassroomStudentRepository;
import vn.edu.qlhs.student.repository.StudentRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final StudentRepository studentRepository;
    private final ClassroomAccessService classroomAccessService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> listByClassroom(Long classroomId, LocalDate from, LocalDate to) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        LocalDate start = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate end = to != null ? to : LocalDate.now();
        validateDateRange(start, end);
        return attendanceRecordRepository
                .findByClassroomIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(classroomId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> daily(Long classroomId, LocalDate date) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        LocalDate target = date != null ? date : LocalDate.now();
        Map<Long, AttendanceRecordEntity> existing = attendanceRecordRepository
                .findByClassroomIdAndAttendanceDate(classroomId, target).stream()
                .collect(Collectors.toMap(AttendanceRecordEntity::getStudentId, r -> r));
        List<StudentEntity> students = classroomStudentRepository.findActiveStudentsByClassroom(classroomId);
        List<AttendanceRecordResponse> result = new ArrayList<>();
        for (StudentEntity student : students) {
            AttendanceRecordEntity record = existing.get(student.getId());
            if (record != null) {
                result.add(toResponse(record, student));
            } else {
                result.add(AttendanceRecordResponse.builder()
                        .studentId(student.getId())
                        .studentName(student.getFullName())
                        .studentCode(student.getStudentCode())
                        .dateOfBirth(student.getDateOfBirth())
                        .attendanceDate(target)
                        .status(AttendanceStatus.PRESENT)
                        .build());
            }
        }
        return result;
    }

    @Transactional
    public List<AttendanceRecordResponse> saveDaily(Long classroomId, DailyAttendanceSaveRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        classroomAccessService.requireAccessibleClassroom(classroomId, principal);
        Set<Long> studentIds = classroomStudentRepository.findActiveStudentsByClassroom(classroomId).stream()
                .map(StudentEntity::getId)
                .collect(Collectors.toSet());
        for (DailyAttendanceItemRequest item : request.getItems()) {
            if (!studentIds.contains(item.getStudentId())) {
                throw new BusinessException("Học sinh không thuộc lớp: " + item.getStudentId());
            }
        }
        List<AttendanceRecordResponse> saved = new ArrayList<>();
        for (DailyAttendanceItemRequest item : request.getItems()) {
            AttendanceRecordEntity entity = attendanceRecordRepository
                    .findByClassroomIdAndStudentIdAndAttendanceDate(classroomId, item.getStudentId(), request.getDate())
                    .orElse(AttendanceRecordEntity.builder()
                            .classroomId(classroomId)
                            .studentId(item.getStudentId())
                            .attendanceDate(request.getDate())
                            .recordedBy(principal.getId())
                            .build());
            if (!entity.getClassroomId().equals(classroomId)) {
                throw new BusinessException("Không thể ghi đè điểm danh của lớp khác");
            }
            entity.setStatus(item.getStatus());
            entity.setNote(item.getNote());
            entity.setRecordedBy(principal.getId());
            entity = attendanceRecordRepository.save(entity);
            StudentEntity student = studentRepository.findById(item.getStudentId()).orElseThrow();
            saved.add(toResponse(entity, student));
        }
        auditLogService.log(principal.getId(), "SAVE_ATTENDANCE", "CLASSROOM", classroomId,
                "Ngày " + request.getDate(), true);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> studentHistory(Long studentId, LocalDate from, LocalDate to) {
        assertStudentAccess(studentId);
        LocalDate start = from != null ? from : LocalDate.now().minusMonths(3);
        LocalDate end = to != null ? to : LocalDate.now();
        validateDateRange(start, end);
        return attendanceRecordRepository
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(studentId, start, end)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> statistics(Long classroomId, LocalDate from, LocalDate to) {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now();
        validateDateRange(start, end);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (AttendanceStatus status : AttendanceStatus.values()) {
            counts.put(status.name(), 0L);
        }
        for (Object[] row : attendanceRecordRepository.countByStatus(classroomId, start, end)) {
            counts.put(row[0].toString(), (Long) row[1]);
        }
        return Map.of("from", start, "to", end, "byStatus", counts);
    }

    @Transactional(readOnly = true)
    public byte[] exportMonthExcel(Long classroomId, YearMonth month) throws IOException {
        classroomAccessService.requireAccessibleClassroom(classroomId, SecurityUtils.currentUser());
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<StudentEntity> roster = new ArrayList<>(classroomStudentRepository.findActiveStudentsByClassroom(classroomId));
        List<AttendanceRecordEntity> records = attendanceRecordRepository
                .findByClassroomIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(classroomId, start, end);
        Map<Long, Map<LocalDate, AttendanceRecordEntity>> byStudent = new HashMap<>();
        Set<Long> seen = roster.stream().map(StudentEntity::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        for (AttendanceRecordEntity record : records) {
            byStudent.computeIfAbsent(record.getStudentId(), id -> new HashMap<>())
                    .put(record.getAttendanceDate(), record);
            if (seen.add(record.getStudentId())) {
                studentRepository.findById(record.getStudentId()).ifPresent(roster::add);
            }
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Thang " + month);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = gridStyle(workbook, true, IndexedColors.GREY_25_PERCENT.getIndex());
            CellStyle weekendHeaderStyle = gridStyle(workbook, true, IndexedColors.LIGHT_GREEN.getIndex());
            CellStyle textStyle = gridStyle(workbook, false, null);
            textStyle.setAlignment(HorizontalAlignment.LEFT);
            CellStyle centerStyle = gridStyle(workbook, false, null);
            CellStyle weekendStyle = gridStyle(workbook, false, IndexedColors.LIGHT_GREEN.getIndex());

            int lastCol = 1 + end.getDayOfMonth();
            Row title = sheet.createRow(0);
            Cell titleCell = title.createCell(0);
            titleCell.setCellValue("Lịch sử điểm danh tháng " + month.getMonthValue() + "/" + month.getYear());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastCol));

            Row legend = sheet.createRow(1);
            legend.createCell(0).setCellValue("C: Có mặt    M: Muộn    P: Có phép    K: Không phép");

            Row dayRow = sheet.createRow(3);
            Row weekdayRow = sheet.createRow(4);
            Cell sttHeader = dayRow.createCell(0);
            sttHeader.setCellValue("STT");
            sttHeader.setCellStyle(headerStyle);
            weekdayRow.createCell(0).setCellStyle(headerStyle);
            Cell nameHeader = dayRow.createCell(1);
            nameHeader.setCellValue("Học sinh");
            nameHeader.setCellStyle(headerStyle);
            weekdayRow.createCell(1).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 4, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(3, 4, 1, 1));

            for (int day = 1; day <= end.getDayOfMonth(); day++) {
                LocalDate date = month.atDay(day);
                boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                CellStyle style = weekend ? weekendHeaderStyle : headerStyle;
                Cell dayCell = dayRow.createCell(1 + day);
                dayCell.setCellValue(day);
                dayCell.setCellStyle(style);
                Cell weekdayCell = weekdayRow.createCell(1 + day);
                weekdayCell.setCellValue(weekdayLabel(date));
                weekdayCell.setCellStyle(style);
            }

            int rowIndex = 5;
            int order = 1;
            for (StudentEntity student : roster) {
                Row row = sheet.createRow(rowIndex++);
                Cell orderCell = row.createCell(0);
                orderCell.setCellValue(order++);
                orderCell.setCellStyle(centerStyle);
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(student.getFullName() != null ? student.getFullName() : "");
                nameCell.setCellStyle(textStyle);
                Map<LocalDate, AttendanceRecordEntity> days = byStudent.getOrDefault(student.getId(), Map.of());
                for (int day = 1; day <= end.getDayOfMonth(); day++) {
                    LocalDate date = month.atDay(day);
                    boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                    Cell cell = row.createCell(1 + day);
                    AttendanceRecordEntity record = days.get(date);
                    cell.setCellValue(record == null ? "—" : statusMark(record.getStatus()));
                    cell.setCellStyle(weekend ? weekendStyle : centerStyle);
                }
            }

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 28 * 256);
            for (int day = 1; day <= end.getDayOfMonth(); day++) {
                sheet.setColumnWidth(1 + day, 5 * 256);
            }
            sheet.createFreezePane(2, 5);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static CellStyle gridStyle(Workbook workbook, boolean bold, Short fill) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        if (bold) {
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
        }
        if (fill != null) {
            style.setFillForegroundColor(fill);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    private static String weekdayLabel(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    private static String statusMark(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "C";
            case LATE -> "M";
            case ABSENT_EXCUSED -> "P";
            case ABSENT_UNEXCUSED -> "K";
        };
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        if (start.plusDays(366).isBefore(end)) {
            throw new BusinessException("Khoảng thời gian tối đa 366 ngày");
        }
    }

    private void assertStudentAccess(Long studentId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getRole() == vn.edu.qlhs.common.enums.UserRole.ADMIN) {
            return;
        }
        var enrollment = classroomStudentRepository.findFirstByStudentIdAndStatusAndDeletedFalseOrderByEnrolledAtDesc(studentId, "ACTIVE");
        if (enrollment.isEmpty()) {
            throw new BusinessException("Không tìm thấy lớp của học sinh");
        }
        classroomAccessService.requireAccessibleClassroom(enrollment.get().getClassroomId(), principal);
    }

    private AttendanceRecordResponse toResponse(AttendanceRecordEntity entity) {
        StudentEntity student = studentRepository.findById(entity.getStudentId()).orElse(null);
        return toResponse(entity, student);
    }

    private AttendanceRecordResponse toResponse(AttendanceRecordEntity entity, StudentEntity student) {
        return AttendanceRecordResponse.builder()
                .id(entity.getId())
                .studentId(entity.getStudentId())
                .studentName(student != null ? student.getFullName() : null)
                .studentCode(student != null ? student.getStudentCode() : null)
                .dateOfBirth(student != null ? student.getDateOfBirth() : null)
                .attendanceDate(entity.getAttendanceDate())
                .status(entity.getStatus())
                .note(entity.getNote())
                .build();
    }
}
