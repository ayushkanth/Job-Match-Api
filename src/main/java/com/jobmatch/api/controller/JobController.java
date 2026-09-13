package com.jobmatch.api.controller;

import com.jobmatch.api.dto.CandidateRecommendationResponse;
import com.jobmatch.api.dto.JobRequest;
import com.jobmatch.api.dto.JobResponse;
import com.jobmatch.api.dto.ScoringWeightsDto;
import com.jobmatch.api.service.JobService;
import com.jobmatch.api.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Endpoints for managing job postings and candidate matching")
public class JobController {

    private final JobService jobService;
    private final RecommendationService recommendationService;

    @PostMapping
    @Operation(summary = "Create a job posting", description = "Stores job details including required skills with mustHave flags, salary range, and remote policy")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        JobResponse response = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID", description = "Retrieves a job posting by primary key")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        JobResponse response = jobService.getJobById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Get best-fit candidates for a job", description = "Returns a ranked list of candidate recommendations for a job posting (reverse matching)")
    public ResponseEntity<List<CandidateRecommendationResponse>> getCandidateRecommendations(
            @PathVariable Long id,
            @Parameter(description = "Maximum number of recommendations to return (default: 10)")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Custom weight for skills dimension")
            @RequestParam(required = false) Double weightSkills,
            @Parameter(description = "Custom weight for experience dimension")
            @RequestParam(required = false) Double weightExperience,
            @Parameter(description = "Custom weight for location dimension")
            @RequestParam(required = false) Double weightLocation,
            @Parameter(description = "Custom weight for salary dimension")
            @RequestParam(required = false) Double weightSalary
    ) {
        ScoringWeightsDto customWeights = null;
        if (weightSkills != null || weightExperience != null || weightLocation != null || weightSalary != null) {
            customWeights = ScoringWeightsDto.builder()
                    .skills(weightSkills)
                    .experience(weightExperience)
                    .location(weightLocation)
                    .salary(weightSalary)
                    .build();
        }

        List<CandidateRecommendationResponse> recommendations = recommendationService.getRecommendationsForJob(id, limit, customWeights);
        return ResponseEntity.ok(recommendations);
    }
}
