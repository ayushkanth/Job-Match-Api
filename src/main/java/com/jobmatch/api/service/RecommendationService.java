package com.jobmatch.api.service;

import com.jobmatch.api.domain.Candidate;
import com.jobmatch.api.domain.Job;
import com.jobmatch.api.dto.*;
import com.jobmatch.api.scorer.JobMatchScorer;
import com.jobmatch.api.scorer.MatchResult;
import com.jobmatch.api.scorer.ScoreBreakdown;
import com.jobmatch.api.scorer.ScoringWeights;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CandidateService candidateService;
    private final JobService jobService;
    private final JobMatchScorer scorer = new JobMatchScorer();

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsForCandidate(Long candidateId, int limit, ScoringWeightsDto customWeights) {
        Candidate candidate = candidateService.getCandidateEntity(candidateId);
        List<Job> allJobs = jobService.getAllJobs();

        ScoringWeights weights = toScoringWeights(customWeights);

        return allJobs.stream()
                // Must-have skills hard filter: filter out ineligible jobs BEFORE scoring
                .filter(job -> scorer.isEligible(candidate, job))
                .map(job -> {
                    MatchResult result = scorer.score(candidate, job, weights);
                    return mapToRecommendationResponse(job, result);
                })
                .sorted(Comparator.comparingDouble(RecommendationResponse::getOverallScore).reversed()
                        .thenComparing(RecommendationResponse::getJobId))
                .limit(Math.max(1, limit))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidateRecommendationResponse> getRecommendationsForJob(Long jobId, int limit, ScoringWeightsDto customWeights) {
        Job job = jobService.getJobEntity(jobId);
        List<Candidate> allCandidates = candidateService.getAllCandidates();

        ScoringWeights weights = toScoringWeights(customWeights);

        return allCandidates.stream()
                // Must-have skills hard filter: filter out ineligible candidates BEFORE scoring
                .filter(candidate -> scorer.isEligible(candidate, job))
                .map(candidate -> {
                    MatchResult result = scorer.score(candidate, job, weights);
                    return mapToCandidateRecommendationResponse(candidate, result);
                })
                .sorted(Comparator.comparingDouble(CandidateRecommendationResponse::getOverallScore).reversed()
                        .thenComparing(CandidateRecommendationResponse::getCandidateId))
                .limit(Math.max(1, limit))
                .toList();
    }

    private ScoringWeights toScoringWeights(ScoringWeightsDto dto) {
        if (dto == null) {
            return ScoringWeights.defaults();
        }
        double skills = dto.getSkills() != null ? dto.getSkills() : ScoringWeights.DEFAULT_SKILLS;
        double experience = dto.getExperience() != null ? dto.getExperience() : ScoringWeights.DEFAULT_EXPERIENCE;
        double location = dto.getLocation() != null ? dto.getLocation() : ScoringWeights.DEFAULT_LOCATION;
        double salary = dto.getSalary() != null ? dto.getSalary() : ScoringWeights.DEFAULT_SALARY;

        return new ScoringWeights(skills, experience, location, salary).normalize();
    }

    private RecommendationResponse mapToRecommendationResponse(Job job, MatchResult result) {
        SalaryRangeDto salaryDto = job.getSalaryRange() != null
                ? SalaryRangeDto.builder()
                .min(job.getSalaryRange().getMin())
                .max(job.getSalaryRange().getMax())
                .build()
                : null;

        return RecommendationResponse.builder()
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .location(job.getLocation())
                .salaryRange(salaryDto)
                .remoteAllowed(job.isRemoteAllowed())
                .overallScore(result.overallScore())
                .breakdown(mapScoreBreakdown(result.breakdown()))
                .build();
    }

    private CandidateRecommendationResponse mapToCandidateRecommendationResponse(Candidate candidate, MatchResult result) {
        return CandidateRecommendationResponse.builder()
                .candidateId(candidate.getId())
                .candidateName(candidate.getName())
                .skills(candidate.getSkills())
                .yearsOfExperience(candidate.getYearsOfExperience())
                .location(candidate.getLocation())
                .expectedSalary(candidate.getExpectedSalary())
                .overallScore(result.overallScore())
                .breakdown(mapScoreBreakdown(result.breakdown()))
                .build();
    }

    private ScoreBreakdownDto mapScoreBreakdown(ScoreBreakdown breakdown) {
        return ScoreBreakdownDto.builder()
                .skills(new DimensionScoreDto(breakdown.skills().score(), breakdown.skills().max()))
                .experience(new DimensionScoreDto(breakdown.experience().score(), breakdown.experience().max()))
                .location(new DimensionScoreDto(breakdown.location().score(), breakdown.location().max()))
                .salary(new DimensionScoreDto(breakdown.salary().score(), breakdown.salary().max()))
                .build();
    }
}
