package vn.edu.qlhs.academicrecord.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.qlhs.common.entity.BaseSoftDeleteEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "academic_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicRecordEntity extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "evaluation_period", nullable = false)
    private String evaluationPeriod;

    @Column(name = "evaluation_type", nullable = false)
    private String evaluationType;

    @Column(name = "score_value", precision = 5, scale = 2)
    private BigDecimal scoreValue;

    @Column(name = "grade_level_text")
    private String gradeLevelText;

    @Column(name = "comment_text", columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDate evaluatedAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
