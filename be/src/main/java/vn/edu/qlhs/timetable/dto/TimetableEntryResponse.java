package vn.edu.qlhs.timetable.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class TimetableEntryResponse {
    private Long id;
    private Long classroomId;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private Integer dayOfWeek;
    private Integer periodNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String note;
}
