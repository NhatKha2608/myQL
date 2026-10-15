package vn.edu.qlhs.score.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ScoreTransactionResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private Integer pointsDelta;
    private String reason;
    private String note;
    private LocalDate transactionDate;
    private Long scoreRuleId;
    private String scoreRuleName;
    private Long createdBy;
    private String createdByName;
    private Instant createdAt;
    private Long reversedTransactionId;
    private boolean reversed;
}
