package com.jobmatch.api.scorer;

public record DimensionScore(double score, double max) {

    public static DimensionScore of(double score, double max) {
        return new DimensionScore(round(score), round(max));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
