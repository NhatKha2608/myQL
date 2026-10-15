package vn.edu.qlhs.timetable.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TimetableSlotRequest {
    private Long subjectId;

    @Size(max = 255)
    private String subjectName;

    /** Thứ 2 = 1 … Thứ 6 = 5. */
    @NotNull
    @Min(1)
    @Max(5)
    private Integer dayOfWeek;

    /** 1–5 buổi sáng, 6–10 buổi chiều. */
    @NotNull
    @Min(1)
    @Max(10)
    private Integer periodNumber;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;
}
