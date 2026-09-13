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
public class JobSkill {

    @Column(name = "skill", nullable = false)
    private String skill;

    @Column(name = "must_have", nullable = false)
    private boolean mustHave;
}
