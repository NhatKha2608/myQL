package vn.edu.qlhs.academicrecord.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class AcademicRecordResponse {
    private Long id;
    private Long classroomId;
    private Long studentId;
    private String studentName;
    private String studentCode;
    private Long subjectId;
    private String subjectName;
    private String evaluationPeriod;
    private String evaluationType;
    private BigDecimal scoreValue;
    private String gradeLevelText;
    private String commentText;
    private LocalDate evaluatedAt;
    private Long createdBy;
    private Instant createdAt;
}
