package com.jobmatch.api.dto;

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
public class CandidateRequest {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotEmpty(message = "Skills list must not be empty")
    private List<@NotBlank(message = "Skill must not be blank") String> skills;

    @NotNull(message = "Years of experience must not be null")
    @Min(value = 0, message = "Years of experience must be non-negative")
    private Integer yearsOfExperience;

    @NotBlank(message = "Location must not be blank")
    private String location;

    @NotNull(message = "Expected salary must not be null")
    @Min(value = 0, message = "Expected salary must be non-negative")
    private Integer expectedSalary;
}
