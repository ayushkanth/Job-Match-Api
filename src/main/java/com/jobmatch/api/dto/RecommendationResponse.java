package com.jobmatch.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private Long jobId;
    private String jobTitle;
    private String location;
    private SalaryRangeDto salaryRange;
    private boolean remoteAllowed;
    private double overallScore;
    private ScoreBreakdownDto breakdown;
}
