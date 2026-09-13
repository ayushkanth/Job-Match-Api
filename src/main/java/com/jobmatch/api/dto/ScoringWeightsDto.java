package com.jobmatch.api.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringWeightsDto {

    @DecimalMin(value = "0.0", message = "Skills weight must be non-negative")
    private Double skills;

    @DecimalMin(value = "0.0", message = "Experience weight must be non-negative")
    private Double experience;

    @DecimalMin(value = "0.0", message = "Location weight must be non-negative")
    private Double location;

    @DecimalMin(value = "0.0", message = "Salary weight must be non-negative")
    private Double salary;
}
