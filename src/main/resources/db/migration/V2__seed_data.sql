-- Seed Candidates (omitting explicit id so the sequence advances naturally)
INSERT INTO candidates (name, years_of_experience, location, expected_salary)
VALUES ('Alice Chen', 5, 'New York', 130000);

INSERT INTO candidate_skills (candidate_id, skill) VALUES
(1, 'Java'),
(1, 'Spring Boot'),
(1, 'PostgreSQL'),
(1, 'Docker'),
(1, 'AWS');

INSERT INTO candidates (name, years_of_experience, location, expected_salary)
VALUES ('Bob Smith', 2, 'San Francisco', 95000);

INSERT INTO candidate_skills (candidate_id, skill) VALUES
(2, 'Java'),
(2, 'SQL'),
(2, 'Git');

INSERT INTO candidates (name, years_of_experience, location, expected_salary)
VALUES ('Carol Davis', 8, 'Chicago', 160000);

INSERT INTO candidate_skills (candidate_id, skill) VALUES
(3, 'Java'),
(3, 'Spring Boot'),
(3, 'Microservices'),
(3, 'Kubernetes'),
(3, 'Kafka'),
(3, 'GCP');

-- Seed Jobs
INSERT INTO jobs (title, min_years_experience, location, salary_min, salary_max, remote_allowed)
VALUES ('Senior Backend Engineer', 4, 'New York', 130000, 160000, true);

INSERT INTO job_required_skills (job_id, skill, must_have) VALUES
(1, 'Java', true),
(1, 'Spring Boot', true),
(1, 'Docker', false),
(1, 'AWS', false);

INSERT INTO jobs (title, min_years_experience, location, salary_min, salary_max, remote_allowed)
VALUES ('Full Stack Java Developer', 3, 'Austin', 100000, 130000, false);

INSERT INTO job_required_skills (job_id, skill, must_have) VALUES
(2, 'Java', true),
(2, 'React', true),
(2, 'PostgreSQL', false);

INSERT INTO jobs (title, min_years_experience, location, salary_min, salary_max, remote_allowed)
VALUES ('Junior Java Specialist', 1, 'San Francisco', 80000, 105000, true);

INSERT INTO job_required_skills (job_id, skill, must_have) VALUES
(3, 'Java', true),
(3, 'Git', false);
