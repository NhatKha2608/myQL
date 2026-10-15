package vn.edu.qlhs.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.qlhs.common.enums.AttendanceStatus;

@Data
public class DailyAttendanceItemRequest {
    @NotNull
    private Long studentId;

    @NotNull
    private AttendanceStatus status;

    private String note;
}
