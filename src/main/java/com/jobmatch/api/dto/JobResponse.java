package com.jobmatch.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private Long id;
    private String title;
    private List<JobSkillDto> requiredSkills;
    private int minYearsExperience;
    private String location;
    private SalaryRangeDto salaryRange;
    private boolean remoteAllowed;
}
