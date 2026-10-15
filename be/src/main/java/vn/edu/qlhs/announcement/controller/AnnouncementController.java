package vn.edu.qlhs.announcement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.qlhs.announcement.dto.AnnouncementRequest;
import vn.edu.qlhs.announcement.dto.AnnouncementResponse;
import vn.edu.qlhs.announcement.service.AnnouncementService;
import vn.edu.qlhs.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/api/v1/classrooms/{classroomId}/announcements")
    public ApiResponse<List<AnnouncementResponse>> list(@PathVariable Long classroomId) {
        return ApiResponse.ok(announcementService.listByClassroom(classroomId));
    }

    @PostMapping("/api/v1/classrooms/{classroomId}/announcements")
    public ApiResponse<AnnouncementResponse> create(
            @PathVariable Long classroomId,
            @Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(announcementService.create(classroomId, request));
    }

    @PutMapping("/api/v1/announcements/{id}")
    public ApiResponse<AnnouncementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(announcementService.update(id, request));
    }

    @DeleteMapping("/api/v1/announcements/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ApiResponse.okMessage("Xóa thông báo thành công");
    }
}
