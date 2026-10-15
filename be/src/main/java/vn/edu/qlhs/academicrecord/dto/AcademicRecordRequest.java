package vn.edu.qlhs.academicrecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AcademicRecordRequest {
    @NotNull
    private Long classroomId;
    @NotNull
    private Long studentId;
    @NotNull
    private Long subjectId;
    @NotBlank
    private String evaluationPeriod;
    @NotBlank
    private String evaluationType;
    private BigDecimal scoreValue;
    private String gradeLevelText;
    private String commentText;
    @NotNull
    private LocalDate evaluatedAt;
}
