package com.jobmatch.api.scorer;

import com.jobmatch.api.domain.Candidate;
import com.jobmatch.api.domain.Job;
import com.jobmatch.api.domain.JobSkill;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure, isolated rule-based scoring engine with no Spring or database dependencies.
 */
public class JobMatchScorer {

    /**
     * Hard filter check for must-have skills.
     * Missing any must-have skill disqualifies the candidate immediately.
     */
    public boolean isEligible(Candidate candidate, Job job) {
        if (candidate == null || job == null) {
            return false;
        }

        List<JobSkill> requiredSkills = job.getRequiredSkills() != null
                ? job.getRequiredSkills()
                : Collections.emptyList();

        List<JobSkill> mustHaves = requiredSkills.stream()
                .filter(JobSkill::isMustHave)
                .toList();

        if (mustHaves.isEmpty()) {
            return true;
        }

        Set<String> candidateSkills = normalizeSkills(candidate.getSkills());

        for (JobSkill mustHave : mustHaves) {
            if (mustHave.getSkill() == null || !candidateSkills.contains(mustHave.getSkill().trim().toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Score a candidate against a job using default weights.
     */
    public MatchResult score(Candidate candidate, Job job) {
        return score(candidate, job, ScoringWeights.defaults());
    }

    /**
     * Score a candidate against a job with custom weights.
     */
    public MatchResult score(Candidate candidate, Job job, ScoringWeights weights) {
        if (weights == null) {
            weights = ScoringWeights.defaults();
        }
        ScoringWeights normalizedWeights = weights.normalize();

        if (!isEligible(candidate, job)) {
            return MatchResult.ineligible(normalizedWeights);
        }

        DimensionScore skillsScore = calculateSkillsScore(candidate, job, normalizedWeights.skills());
        DimensionScore experienceScore = calculateExperienceScore(candidate, job, normalizedWeights.experience());
        DimensionScore locationScore = calculateLocationScore(candidate, job, normalizedWeights.location());
        DimensionScore salaryScore = calculateSalaryScore(candidate, job, normalizedWeights.salary());

        ScoreBreakdown breakdown = new ScoreBreakdown(skillsScore, experienceScore, locationScore, salaryScore);
        double totalScore = skillsScore.score() + experienceScore.score() + locationScore.score() + salaryScore.score();

        return MatchResult.eligible(totalScore, breakdown);
    }

    public DimensionScore calculateSkillsScore(Candidate candidate, Job job, double maxPoints) {
        List<JobSkill> requiredSkills = job.getRequiredSkills() != null
                ? job.getRequiredSkills()
                : Collections.emptyList();

        if (requiredSkills.isEmpty()) {
            return DimensionScore.of(maxPoints, maxPoints);
        }

        long mustHaveCount = requiredSkills.stream().filter(JobSkill::isMustHave).count();
        long niceToHaveCount = requiredSkills.size() - mustHaveCount;

        // If job only has must-haves, candidate already passed eligibility check, so 100%
        if (niceToHaveCount == 0) {
            return DimensionScore.of(maxPoints, maxPoints);
        }

        Set<String> candidateSkills = normalizeSkills(candidate.getSkills());

        long matchedNiceToHaves = requiredSkills.stream()
                .filter(js -> !js.isMustHave())
                .filter(js -> js.getSkill() != null && candidateSkills.contains(js.getSkill().trim().toLowerCase()))
                .count();

        // Overall ratio = (mustHavesMatched + niceToHavesMatched) / totalSkills
        double ratio = (double) (mustHaveCount + matchedNiceToHaves) / requiredSkills.size();
        double points = maxPoints * ratio;

        return DimensionScore.of(points, maxPoints);
    }

    public DimensionScore calculateExperienceScore(Candidate candidate, Job job, double maxPoints) {
        int minExp = Math.max(0, job.getMinYearsExperience());
        int candidateExp = Math.max(0, candidate.getYearsOfExperience());

        if (minExp == 0 || candidateExp >= minExp) {
            return DimensionScore.of(maxPoints, maxPoints);
        }

        double ratio = (double) candidateExp / minExp;
        double points = maxPoints * ratio;

        return DimensionScore.of(points, maxPoints);
    }

    public DimensionScore calculateLocationScore(Candidate candidate, Job job, double maxPoints) {
        String candLocation = candidate.getLocation() != null ? candidate.getLocation().trim().toLowerCase() : "";
        String jobLocation = job.getLocation() != null ? job.getLocation().trim().toLowerCase() : "";

        if (!candLocation.isEmpty() && candLocation.equalsIgnoreCase(jobLocation)) {
            return DimensionScore.of(maxPoints, maxPoints);
        }

        if (job.isRemoteAllowed()) {
            // 80% point credit when remote is allowed
            return DimensionScore.of(maxPoints * 0.8, maxPoints);
        }

        return DimensionScore.of(0.0, maxPoints);
    }

    public DimensionScore calculateSalaryScore(Candidate candidate, Job job, double maxPoints) {
        int expected = candidate.getExpectedSalary();
        int min = job.getSalaryRange() != null ? job.getSalaryRange().getMin() : 0;
        int max = job.getSalaryRange() != null ? job.getSalaryRange().getMax() : 0;

        if (expected > max) {
            return DimensionScore.of(0.0, maxPoints);
        }

        if (expected <= min) {
            return DimensionScore.of(maxPoints, maxPoints);
        }

        // Gradient when min < expected <= max
        if (max == min) {
            return DimensionScore.of(maxPoints, maxPoints);
        }

        double gradient = (double) (expected - min) / (max - min);
        double points = maxPoints * (1.0 - 0.5 * gradient);

        return DimensionScore.of(points, maxPoints);
    }

    private Set<String> normalizeSkills(List<String> skills) {
        return skills.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
