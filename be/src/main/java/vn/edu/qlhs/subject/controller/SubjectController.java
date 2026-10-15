package vn.edu.qlhs.subject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qlhs.common.dto.ApiResponse;
import vn.edu.qlhs.subject.dto.SubjectRequest;
import vn.edu.qlhs.subject.dto.SubjectResponse;
import vn.edu.qlhs.subject.service.SubjectService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ApiResponse<List<SubjectResponse>> list() {
        return ApiResponse.ok(subjectService.listActive());
    }

    @PostMapping
    public ApiResponse<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok("Đã thêm môn học", subjectService.create(request));
    }
}
