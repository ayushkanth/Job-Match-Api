package com.jobmatch.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponse {

    private Long id;
    private String name;
    private List<String> skills;
    private int yearsOfExperience;
    private String location;
    private int expectedSalary;
}
