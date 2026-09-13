package com.jobmatch.api.scorer;

public record MatchResult(
        boolean eligible,
        double overallScore,
        ScoreBreakdown breakdown
) {
    public static MatchResult ineligible(ScoringWeights weights) {
        return new MatchResult(false, 0.0, ScoreBreakdown.empty(weights));
    }

    public static MatchResult eligible(double overallScore, ScoreBreakdown breakdown) {
        double roundedScore = Math.round(overallScore * 100.0) / 100.0;
        return new MatchResult(true, Math.min(100.0, Math.max(0.0, roundedScore)), breakdown);
    }
}
