package com.jobmatch.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotEmpty(message = "Required skills must not be empty")
    @Valid
    private List<JobSkillDto> requiredSkills;

    @NotNull(message = "Minimum years of experience must not be null")
    @Min(value = 0, message = "Minimum years of experience must be non-negative")
    private Integer minYearsExperience;

    @NotBlank(message = "Location must not be blank")
    private String location;

    @NotNull(message = "Salary range must not be null")
    @Valid
    private SalaryRangeDto salaryRange;

    private boolean remoteAllowed;
}
