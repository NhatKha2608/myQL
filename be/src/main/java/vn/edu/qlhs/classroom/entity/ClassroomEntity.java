package vn.edu.qlhs.classroom.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.qlhs.common.entity.BaseSoftDeleteEntity;

@Entity
@Table(name = "classrooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomEntity extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "homeroom_teacher_id", nullable = false)
    private Long homeroomTeacherId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status;
}
