package vn.edu.qlhs.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qlhs.common.dto.ApiResponse;
import vn.edu.qlhs.report.service.ReportService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/api/v1/reports/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam(required = false) Long classroomId) {
        return ApiResponse.ok(reportService.summary(classroomId));
    }

    @GetMapping("/api/v1/reports/attendance")
    public ApiResponse<Map<String, Object>> attendance(
            @RequestParam Long classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.attendanceReport(classroomId, from, to));
    }

    @GetMapping("/api/v1/reports/scores")
    public ApiResponse<Map<String, Object>> scores(
            @RequestParam Long classroomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.scoresReport(classroomId, from, to));
    }

    @GetMapping("/api/v1/reports/academic")
    public ApiResponse<Map<String, Object>> academic(@RequestParam Long classroomId) {
        return ApiResponse.ok(reportService.academicReport(classroomId));
    }

    @GetMapping("/api/v1/reports/export")
    public ResponseEntity<byte[]> export(
            @RequestParam Long classroomId,
            @RequestParam(defaultValue = "attendance") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws IOException {
        byte[] data = reportService.export(classroomId, type, from, to);
        String filename = "scores".equalsIgnoreCase(type) ? "bao-cao-thi-dua.xlsx" : "bao-cao-diem-danh.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
