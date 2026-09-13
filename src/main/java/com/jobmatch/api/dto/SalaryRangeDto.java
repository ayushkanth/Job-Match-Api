package com.jobmatch.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRangeDto {

    @NotNull(message = "Minimum salary must not be null")
    @Min(value = 0, message = "Minimum salary must be non-negative")
    private Integer min;

    @NotNull(message = "Maximum salary must not be null")
    @Min(value = 0, message = "Maximum salary must be non-negative")
    private Integer max;
}
