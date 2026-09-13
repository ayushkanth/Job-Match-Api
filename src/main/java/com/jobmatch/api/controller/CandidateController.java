package com.jobmatch.api.controller;

import com.jobmatch.api.dto.CandidateRequest;
import com.jobmatch.api.dto.CandidateResponse;
import com.jobmatch.api.dto.RecommendationResponse;
import com.jobmatch.api.dto.ScoringWeightsDto;
import com.jobmatch.api.service.CandidateService;
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
@RequestMapping("/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidates", description = "Endpoints for managing candidate profiles and recommendations")
public class CandidateController {

    private final CandidateService candidateService;
    private final RecommendationService recommendationService;

    @PostMapping
    @Operation(summary = "Create a candidate profile", description = "Stores candidate details including skills, experience, location, and salary expectation")
    public ResponseEntity<CandidateResponse> createCandidate(@Valid @RequestBody CandidateRequest request) {
        CandidateResponse response = candidateService.createCandidate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get candidate by ID", description = "Retrieves a candidate profile by primary key")
    public ResponseEntity<CandidateResponse> getCandidate(@PathVariable Long id) {
        CandidateResponse response = candidateService.getCandidateById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/recommendation")
    @Operation(summary = "Get job recommendations for a candidate", description = "Returns a ranked list of job recommendations evaluated by the transparent rule-based scoring engine")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
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

        List<RecommendationResponse> recommendations = recommendationService.getRecommendationsForCandidate(id, limit, customWeights);
        return ResponseEntity.ok(recommendations);
    }
}
