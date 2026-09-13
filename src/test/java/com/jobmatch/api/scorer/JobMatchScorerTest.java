package com.jobmatch.api.scorer;

import com.jobmatch.api.domain.Candidate;
import com.jobmatch.api.domain.Job;
import com.jobmatch.api.domain.JobSkill;
import com.jobmatch.api.domain.SalaryRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JobMatchScorer Unit Tests (Pure Rules Engine)")
class JobMatchScorerTest {

    private JobMatchScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new JobMatchScorer();
    }

    @Test
    @DisplayName("Candidate missing a must-have skill is strictly ineligible (hard filter)")
    void missingMustHaveSkill_shouldExcludeJobEntirely() {
        Candidate candidate = Candidate.builder()
                .name("Alex")
                .skills(List.of("Java", "Docker"))
                .yearsOfExperience(5)
                .location("New York")
                .expectedSalary(120000)
                .build();

        Job job = Job.builder()
                .title("Backend Engineer")
                .requiredSkills(List.of(
                        JobSkill.builder().skill("Java").mustHave(true).build(),
                        JobSkill.builder().skill("Spring Boot").mustHave(true).build() // Missing
                ))
                .minYearsExperience(3)
                .location("New York")
                .salaryRange(new SalaryRange(100000, 140000))
                .remoteAllowed(false)
                .build();

        assertFalse(scorer.isEligible(candidate, job), "Candidate missing must-have skill should not be eligible");

        MatchResult result = scorer.score(candidate, job);
        assertFalse(result.eligible());
        assertEquals(0.0, result.overallScore());
    }

    @Test
    @DisplayName("Candidate matching all must-haves but missing nice-to-haves is eligible with proportional score")
    void missingOnlyNiceToHaveSkills_shouldBeEligibleWithLowerScore() {
        Candidate candidate = Candidate.builder()
                .name("Alex")
                .skills(List.of("Java", "Spring Boot")) // Has both must-haves, missing Docker & Kubernetes
                .yearsOfExperience(5)
                .location("New York")
                .expectedSalary(120000)
                .build();

        Job job = Job.builder()
                .title("Senior Developer")
                .requiredSkills(List.of(
                        JobSkill.builder().skill("Java").mustHave(true).build(),
                        JobSkill.builder().skill("Spring Boot").mustHave(true).build(),
                        JobSkill.builder().skill("Docker").mustHave(false).build(),
                        JobSkill.builder().skill("Kubernetes").mustHave(false).build()
                ))
                .minYearsExperience(5)
                .location("New York")
                .salaryRange(new SalaryRange(120000, 150000))
                .remoteAllowed(false)
                .build();

        assertTrue(scorer.isEligible(candidate, job));

        MatchResult result = scorer.score(candidate, job);
        assertTrue(result.eligible());

        // 2 out of 4 skills matched -> 50% of 50.0 skills weight = 25.0
        assertEquals(25.0, result.breakdown().skills().score());
        assertEquals(50.0, result.breakdown().skills().max());
    }

    @Test
    @DisplayName("Experience below requirement penalizes proportionally without excluding candidate")
    void experienceShortfall_shouldPenalizeProportionallyWithoutExcluding() {
        // Job requires 5 years, candidate has 3 years -> 3/5 = 60% of 20 = 12.0
        Candidate candidate = Candidate.builder()
                .name("Jordan")
                .skills(List.of("Java"))
                .yearsOfExperience(3)
                .location("Boston")
                .expectedSalary(100000)
                .build();

        Job job = Job.builder()
                .title("Tech Lead")
                .requiredSkills(List.of(JobSkill.builder().skill("Java").mustHave(true).build()))
                .minYearsExperience(5)
                .location("Boston")
                .salaryRange(new SalaryRange(100000, 120000))
                .remoteAllowed(false)
                .build();

        assertTrue(scorer.isEligible(candidate, job));

        MatchResult result = scorer.score(candidate, job);
        assertEquals(12.0, result.breakdown().experience().score());
        assertEquals(20.0, result.breakdown().experience().max());

        // Candidate with 0 years experience -> 0/5 = 0.0 points
        Candidate zeroExp = Candidate.builder()
                .name("Novice")
                .skills(List.of("Java"))
                .yearsOfExperience(0)
                .location("Boston")
                .expectedSalary(100000)
                .build();

        MatchResult zeroResult = scorer.score(zeroExp, job);
        assertTrue(zeroResult.eligible(), "Even with 0 experience, candidate remains eligible");
        assertEquals(0.0, zeroResult.breakdown().experience().score());
    }

    @Test
    @DisplayName("Salary scoring: zero points when expected salary exceeds job maximum")
    void salaryExceedsMax_shouldScoreZeroOnSalary() {
        Candidate candidate = Candidate.builder()
                .name("Sam")
                .skills(List.of("Java"))
                .yearsOfExperience(5)
                .location("Chicago")
                .expectedSalary(160000)
                .build();

        Job job = Job.builder()
                .title("Developer")
                .requiredSkills(List.of(JobSkill.builder().skill("Java").mustHave(true).build()))
                .minYearsExperience(3)
                .location("Chicago")
                .salaryRange(new SalaryRange(100000, 140000)) // Max 140k < Expected 160k
                .remoteAllowed(false)
                .build();

        MatchResult result = scorer.score(candidate, job);
        assertEquals(0.0, result.breakdown().salary().score());
        assertEquals(15.0, result.breakdown().salary().max());
    }

    @Test
    @DisplayName("Salary scoring: full points when expected salary is at or below job minimum")
    void salaryAtOrBelowMin_shouldScoreMaxOnSalary() {
        Candidate candidate = Candidate.builder()
                .name("Sam")
                .skills(List.of("Java"))
                .yearsOfExperience(5)
                .location("Chicago")
                .expectedSalary(90000)
                .build();

        Job job = Job.builder()
                .title("Developer")
                .requiredSkills(List.of(JobSkill.builder().skill("Java").mustHave(true).build()))
                .minYearsExperience(3)
                .location("Chicago")
                .salaryRange(new SalaryRange(100000, 140000)) // Min 100k > Expected 90k
                .remoteAllowed(false)
                .build();

        MatchResult result = scorer.score(candidate, job);
        assertEquals(15.0, result.breakdown().salary().score());
        assertEquals(15.0, result.breakdown().salary().max());
    }

    @Test
    @DisplayName("Salary scoring: gradient formula between min and max")
    void salaryWithinRange_shouldScoreAlongGradient() {
        Job job = Job.builder()
                .title("Developer")
                .requiredSkills(List.of(JobSkill.builder().skill("Java").mustHave(true).build()))
                .minYearsExperience(3)
                .location("Chicago")
                .salaryRange(new SalaryRange(100000, 200000))
                .remoteAllowed(false)
                .build();

        // Midpoint: 150000 -> gradient = 0.5 -> 15.0 * (1 - 0.5 * 0.5) = 15.0 * 0.75 = 11.25
        Candidate midCandidate = Candidate.builder()
                .name("Mid")
                .skills(List.of("Java"))
                .yearsOfExperience(3)
                .location("Chicago")
                .expectedSalary(150000)
                .build();

        MatchResult midResult = scorer.score(midCandidate, job);
        assertEquals(11.25, midResult.breakdown().salary().score());

        // At Max: 200000 -> gradient = 1.0 -> 15.0 * (1 - 0.5 * 1.0) = 15.0 * 0.5 = 7.50
        Candidate maxCandidate = Candidate.builder()
                .name("Max")
                .skills(List.of("Java"))
                .yearsOfExperience(3)
                .location("Chicago")
                .expectedSalary(200000)
                .build();

        MatchResult maxResult = scorer.score(maxCandidate, job);
        assertEquals(7.50, maxResult.breakdown().salary().score());
    }

    @Test
    @DisplayName("Location scoring: exact match > remoteAllowed > mismatch")
    void locationScoring_exactVsRemoteVsMismatch() {
        Job baseJob = Job.builder()
                .title("Engineer")
                .requiredSkills(List.of(JobSkill.builder().skill("Java").mustHave(true).build()))
                .minYearsExperience(2)
                .location("Seattle")
                .salaryRange(new SalaryRange(100000, 120000))
                .build();

        Candidate candidateSeattle = Candidate.builder()
                .name("Local")
                .skills(List.of("Java"))
                .yearsOfExperience(2)
                .location("Seattle")
                .expectedSalary(100000)
                .build();

        Candidate candidateRemote = Candidate.builder()
                .name("Remote")
                .skills(List.of("Java"))
                .yearsOfExperience(2)
                .location("Miami")
                .expectedSalary(100000)
                .build();

        // 1. Exact match
        baseJob.setRemoteAllowed(false);
        MatchResult exactResult = scorer.score(candidateSeattle, baseJob);
        assertEquals(15.0, exactResult.breakdown().location().score(), "Exact match gets 15.0");

        // 2. Remote allowed
        baseJob.setRemoteAllowed(true);
        MatchResult remoteResult = scorer.score(candidateRemote, baseJob);
        assertEquals(12.0, remoteResult.breakdown().location().score(), "Remote allowed gets 12.0 (80%)");

        // 3. Mismatch (different city, remote not allowed)
        baseJob.setRemoteAllowed(false);
        MatchResult mismatchResult = scorer.score(candidateRemote, baseJob);
        assertEquals(0.0, mismatchResult.breakdown().location().score(), "Mismatch gets 0.0");

        assertTrue(exactResult.breakdown().location().score() > remoteResult.breakdown().location().score());
        assertTrue(remoteResult.breakdown().location().score() > mismatchResult.breakdown().location().score());
    }

    @Test
    @DisplayName("Full end-to-end hand-computed scenario matching documentation")
    void fullEndToEndHandComputedScenario() {
        /*
         * Hand-computed scenario:
         * Job requirements:
         *  - Skills: Java (must), Spring (must), Docker (nice), Kubernetes (nice) -> 4 skills total
         *  - Min Experience: 4 years
         *  - Location: San Francisco (remoteAllowed = true)
         *  - Salary Range: [120000, 160000]
         *
         * Candidate profile:
         *  - Skills: ["Java", "Spring", "Docker"] -> has 2 must + 1 nice = 3 out of 4 skills matched
         *  - Experience: 3 years -> 3 / 4 shortfall ratio
         *  - Location: Austin (not SF, but job allows remote!)
         *  - Expected Salary: 140000 (midpoint of [120k, 160k])
         *
         * Calculations with default weights (50 / 20 / 15 / 15):
         *  1. Skills Score: 50.0 * (3 / 4) = 37.50
         *  2. Experience Score: 20.0 * (3 / 4) = 15.00
         *  3. Location Score: 15.0 * 0.80 = 12.00
         *  4. Salary Score:
         *     gradient = (140k - 120k) / (160k - 120k) = 20k / 40k = 0.5
         *     points = 15.0 * (1.0 - 0.5 * 0.5) = 15.0 * 0.75 = 11.25
         *
         * Overall Expected Score = 37.50 + 15.00 + 12.00 + 11.25 = 75.75
         */
        Candidate candidate = Candidate.builder()
                .name("Hand-Calculated Candidate")
                .skills(List.of("Java", "Spring", "Docker"))
                .yearsOfExperience(3)
                .location("Austin")
                .expectedSalary(140000)
                .build();

        Job job = Job.builder()
                .title("Full Stack Platform Engineer")
                .requiredSkills(List.of(
                        JobSkill.builder().skill("Java").mustHave(true).build(),
                        JobSkill.builder().skill("Spring").mustHave(true).build(),
                        JobSkill.builder().skill("Docker").mustHave(false).build(),
                        JobSkill.builder().skill("Kubernetes").mustHave(false).build()
                ))
                .minYearsExperience(4)
                .location("San Francisco")
                .salaryRange(new SalaryRange(120000, 160000))
                .remoteAllowed(true)
                .build();

        MatchResult result = scorer.score(candidate, job);

        assertTrue(result.eligible());
        assertEquals(37.50, result.breakdown().skills().score(), 0.001);
        assertEquals(15.00, result.breakdown().experience().score(), 0.001);
        assertEquals(12.00, result.breakdown().location().score(), 0.001);
        assertEquals(11.25, result.breakdown().salary().score(), 0.001);
        assertEquals(75.75, result.overallScore(), 0.001);
    }

    @Test
    @DisplayName("Configurable weights: custom weights are normalized to 100")
    void customWeights_shouldNormalizeCorrectly() {
        Candidate candidate = Candidate.builder()
                .name("Alex")
                .skills(List.of("Java"))
                .yearsOfExperience(5)
                .location("Boston")
                .expectedSalary(100000)
                .build();

        Job job = Job.builder()
                .title("Java Engineer")
                .requiredSkills(List.of(JobSkill.builder().skill("Java").mustHave(true).build()))
                .minYearsExperience(5)
                .location("Boston")
                .salaryRange(new SalaryRange(100000, 120000))
                .remoteAllowed(false)
                .build();

        // Custom weights totaling 200: 100 / 40 / 30 / 30 -> normalizes to 50 / 20 / 15 / 15
        ScoringWeights customWeights = new ScoringWeights(100.0, 40.0, 30.0, 30.0);
        MatchResult result = scorer.score(candidate, job, customWeights);

        assertTrue(result.eligible());
        assertEquals(100.0, result.overallScore());
        assertEquals(50.0, result.breakdown().skills().score());
        assertEquals(20.0, result.breakdown().experience().score());
        assertEquals(15.0, result.breakdown().location().score());
        assertEquals(15.0, result.breakdown().salary().score());
    }
}
