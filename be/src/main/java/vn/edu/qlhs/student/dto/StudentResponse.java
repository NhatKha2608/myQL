package vn.edu.qlhs.student.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class StudentResponse {
    private Long id;
    private String studentCode;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String avatarUrl;
    private String address;
    private String status;
    private Long classroomId;
    private String classroomName;
}
