package com.jobmatch.api.scorer;

public record ScoreBreakdown(
        DimensionScore skills,
        DimensionScore experience,
        DimensionScore location,
        DimensionScore salary
) {
    public static ScoreBreakdown empty(ScoringWeights weights) {
        return new ScoreBreakdown(
                DimensionScore.of(0.0, weights.skills()),
                DimensionScore.of(0.0, weights.experience()),
                DimensionScore.of(0.0, weights.location()),
                DimensionScore.of(0.0, weights.salary())
        );
    }
}
