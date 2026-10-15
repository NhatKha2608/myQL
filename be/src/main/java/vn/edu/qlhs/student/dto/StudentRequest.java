package vn.edu.qlhs.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentRequest {
    /** Để trống khi thêm mới: hệ thống tự gán một số duy nhất. */
    private String studentCode;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private LocalDate dateOfBirth;
    private String gender;
    private String avatarUrl;
    private String address;
    private String status;

    /** Ghi danh vào lớp khi tạo/cập nhật (tùy chọn) */
    private Long classroomId;
}
