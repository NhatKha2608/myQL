package vn.edu.qlhs.timetable.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.qlhs.common.dto.ApiResponse;
import vn.edu.qlhs.timetable.dto.TimetableEntryRequest;
import vn.edu.qlhs.timetable.dto.TimetableEntryResponse;
import vn.edu.qlhs.timetable.dto.TimetableReplaceRequest;
import vn.edu.qlhs.timetable.service.TimetableService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping("/api/v1/classrooms/{classroomId}/timetable")
    public ApiResponse<List<TimetableEntryResponse>> list(@PathVariable Long classroomId) {
        return ApiResponse.ok(timetableService.listByClassroom(classroomId));
    }

    @PutMapping("/api/v1/classrooms/{classroomId}/timetable")
    public ApiResponse<List<TimetableEntryResponse>> replace(
            @PathVariable Long classroomId,
            @Valid @RequestBody TimetableReplaceRequest request) {
        return ApiResponse.ok("Đã lưu thời khóa biểu", timetableService.replace(classroomId, request));
    }

    @PostMapping("/api/v1/classrooms/{classroomId}/timetable")
    public ApiResponse<TimetableEntryResponse> create(
            @PathVariable Long classroomId,
            @Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.ok(timetableService.create(classroomId, request));
    }

    @PutMapping("/api/v1/timetable/{id}")
    public ApiResponse<TimetableEntryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.ok(timetableService.update(id, request));
    }

    @DeleteMapping("/api/v1/timetable/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        timetableService.delete(id);
        return ApiResponse.okMessage("Xóa tiết học thành công");
    }
}
