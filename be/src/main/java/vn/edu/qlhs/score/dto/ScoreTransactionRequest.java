package vn.edu.qlhs.score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ScoreTransactionRequest {
    @NotNull
    private Long studentId;
    private Long scoreRuleId;
    /** Required when scoreRuleId is null; ignored when a rule is provided. */
    private Integer pointsDelta;
    @NotBlank
    private String reason;
    private String note;
    private LocalDate transactionDate;
}
