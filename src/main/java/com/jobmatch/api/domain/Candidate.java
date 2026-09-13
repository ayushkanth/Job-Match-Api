package com.jobmatch.api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "candidate_skills", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "skill", nullable = false)
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "expected_salary", nullable = false)
    private int expectedSalary;
}
