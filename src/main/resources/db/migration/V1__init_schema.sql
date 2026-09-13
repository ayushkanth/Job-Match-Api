CREATE TABLE candidates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    years_of_experience INT NOT NULL,
    location VARCHAR(255) NOT NULL,
    expected_salary INT NOT NULL
);

CREATE TABLE candidate_skills (
    candidate_id BIGINT NOT NULL,
    skill VARCHAR(255) NOT NULL,
    PRIMARY KEY (candidate_id, skill),
    CONSTRAINT fk_candidate_skills_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    min_years_experience INT NOT NULL,
    location VARCHAR(255) NOT NULL,
    salary_min INT NOT NULL,
    salary_max INT NOT NULL,
    remote_allowed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE job_required_skills (
    job_id BIGINT NOT NULL,
    skill VARCHAR(255) NOT NULL,
    must_have BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (job_id, skill),
    CONSTRAINT fk_job_required_skills_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
