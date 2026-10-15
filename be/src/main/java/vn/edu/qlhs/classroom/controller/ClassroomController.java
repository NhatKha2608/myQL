package vn.edu.qlhs.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import vn.edu.qlhs.classroom.dto.ClassroomRequest;
import vn.edu.qlhs.classroom.dto.ClassroomResponse;
import vn.edu.qlhs.classroom.service.ClassroomService;
import vn.edu.qlhs.common.dto.ApiResponse;
import vn.edu.qlhs.common.dto.PageResponse;
import vn.edu.qlhs.student.dto.StudentResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;

    @GetMapping
    public ApiResponse<PageResponse<ClassroomResponse>> list(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(classroomService.list(pageable)));
    }

    @PostMapping
    public ApiResponse<ClassroomResponse> create(@Valid @RequestBody ClassroomRequest request) {
        return ApiResponse.ok(classroomService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClassroomResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(classroomService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ClassroomResponse> update(@PathVariable Long id, @Valid @RequestBody ClassroomRequest request) {
        return ApiResponse.ok(classroomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        classroomService.delete(id);
        return ApiResponse.okMessage("Xóa lớp học thành công");
    }

    @GetMapping("/{id}/students")
    public ApiResponse<List<StudentResponse>> students(@PathVariable Long id) {
        return ApiResponse.ok(classroomService.listStudents(id));
    }
}
