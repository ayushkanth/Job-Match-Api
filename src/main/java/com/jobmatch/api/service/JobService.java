package com.jobmatch.api.service;

import com.jobmatch.api.domain.Job;
import com.jobmatch.api.domain.JobSkill;
import com.jobmatch.api.domain.SalaryRange;
import com.jobmatch.api.dto.JobRequest;
import com.jobmatch.api.dto.JobResponse;
import com.jobmatch.api.dto.JobSkillDto;
import com.jobmatch.api.dto.SalaryRangeDto;
import com.jobmatch.api.exception.ResourceNotFoundException;
import com.jobmatch.api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    @Transactional
    public JobResponse createJob(JobRequest request) {
        if (request.getSalaryRange().getMin() > request.getSalaryRange().getMax()) {
            throw new IllegalArgumentException("Salary range minimum cannot be greater than maximum");
        }

        List<JobSkill> skills = request.getRequiredSkills() != null
                ? request.getRequiredSkills().stream()
                .map(s -> JobSkill.builder()
                        .skill(s.getSkill().trim())
                        .mustHave(s.isMustHave())
                        .build())
                .toList()
                : new ArrayList<>();

        SalaryRange salaryRange = SalaryRange.builder()
                .min(request.getSalaryRange().getMin())
                .max(request.getSalaryRange().getMax())
                .build();

        Job job = Job.builder()
                .title(request.getTitle().trim())
                .requiredSkills(new ArrayList<>(skills))
                .minYearsExperience(request.getMinYearsExperience())
                .location(request.getLocation().trim())
                .salaryRange(salaryRange)
                .remoteAllowed(request.isRemoteAllowed())
                .build();

        Job saved = jobRepository.save(job);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        Job job = getJobEntity(id);
        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public Job getJobEntity(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public JobResponse mapToResponse(Job job) {
        List<JobSkillDto> skillDtos = job.getRequiredSkills() != null
                ? job.getRequiredSkills().stream()
                .map(s -> JobSkillDto.builder()
                        .skill(s.getSkill())
                        .mustHave(s.isMustHave())
                        .build())
                .toList()
                : new ArrayList<>();

        SalaryRangeDto salaryDto = job.getSalaryRange() != null
                ? SalaryRangeDto.builder()
                .min(job.getSalaryRange().getMin())
                .max(job.getSalaryRange().getMax())
                .build()
                : null;

        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .requiredSkills(skillDtos)
                .minYearsExperience(job.getMinYearsExperience())
                .location(job.getLocation())
                .salaryRange(salaryDto)
                .remoteAllowed(job.isRemoteAllowed())
                .build();
    }
}
