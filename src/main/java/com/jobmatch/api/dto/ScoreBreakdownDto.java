package com.jobmatch.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdownDto {

    private DimensionScoreDto skills;
    private DimensionScoreDto experience;
    private DimensionScoreDto location;
    private DimensionScoreDto salary;
}
