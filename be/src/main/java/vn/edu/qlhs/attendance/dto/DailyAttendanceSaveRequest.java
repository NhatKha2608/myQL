package vn.edu.qlhs.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyAttendanceSaveRequest {
    @NotNull
    private LocalDate date;

    @NotEmpty
    @Valid
    private List<DailyAttendanceItemRequest> items;
}
