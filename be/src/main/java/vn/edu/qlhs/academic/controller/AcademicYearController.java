package vn.edu.qlhs.academic.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qlhs.academic.dto.AcademicYearResponse;
import vn.edu.qlhs.academic.service.AcademicYearService;
import vn.edu.qlhs.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping
    public ApiResponse<List<AcademicYearResponse>> list() {
        return ApiResponse.ok(academicYearService.listAll());
    }
}
