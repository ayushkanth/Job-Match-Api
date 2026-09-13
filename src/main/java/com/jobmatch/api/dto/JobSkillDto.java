package com.jobmatch.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillDto {

    @NotBlank(message = "Skill name must not be blank")
    private String skill;

    private boolean mustHave;
}
