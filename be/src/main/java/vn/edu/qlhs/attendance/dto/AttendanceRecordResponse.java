package vn.edu.qlhs.attendance.dto;

import lombok.Builder;
import lombok.Data;
import vn.edu.qlhs.common.enums.AttendanceStatus;

import java.time.LocalDate;

@Data
@Builder
public class AttendanceRecordResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentCode;
    private LocalDate dateOfBirth;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private String note;
}
