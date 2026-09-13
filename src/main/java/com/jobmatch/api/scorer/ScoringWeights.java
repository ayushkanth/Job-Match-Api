package com.jobmatch.api.scorer;

public record ScoringWeights(
        double skills,
        double experience,
        double location,
        double salary
) {
    public static final double DEFAULT_SKILLS = 50.0;
    public static final double DEFAULT_EXPERIENCE = 20.0;
    public static final double DEFAULT_LOCATION = 15.0;
    public static final double DEFAULT_SALARY = 15.0;

    public static ScoringWeights defaults() {
        return new ScoringWeights(
                DEFAULT_SKILLS,
                DEFAULT_EXPERIENCE,
                DEFAULT_LOCATION,
                DEFAULT_SALARY
        );
    }

    public ScoringWeights normalize() {
        double sum = skills + experience + location + salary;
        if (sum <= 0.0) {
            return defaults();
        }
        if (Math.abs(sum - 100.0) < 0.0001) {
            return this;
        }
        double factor = 100.0 / sum;
        return new ScoringWeights(
                skills * factor,
                experience * factor,
                location * factor,
                salary * factor
        );
    }
}
