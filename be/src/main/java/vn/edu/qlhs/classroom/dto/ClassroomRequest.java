package vn.edu.qlhs.classroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassroomRequest {
    @NotBlank(message = "Tên lớp không được để trống")
    private String name;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer gradeLevel;

    @NotNull
    private Long academicYearId;

    private String description;

    private String status;
}
