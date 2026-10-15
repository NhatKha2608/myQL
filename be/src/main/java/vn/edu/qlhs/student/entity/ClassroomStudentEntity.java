package vn.edu.qlhs.student.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.qlhs.common.entity.BaseSoftDeleteEntity;

import java.time.LocalDate;

@Entity
@Table(name = "classroom_students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomStudentEntity extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDate enrolledAt;

    @Column(name = "left_at")
    private LocalDate leftAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
