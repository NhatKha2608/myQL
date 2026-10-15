package vn.edu.qlhs.attendance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.qlhs.attendance.dto.AttendanceRecordResponse;
import vn.edu.qlhs.attendance.dto.DailyAttendanceSaveRequest;
import vn.edu.qlhs.attendance.service.AttendanceService;
import vn.edu.qlhs.common.dto.ApiResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/api/v1/classrooms/{classroomId}/attendance")
    public ApiResponse<List<AttendanceRecordResponse>> list(
            @PathVariable Long classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceService.listByClassroom(classroomId, from, to));
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/attendance/daily")
    public ApiResponse<List<AttendanceRecordResponse>> daily(
            @PathVariable Long classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(attendanceService.daily(classroomId, date));
    }

    @PutMapping("/api/v1/classrooms/{classroomId}/attendance/daily")
    public ApiResponse<List<AttendanceRecordResponse>> saveDaily(
            @PathVariable Long classroomId,
            @Valid @RequestBody DailyAttendanceSaveRequest request) {
        return ApiResponse.ok(attendanceService.saveDaily(classroomId, request));
    }

    @GetMapping("/api/v1/students/{studentId}/attendance-history")
    public ApiResponse<List<AttendanceRecordResponse>> history(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceService.studentHistory(studentId, from, to));
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/attendance/export")
    public ResponseEntity<byte[]> exportMonth(
            @PathVariable Long classroomId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) throws IOException {
        byte[] data = attendanceService.exportMonthExcel(classroomId, month);
        String filename = "lich-su-diem-danh-" + month + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/attendance/statistics")
    public ApiResponse<Map<String, Object>> statistics(
            @PathVariable Long classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceService.statistics(classroomId, from, to));
    }
}
