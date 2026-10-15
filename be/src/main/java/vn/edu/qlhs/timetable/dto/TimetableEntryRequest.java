package vn.edu.qlhs.timetable.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class TimetableEntryRequest {
    @NotNull
    private Long subjectId;
    @NotNull
    @Min(1)
    @Max(7)
    private Integer dayOfWeek;
    @NotNull
    @Min(1)
    private Integer periodNumber;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    private String room;
    private String note;
}
