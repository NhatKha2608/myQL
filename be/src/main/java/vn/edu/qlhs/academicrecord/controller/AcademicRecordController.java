package vn.edu.qlhs.academicrecord.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.qlhs.academicrecord.dto.AcademicRecordRequest;
import vn.edu.qlhs.academicrecord.dto.AcademicRecordResponse;
import vn.edu.qlhs.academicrecord.service.AcademicRecordService;
import vn.edu.qlhs.common.dto.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AcademicRecordController {

    private final AcademicRecordService academicRecordService;

    @GetMapping("/api/v1/students/{studentId}/academic-records")
    public ApiResponse<List<AcademicRecordResponse>> listByStudent(@PathVariable Long studentId) {
        return ApiResponse.ok(academicRecordService.listByStudent(studentId));
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/academic-records")
    public ApiResponse<List<AcademicRecordResponse>> listByClassroom(@PathVariable Long classroomId) {
        return ApiResponse.ok(academicRecordService.listByClassroom(classroomId));
    }

    @PostMapping("/api/v1/academic-records")
    public ApiResponse<AcademicRecordResponse> create(@Valid @RequestBody AcademicRecordRequest request) {
        return ApiResponse.ok(academicRecordService.create(request));
    }

    @PutMapping("/api/v1/academic-records/{id}")
    public ApiResponse<AcademicRecordResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AcademicRecordRequest request) {
        return ApiResponse.ok(academicRecordService.update(id, request));
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/academic-statistics")
    public ApiResponse<Map<String, Object>> statistics(@PathVariable Long classroomId) {
        return ApiResponse.ok(academicRecordService.classroomStatistics(classroomId));
    }
}
