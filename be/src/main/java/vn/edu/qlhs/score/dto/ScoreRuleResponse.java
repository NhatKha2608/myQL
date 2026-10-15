package vn.edu.qlhs.score.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ScoreRuleResponse {
    private Long id;
    private Long classroomId;
    private String name;
    private String description;
    private Integer pointValue;
    private String category;
    private boolean active;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
