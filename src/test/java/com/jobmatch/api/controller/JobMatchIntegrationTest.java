package com.jobmatch.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmatch.api.domain.Candidate;
import com.jobmatch.api.domain.JobSkill;
import com.jobmatch.api.dto.CandidateRequest;
import com.jobmatch.api.dto.JobRequest;
import com.jobmatch.api.dto.JobSkillDto;
import com.jobmatch.api.dto.SalaryRangeDto;
import com.jobmatch.api.repository.CandidateRepository;
import com.jobmatch.api.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
@DisplayName("Job Match API Integration Tests")
class JobMatchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("POST /candidates creates profile and returns 201 Created")
    void createCandidate_shouldReturn201() throws Exception {
        CandidateRequest request = CandidateRequest.builder()
                .name("Diana Prince")
                .skills(List.of("Java", "Spring Boot", "Docker"))
                .yearsOfExperience(4)
                .location("New York")
                .expectedSalary(125000)
                .build();

        mockMvc.perform(post("/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Diana Prince")))
                .andExpect(jsonPath("$.skills", hasSize(3)))
                .andExpect(jsonPath("$.expectedSalary", is(125000)));
    }

    @Test
    @DisplayName("POST /jobs creates job posting and returns 201 Created")
    void createJob_shouldReturn201() throws Exception {
        JobRequest request = JobRequest.builder()
                .title("Senior Cloud Architect")
                .requiredSkills(List.of(
                        new JobSkillDto("Java", true),
                        new JobSkillDto("AWS", true),
                        new JobSkillDto("Kubernetes", false)
                ))
                .minYearsExperience(6)
                .location("Seattle")
                .salaryRange(new SalaryRangeDto(150000, 190000))
                .remoteAllowed(true)
                .build();

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Senior Cloud Architect")))
                .andExpect(jsonPath("$.requiredSkills", hasSize(3)))
                .andExpect(jsonPath("$.remoteAllowed", is(true)));
    }

    @Test
    @DisplayName("GET /candidates/{id}/recommendations proves must-have hard filter excludes disqualified jobs from HTTP response")
    void recommendations_mustHaveHardFilter_excludesJobFromHttpResponse() throws Exception {
        // Candidate 1 (Alice Chen) has Java, Spring Boot, PostgreSQL, Docker, AWS
        // Seed Job 1 requires Java (must) and Spring Boot (must) -> Alice is ELIGIBLE
        // Seed Job 2 requires Java (must) and React (must) -> Alice lacks React, so is INELIGIBLE!
        // Seed Job 3 requires Java (must) -> Alice is ELIGIBLE

        mockMvc.perform(get("/candidates/1/recommendations"))
                .andExpect(status().isOk())
                // Verify that Job 2 (which requires React) is NOT in the recommendations list
                .andExpect(jsonPath("$[*].jobId", not(hasItem(2))))
                .andExpect(jsonPath("$[*].jobTitle", not(hasItem("Full Stack Java Developer"))))
                // Verify eligible jobs (Job 1 and Job 3) are present
                .andExpect(jsonPath("$[*].jobId", hasItem(1)))
                .andExpect(jsonPath("$[*].jobId", hasItem(3)))
                // Verify top recommendation has breakdown scores populated
                .andExpect(jsonPath("$[0].jobId", is(1)))
                .andExpect(jsonPath("$[0].jobTitle", is("Senior Backend Engineer")))
                .andExpect(jsonPath("$[0].overallScore", greaterThan(0.0)))
                .andExpect(jsonPath("$[0].breakdown.skills.score", greaterThan(0.0)))
                .andExpect(jsonPath("$[0].breakdown.experience.score", greaterThan(0.0)))
                .andExpect(jsonPath("$[0].breakdown.location.score", greaterThan(0.0)))
                .andExpect(jsonPath("$[0].breakdown.salary.score", greaterThan(0.0)));
    }

    @Test
    @DisplayName("GET /candidates/{id}/recommendations respects limit parameter and custom weights")
    void recommendations_respectsLimitAndCustomWeights() throws Exception {
        mockMvc.perform(get("/candidates/1/recommendations")
                        .param("limit", "1")
                        .param("weightSkills", "60")
                        .param("weightExperience", "20")
                        .param("weightLocation", "10")
                        .param("weightSalary", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].jobId", is(1)))
                .andExpect(jsonPath("$[0].breakdown.skills.max", is(60.0)))
                .andExpect(jsonPath("$[0].breakdown.experience.max", is(20.0)))
                .andExpect(jsonPath("$[0].breakdown.location.max", is(10.0)))
                .andExpect(jsonPath("$[0].breakdown.salary.max", is(10.0)));
    }

    @Test
    @DisplayName("GET /candidates/{id}/recommendations returns 404 when candidate is not found")
    void recommendations_candidateNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/candidates/999999/recommendations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Candidate not found")));
    }

    @Test
    @DisplayName("GET /jobs/{id}/recommendations (bonus reverse view) excludes candidates missing must-have skills")
    void jobCandidateRecommendations_excludesDisqualifiedCandidates() throws Exception {
        // Job 1 requires Java (must) and Spring Boot (must)
        // Candidate 1 (Alice) has Java + Spring Boot -> ELIGIBLE
        // Candidate 2 (Bob) has Java, SQL, Git -> lacks Spring Boot -> INELIGIBLE!
        // Candidate 3 (Carol) has Java, Spring Boot -> ELIGIBLE

        mockMvc.perform(get("/jobs/1/recommendations"))
                .andExpect(status().isOk())
                // Bob (candidate 2) must be excluded entirely because he lacks Spring Boot
                .andExpect(jsonPath("$[*].candidateId", not(hasItem(2))))
                .andExpect(jsonPath("$[*].candidateName", not(hasItem("Bob Smith"))))
                // Alice and Carol must be included
                .andExpect(jsonPath("$[*].candidateId", hasItem(1)))
                .andExpect(jsonPath("$[*].candidateId", hasItem(3)))
                .andExpect(jsonPath("$[0].overallScore", greaterThan(0.0)));
    }
}
