package com.jobmatch.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRange {

    @Column(name = "salary_min", nullable = false)
    private int min;

    @Column(name = "salary_max", nullable = false)
    private int max;
}
