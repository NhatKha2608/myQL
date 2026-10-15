package vn.edu.qlhs.student.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.qlhs.common.dto.ApiResponse;
import vn.edu.qlhs.common.dto.PageResponse;
import vn.edu.qlhs.student.dto.StudentRequest;
import vn.edu.qlhs.student.dto.StudentResponse;
import vn.edu.qlhs.student.service.StudentService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ApiResponse<PageResponse<StudentResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(studentService.search(keyword, classroomId, status, pageable)));
    }

    @PostMapping
    public ApiResponse<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.create(request));
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Integer>> importExcel(@RequestParam Long classroomId,
                                                         @RequestParam("file") MultipartFile file) throws IOException {
        int count = studentService.importExcel(file, classroomId);
        return ApiResponse.ok(Map.of("imported", count));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] data = studentService.importTemplateExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau-hoc-sinh.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Long classroomId) throws IOException {
        byte[] data = studentService.exportExcel(classroomId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hoc-sinh.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(studentService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudentResponse> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResponse.okMessage("Xóa học sinh thành công");
    }
}
