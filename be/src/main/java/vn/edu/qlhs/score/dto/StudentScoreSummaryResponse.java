package vn.edu.qlhs.score.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentScoreSummaryResponse {
    private Long studentId;
    private String studentName;
    private String studentCode;
    private Integer totalPoints;
}
