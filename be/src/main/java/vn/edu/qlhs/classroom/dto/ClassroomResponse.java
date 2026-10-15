package vn.edu.qlhs.classroom.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomResponse {
    private Long id;
    private String name;
    private Integer gradeLevel;
    private Long academicYearId;
    private String academicYearName;
    private Long homeroomTeacherId;
    private String homeroomTeacherName;
    private String description;
    private String status;
    private long studentCount;
}
