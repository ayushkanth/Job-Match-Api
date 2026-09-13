package com.jobmatch.api.service;

import com.jobmatch.api.domain.Candidate;
import com.jobmatch.api.dto.CandidateRequest;
import com.jobmatch.api.dto.CandidateResponse;
import com.jobmatch.api.exception.ResourceNotFoundException;
import com.jobmatch.api.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;

    @Transactional
    public CandidateResponse createCandidate(CandidateRequest request) {
        Candidate candidate = Candidate.builder()
                .name(request.getName().trim())
                .skills(request.getSkills() != null ? new ArrayList<>(request.getSkills()) : new ArrayList<>())
                .yearsOfExperience(request.getYearsOfExperience())
                .location(request.getLocation().trim())
                .expectedSalary(request.getExpectedSalary())
                .build();

        Candidate saved = candidateRepository.save(candidate);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(Long id) {
        Candidate candidate = getCandidateEntity(id);
        return mapToResponse(candidate);
    }

    @Transactional(readOnly = true)
    public Candidate getCandidateEntity(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    public CandidateResponse mapToResponse(Candidate candidate) {
        return CandidateResponse.builder()
                .id(candidate.getId())
                .name(candidate.getName())
                .skills(candidate.getSkills())
                .yearsOfExperience(candidate.getYearsOfExperience())
                .location(candidate.getLocation())
                .expectedSalary(candidate.getExpectedSalary())
                .build();
    }
}
