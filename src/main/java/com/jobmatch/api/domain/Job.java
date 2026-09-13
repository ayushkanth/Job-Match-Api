package com.jobmatch.api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_required_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Builder.Default
    private List<JobSkill> requiredSkills = new ArrayList<>();

    @Column(name = "min_years_experience", nullable = false)
    private int minYearsExperience;

    @Column(name = "location", nullable = false)
    private String location;

    @Embedded
    private SalaryRange salaryRange;

    @Column(name = "remote_allowed", nullable = false)
    private boolean remoteAllowed;
}
