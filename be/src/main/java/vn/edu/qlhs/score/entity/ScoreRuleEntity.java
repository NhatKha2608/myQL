package vn.edu.qlhs.score.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.qlhs.common.entity.BaseSoftDeleteEntity;

@Entity
@Table(name = "score_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreRuleEntity extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "point_value", nullable = false)
    private Integer pointValue;

    private String category;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
