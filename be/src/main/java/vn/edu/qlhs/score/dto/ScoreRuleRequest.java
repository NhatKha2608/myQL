package vn.edu.qlhs.score.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreRuleRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private Integer pointValue;
    private String category;
    private Boolean active;
}
