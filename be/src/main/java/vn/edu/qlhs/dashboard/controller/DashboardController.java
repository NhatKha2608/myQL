package vn.edu.qlhs.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qlhs.common.dto.ApiResponse;
import vn.edu.qlhs.dashboard.service.DashboardService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/v1/dashboard/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(dashboardService.teacherSummary());
    }

    @GetMapping("/api/v1/classrooms/{classroomId}/dashboard")
    public ApiResponse<Map<String, Object>> classroomDashboard(@PathVariable Long classroomId) {
        return ApiResponse.ok(dashboardService.classroomSummary(classroomId));
    }
}
